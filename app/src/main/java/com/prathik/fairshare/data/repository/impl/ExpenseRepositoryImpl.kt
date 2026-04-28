package com.prathik.fairshare.data.repository.impl

import com.prathik.fairshare.data.local.ExpenseDao
import com.prathik.fairshare.data.local.ExpensePayerDao
import com.prathik.fairshare.data.local.ExpensePayerEntity
import com.prathik.fairshare.data.local.ExpenseSplitDao
import com.prathik.fairshare.data.local.ExpenseSplitEntity
import com.prathik.fairshare.data.local.ExpenseEntity
import com.prathik.fairshare.data.model.mapper.toDomain
import com.prathik.fairshare.data.model.request.AddCommentRequest
import com.prathik.fairshare.data.model.request.CreateExpenseRequest
import com.prathik.fairshare.data.model.request.ItemAssignmentRequest
import com.prathik.fairshare.data.model.request.UpdateExpenseRequest
import com.prathik.fairshare.data.network.api.ExpenseApiService
import com.prathik.fairshare.data.network.mapSuccess
import com.prathik.fairshare.data.network.safeApiCall
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.model.ExpenseCategory
import com.prathik.fairshare.domain.model.ExpenseComment
import com.prathik.fairshare.domain.model.ExpenseItem
import com.prathik.fairshare.domain.model.SplitType
import com.prathik.fairshare.domain.repository.ExpenseMutationContext
import com.prathik.fairshare.domain.repository.ExpenseRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val expenseService: ExpenseApiService,
    private val expenseDao: ExpenseDao,
    private val expensePayerDao: ExpensePayerDao,
    private val expenseSplitDao: ExpenseSplitDao,
) : ExpenseRepository {

    override suspend fun getGroupExpenses(groupId: String): ApiResult<List<Expense>> {
        // Always fetch from network first so the UI never shows stale data
        // after creating/editing an expense. The background fire-and-forget
        // pattern caused new expenses to be invisible until the next cold load.
        val result = safeApiCall { expenseService.getGroupExpenses(groupId) }
        if (result is ApiResult.Success) {
            val entities = result.data.map { it.toEntity() }
            expenseDao.deleteByGroupIdExcludingPendingCreates(groupId)
            expenseDao.insertAll(entities)
            return ApiResult.Success(result.data.map { it.toDomain() })
        }
        // Network failed — three-tier offline fallback:
        // 1. Visible (non-deleted) rows → show them.
        val cached = expenseDao.getByGroupId(groupId)
        if (cached.isNotEmpty()) {
            return ApiResult.Success(cached.map { it.toDomain() })
        }
        // 2. No visible rows but soft-deleted rows exist → all were deleted offline.
        //    Return Success(emptyList()) so GroupDetail shows $0.00 Pending sync
        //    rather than "No internet connection".
        val anyRows = expenseDao.getByGroupIdIncludingDeleted(groupId)
        if (anyRows.isNotEmpty()) {
            android.util.Log.d("ExpenseCache",
                "getGroupExpenses offline: ${anyRows.size} soft-deleted rows — returning empty list")
            return ApiResult.Success(emptyList())
        }
        // 3. Truly nothing cached → surface the original network error.
        return result.mapSuccess { list -> list.map { it.toDomain() } }
    }

    override suspend fun getExpense(expenseId: String): ApiResult<Expense> {
        val result = safeApiCall { expenseService.getExpense(expenseId) }
        if (result is ApiResult.Success) {
            val response = result.data
            // Cache the basic entity for list-level operations.
            expenseDao.insert(response.toEntity())
            // Cache full payer and split rows so offline edit has complete data.
            expensePayerDao.deleteByExpenseId(expenseId)
            expensePayerDao.insertAll(
                (response.payers ?: emptyList()).map { it.toPayerEntity(expenseId) }
            )
            expenseSplitDao.deleteByExpenseId(expenseId)
            expenseSplitDao.insertAll(
                (response.splits ?: emptyList()).map { it.toSplitEntity(expenseId) }
            )
            return ApiResult.Success(response.toDomain())
        }
        // Network failed — reconstruct from cache.
        val cached = expenseDao.getById(expenseId) ?: return result.mapSuccess { it.toDomain() }
        val cachedPayers = expensePayerDao.getByExpenseId(expenseId)
        val cachedSplits = expenseSplitDao.getByExpenseId(expenseId)
        // Full offline edit is only safe when BOTH payer and split rows are cached.
        // If either is missing (e.g. old cache before Wave 2D-2B, or a partially
        // written cache), fall back to the lightweight toDomain() so EditExpenseViewModel
        // stays in metadata-only mode instead of enabling financial edits with
        // incomplete data.
        return if (cachedPayers.isNotEmpty() && cachedSplits.isNotEmpty()) {
            ApiResult.Success(cached.toDomain(cachedPayers, cachedSplits))
        } else {
            ApiResult.Success(cached.toDomain())
        }
    }

    override suspend fun createExpense(
        groupId: String?,
        description: String,
        totalAmount: Double,
        currency: String,
        splitType: SplitType?,
        category: ExpenseCategory?,
        notes: String?,
        expenseDate: String?,
        payerData: Map<String, Double>?,
        splitData: Map<String, Double>?,
        receiptId: String?,
        idempotencyKey: String?,
        remainderPointer: Int?,
        itemAssignments: Map<String, List<String>>?,
        repeatInterval: String?,
    ): ApiResult<Expense> {
        val result = safeApiCall {
            expenseService.createExpense(
                CreateExpenseRequest(
                    groupId     = groupId,
                    description = description,
                    totalAmount = totalAmount,
                    currency    = currency,
                    splitType   = splitType,
                    category    = category,
                    notes       = notes,
                    expenseDate = expenseDate,
                    payerData   = payerData,
                    splitData        = splitData,
                    receiptId        = receiptId,
                    idempotencyKey   = idempotencyKey,
                    remainderPointer = remainderPointer,
                    itemAssignments  = itemAssignments,
                    repeatInterval   = repeatInterval,
                )
            )
        }
        // Cache the server-confirmed expense so the real row exists in Room before
        // SyncWorker deletes the local placeholder, and full detail is available offline.
        if (result is ApiResult.Success) {
            val response = result.data
            val expenseId = response.id
            expenseDao.insert(response.toEntity())
            // Cache payer/split rows so ExpenseDetail renders offline without a
            // separate getExpense() fetch after create.
            if (!response.payers.isNullOrEmpty()) {
                expensePayerDao.deleteByExpenseId(expenseId)
                expensePayerDao.insertAll(response.payers!!.map { it.toPayerEntity(expenseId) })
            }
            if (!response.splits.isNullOrEmpty()) {
                expenseSplitDao.deleteByExpenseId(expenseId)
                expenseSplitDao.insertAll(response.splits!!.map { it.toSplitEntity(expenseId) })
            }
        }
        return result.mapSuccess { it.toDomain() }
    }

    override suspend fun updateExpense(
        expenseId: String,
        description: String?,
        totalAmount: Double?,
        currency: String?,
        splitType: SplitType?,
        category: ExpenseCategory?,
        notes: String?,
        expenseDate: String?,
        payerData: Map<String, Double>?,
        splitData: Map<String, Double>?,
        repeatInterval: String?,
        clearRepeat: Boolean?,
        nextRepeatDate: String?,
        idempotencyKey: String?,
    ): ApiResult<Expense> {
        val result = safeApiCall {
            expenseService.updateExpense(
                expenseId,
                UpdateExpenseRequest(
                    description    = description,
                    totalAmount    = totalAmount,
                    currency       = currency,
                    splitType      = splitType,
                    category       = category,
                    notes          = notes,
                    expenseDate    = expenseDate,
                    payerData      = payerData,
                    splitData      = splitData,
                    repeatInterval = repeatInterval,
                    clearRepeat    = clearRepeat,
                    nextRepeatDate = nextRepeatDate,
                    idempotencyKey = idempotencyKey,
                )
            )
        }
        if (result is ApiResult.Success) {
            val response = result.data
            val existingOtherUserId = expenseDao.getById(expenseId)?.otherUserId
            expenseDao.insert(response.toEntity(otherUserId = existingOtherUserId))

            val hasPayers = !response.payers.isNullOrEmpty()
            val hasSplits = !response.splits.isNullOrEmpty()
            val isFinancialUpdate = totalAmount != null || currency != null ||
                    splitType != null || payerData != null || splitData != null

            when {
                hasPayers && hasSplits -> {
                    // Replace payer/split cache with server-confirmed data.
                    expensePayerDao.deleteByExpenseId(expenseId)
                    expensePayerDao.insertAll(
                        response.payers!!.map { it.toPayerEntity(expenseId) }
                    )
                    expenseSplitDao.deleteByExpenseId(expenseId)
                    expenseSplitDao.insertAll(
                        response.splits!!.map { it.toSplitEntity(expenseId) }
                    )
                }
                isFinancialUpdate -> {
                    // Financial change but response has no payer/split rows —
                    // delete stale cache so offline shows "unavailable" rather
                    // than wrong old breakdown.
                    expensePayerDao.deleteByExpenseId(expenseId)
                    expenseSplitDao.deleteByExpenseId(expenseId)
                }
                // Metadata-only update: keep existing payer/split rows unchanged.
            }
        }
        return result.mapSuccess { it.toDomain() }
    }

    override suspend fun deleteExpense(expenseId: String, idempotencyKey: String?): ApiResult<Unit> {
        val result = safeApiCall { expenseService.deleteExpense(expenseId, idempotencyKey) }
        return when (result) {
            is ApiResult.Success -> {
                // Backend delete is a soft-delete. Mirror that locally: mark isDeleted = true
                // instead of removing the row. This keeps the cached expense accessible so
                // the Activity restore path (notification → ExpenseDetail offline → Restore)
                // works after going offline. Group/friend lists already filter isDeleted = 0.
                // Payer/split rows are preserved so the split breakdown stays available offline.
                expenseDao.updateLocalDeletedStatus(expenseId, true)
                ApiResult.Success(Unit)
            }
            is ApiResult.NetworkError    -> result
            is ApiResult.HttpError       -> result
            is ApiResult.ValidationError -> result
            is ApiResult.Unauthorized    -> result
            is ApiResult.NotFound        -> result
            is ApiResult.Forbidden       -> result
            is ApiResult.Conflict        -> result
        }
    }

    override suspend fun restoreExpense(expenseId: String, idempotencyKey: String?): ApiResult<Expense> {
        val result = safeApiCall { expenseService.restoreExpense(expenseId, idempotencyKey) }
        if (result is ApiResult.Success) {
            val response = result.data
            val existingOtherUserId = expenseDao.getById(expenseId)?.otherUserId
            expenseDao.insert(response.toEntity(otherUserId = existingOtherUserId))
            // Cache payer/split rows if the restore response includes them.
            if (!response.payers.isNullOrEmpty() && !response.splits.isNullOrEmpty()) {
                expensePayerDao.deleteByExpenseId(expenseId)
                expensePayerDao.insertAll(response.payers!!.map { it.toPayerEntity(expenseId) })
                expenseSplitDao.deleteByExpenseId(expenseId)
                expenseSplitDao.insertAll(response.splits!!.map { it.toSplitEntity(expenseId) })
            }
        }
        return result.mapSuccess { it.toDomain() }
    }

    override suspend fun searchExpenses(query: String): ApiResult<List<Expense>> =
        safeApiCall { expenseService.searchExpenses(query) }
            .mapSuccess { list -> list.map { it.toDomain() } }

    override suspend fun getRecurringExpenses(groupId: String): ApiResult<List<Expense>> =
        safeApiCall { expenseService.getRecurringExpenses(groupId) }
            .mapSuccess { list -> list.map { it.toDomain() } }

    override suspend fun getDirectRecurringExpenses(friendId: String): ApiResult<List<Expense>> =
        safeApiCall { expenseService.getDirectRecurringExpenses(friendId) }
            .mapSuccess { list -> list.map { it.toDomain() } }

    override suspend fun stopRecurring(expenseId: String): ApiResult<Unit> =
        safeApiCall { expenseService.stopRecurring(expenseId) }.mapSuccess { }

    override suspend fun getDirectExpensesWithFriend(friendId: String): ApiResult<List<Expense>> {
        val result = safeApiCall { expenseService.getDirectExpensesWithFriend(friendId) }
        if (result is ApiResult.Success) {
            // Cache with otherUserId so Friend Detail can fall back offline.
            val entities = result.data.map { it.toEntity(otherUserId = friendId) }
            // Delete stale direct-expense rows for this friend, then reinsert fresh.
            expenseDao.deleteByOtherUserIdExcludingPendingCreates(friendId)
            expenseDao.insertAll(entities)
            return result.mapSuccess { list -> list.map { it.toDomain() } }
        }
        val cached = expenseDao.getByOtherUserId(friendId)
        if (cached.isNotEmpty()) return ApiResult.Success(cached.map { it.toDomain() })
        return result.mapSuccess { list -> list.map { it.toDomain() } }
    }

    private fun com.prathik.fairshare.data.model.response.ExpenseResponse.toEntity(
        otherUserId: String? = null,
    ) = ExpenseEntity(
        id           = id,
        description  = description,
        totalAmount  = totalAmount,
        currency     = currency,
        groupId      = groupId,
        groupName    = groupName,
        addedById    = addedById,
        addedByName  = addedByName,
        splitType    = splitType,
        category     = category,
        notes        = notes,
        expenseDate  = expenseDate,
        isDeleted    = isDeleted,
        commentCount = commentCount,
        itemCount    = itemCount,
        yourPaid     = yourPaid,
        yourShare    = yourShare,
        yourBalance  = yourBalance,
        createdAt    = createdAt,
        updatedAt    = updatedAt,
        otherUserId  = otherUserId,
    )

    /**
     * Basic cache mapping — payers and splits are empty.
     * Used by list-level operations ([getGroupExpenses]) where payer detail is not needed.
     * For single-expense detail with full offline edit support, use [toDomain(payers, splits)].
     */
    private fun com.prathik.fairshare.data.local.ExpenseEntity.toDomain() = Expense(
        id           = id,
        description  = description,
        totalAmount  = totalAmount,
        currency     = currency,
        groupId      = groupId,
        groupName    = groupName,
        addedById    = addedById,
        addedByName  = addedByName,
        splitType    = com.prathik.fairshare.domain.model.SplitType.valueOf(splitType),
        category     = category?.let {
            try { com.prathik.fairshare.domain.model.ExpenseCategory.valueOf(it) }
            catch (e: IllegalArgumentException) { null }
        },
        notes        = notes,
        expenseDate  = expenseDate,
        isDeleted    = isDeleted,
        payers       = emptyList(),   // lightweight cache — payer details fetched on demand
        splits       = emptyList(),   // lightweight cache — split details fetched on demand
        commentCount = commentCount,
        itemCount    = itemCount,
        yourPaid     = yourPaid,
        yourShare    = yourShare,
        yourBalance  = yourBalance,
        createdAt    = createdAt,
        updatedAt    = updatedAt,
    )

    /**
     * Full cache mapping — reconstructs a complete [Expense] with payers and splits
     * from separately cached [ExpensePayerEntity] and [ExpenseSplitEntity] rows.
     *
     * Only called from [getExpense] offline fallback when BOTH [payers] and [splits]
     * are non-empty. The call site enforces this guard:
     *
     *   if (cachedPayers.isNotEmpty() && cachedSplits.isNotEmpty())
     *       cached.toDomain(cachedPayers, cachedSplits)
     *   else
     *       cached.toDomain()   // metadata-only fallback
     *
     * This ensures [EditExpenseViewModel] only enables full financial offline edit
     * when complete payer and split data is available. Partial data is never passed
     * to this overload.
     */
    private fun com.prathik.fairshare.data.local.ExpenseEntity.toDomain(
        payers: List<ExpensePayerEntity>,
        splits: List<ExpenseSplitEntity>,
    ) = Expense(
        id           = id,
        description  = description,
        totalAmount  = totalAmount,
        currency     = currency,
        groupId      = groupId,
        groupName    = groupName,
        addedById    = addedById,
        addedByName  = addedByName,
        splitType    = com.prathik.fairshare.domain.model.SplitType.valueOf(splitType),
        category     = category?.let {
            try { com.prathik.fairshare.domain.model.ExpenseCategory.valueOf(it) }
            catch (e: IllegalArgumentException) { null }
        },
        notes        = notes,
        expenseDate  = expenseDate,
        isDeleted    = isDeleted,
        payers       = payers.map { p ->
            com.prathik.fairshare.domain.model.Expense.PayerDetail(
                userId     = p.userId,
                fullName   = p.fullName,
                amountPaid = p.amountPaid,
            )
        },
        splits       = splits.map { s ->
            com.prathik.fairshare.domain.model.Expense.SplitDetail(
                userId     = s.userId,
                fullName   = s.fullName,
                amountOwed = s.amountOwed,
                percentage = s.percentage,
                shares     = s.shares,
                isSettled  = s.isSettled,
            )
        },
        commentCount = commentCount,
        itemCount    = itemCount,
        yourPaid     = yourPaid,
        yourShare    = yourShare,
        yourBalance  = yourBalance,
        createdAt    = createdAt,
        updatedAt    = updatedAt,
    )

    // ── Optimistic balance read (Wave 2D-Balance Optimism) ──────────────────────

    override suspend fun getCachedExpense(expenseId: String): com.prathik.fairshare.domain.model.Expense? =
        expenseDao.getById(expenseId)?.toDomain()

    override suspend fun getCachedDirectOtherUserId(expenseId: String): String? {
        val entity = expenseDao.getById(expenseId) ?: return null
        return if (entity.groupId.isNullOrEmpty()) entity.otherUserId else null
    }

    override suspend fun getCachedExpenseMutationContext(expenseId: String): ExpenseMutationContext? {
        val entity = expenseDao.getById(expenseId) ?: return null
        val payerIds = expensePayerDao.getByExpenseId(expenseId).map { it.userId }.toSet()
        val splitIds = expenseSplitDao.getByExpenseId(expenseId).map { it.userId }.toSet()
        // Fallback: if no cached payer/split rows, use otherUserId for direct expenses.
        val fallbackParticipants = if (payerIds.isEmpty() && splitIds.isEmpty()) {
            setOfNotNull(entity.otherUserId)
        } else emptySet()
        return ExpenseMutationContext(
            expenseId      = expenseId,
            groupId        = entity.groupId,
            otherUserId    = entity.otherUserId,
            participantIds = payerIds + splitIds + fallbackParticipants,
        )
    }

    // ── Local cache operations for offline optimistic UI (Wave 2D-Final) ───────

    override suspend fun insertLocalPendingExpense(
        localId     : String,
        groupId     : String?,
        description : String,
        totalAmount : Double,
        currency    : String,
        splitType   : com.prathik.fairshare.domain.model.SplitType,
        category    : com.prathik.fairshare.domain.model.ExpenseCategory?,
        addedById   : String,
        addedByName : String,
        expenseDate : String,
        yourPaid    : Double,
        yourShare   : Double,
        otherUserId : String?,
        payerData   : Map<String, Double>,
        splitData   : Map<String, Double>,
        memberNames : Map<String, String>,
    ) {
        val now = java.time.LocalDateTime.now().toString()
        expenseDao.insert(
            ExpenseEntity(
                id           = localId,
                description  = description,
                totalAmount  = totalAmount,
                currency     = currency,
                groupId      = groupId,
                groupName    = null,
                addedById    = addedById,
                addedByName  = addedByName,
                splitType    = splitType.name,
                category     = category?.name,
                notes        = null,
                expenseDate  = expenseDate,
                isDeleted    = false,
                commentCount = 0,
                itemCount    = 0,
                yourPaid     = yourPaid,
                yourShare    = yourShare,
                yourBalance  = yourPaid - yourShare,
                createdAt    = now,
                updatedAt    = now,
                otherUserId  = otherUserId,
            )
        )
        // Cache payer rows so ExpenseDetail renders 'Who Paid' offline without
        // requiring a network fetch. Uses the same payerData the request carries.
        if (payerData.isNotEmpty()) {
            expensePayerDao.deleteByExpenseId(localId)
            expensePayerDao.insertAll(payerData.map { (uid, amt) ->
                ExpensePayerEntity(
                    id         = "${localId}_${uid}",
                    expenseId  = localId,
                    userId     = uid,
                    fullName   = memberNames[uid] ?: uid,
                    amountPaid = amt,
                )
            })
        }
        // Cache split rows so ExpenseDetail renders 'Split Breakdown' offline.
        if (splitData.isNotEmpty()) {
            expenseSplitDao.deleteByExpenseId(localId)
            expenseSplitDao.insertAll(splitData.map { (uid, amt) ->
                ExpenseSplitEntity(
                    id         = "${localId}_${uid}",
                    expenseId  = localId,
                    userId     = uid,
                    fullName   = memberNames[uid] ?: uid,
                    amountOwed = amt,
                    percentage = null,
                    shares     = null,
                    isSettled  = false,
                )
            })
        }
    }

    override suspend fun deleteLocalExpense(localId: String) {
        expenseDao.deleteById(localId)
    }

    override suspend fun deleteLocalPendingPayersAndSplits(localId: String) {
        expensePayerDao.deleteByExpenseId(localId)
        expenseSplitDao.deleteByExpenseId(localId)
    }

    override suspend fun applyLocalPendingExpenseUpdate(
        expenseId    : String,
        description  : String?,
        totalAmount  : Double?,
        currency     : String?,
        splitType    : com.prathik.fairshare.domain.model.SplitType?,
        category     : com.prathik.fairshare.domain.model.ExpenseCategory?,
        notes        : String?,
        expenseDate  : String?,
        payerData    : Map<String, Double>?,
        splitData    : Map<String, Double>?,
        currentUserId: String,
        memberNames  : Map<String, String>,
    ): Pair<Double, Double>? {
        val existing = expenseDao.getById(expenseId) ?: return null
        val oldYourBalance = existing.yourBalance

        // Compute new financial values — only override fields present in the request.
        val newTotal    = totalAmount ?: existing.totalAmount
        val newCurrency = currency   ?: existing.currency
        val newSplitType = splitType?.name ?: existing.splitType
        val newCategory  = category?.name  ?: existing.category

        val newYourPaid  = payerData?.get(currentUserId) ?: run {
            // No payerData change — scale proportionally if amount changed.
            if (totalAmount != null && existing.totalAmount != 0.0)
                existing.yourPaid * (newTotal / existing.totalAmount)
            else existing.yourPaid
        }
        val newYourShare = splitData?.get(currentUserId) ?: run {
            if (totalAmount != null && existing.totalAmount != 0.0)
                existing.yourShare * (newTotal / existing.totalAmount)
            else existing.yourShare
        }
        val newYourBalance = newYourPaid - newYourShare

        val now = java.time.LocalDateTime.now().toString()
        expenseDao.insert(existing.copy(
            description  = description  ?: existing.description,
            totalAmount  = newTotal,
            currency     = newCurrency,
            splitType    = newSplitType,
            category     = newCategory,
            notes        = notes        ?: existing.notes,
            expenseDate  = expenseDate  ?: existing.expenseDate,
            yourPaid     = newYourPaid,
            yourShare    = newYourShare,
            yourBalance  = newYourBalance,
            updatedAt    = now,
        ))

        // Replace payer/split cache rows if new data provided.
        if (payerData != null) {
            expensePayerDao.deleteByExpenseId(expenseId)
            expensePayerDao.insertAll(payerData.map { (uid, amt) ->
                ExpensePayerEntity(
                    id         = "${expenseId}_${uid}",
                    expenseId  = expenseId,
                    userId     = uid,
                    fullName   = memberNames[uid] ?: uid,
                    amountPaid = amt,
                )
            })
        }
        if (splitData != null) {
            expenseSplitDao.deleteByExpenseId(expenseId)
            expenseSplitDao.insertAll(splitData.map { (uid, amt) ->
                ExpenseSplitEntity(
                    id         = "${expenseId}_${uid}",
                    expenseId  = expenseId,
                    userId     = uid,
                    fullName   = memberNames[uid] ?: uid,
                    amountOwed = amt,
                    percentage = null,
                    shares     = null,
                    isSettled  = false,
                )
            })
        }
        android.util.Log.d("ExpenseCache",
            "applyLocalPendingExpenseUpdate: $expenseId old=$oldYourBalance new=$newYourBalance")
        return Pair(oldYourBalance, newYourBalance)
    }

    override suspend fun propagateOtherUserId(fromId: String, toId: String) {
        val placeholder = expenseDao.getById(fromId)
        val otherUserId = placeholder?.otherUserId ?: return
        expenseDao.updateOtherUserId(toId, otherUserId)
    }

    override suspend fun updateLocalDeletedStatus(expenseId: String, isDeleted: Boolean) {
        expenseDao.updateLocalDeletedStatus(expenseId, isDeleted)
    }

    // ── Payer / split entity helpers ──────────────────────────────────────────

    private fun com.prathik.fairshare.data.model.response.ExpenseResponse.PayerDetail.toPayerEntity(
        expenseId: String,
    ) = ExpensePayerEntity(
        id         = "${expenseId}_${userId}",
        expenseId  = expenseId,
        userId     = userId,
        fullName   = fullName,
        amountPaid = amountPaid,
    )

    private fun com.prathik.fairshare.data.model.response.ExpenseResponse.SplitDetail.toSplitEntity(
        expenseId: String,
    ) = ExpenseSplitEntity(
        id         = "${expenseId}_${userId}",
        expenseId  = expenseId,
        userId     = userId,
        fullName   = fullName,
        amountOwed = amountOwed,
        percentage = percentage,
        shares     = shares,
        isSettled  = isSettled,
    )
}