package com.prathik.fairshare.ui.expense

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.data.local.EncryptedTokenStore
import com.prathik.fairshare.data.model.request.UpdateExpenseRequest
import com.prathik.fairshare.data.sync.OperationType
import com.prathik.fairshare.data.sync.ExpenseMutationCacheRefresher
import com.prathik.fairshare.data.sync.PendingOperationRepository
import com.prathik.fairshare.data.sync.SyncWorker
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.model.ExpenseCategory
import com.prathik.fairshare.domain.model.GroupMember
import com.prathik.fairshare.domain.model.SplitType
import com.prathik.fairshare.domain.usecase.expense.GetExpenseUseCase
import com.prathik.fairshare.domain.usecase.expense.UpdateExpenseUseCase
import com.prathik.fairshare.domain.usecase.group.GetGroupMembersUseCase
import com.prathik.fairshare.domain.usecase.group.GetGroupUseCase
import com.prathik.fairshare.data.local.PendingBalanceImpactEntity
import com.prathik.fairshare.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * ViewModel for EditExpenseScreen.
 *
 * UPDATE_EXPENSE uses try-first queue semantics.
 * - A stable idempotencyKey is generated BEFORE the network call.
 * - If online and backend succeeds, no PendingOperation is created.
 * - If online and backend returns HTTP/validation/server error, no PendingOperation.
 * - Only on NetworkError is the operation enqueued with the same idempotencyKey
 *   that was already sent to the backend, so SyncWorker replay is deduplicated.
 */
