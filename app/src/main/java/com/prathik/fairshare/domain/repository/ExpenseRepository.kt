package com.prathik.fairshare.domain.repository

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.model.ExpenseCategory
import com.prathik.fairshare.domain.model.SplitType

/**
 * Contract for all expense-related operations.
 * Implementation lives in data/repository/impl/ExpenseRepositoryImpl.kt
 */
interface ExpenseRepository {

    /**
     * Fetches all expenses for a group, ordered by date descending.
     * Returns empty list if the group has no expenses yet.
     */
    suspend fun getGroupExpenses(groupId: String): ApiResult<List<Expense>>

    /**
     * Fetches a single expense by ID.
     * Returns [ApiResult.NotFound] if expense doesn't exist.
     * Returns [ApiResult.Forbidden] if user is not a member of the group.
     */
    suspend fun getExpense(expenseId: String): ApiResult<Expense>

    /**
     * Creates a new expense in a group.
     * payerData — null means current user paid the full amount.
     * splitData — null means split equally among all group members.
     * splitType — null defaults to EQUAL on the backend.
     */
    suspend fun createExpense(
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
        idempotencyKey: String? = null,
        remainderPointer: Int? = null,
        itemAssignments: Map<String, List<String>>? = null,
        repeatInterval: String? = null,
    ): ApiResult<Expense>

    /**
     * Updates an existing expense.
     * Only non-null fields are updated.
     * Returns [ApiResult.Forbidden] if user is not the one who added the expense.
     */
    suspend fun updateExpense(
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
        repeatInterval: String? = null,
        clearRepeat: Boolean? = null,
        nextRepeatDate: String? = null,
        idempotencyKey: String? = null,
    ): ApiResult<Expense>

    /**
     * Soft deletes an expense — can be restored later.
     * idempotencyKey prevents balance reversal from happening twice on retry.
     */
    suspend fun deleteExpense(expenseId: String, idempotencyKey: String? = null): ApiResult<Unit>

    /**
     * Restores a previously soft-deleted expense.
     * idempotencyKey prevents balance re-apply from happening twice on retry.
     */
    suspend fun restoreExpense(expenseId: String, idempotencyKey: String? = null): ApiResult<Expense>

    /**
     * Searches expenses by description keyword across all groups.
     */
    suspend fun searchExpenses(query: String): ApiResult<List<Expense>>

    /**
     * Fetches all recurring expenses for a group.
     */
    suspend fun getRecurringExpenses(groupId: String): ApiResult<List<Expense>>
    suspend fun getDirectRecurringExpenses(friendId: String): ApiResult<List<Expense>>

    /**
     * Stops a recurring expense from auto-generating future entries.
     */
    suspend fun stopRecurring(expenseId: String): ApiResult<Unit>

    /**
     * Fetches all direct (non-group) expenses shared between current user and a friend.
     */
    suspend fun getDirectExpensesWithFriend(friendId: String): ApiResult<List<Expense>>

    // ── Local cache helpers for offline optimistic UI (Wave 2D-Final) ─────────

    /**
     * Insert a placeholder expense row into Room immediately after an offline
     * create is queued. The [localId] matches [PendingOperationEntity.localResourceId]
     * so the pending-dot indicator in expense lists lights up automatically.
     * The row is deleted when SyncWorker confirms the create with the backend.
     */
    suspend fun insertLocalPendingExpense(
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
        otherUserId : String? = null,
        // Payer/split rows — passed at offline-create time so ExpenseDetail renders
        // Who Paid and Split Breakdown without requiring an online fetch first.
        payerData   : Map<String, Double> = emptyMap(),  // userId → amount paid
        splitData   : Map<String, Double> = emptyMap(),  // userId → amount owed
        memberNames : Map<String, String> = emptyMap(),  // userId → displayName
    )

    /**
     * Delete a locally-inserted placeholder expense by its [localId].
     * Called from SyncWorker after the backend confirms the CREATE_EXPENSE.
     */
    suspend fun deleteLocalExpense(localId: String)

    /**
     * Delete cached payer/split rows for a local placeholder expense.
     * Called from SyncWorker after CREATE_EXPENSE succeeds so orphan rows
     * from the placeholder don't persist alongside the server expense rows.
     */
    suspend fun deleteLocalPendingPayersAndSplits(localId: String)

    /**
     * Apply an offline UPDATE_EXPENSE optimistically to local Room cache.
     * Preserves all server-only fields. Returns (oldYourBalance, newYourBalance)
     * for the pending op impact snapshot, or null if not cached.
     */
    suspend fun applyLocalPendingExpenseUpdate(
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
    ): Pair<Double, Double>?  // (oldYourBalance, newYourBalance) or null

    /**
     * Update the [isDeleted] flag on a cached expense immediately when an
     * offline delete or restore is queued. Gives instant list-level feedback
     * without waiting for SyncWorker to reach the backend.
     */
    suspend fun updateLocalDeletedStatus(expenseId: String, isDeleted: Boolean)

    /**
     * Read-only Room lookup — never hits the network.
     * Returns null if the expense is not cached locally.
     * Used for optimistic balance calculation from pending operations.
     */
    suspend fun getCachedExpense(expenseId: String): com.prathik.fairshare.domain.model.Expense?

    /**
     * Like [getCachedExpense] but also reads cached ExpensePayerEntity + ExpenseSplitEntity rows,
     * so the returned Expense has populated payers/splits for friend impact calculation.
     * Returns null if the expense entity is not cached.
     */
    suspend fun getCachedExpenseWithDetail(expenseId: String): com.prathik.fairshare.domain.model.Expense?

    /**
     * Capture pre-mutation context from Room for SyncWorker so delete/restore/update
     * can refresh the correct group/friend cache scopes after backend success.
     */
    suspend fun getCachedExpenseMutationContext(expenseId: String): ExpenseMutationContext?

    /** Returns the [otherUserId] of a cached direct expense, or null if not cached / not a direct expense. */
    suspend fun getCachedDirectOtherUserId(expenseId: String): String?

    /**
     * Copy the [otherUserId] from the local placeholder ([fromId]) to the
     * server-confirmed expense ([toId]).
     */
    suspend fun propagateOtherUserId(fromId: String, toId: String)
}

/** Lightweight snapshot of cached expense context for SyncWorker refresh. */
data class ExpenseMutationContext(
    val expenseId     : String,
    val groupId       : String?,
    val otherUserId   : String?,
    val participantIds: Set<String>,  // union of payer + split userIds
)