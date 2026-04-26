package com.prathik.fairshare.ui.expense

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.data.local.EncryptedTokenStore
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.model.ExpenseCategory
import com.prathik.fairshare.domain.model.GroupMember
import com.prathik.fairshare.domain.model.SplitType
import com.prathik.fairshare.domain.usecase.expense.GetExpenseUseCase
import com.prathik.fairshare.domain.usecase.expense.UpdateExpenseUseCase
import com.prathik.fairshare.domain.usecase.group.GetGroupMembersUseCase
import com.prathik.fairshare.domain.usecase.group.GetGroupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * ViewModel for EditExpenseScreen.
 *
 * Loads an existing expense by ID and pre-populates all form fields.
 * On save, calls UpdateExpenseUseCase with the current form state.
 *
 * Wave 2D scope: update is online-only for now. Queue-backed offline
 * update/delete/restore will be wired in Wave 2D-2 once SyncWorker
 * actually replays those operation types.
 */
@HiltViewModel
class EditExpenseViewModel @Inject constructor(
    private val getExpenseUseCase: GetExpenseUseCase,
    private val updateExpenseUseCase: UpdateExpenseUseCase,
    private val getGroupMembersUseCase: GetGroupMembersUseCase,
    private val getGroupUseCase: GetGroupUseCase,
    private val tokenStore: EncryptedTokenStore,
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

    init {
        loadExpense()
    }

    // ── Load existing expense ─────────────────────────────────────────────────

    private fun loadExpense() {
        viewModelScope.launch {
            _loadState.value = EditLoadState.Loading
            when (val result = getExpenseUseCase(expenseId)) {
                is ApiResult.Success -> {
                    val expense = result.data
                    populateForm(expense)
                    loadMembers(expense)
                    _loadState.value = EditLoadState.Success
                }

                is ApiResult.NetworkError -> {
                    _loadState.value =
                        EditLoadState.Error("No internet connection.", isNetwork = true)
                }

                else -> {
                    _loadState.value =
                        EditLoadState.Error("Failed to load expense.", isNetwork = false)
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
            if (Math.abs(payerAmount - total) > 0.001) {
                mapOf(rawPayerData.keys.first() to total)
            } else {
                rawPayerData
            }
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

                    else -> {
                        _members.value = synthesizeMembers(expense)
                    }
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
                id = payer.userId,
                userId = payer.userId,
                fullName = if (payer.userId == currentUserId) "You" else payer.fullName,
                email = "",
                profilePictureUrl = null,
                joinedAt = now,
            )
        }
        expense.splits.forEach { split ->
            if (!userMap.containsKey(split.userId)) {
                userMap[split.userId] = GroupMember(
                    id = split.userId,
                    userId = split.userId,
                    fullName = if (split.userId == currentUserId) "You" else split.fullName,
                    email = "",
                    profilePictureUrl = null,
                    joinedAt = now,
                )
            }
        }

        return userMap.values.toList()
    }

    // ── Form field updates ────────────────────────────────────────────────────

    fun onDescriptionChanged(value: String) {
        _description.value = value
        if (!categoryManuallySet) {
            _category.value = detectCategory(value)
        }
    }

    fun onAmountChanged(value: String) {
        _amount.value = value
        recalculateSplits()
    }

    fun onCurrencyChanged(value: String) {
        _currency.value = value
    }

    fun onSplitTypeChanged(value: SplitType) {
        val previousType = _splitType.value
        _splitType.value = value

        if (previousType != value) {
            val wasEqual = previousType == SplitType.EQUAL
            val isNowNonEqual = value != SplitType.EQUAL
            if (wasEqual && isNowNonEqual) {
                _splitData.value = emptyMap()
            } else if (!wasEqual && isNowNonEqual && previousType != value) {
                _splitData.value = emptyMap()
            }
        }

        recalculateSplits()
    }

    fun onCategoryChanged(value: ExpenseCategory?) {
        _category.value = value
        categoryManuallySet = value != null
    }

    fun onNotesChanged(value: String) {
        _notes.value = value
    }

    fun onRepeatIntervalChanged(value: String?) {
        _repeatInterval.value = value
        if (value != null) _clearRepeat.value = false
    }

    fun onClearRepeat() {
        _clearRepeat.value = true
        _repeatInterval.value = null
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
        if (current.contains(userId)) {
            current.remove(userId)
        } else {
            if (included <= 1) return
            current.add(userId)
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
            val singlePayerId = _payerData.value.keys.first()
            _payerData.value = mapOf(singlePayerId to total)
        }

        when (_splitType.value) {
            SplitType.EQUAL -> {
                val included = members.filter { !_equalExcluded.value.contains(it.userId) }
                if (included.isEmpty()) return
                val totalCents = Math.round(total * 100)
                val shareCents = totalCents / included.size
                val remainderCents = totalCents - (shareCents * included.size)

                val pointer = _lastRemainderIndex.value
                val startIndex = if (remainderCents > 0 && included.isNotEmpty())
                    pointer % included.size else 0

                val rotated = included.drop(startIndex) + included.take(startIndex)

                val includedSplits = rotated.mapIndexed { index, member ->
                    val amount = if (index < remainderCents) (shareCents + 1) / 100.0
                    else shareCents / 100.0
                    member.userId to amount
                }.toMap()
                _splitData.value = members.associate { m ->
                    m.userId to (includedSplits[m.userId] ?: 0.0)
                }
            }

            SplitType.UNEQUAL,
            SplitType.PERCENTAGE,
            SplitType.SHARES -> { /* manual */
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
                "cloth", "shirt", "shoes", "fashion",
                "zara", "h&m"
            ) -> ExpenseCategory.CLOTHING

            else -> null
        }
    }

    private fun String.containsAny(vararg keywords: String) =
        keywords.any { this.contains(it) }

    // ── Submit update — online only (Wave 2D scope) ───────────────────────────

    fun save() {
        val desc = _description.value.trim()
        val amt = _amount.value.toDoubleOrNull()

        if (desc.isBlank()) {
            _saveState.value = EditSaveState.Error("Please enter a description."); return
        }
        if (amt == null || amt <= 0) {
            _saveState.value = EditSaveState.Error("Please enter a valid amount."); return
        }
        if (_payerData.value.isEmpty()) {
            _saveState.value = EditSaveState.Error("Please select who paid."); return
        }
        if (_splitData.value.isEmpty()) {
            _saveState.value = EditSaveState.Error("Please set how to split."); return
        }

        val finalCategory = _category.value ?: ExpenseCategory.GENERAL

        viewModelScope.launch {
            _saveState.value = EditSaveState.Loading
            val idempotencyKey = java.util.UUID.randomUUID().toString()
            val result = updateExpenseUseCase(
                expenseId = expenseId,
                description = desc,
                totalAmount = amt,
                currency = _currency.value,
                splitType = _splitType.value,
                category = finalCategory,
                notes = _notes.value.ifBlank { null },
                expenseDate = _expenseDate.value,
                payerData = _payerData.value,
                splitData = _splitData.value.filter { it.value > 0.0 },
                repeatInterval = if (_clearRepeat.value) null else _repeatInterval.value,
                clearRepeat = if (_clearRepeat.value) true else null,
                idempotencyKey = idempotencyKey,
            )
            when (result) {
                is ApiResult.Success -> _saveState.value = EditSaveState.Success
                is ApiResult.NetworkError -> {
                    android.util.Log.e(
                        "EditExpenseVM",
                        "NetworkError: ${result.exception}",
                        result.exception
                    )
                    _saveState.value =
                        EditSaveState.Error("No internet connection. Please try again.")
                }

                is ApiResult.ValidationError -> _saveState.value =
                    EditSaveState.Error(result.message)

                is ApiResult.Forbidden -> _saveState.value = EditSaveState.Error(result.message)
                is ApiResult.Unauthorized -> _saveState.value = EditSaveState.Error(result.message)
                is ApiResult.NotFound -> _saveState.value =
                    EditSaveState.Error("Expense not found. It may have been deleted.")

                is ApiResult.Conflict -> _saveState.value = EditSaveState.Error(result.message)
                is ApiResult.HttpError -> _saveState.value =
                    EditSaveState.Error("HTTP ${result.code}: ${result.message}")
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
    data class Error(val message: String) : EditSaveState()
}