@HiltViewModel
class EditExpenseViewModel @Inject constructor(
    private val getExpenseUseCase: GetExpenseUseCase,
    private val updateExpenseUseCase: UpdateExpenseUseCase,
    private val getGroupMembersUseCase: GetGroupMembersUseCase,
    private val getGroupUseCase: GetGroupUseCase,
    private val tokenStore: EncryptedTokenStore,
    private val pendingOperationRepository : PendingOperationRepository,
    private val expenseRepository           : ExpenseRepository,
    private val mutationCacheRefresher     : ExpenseMutationCacheRefresher,
    private val json: Json,
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val expenseId: String = savedStateHandle.get<String>("expenseId") ?: ""
    val currentUserId: String? = tokenStore.getUserId()

    // ── Load state ────────────────────────────────────────────────────────────
    private val _loadState = MutableStateFlow<EditLoadState>(EditLoadState.Loading)
    val loadState: StateFlow<EditLoadState> = _loadState.asStateFlow()

    // ── Form state ────────────────────────────────────────────────────────────
    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _currency = MutableStateFlow(tokenStore.getPreferredCurrency())
    val currency: StateFlow<String> = _currency.asStateFlow()

    private val _groupId = MutableStateFlow<String?>(null)
    val groupId: StateFlow<String?> = _groupId.asStateFlow()

    private val _groupName = MutableStateFlow<String?>(null)
    val groupName: StateFlow<String?> = _groupName.asStateFlow()

    private val _lastRemainderIndex = MutableStateFlow(0)
    val lastRemainderIndex: StateFlow<Int> = _lastRemainderIndex.asStateFlow()

    private val _splitType = MutableStateFlow(SplitType.EQUAL)
    val splitType: StateFlow<SplitType> = _splitType.asStateFlow()

    private val _category = MutableStateFlow<ExpenseCategory?>(null)
    val category: StateFlow<ExpenseCategory?> = _category.asStateFlow()

    private var categoryManuallySet = false

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    private val _repeatInterval = MutableStateFlow<String?>(null)
    val repeatInterval: StateFlow<String?> = _repeatInterval.asStateFlow()

    private val _clearRepeat = MutableStateFlow(false)
    val clearRepeat: StateFlow<Boolean> = _clearRepeat.asStateFlow()

    private val _itemCount = MutableStateFlow(0)
    val itemCount: StateFlow<Int> = _itemCount.asStateFlow()

    private val _isTemplate = MutableStateFlow(false)
    val isTemplate: StateFlow<Boolean> = _isTemplate.asStateFlow()

    private val _expenseDate = MutableStateFlow(LocalDateTime.now().toString())
    val expenseDate: StateFlow<String> = _expenseDate.asStateFlow()

    private val _payerData = MutableStateFlow<Map<String, Double>>(emptyMap())
    val payerData: StateFlow<Map<String, Double>> = _payerData.asStateFlow()

    private val _splitData = MutableStateFlow<Map<String, Double>>(emptyMap())
    val splitData: StateFlow<Map<String, Double>> = _splitData.asStateFlow()

    // ── Members ───────────────────────────────────────────────────────────────
    private val _members = MutableStateFlow<List<GroupMember>>(emptyList())
    val members: StateFlow<List<GroupMember>> = _members.asStateFlow()

    private val _equalExcluded = MutableStateFlow<Set<String>>(emptySet())
    val equalExcluded: StateFlow<Set<String>> = _equalExcluded.asStateFlow()

    // ── Save state ────────────────────────────────────────────────────────────
    private val _saveState = MutableStateFlow<EditSaveState>(EditSaveState.Idle)
    val saveState: StateFlow<EditSaveState> = _saveState.asStateFlow()

    /**
     * True when the expense was loaded from local Room cache (offline fallback).
     * In this state, payers and splits are empty because the cache only stores
     * the lightweight summary. The save() function sends null for payerData/splitData
     * so the backend preserves the existing allocation unchanged.
     * The screen shows an info banner when this is true.
     */
    private val _isFromCache = MutableStateFlow(false)
    val isFromCache: StateFlow<Boolean> = _isFromCache.asStateFlow()

    /** Participant IDs from the expense as originally loaded — used as oldParticipantIds
     * on update so removed participants' caches are also refreshed. */
    private var originalParticipantIds: Set<String> = emptySet()

    init {
        loadExpense()
    }

    // ── Load existing expense ─────────────────────────────────────────────────

    private fun loadExpense() {
        viewModelScope.launch {
            // Step 1: Render from cache immediately — populates form before network.
            // Only done once at init; we do NOT re-apply on background refresh to avoid
            // overwriting changes the user has already started making.
            val cached = expenseRepository.getCachedExpenseWithDetail(expenseId)
                ?: expenseRepository.getCachedExpense(expenseId)
            if (cached != null) {
                _isFromCache.value = cached.payers.isEmpty() && cached.splits.isEmpty()
                originalParticipantIds = (cached.payers.map { it.userId } +
                        cached.splits.map { it.userId }).toSet()
                populateForm(cached)
                loadMembers(cached)
                _loadState.value = EditLoadState.Success
            } else {
                _loadState.value = EditLoadState.Loading
            }

            // Step 2: Network fetch — only populates form on cold load (no cache).
            // If any cached expense was rendered, the form is already visible and
            // must not be overwritten — the user may have started typing.
            when (val result = getExpenseUseCase(expenseId)) {
                is ApiResult.Success -> {
                    val expense = result.data
                    // Always update participant tracking for correct save/sync behavior.
                    originalParticipantIds = (expense.payers.map { it.userId } +
                            expense.splits.map { it.userId }).toSet()
                    if (cached == null) {
                        // Cold load — populate form from network (user has not seen the form yet).
                        _isFromCache.value = false
                        populateForm(expense)
                        loadMembers(expense)
                    } else {
                        // Cached form already rendered — do not overwrite user edits.
                        // Update _isFromCache only if the cached data had full payer/split detail.
                        if (cached.payers.isNotEmpty() && cached.splits.isNotEmpty()) {
                            _isFromCache.value = false
                        }
                        // Members may still be missing from basic cache — safe to load.
                        if (_members.value.isEmpty()) loadMembers(expense)
                    }
                    _loadState.value = EditLoadState.Success
                }
                is ApiResult.NetworkError -> {
                    if (_loadState.value !is EditLoadState.Success) {
                        _loadState.value = EditLoadState.Error("No internet connection.", isNetwork = true)
                    }
                }
                else -> {
                    if (_loadState.value !is EditLoadState.Success) {
                        _loadState.value = EditLoadState.Error("Failed to load expense.", isNetwork = false)
                    }
                }
            }
        }
    }

    private fun populateForm(expense: Expense) {
        _description.value = expense.description
        _amount.value = expense.totalAmount.toBigDecimal().stripTrailingZeros().toPlainString()
        _currency.value = expense.currency
        _groupId.value = expense.groupId
        _groupName.value = expense.groupName
        _splitType.value = expense.splitType
        _notes.value = expense.notes ?: ""
        _expenseDate.value = expense.expenseDate

        if (expense.category != null) {
            _category.value = expense.category
            categoryManuallySet = true
        }

        _repeatInterval.value = expense.repeatInterval
        _isTemplate.value = expense.isTemplate

        val rawPayerData = expense.payers.associate { it.userId to it.amountPaid }
        _payerData.value = if (rawPayerData.size == 1) {
            val payerAmount = rawPayerData.values.first()
            val total = expense.totalAmount
            if (Math.abs(payerAmount - total) > 0.001) mapOf(rawPayerData.keys.first() to total)
            else rawPayerData
        } else {
            rawPayerData
        }

        _splitData.value = expense.splits.associate { split ->
            split.userId to when (expense.splitType) {
                SplitType.SHARES -> split.shares?.toDouble() ?: split.amountOwed
                SplitType.PERCENTAGE -> split.percentage ?: split.amountOwed
                else -> split.amountOwed
            }
        }

        _itemCount.value = expense.itemCount

        // When loaded from cache, payers and splits are empty (not stored in Room).
        // Synthesize display values from the lightweight yourPaid/yourShare fields
        // so the form shows something useful. These are NOT sent to the backend on
        // save — save() sends null instead so the backend preserves existing allocation.
        if (expense.payers.isEmpty() && currentUserId != null) {
            val displayPaid = if (expense.yourPaid > 0) expense.yourPaid else expense.totalAmount
            _payerData.value = mapOf(currentUserId to displayPaid)
        }
        if (expense.splits.isEmpty() && currentUserId != null && expense.yourShare > 0) {
            _splitData.value = mapOf(currentUserId to expense.yourShare)
        }
    }

    private fun loadMembers(expense: Expense) {
        val gid = expense.groupId
        if (gid != null) {
            viewModelScope.launch {
                when (val result = getGroupMembersUseCase(gid)) {
                    is ApiResult.Success -> {
                        _members.value = result.data
                        val splitUserIds = expense.splits.map { it.userId }.toSet()
                        _equalExcluded.value = result.data
                            .map { it.userId }
                            .filter { it !in splitUserIds }
                            .toSet()
                    }

                    else -> _members.value = synthesizeMembers(expense)
                }
            }
        } else {
            _members.value = synthesizeMembers(expense)
        }
    }

    private fun synthesizeMembers(expense: Expense): List<GroupMember> {
        val now = LocalDateTime.now().toString()
        val userMap = mutableMapOf<String, GroupMember>()
        expense.payers.forEach { payer ->
            userMap[payer.userId] = GroupMember(
                id = payer.userId, userId = payer.userId,
                fullName = if (payer.userId == currentUserId) "You" else payer.fullName,
                email = "", profilePictureUrl = null, joinedAt = now,
            )
        }
        expense.splits.forEach { split ->
            if (!userMap.containsKey(split.userId)) {
                userMap[split.userId] = GroupMember(
                    id = split.userId, userId = split.userId,
                    fullName = if (split.userId == currentUserId) "You" else split.fullName,
                    email = "", profilePictureUrl = null, joinedAt = now,
                )
            }
        }
        return userMap.values.toList()
    }

    // ── Form field updates ────────────────────────────────────────────────────

    fun onDescriptionChanged(value: String) {
        _description.value = value
        if (!categoryManuallySet) _category.value = detectCategory(value)
    }

    fun onAmountChanged(value: String) {
        _amount.value = value; recalculateSplits()
    }

    fun onCurrencyChanged(value: String) {
        _currency.value = value
    }

    fun onSplitTypeChanged(value: SplitType) {
        val prev = _splitType.value
        _splitType.value = value
        if (prev != value) {
            if (prev == SplitType.EQUAL && value != SplitType.EQUAL) _splitData.value = emptyMap()
            else if (prev != SplitType.EQUAL && value != SplitType.EQUAL) _splitData.value =
                emptyMap()
        }
        recalculateSplits()
    }

    fun onCategoryChanged(value: ExpenseCategory?) {
        _category.value = value; categoryManuallySet = value != null
    }

    fun onNotesChanged(value: String) {
        _notes.value = value
    }

    fun onRepeatIntervalChanged(value: String?) {
        _repeatInterval.value = value
        if (value != null) _clearRepeat.value = false
    }

    fun onClearRepeat() {
        _clearRepeat.value = true; _repeatInterval.value = null
    }

    fun onDateChanged(value: String) {
        _expenseDate.value = value
    }

    fun onPayerChanged(userId: String, amount: Double) {
        _payerData.value = _payerData.value.toMutableMap().apply {
            if (amount > 0) put(userId, amount) else remove(userId)
        }
    }

    fun onSplitChanged(userId: String, amount: Double) {
        _splitData.value = _splitData.value.toMutableMap().apply {
            if (amount > 0) put(userId, amount) else remove(userId)
        }
    }

    fun onToggleEqualMember(userId: String) {
        val current = _equalExcluded.value.toMutableSet()
        val included = _members.value.count { !current.contains(it.userId) }
        if (current.contains(userId)) current.remove(userId)
        else {
            if (included <= 1) return; current.add(userId)
        }
        _equalExcluded.value = current
        recalculateSplits()
    }

    fun onSplitDataConfirmed(newSplitData: Map<String, Double>) {
        _splitData.value = newSplitData
    }

    fun setItemAssignments(assignments: Map<String, List<String>>, splitData: Map<String, Double>) {
        _splitData.value = splitData
    }

    // ── Split recalculation ───────────────────────────────────────────────────

    private fun recalculateSplits() {
        val total = _amount.value.toDoubleOrNull() ?: return
        val members = _members.value
        if (members.isEmpty()) return

        if (_payerData.value.size == 1) {
            _payerData.value = mapOf(_payerData.value.keys.first() to total)
        }

        when (_splitType.value) {
            SplitType.EQUAL -> {
                val included = members.filter { !_equalExcluded.value.contains(it.userId) }
                if (included.isEmpty()) return
                val totalCents = Math.round(total * 100)
                val shareCents = totalCents / included.size
                val remainderCents = totalCents - (shareCents * included.size)
                val pointer = _lastRemainderIndex.value
                val startIndex = if (remainderCents > 0) pointer % included.size else 0
                val rotated = included.drop(startIndex) + included.take(startIndex)
                val includedSplits = rotated.mapIndexed { i, m ->
                    m.userId to if (i < remainderCents) (shareCents + 1) / 100.0 else shareCents / 100.0
                }.toMap()
                _splitData.value =
                    members.associate { m -> m.userId to (includedSplits[m.userId] ?: 0.0) }
            }

            SplitType.UNEQUAL, SplitType.PERCENTAGE, SplitType.SHARES -> { /* manual */
            }
        }
    }

    // ── Category auto-detection ───────────────────────────────────────────────

    private fun detectCategory(description: String): ExpenseCategory? {
        val lower = description.lowercase()
        return when {
            lower.containsAny(
                "dinner", "lunch", "breakfast", "restaurant", "food",
                "cafe", "coffee", "eat", "pizza", "burger", "sushi"
            ) -> ExpenseCategory.DINING_OUT

            lower.containsAny(
                "grocery", "groceries", "walmart", "target",
                "supermarket", "vegetables", "fruit", "milk"
            ) -> ExpenseCategory.GROCERIES

            lower.containsAny(
                "uber",
                "lyft",
                "taxi",
                "cab",
                "ola",
                "rapido"
            ) -> ExpenseCategory.TAXI

            lower.containsAny(
                "bus",
                "train",
                "metro",
                "subway",
                "commute"
            ) -> ExpenseCategory.BUS_TRAIN

            lower.containsAny("rent", "apartment", "flat", "lease") -> ExpenseCategory.RENT
            lower.containsAny("electric", "electricity", "power") -> ExpenseCategory.ELECTRICITY
            lower.containsAny("water") -> ExpenseCategory.WATER
            lower.containsAny("gas", "fuel", "petrol", "shell", "pump") -> ExpenseCategory.GAS_FUEL
            lower.containsAny(
                "internet",
                "wifi",
                "phone",
                "broadband",
                "sim"
            ) -> ExpenseCategory.TV_PHONE_INTERNET

            lower.containsAny(
                "movie", "netflix", "hulu", "cinema", "theatre",
                "amazon prime", "disney"
            ) -> ExpenseCategory.MOVIES

            lower.containsAny("parking", "park") -> ExpenseCategory.PARKING
            lower.containsAny(
                "hotel",
                "airbnb",
                "hostel",
                "motel",
                "resort"
            ) -> ExpenseCategory.HOTEL

            lower.containsAny(
                "flight", "plane", "airline", "airport",
                "indigo", "air india"
            ) -> ExpenseCategory.PLANE

            lower.containsAny(
                "medical", "doctor", "hospital", "pharmacy",
                "medicine", "clinic"
            ) -> ExpenseCategory.MEDICAL

            lower.containsAny(
                "gym", "sport", "football", "cricket",
                "badminton", "fitness"
            ) -> ExpenseCategory.SPORTS

            lower.containsAny("gift", "birthday", "present") -> ExpenseCategory.GIFTS
            lower.containsAny(
                "beer", "wine", "alcohol", "bar", "pub",
                "whiskey", "vodka"
            ) -> ExpenseCategory.LIQUOR

            lower.containsAny("pet", "dog", "cat", "vet") -> ExpenseCategory.PETS
            lower.containsAny(
                "cloth",
                "shirt",
                "shoes",
                "fashion",
                "zara",
                "h&m"
            ) -> ExpenseCategory.CLOTHING

            else -> null
        }
    }

    private fun String.containsAny(vararg keywords: String) = keywords.any { this.contains(it) }

    // ── Submit update (Wave 2D-2 — queue-backed offline) ─────────────────────

    fun save() {
        val desc = _description.value.trim()
        val amt = _amount.value.toDoubleOrNull()

        if (desc.isBlank()) {
            _saveState.value = EditSaveState.Error("Please enter a description."); return
        }

        val userId = currentUserId ?: run {
            _saveState.value = EditSaveState.Error("Not logged in. Please sign in again.")
            return
        }

        // Financial field validation only applies when loaded online with full payer/split data.
        // In cache mode, amount/currency/payer/split are nulled before sending so the backend
        // preserves existing allocation. Validating them here would be incorrect.
        val fromCache = _isFromCache.value
        if (!fromCache) {
            if (amt == null || amt <= 0) {
                _saveState.value = EditSaveState.Error("Please enter a valid amount."); return
            }
            if (_payerData.value.isEmpty()) {
                _saveState.value = EditSaveState.Error("Please select who paid."); return
            }
            if (_splitData.value.isEmpty()) {
                _saveState.value = EditSaveState.Error("Please set how to split."); return
            }
        }

        val finalCategory = _category.value ?: ExpenseCategory.GENERAL
        val filteredSplitData = _splitData.value.filter { it.value > 0.0 }
        val repeatIntervalSend = if (_clearRepeat.value) null else _repeatInterval.value
        val clearRepeatSend = if (_clearRepeat.value) true else null

        viewModelScope.launch {
            _saveState.value = EditSaveState.Loading

            // Build the full request body for queue storage.
            // idempotencyKey is intentionally absent here — it comes from the
            // pending operation row so every retry reuses the same key.
            // Cache-mode: only metadata fields are sent. Financial fields (amount,
            // currency, splitType, payerData, splitData) are nulled so the backend
            // preserves the existing allocation. Sending a new totalAmount with null
            // payer/split rows would cause balance drift.
            val effectiveTotalAmount = if (fromCache) null else amt
            val effectiveCurrency = if (fromCache) null else _currency.value
            val effectiveSplitType = if (fromCache) null else _splitType.value
            val effectivePayerData = if (fromCache) null else _payerData.value
            val effectiveSplitData = if (fromCache) null else filteredSplitData

            val request = UpdateExpenseRequest(
                description = desc,
                totalAmount = effectiveTotalAmount,
                currency = effectiveCurrency,
                splitType = effectiveSplitType,
                category = finalCategory,
                notes = _notes.value.ifBlank { null },
                expenseDate = _expenseDate.value,
                payerData = effectivePayerData,
                splitData = effectiveSplitData,
                repeatInterval = repeatIntervalSend,
                clearRepeat = clearRepeatSend,
            )

            // Try-first: generate a stable idempotencyKey BEFORE the network call,
            // but do NOT enqueue yet. We only enqueue on NetworkError so that online
            // HTTP/server errors never create a pending operation or show a sync banner.
            val idempotencyKey = java.util.UUID.randomUUID().toString()

            val result = updateExpenseUseCase(
                expenseId = expenseId,
                description = desc,
                totalAmount = effectiveTotalAmount,
                currency = effectiveCurrency,
                splitType = effectiveSplitType,
                category = finalCategory,
                notes = _notes.value.ifBlank { null },
                expenseDate = _expenseDate.value,
                payerData = effectivePayerData,
                splitData = effectiveSplitData,
                repeatInterval = repeatIntervalSend,
                clearRepeat = clearRepeatSend,
                idempotencyKey = idempotencyKey,
            )

            when (result) {
                is ApiResult.Success -> {
                    // Online success — no pending operation was created.
                    // Await cache cascade with real old+new participants.
                    val splits = effectiveSplitData?.keys ?: emptySet()
                    val payers = _payerData.value.keys
                    mutationCacheRefresher.refreshAfterUpdateSuccess(
                        expense           = result.data,
                        groupId           = _groupId.value,
                        currentUserId     = userId,
                        oldParticipantIds = originalParticipantIds,
                        newParticipantIds = splits + payers,
                    )
                    _saveState.value = EditSaveState.Success
                }

                is ApiResult.NetworkError -> {
                    // True offline: enqueue NOW with the same idempotencyKey sent to backend.
                    val enqueued = pendingOperationRepository.enqueue(
                        userId          = userId,
                        operationType   = OperationType.UPDATE_EXPENSE,
                        endpoint        = "/api/expenses/$expenseId",
                        method          = "PUT",
                        requestBodyJson = json.encodeToString(request),
                        localResourceId = expenseId,
                        idempotencyKey  = idempotencyKey,
                    )
                    // 1. Apply Room update FIRST so ExpenseDetail shows new values
                    //    and the impact row exists before pending-ops observers fire.
                    // Capture old participants BEFORE edit overwrites Room — SyncWorker
                    // needs them to refresh removed participants after sync.
                    val oldContextBeforeEdit =
                        expenseRepository.getCachedExpenseMutationContext(expenseId)
                    if (!fromCache) {
                        val memberNames = _members.value.associate { it.userId to it.fullName }
                            .toMutableMap()
                            .also { it[userId] = tokenStore.getFullName() ?: "You" }
                        val impact = expenseRepository.applyLocalPendingExpenseUpdate(
                            expenseId     = expenseId,
                            description   = desc,
                            totalAmount   = effectiveTotalAmount,
                            currency      = effectiveCurrency,
                            splitType     = effectiveSplitType,
                            category      = finalCategory,
                            notes         = _notes.value.ifBlank { null },
                            expenseDate   = _expenseDate.value,
                            payerData     = effectivePayerData,
                            splitData     = effectiveSplitData,
                            currentUserId = userId,
                            memberNames   = memberNames,
                        )
                        // 2. Persist impact row BEFORE markRetryable so observers
                        //    never see a pending UPDATE op without a matching delta row.
                        if (impact != null) {
                            val (oldBal, newBal) = impact
                            // Resolve groupId/otherUserId from cached expense so direct
                            // friend expense updates also appear on FriendDetail/FriendsHome.
                            val cachedGroupId   = _groupId.value
                            val cachedOtherId   = if (cachedGroupId == null)
                                expenseRepository.getCachedDirectOtherUserId(expenseId)
                            else null
                            pendingOperationRepository.saveBalanceImpact(
                                PendingBalanceImpactEntity(
                                    operationId       = enqueued.operationId,
                                    expenseId         = expenseId,
                                    groupId           = cachedGroupId,
                                    otherUserId       = cachedOtherId,
                                    currency          = effectiveCurrency ?: _currency.value,
                                    oldYourBalance    = oldBal,
                                    newYourBalance    = newBal,
                                    // Stored so SyncWorker can refresh removed participants
                                    // after sync (Room is already overwritten by then).
                                    oldParticipantIds = originalParticipantIds.joinToString(","),
                                )
                            )
                        }
                    }
                    // 3. markRetryable AFTER impact is written — triggers observers safely.
                    pendingOperationRepository.markRetryable(
                        enqueued.operationId, result.exception.message ?: "Network error"
                    )
                    SyncWorker.triggerImmediateSync(appContext)
                    android.util.Log.w("EditExpenseVM", "Offline edit queued: ${enqueued.operationId}")
                    _saveState.value = EditSaveState.SavedOffline
                }

                is ApiResult.ValidationError -> {
                    _saveState.value = EditSaveState.Error(result.message)
                }

                is ApiResult.Forbidden -> {
                    _saveState.value = EditSaveState.Error(result.message)
                }

                is ApiResult.Unauthorized -> {
                    _saveState.value = EditSaveState.Error(result.message)
                }

                is ApiResult.NotFound -> {
                    _saveState.value =
                        EditSaveState.Error("Expense not found. It may have been deleted.")
                }

                is ApiResult.Conflict -> {
                    _saveState.value = EditSaveState.Error(result.message)
                }

                is ApiResult.HttpError -> {
                    _saveState.value = EditSaveState.Error("HTTP ${result.code}: ${result.message}")
                }
            }
        }
    }

    fun resetSaveState() {
        _saveState.value = EditSaveState.Idle
    }
}

// ── UI States ────────────────────────────────────────────────────────────────

sealed class EditLoadState {
    object Loading : EditLoadState()
    object Success : EditLoadState()
    data class Error(val message: String, val isNetwork: Boolean) : EditLoadState()
}

sealed class EditSaveState {
    object Idle : EditSaveState()
    object Loading : EditSaveState()
    object Success : EditSaveState()

    /** Wave 2D-2: queued offline — SyncWorker will send UPDATE_EXPENSE when network returns. */
    object SavedOffline : EditSaveState()
    data class Error(val message: String) : EditSaveState()
}