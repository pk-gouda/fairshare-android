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
 * Members are loaded from:
 * - getGroupMembers if the expense has a groupId
 * - synthesized from payers + splits if it's a direct expense (no group)
 *
 * The form mirrors AddExpenseViewModel's state shape so the same UI
 * composables (bottom sheets, pills, etc.) work unchanged.
 */
@HiltViewModel
class EditExpenseViewModel @Inject constructor(
    private val getExpenseUseCase: GetExpenseUseCase,
    private val updateExpenseUseCase: UpdateExpenseUseCase,
    private val getGroupMembersUseCase: GetGroupMembersUseCase,
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

    private val _splitType = MutableStateFlow(SplitType.EQUAL)
    val splitType: StateFlow<SplitType> = _splitType.asStateFlow()

    private val _category = MutableStateFlow<ExpenseCategory?>(null)
    val category: StateFlow<ExpenseCategory?> = _category.asStateFlow()

    private var categoryManuallySet = false

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    private val _expenseDate = MutableStateFlow(LocalDateTime.now().toString())
    val expenseDate: StateFlow<String> = _expenseDate.asStateFlow()

    // payerId → amount they paid
    private val _payerData = MutableStateFlow<Map<String, Double>>(emptyMap())
    val payerData: StateFlow<Map<String, Double>> = _payerData.asStateFlow()

    // userId → their share
    private val _splitData = MutableStateFlow<Map<String, Double>>(emptyMap())
    val splitData: StateFlow<Map<String, Double>> = _splitData.asStateFlow()

    // ── Members ───────────────────────────────────────────────────────────────
    private val _members = MutableStateFlow<List<GroupMember>>(emptyList())
    val members: StateFlow<List<GroupMember>> = _members.asStateFlow()

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
                    _loadState.value = EditLoadState.Error("No internet connection.", isNetwork = true)
                }
                else -> {
                    _loadState.value = EditLoadState.Error("Failed to load expense.", isNetwork = false)
                }
            }
        }
    }

    /**
     * Pre-populate all form fields from the fetched expense.
     */
    private fun populateForm(expense: Expense) {
        _description.value = expense.description
        _amount.value = expense.totalAmount.toBigDecimal().stripTrailingZeros().toPlainString()
        _currency.value = expense.currency
        _groupId.value = expense.groupId
        _groupName.value = expense.groupName
        _splitType.value = expense.splitType
        _notes.value = expense.notes ?: ""
        _expenseDate.value = expense.expenseDate

        // Category
        if (expense.category != null) {
            _category.value = expense.category
            categoryManuallySet = true
        }

        // Payer data — from expense.payers
        _payerData.value = expense.payers.associate { it.userId to it.amountPaid }

        // Split data — from expense.splits
        _splitData.value = expense.splits.associate { it.userId to it.amountOwed }
    }

    /**
     * Load members from the group if it's a group expense,
     * or synthesize from payer/split participants for direct expenses.
     */
    private fun loadMembers(expense: Expense) {
        val gid = expense.groupId
        if (gid != null) {
            viewModelScope.launch {
                when (val result = getGroupMembersUseCase(gid)) {
                    is ApiResult.Success -> _members.value = result.data
                    else -> {
                        // Fallback: synthesize from expense participants
                        _members.value = synthesizeMembers(expense)
                    }
                }
            }
        } else {
            // Direct expense — synthesize members from payers + splits
            _members.value = synthesizeMembers(expense)
        }
    }

    /**
     * Build GroupMember objects from expense payer/split data.
     * This ensures bottom sheets have the member list they need.
     */
    private fun synthesizeMembers(expense: Expense): List<GroupMember> {
        val now = LocalDateTime.now().toString()
        val userMap = mutableMapOf<String, GroupMember>()

        expense.payers.forEach { payer ->
            userMap[payer.userId] = GroupMember(
                id                = payer.userId,
                userId            = payer.userId,
                fullName          = if (payer.userId == currentUserId) "You" else payer.fullName,
                email             = "",
                profilePictureUrl = null,
                joinedAt          = now,
            )
        }
        expense.splits.forEach { split ->
            if (!userMap.containsKey(split.userId)) {
                userMap[split.userId] = GroupMember(
                    id                = split.userId,
                    userId            = split.userId,
                    fullName          = if (split.userId == currentUserId) "You" else split.fullName,
                    email             = "",
                    profilePictureUrl = null,
                    joinedAt          = now,
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
        _splitType.value = value
        recalculateSplits()
    }

    fun onCategoryChanged(value: ExpenseCategory?) {
        _category.value = value
        categoryManuallySet = value != null
    }

    fun onNotesChanged(value: String) {
        _notes.value = value
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

    fun onSplitDataConfirmed(newSplitData: Map<String, Double>) {
        _splitData.value = newSplitData
    }

    // ── Split recalculation ───────────────────────────────────────────────────

    private fun recalculateSplits() {
        val total = _amount.value.toDoubleOrNull() ?: return
        val members = _members.value
        if (members.isEmpty()) return

        // Update payer amount if only current user is payer
        val uid = currentUserId ?: return
        if (_payerData.value.size == 1 && _payerData.value.containsKey(uid)) {
            _payerData.value = mapOf(uid to total)
        }

        when (_splitType.value) {
            SplitType.EQUAL -> {
                val totalCents = Math.round(total * 100)
                val shareCents = totalCents / members.size
                val remainder = totalCents - (shareCents * members.size)

                // Same fair hash strategy as AddExpense — description + amount
                // determines who gets the remainder cent for this specific expense.
                val remainderIndex = if (remainder > 0)
                    Math.abs((_description.value + total.toString()).hashCode()) % members.size
                else -1

                _splitData.value = members.mapIndexed { index, member ->
                    val amount = if (index == remainderIndex) (shareCents + remainder) / 100.0
                    else shareCents / 100.0
                    member.userId to amount
                }.toMap()
            }

            SplitType.UNEQUAL,
            SplitType.PERCENTAGE,
            SplitType.SHARES -> {
                // Manual — user sets values in bottom sheet
            }
        }
    }

    // ── Category auto-detection (same as AddExpenseViewModel) ─────────────────

    private fun detectCategory(description: String): ExpenseCategory? {
        val lower = description.lowercase()
        return when {
            lower.containsAny("dinner", "lunch", "breakfast", "restaurant", "food",
                "cafe", "coffee", "eat", "pizza", "burger", "sushi") -> ExpenseCategory.DINING_OUT
            lower.containsAny("grocery", "groceries", "walmart", "target",
                "supermarket", "vegetables", "fruit", "milk") -> ExpenseCategory.GROCERIES
            lower.containsAny("uber", "lyft", "taxi", "cab", "ola", "rapido") -> ExpenseCategory.TAXI
            lower.containsAny("bus", "train", "metro", "subway", "commute") -> ExpenseCategory.BUS_TRAIN
            lower.containsAny("rent", "apartment", "flat", "lease") -> ExpenseCategory.RENT
            lower.containsAny("electric", "electricity", "power") -> ExpenseCategory.ELECTRICITY
            lower.containsAny("water") -> ExpenseCategory.WATER
            lower.containsAny("gas", "fuel", "petrol", "shell", "pump") -> ExpenseCategory.GAS_FUEL
            lower.containsAny("internet", "wifi", "phone", "broadband", "sim") -> ExpenseCategory.TV_PHONE_INTERNET
            lower.containsAny("movie", "netflix", "hulu", "cinema", "theatre",
                "amazon prime", "disney") -> ExpenseCategory.MOVIES
            lower.containsAny("parking", "park") -> ExpenseCategory.PARKING
            lower.containsAny("hotel", "airbnb", "hostel", "motel", "resort") -> ExpenseCategory.HOTEL
            lower.containsAny("flight", "plane", "airline", "airport",
                "indigo", "air india") -> ExpenseCategory.PLANE
            lower.containsAny("medical", "doctor", "hospital", "pharmacy",
                "medicine", "clinic") -> ExpenseCategory.MEDICAL
            lower.containsAny("gym", "sport", "football", "cricket",
                "badminton", "fitness") -> ExpenseCategory.SPORTS
            lower.containsAny("gift", "birthday", "present") -> ExpenseCategory.GIFTS
            lower.containsAny("beer", "wine", "alcohol", "bar", "pub",
                "whiskey", "vodka") -> ExpenseCategory.LIQUOR
            lower.containsAny("pet", "dog", "cat", "vet") -> ExpenseCategory.PETS
            lower.containsAny("cloth", "shirt", "shoes", "fashion",
                "zara", "h&m") -> ExpenseCategory.CLOTHING
            else -> null
        }
    }

    private fun String.containsAny(vararg keywords: String) =
        keywords.any { this.contains(it) }

    // ── Submit update ─────────────────────────────────────────────────────────

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
            val result = updateExpenseUseCase(
                expenseId   = expenseId,
                description = desc,
                totalAmount = amt,
                currency    = _currency.value,
                splitType   = _splitType.value,
                category    = finalCategory,
                notes       = _notes.value.ifBlank { null },
                expenseDate = _expenseDate.value,
                payerData   = _payerData.value,
                splitData   = _splitData.value,
            )
            when (result) {
                is ApiResult.Success -> _saveState.value = EditSaveState.Success
                is ApiResult.NetworkError -> {
                    android.util.Log.e("EditExpenseVM", "NetworkError: ${result.exception}", result.exception)
                    _saveState.value =
                        EditSaveState.Error("Network error: ${result.exception.message ?: result.message}")
                }
                is ApiResult.ValidationError -> _saveState.value =
                    EditSaveState.Error(result.message)
                is ApiResult.Forbidden -> _saveState.value =
                    EditSaveState.Error(result.message)
                is ApiResult.Unauthorized -> _saveState.value =
                    EditSaveState.Error(result.message)
                is ApiResult.NotFound -> _saveState.value =
                    EditSaveState.Error("Expense not found. It may have been deleted.")
                is ApiResult.Conflict -> _saveState.value =
                    EditSaveState.Error(result.message)
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