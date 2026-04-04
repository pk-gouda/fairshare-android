package com.prathik.fairshare.ui.expense

import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.data.local.EncryptedTokenStore
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.ExpenseCategory
import com.prathik.fairshare.domain.model.Friend
import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.model.GroupMember
import com.prathik.fairshare.domain.model.Receipt
import com.prathik.fairshare.domain.model.SplitType
import com.prathik.fairshare.domain.usecase.expense.CreateExpenseUseCase
import com.prathik.fairshare.domain.usecase.friend.GetFriendsUseCase
import com.prathik.fairshare.domain.usecase.group.GetGroupMembersUseCase
import com.prathik.fairshare.domain.usecase.group.GetGroupsUseCase
import com.prathik.fairshare.domain.usecase.receipt.ScanReceiptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * ViewModel for AddExpenseScreen.
 *
 * Manages all form state: description, amount, currency, group,
 * split type, payers, splits, category, notes, date, receipt.
 * Also manages Transfer tab state.
 *
 * groupId is optional — passed from GroupDetail when adding
 * an expense within a specific group. Null when adding globally.
 *
 * Currency defaults to user's preferredCurrency from EncryptedTokenStore.
 * Current user is set as default payer when members load.
 * Category auto-detects from description keywords.
 */
@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val createExpenseUseCase: CreateExpenseUseCase,
    private val getGroupsUseCase: GetGroupsUseCase,
    private val getGroupMembersUseCase: GetGroupMembersUseCase,
    private val getFriendsUseCase: GetFriendsUseCase,
    private val scanReceiptUseCase: ScanReceiptUseCase,
    private val tokenStore: EncryptedTokenStore,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Optional groupId from nav argument — non-blank only
    val preselectedGroupId: String? = savedStateHandle.get<String>("groupId")
        ?.takeIf { it.isNotBlank() }

    // Optional friendId from nav argument — pre-selects friend in split
    val preselectedFriendId: String? = savedStateHandle.get<String>("friendId")
        ?.takeIf { it.isNotBlank() }

    // Current user ID — used to default payer to "You"
    val currentUserId: String? = tokenStore.getUserId()

    // ── Tab state ─────────────────────────────────────────────────────────────
    private val _activeTab = MutableStateFlow(ExpenseTab.EXPENSE)
    val activeTab: StateFlow<ExpenseTab> = _activeTab.asStateFlow()

    fun onTabChanged(tab: ExpenseTab) {
        _activeTab.value = tab
    }

    // ── Form state (shared between tabs) ──────────────────────────────────────
    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount.asStateFlow()

    // Currency defaults to user's preferred currency from token store
    private val _currency = MutableStateFlow(tokenStore.getPreferredCurrency())
    val currency: StateFlow<String> = _currency.asStateFlow()

    private val _selectedGroupId = MutableStateFlow(preselectedGroupId)
    val selectedGroupId: StateFlow<String?> = _selectedGroupId.asStateFlow()

    private val _splitType = MutableStateFlow(SplitType.EQUAL)
    val splitType: StateFlow<SplitType> = _splitType.asStateFlow()

    // Category — null means "Auto-detect" mode
    private val _category = MutableStateFlow<ExpenseCategory?>(null)
    val category: StateFlow<ExpenseCategory?> = _category.asStateFlow()

    // Whether category was manually set (overrides auto-detect)
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

    // ── Transfer tab state ────────────────────────────────────────────────────
    // fromUserId defaults to current user
    private val _transferFromId = MutableStateFlow(currentUserId)
    val transferFromId: StateFlow<String?> = _transferFromId.asStateFlow()

    private val _transferToId = MutableStateFlow<String?>(null)
    val transferToId: StateFlow<String?> = _transferToId.asStateFlow()

    fun onTransferFromChanged(userId: String) {
        _transferFromId.value = userId
    }

    fun onTransferToChanged(userId: String) {
        _transferToId.value = userId
    }

    // ── Groups + Members ──────────────────────────────────────────────────────
    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    private val _members = MutableStateFlow<List<GroupMember>>(emptyList())
    val members: StateFlow<List<GroupMember>> = _members.asStateFlow()

    // ── Receipt state ─────────────────────────────────────────────────────────
    private val _receiptState = MutableStateFlow<ReceiptScanState>(ReceiptScanState.Idle)
    val receiptState: StateFlow<ReceiptScanState> = _receiptState.asStateFlow()

    private var scannedReceiptId: String? = null

    // ── UI State ──────────────────────────────────────────────────────────────
    private val _uiState = MutableStateFlow<AddExpenseUiState>(AddExpenseUiState.Idle)
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    // Pre-selected friend when coming from FriendDetail (direct expense)
    private val _preselectedFriend = MutableStateFlow<Friend?>(null)
    val preselectedFriend: StateFlow<Friend?> = _preselectedFriend.asStateFlow()

    init {
        loadGroups()
        preselectedGroupId?.let { loadMembers(it) }
        preselectedFriendId?.let { loadFriendAsSplitParticipant(it) }
    }

    // Pre-select a friend as the split participant for direct expenses from FriendDetail
    private fun loadFriendAsSplitParticipant(friendId: String) {
        viewModelScope.launch {
            when (val result = getFriendsUseCase()) {
                is ApiResult.Success -> {
                    val friend = result.data.find { it.id == friendId } ?: return@launch
                    val currentId = currentUserId ?: return@launch
                    _preselectedFriend.value = friend

                    // Synthesize virtual GroupMember objects for you + friend
                    // so the existing SplitPreviewCard and PayerSheet work unchanged
                    val now = java.time.LocalDateTime.now().toString()
                    val youAsMember = com.prathik.fairshare.domain.model.GroupMember(
                        id                = currentId,
                        userId            = currentId,
                        fullName          = "You",
                        email             = "",
                        profilePictureUrl = null,
                        joinedAt          = now,
                    )
                    val friendAsMember = com.prathik.fairshare.domain.model.GroupMember(
                        id                = friendId,
                        userId            = friendId,
                        fullName          = friend.fullName,
                        email             = friend.email,
                        profilePictureUrl = friend.profilePictureUrl,
                        joinedAt          = now,
                    )
                    _members.value   = listOf(youAsMember, friendAsMember)
                    _payerData.value = mapOf(currentId to 0.0)
                    recalculateSplits()
                }
                else -> Unit
            }
        }
    }

    // ── Form field updates ────────────────────────────────────────────────────

    fun onDescriptionChanged(value: String) {
        _description.value = value
        // Auto-detect category from keywords — only if not manually set
        if (!categoryManuallySet) {
            _category.value = detectCategory(value)
        }
    }

    fun onNotesChanged(value: String) {
        _notes.value = value
    }

    fun onDateChanged(value: String) {
        _expenseDate.value = value
    }

    fun onCurrencyChanged(value: String) {
        _currency.value = value
    }

    fun onAmountChanged(value: String) {
        _amount.value = value
        recalculateSplits()
    }

    fun onCategoryChanged(value: ExpenseCategory?) {
        _category.value = value
        categoryManuallySet = value != null
    }

    fun onSplitTypeChanged(value: SplitType) {
        _splitType.value = value
        recalculateSplits()
    }

    fun onGroupSelected(groupId: String) {
        _selectedGroupId.value = groupId
        loadMembers(groupId)
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

    // ── Category auto-detection ───────────────────────────────────────────────

    /**
     * Detects expense category from description keywords.
     * Returns null if no keyword matches (shows "Auto-detect" in UI).
     */
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

            else -> null // no match — show "Auto-detect"
        }
    }

    private fun String.containsAny(vararg keywords: String) =
        keywords.any { this.contains(it) }

    // ── Data loading ──────────────────────────────────────────────────────────

    private fun loadGroups() {
        viewModelScope.launch {
            when (val result = getGroupsUseCase()) {
                is ApiResult.Success -> _groups.value = result.data
                else -> Unit
            }
        }
    }

    private fun loadMembers(groupId: String) {
        viewModelScope.launch {
            when (val result = getGroupMembersUseCase(groupId)) {
                is ApiResult.Success -> {
                    _members.value = result.data

                    // Default payer to current user if they're a member
                    val total = _amount.value.toDoubleOrNull() ?: 0.0
                    val currentUserMember = result.data.find { it.userId == currentUserId }
                    if (currentUserMember != null && _payerData.value.isEmpty()) {
                        _payerData.value = mapOf(currentUserMember.userId to total)
                    }

                    recalculateSplits()
                }

                else -> Unit
            }
        }
    }

    // ── Split calculation ─────────────────────────────────────────────────────

    /**
     * Recalculates split amounts when amount, splitType, or members change.
     * Only auto-calculates for EQUAL split — other types are set manually.
     */
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
                // Rounding fix: work in cents to avoid floating point loss
                val totalCents = Math.round(total * 100)
                val shareCents = totalCents / members.size
                val remainder = totalCents - (shareCents * members.size)
                _splitData.value = members.mapIndexed { index, member ->
                    val amount = if (index == 0) (shareCents + remainder) / 100.0
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

    // ── Receipt scan ──────────────────────────────────────────────────────────

    /**
     * Scans a receipt bitmap — compresses to JPEG, base64 encodes,
     * sends to backend Gemini AI, pre-fills form from result.
     */
    fun scanReceipt(bitmap: Bitmap) {
        viewModelScope.launch {
            _receiptState.value = ReceiptScanState.Scanning

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

            when (val result = scanReceiptUseCase(
                imageBase64 = base64,
                mimeType = "image/jpeg",
                preferredCurrency = _currency.value,
            )) {
                is ApiResult.Success -> {
                    val receipt = result.data
                    scannedReceiptId = receipt.id
                    receipt.merchantName?.let { _description.value = it }
                    receipt.totalAmount.let { _amount.value = it.toString() }
                    receipt.receiptDate?.let { _expenseDate.value = it }
                    receipt.currency?.let { _currency.value = it }
                    _receiptState.value = ReceiptScanState.Success(receipt)
                    recalculateSplits()
                }

                is ApiResult.NetworkError -> {
                    _receiptState.value = ReceiptScanState.Error("No internet connection.")
                }

                else -> {
                    _receiptState.value = ReceiptScanState.Error(
                        "Failed to scan receipt. Try again or enter manually."
                    )
                }
            }
        }
    }

    // ── Submit expense ────────────────────────────────────────────────────────

    fun submit() {
        if (_activeTab.value == ExpenseTab.TRANSFER) {
            submitTransfer()
        } else {
            submitExpense()
        }
    }

    private fun submitExpense() {
        val groupId = _selectedGroupId.value
        val description = _description.value.trim()
        val amount = _amount.value.toDoubleOrNull()

        // groupId is null for direct friend expenses — that's valid
        if (description.isBlank()) {
            _uiState.value = AddExpenseUiState.Error("Please enter a description."); return
        }
        if (amount == null || amount <= 0) {
            _uiState.value = AddExpenseUiState.Error("Please enter a valid amount."); return
        }
        if (_payerData.value.isEmpty()) {
            _uiState.value = AddExpenseUiState.Error("Please select who paid."); return
        }
        if (_splitData.value.isEmpty()) {
            _uiState.value = AddExpenseUiState.Error("Please set how to split."); return
        }

        // Use detected or manually set category, fall back to GENERAL
        val finalCategory = _category.value ?: ExpenseCategory.GENERAL

        viewModelScope.launch {
            _uiState.value = AddExpenseUiState.Loading
            when (val result = createExpenseUseCase(
                groupId = groupId,
                description = description,
                totalAmount = amount,
                currency = _currency.value,
                splitType = _splitType.value,
                category = finalCategory,
                notes = _notes.value.ifBlank { null },
                expenseDate = _expenseDate.value,
                payerData = _payerData.value,
                splitData = _splitData.value,
                receiptId = scannedReceiptId,
            )) {
                is ApiResult.Success -> _uiState.value = AddExpenseUiState.Success
                is ApiResult.NetworkError -> _uiState.value =
                    AddExpenseUiState.Error("No internet connection.")

                else -> _uiState.value =
                    AddExpenseUiState.Error("Failed to create expense. Please try again.")
            }
        }
    }

    private fun submitTransfer() {
        val groupId = _selectedGroupId.value
        val fromId = _transferFromId.value
        val toId = _transferToId.value
        val amount = _amount.value.toDoubleOrNull()

        // groupId is null for direct friend transfers — that's valid
        if (fromId == null) {
            _uiState.value = AddExpenseUiState.Error("Please select who is paying."); return
        }
        if (toId == null) {
            _uiState.value = AddExpenseUiState.Error("Please select who receives."); return
        }
        if (fromId == toId) {
            _uiState.value =
                AddExpenseUiState.Error("From and To cannot be the same person."); return
        }
        if (amount == null || amount <= 0) {
            _uiState.value = AddExpenseUiState.Error("Please enter a valid amount."); return
        }

        viewModelScope.launch {
            _uiState.value = AddExpenseUiState.Loading
            when (val result = createExpenseUseCase(
                groupId = groupId,
                description = "${members.value.find { it.userId == fromId }?.fullName ?: "Someone"} → ${members.value.find { it.userId == toId }?.fullName ?: "Someone"}",
                totalAmount = amount,
                currency = _currency.value,
                splitType = SplitType.UNEQUAL,
                category = ExpenseCategory.GENERAL,
                notes = _notes.value.ifBlank { null },
                expenseDate = _expenseDate.value,
                payerData = mapOf(fromId to amount),
                splitData = mapOf(toId to amount),
                receiptId = null,
            )) {
                is ApiResult.Success -> _uiState.value = AddExpenseUiState.Success
                is ApiResult.NetworkError -> _uiState.value =
                    AddExpenseUiState.Error("No internet connection.")

                else -> _uiState.value =
                    AddExpenseUiState.Error("Failed to save transfer. Please try again.")
            }
        }
    }

    fun resetUiState() {
        _uiState.value = AddExpenseUiState.Idle
    }
}

// ── Enums + UI States ─────────────────────────────────────────────────────────

enum class ExpenseTab { EXPENSE, TRANSFER }

sealed class AddExpenseUiState {
    object Idle : AddExpenseUiState()
    object Loading : AddExpenseUiState()
    object Success : AddExpenseUiState()
    data class Error(val message: String) : AddExpenseUiState()
}

sealed class ReceiptScanState {
    object Idle : ReceiptScanState()
    object Scanning : ReceiptScanState()
    data class Success(val receipt: Receipt) : ReceiptScanState()
    data class Error(val message: String) : ReceiptScanState()
}