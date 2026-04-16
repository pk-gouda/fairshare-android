package com.prathik.fairshare.ui.expense

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
import java.time.LocalDateTime
import javax.inject.Inject

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

    val preselectedGroupId: String? = savedStateHandle.get<String>("groupId")
        ?.takeIf { it.isNotBlank() }

    val preselectedFriendId: String? = savedStateHandle.get<String>("friendId")
        ?.takeIf { it.isNotBlank() }

    val currentUserId: String? = tokenStore.getUserId()

    private val _activeTab = MutableStateFlow(ExpenseTab.EXPENSE)
    val activeTab: StateFlow<ExpenseTab> = _activeTab.asStateFlow()

    fun onTabChanged(tab: ExpenseTab) {
        _activeTab.value = tab
    }

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _currency = MutableStateFlow(tokenStore.getPreferredCurrency())
    val currency: StateFlow<String> = _currency.asStateFlow()

    private val _selectedGroupId = MutableStateFlow(preselectedGroupId)
    val selectedGroupId: StateFlow<String?> = _selectedGroupId.asStateFlow()

    private val _splitType = MutableStateFlow(SplitType.EQUAL)
    val splitType: StateFlow<SplitType> = _splitType.asStateFlow()

    private val _category = MutableStateFlow<ExpenseCategory?>(null)
    val category: StateFlow<ExpenseCategory?> = _category.asStateFlow()

    private var categoryManuallySet = false

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    private val _expenseDate = MutableStateFlow(LocalDateTime.now().toString())
    val expenseDate: StateFlow<String> = _expenseDate.asStateFlow()

    private val _payerData = MutableStateFlow<Map<String, Double>>(emptyMap())
    val payerData: StateFlow<Map<String, Double>> = _payerData.asStateFlow()

    private val _splitData = MutableStateFlow<Map<String, Double>>(emptyMap())
    val splitData: StateFlow<Map<String, Double>> = _splitData.asStateFlow()

    private val _equalExcluded = MutableStateFlow<Set<String>>(emptySet())
    val equalExcluded: StateFlow<Set<String>> = _equalExcluded.asStateFlow()

    fun onToggleEqualMember(userId: String) {
        val current = _equalExcluded.value.toMutableSet()
        if (current.contains(userId)) current.remove(userId) else current.add(userId)
        val includedCount = (_members.value.size - current.size)
        if (includedCount < 1) return
        _equalExcluded.value = current
        recalculateSplits()
    }

    private var _pointerAtCreation: Int? = null

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

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    private val _members = MutableStateFlow<List<GroupMember>>(emptyList())
    val members: StateFlow<List<GroupMember>> = _members.asStateFlow()

    private val _receiptState = MutableStateFlow<ReceiptScanState>(ReceiptScanState.Idle)
    val receiptState: StateFlow<ReceiptScanState> = _receiptState.asStateFlow()

    private var scannedReceiptId: String? = null
    val currentReceiptId: String? get() = scannedReceiptId

    // itemId → list of userIds assigned to that item (for backend item-linking)
    private val _itemAssignments = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val itemAssignments: StateFlow<Map<String, List<String>>> = _itemAssignments.asStateFlow()

    // Accurate per-person dollar amounts from item assignment screen.
    // Sent as splitData to bypass the backend's broken all-member auto-computation.
    private val _itemSplitData = MutableStateFlow<Map<String, Double>>(emptyMap())

    /**
     * Called from ItemAssignmentScreen onDone.
     * assignments → item-level user lists for backend item-linking
     * splitData   → accurate per-person dollar totals; sent as splitData to backend
     *               so only assigned members appear in the expense split.
     */
    fun setItemAssignments(
        assignments: Map<String, List<String>>,
        splitData: Map<String, Double>,
    ) {
        _itemAssignments.value = assignments
        _itemSplitData.value = splitData
    }

    private val _uiState = MutableStateFlow<AddExpenseUiState>(AddExpenseUiState.Idle)
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    private val _preselectedFriend = MutableStateFlow<Friend?>(null)
    val preselectedFriend: StateFlow<Friend?> = _preselectedFriend.asStateFlow()

    init {
        loadGroups()
        preselectedGroupId?.let { loadMembers(it) }
        preselectedFriendId?.let { loadFriendAsSplitParticipant(it) }
    }

    private fun loadFriendAsSplitParticipant(friendId: String) {
        viewModelScope.launch {
            when (val result = getFriendsUseCase()) {
                is ApiResult.Success -> {
                    val friend = result.data.find { it.id == friendId } ?: return@launch
                    val currentId = currentUserId ?: return@launch
                    _preselectedFriend.value = friend

                    val now = java.time.LocalDateTime.now().toString()
                    val youAsMember = com.prathik.fairshare.domain.model.GroupMember(
                        id = currentId,
                        userId = currentId,
                        fullName = "You",
                        email = "",
                        profilePictureUrl = null,
                        joinedAt = now,
                    )
                    val friendAsMember = com.prathik.fairshare.domain.model.GroupMember(
                        id = friendId,
                        userId = friendId,
                        fullName = friend.fullName,
                        email = friend.email,
                        profilePictureUrl = friend.profilePictureUrl,
                        joinedAt = now,
                    )
                    _members.value = listOf(youAsMember, friendAsMember)
                    _payerData.value = mapOf(currentId to 0.0)
                    recalculateSplits()
                }

                else -> Unit
            }
        }
    }

    fun onDescriptionChanged(value: String) {
        _description.value = value
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
        val previous = _splitType.value
        _splitType.value = value
        if (previous != value) {
            if (previous == SplitType.EQUAL && value != SplitType.EQUAL) {
                _splitData.value = emptyMap()
            } else if (previous != SplitType.EQUAL && value != SplitType.EQUAL) {
                _splitData.value = emptyMap()
            }
        }
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

    fun onSplitDataConfirmed(newSplitData: Map<String, Double>) {
        _splitData.value = newSplitData
    }

    private fun detectCategory(description: String): ExpenseCategory? {
        val lower = description.lowercase()
        return when {
            lower.containsAny(
                "dinner",
                "lunch",
                "breakfast",
                "restaurant",
                "food",
                "cafe",
                "coffee",
                "eat",
                "pizza",
                "burger",
                "sushi"
            ) -> ExpenseCategory.DINING_OUT

            lower.containsAny(
                "grocery",
                "groceries",
                "walmart",
                "target",
                "supermarket",
                "vegetables",
                "fruit",
                "milk"
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
                "movie",
                "netflix",
                "hulu",
                "cinema",
                "theatre",
                "amazon prime",
                "disney"
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
                "flight",
                "plane",
                "airline",
                "airport",
                "indigo",
                "air india"
            ) -> ExpenseCategory.PLANE

            lower.containsAny(
                "medical",
                "doctor",
                "hospital",
                "pharmacy",
                "medicine",
                "clinic"
            ) -> ExpenseCategory.MEDICAL

            lower.containsAny(
                "gym",
                "sport",
                "football",
                "cricket",
                "badminton",
                "fitness"
            ) -> ExpenseCategory.SPORTS

            lower.containsAny("gift", "birthday", "present") -> ExpenseCategory.GIFTS
            lower.containsAny(
                "beer",
                "wine",
                "alcohol",
                "bar",
                "pub",
                "whiskey",
                "vodka"
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

    private fun recalculateSplits() {
        val total = _amount.value.toDoubleOrNull() ?: return
        val members = _members.value
        if (members.isEmpty()) return

        val uid = currentUserId ?: return
        if (_payerData.value.size == 1 && _payerData.value.containsKey(uid)) {
            _payerData.value = mapOf(uid to total)
        }

        when (_splitType.value) {
            SplitType.EQUAL -> {
                val included = members.filter { !_equalExcluded.value.contains(it.userId) }
                if (included.isEmpty()) return

                val totalCents = Math.round(total * 100)
                val shareCents = totalCents / included.size
                val remainderCents = totalCents - (shareCents * included.size)

                val pointer = _groups.value
                    .find { it.id == _selectedGroupId.value }
                    ?.lastRemainderIndex ?: 0
                val startIndex = if (remainderCents > 0 && included.isNotEmpty())
                    pointer % included.size else 0
                _pointerAtCreation = pointer

                val rotated = included.drop(startIndex) + included.take(startIndex)

                _splitData.value = rotated.mapIndexed { index, member ->
                    val amount = if (index < remainderCents) (shareCents + 1) / 100.0
                    else shareCents / 100.0
                    member.userId to amount
                }.toMap()
            }

            SplitType.UNEQUAL, SplitType.PERCENTAGE, SplitType.SHARES -> { /* manual */
            }
        }
    }

    fun scanReceipt(imageBytes: ByteArray) {
        viewModelScope.launch {
            _receiptState.value = ReceiptScanState.Scanning
            try {
                val base64 = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    var bitmap = android.graphics.BitmapFactory.decodeByteArray(
                        imageBytes,
                        0,
                        imageBytes.size
                    )
                    if (bitmap == null) return@withContext null
                    if (bitmap.width > 1000) {
                        val ratio = 1000f / bitmap.width
                        val h = (bitmap.height * ratio).toInt()
                        bitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, 1000, h, true)
                    }
                    val outputStream = java.io.ByteArrayOutputStream()
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 75, outputStream)
                    android.util.Base64.encodeToString(
                        outputStream.toByteArray(),
                        android.util.Base64.NO_WRAP
                    )
                }
                if (base64 == null) {
                    _receiptState.value =
                        ReceiptScanState.Error("Could not decode image. Try scanning again.")
                    return@launch
                }
                when (val result = scanReceiptUseCase(
                    imageBase64 = base64,
                    mimeType = "image/jpeg",
                    preferredCurrency = _currency.value,
                )) {
                    is ApiResult.Success -> {
                        val receipt = result.data
                        scannedReceiptId = receipt.id
                        receipt.merchantName?.let { _description.value = it }
                        if (receipt.totalAmount > 0) {
                            _amount.value = receipt.totalAmount.toString()
                        }
                        // Always use today's date — the receipt date is when the
                        // merchant printed the receipt, not when the expense was split.
                        _expenseDate.value = java.time.LocalDateTime.now().toString()
                        receipt.currency?.let { _currency.value = it }
                        _receiptState.value = ReceiptScanState.Success(receipt)
                        recalculateSplits()
                    }

                    is ApiResult.NetworkError -> {
                        _receiptState.value =
                            ReceiptScanState.Error("No internet connection. Check your network and try again.")
                    }

                    else -> {
                        _receiptState.value =
                            ReceiptScanState.Error("Receipt scan failed. Try again or enter the amount manually.")
                    }
                }
            } catch (e: Exception) {
                _receiptState.value =
                    ReceiptScanState.Error("Something went wrong scanning the receipt. Try again.")
            }
        }
    }

    fun setScanError(message: String) {
        _receiptState.value = ReceiptScanState.Error(message)
    }

    fun submit() {
        if (_activeTab.value == ExpenseTab.TRANSFER) submitTransfer() else submitExpense()
    }

    private fun submitExpense() {
        val groupId = _selectedGroupId.value
        val description = _description.value.trim()
        val amount = _amount.value.toDoubleOrNull()

        if (description.isBlank()) {
            _uiState.value = AddExpenseUiState.Error("Please enter a description."); return
        }
        if (amount == null || amount <= 0) {
            _uiState.value = AddExpenseUiState.Error("Please enter a valid amount."); return
        }
        if (_payerData.value.isEmpty()) {
            _uiState.value = AddExpenseUiState.Error("Please select who paid."); return
        }

        // Determine the split data to send:
        // - If item assignments were done, use the accurate per-person amounts from the
        //   assignment screen. This prevents the backend from falling back to splitting
        //   equally among ALL group members (which would include unassigned members).
        // - Otherwise use the manual _splitData (equal/unequal/percent/shares).
        val effectiveSplitData: Map<String, Double>? = when {
            _itemSplitData.value.isNotEmpty() -> _itemSplitData.value
            _splitData.value.isNotEmpty() -> _splitData.value
            else -> null
        }

        if (effectiveSplitData == null && _itemAssignments.value.isEmpty()) {
            _uiState.value = AddExpenseUiState.Error("Please set how to split."); return
        }

        val finalCategory = _category.value ?: ExpenseCategory.GENERAL

        viewModelScope.launch {
            _uiState.value = AddExpenseUiState.Loading
            when (val result = createExpenseUseCase(
                groupId = groupId,
                description = description,
                totalAmount = amount,
                currency = _currency.value,
                // ✅ Force UNEQUAL when item assignments are used so the backend
                // uses the exact per-person amounts from effectiveSplitData instead of
                // recalculating equal splits from scratch (which ignores splitData values).
                splitType = if (_itemSplitData.value.isNotEmpty()) SplitType.UNEQUAL else _splitType.value,
                category = finalCategory,
                notes = _notes.value.ifBlank { null },
                expenseDate = _expenseDate.value,
                payerData = _payerData.value,
                splitData = effectiveSplitData,
                receiptId = scannedReceiptId,
                remainderPointer = _pointerAtCreation,
                itemAssignments = _itemAssignments.value.ifEmpty { null },
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