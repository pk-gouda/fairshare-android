package com.prathik.fairshare.ui.friends

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Friend
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.repository.FriendRepository
import com.prathik.fairshare.domain.usecase.group.GetGroupsUseCase
import com.prathik.fairshare.domain.usecase.balance.GetAllBalancesUseCase
import com.prathik.fairshare.domain.usecase.user.GetMyProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class FriendType {
    object Accepted    : FriendType()
    object Pending     : FriendType()
    data class Invited(val email: String) : FriendType()
    object Placeholder : FriendType()
}

@HiltViewModel
class FriendSettingsViewModel @Inject constructor(
    savedStateHandle            : SavedStateHandle,
    private val friendRepository       : FriendRepository,
    private val balanceRepository      : com.prathik.fairshare.domain.repository.BalanceRepository,
    private val getGroupsUseCase       : GetGroupsUseCase,
    private val getAllBalancesUseCase   : GetAllBalancesUseCase,
    private val getMyProfileUseCase    : GetMyProfileUseCase,
    private val expenseRepository      : com.prathik.fairshare.domain.repository.ExpenseRepository,
) : ViewModel() {

    val friendId: String = checkNotNull(savedStateHandle["friendId"])

    private var myEmail: String = ""

    private val _friend       = MutableStateFlow<Friend?>(null)
    val friend: StateFlow<Friend?> = _friend.asStateFlow()

    private val _friendType   = MutableStateFlow<FriendType>(FriendType.Accepted)
    val friendType: StateFlow<FriendType> = _friendType.asStateFlow()

    private val _sharedGroups = MutableStateFlow<List<Group>>(emptyList())
    val sharedGroups: StateFlow<List<Group>> = _sharedGroups.asStateFlow()

    /**
     * True if the shared-groups API call failed.
     * An empty [sharedGroups] list should NOT be treated as "confirmed no shared groups"
     * when this is true — the UI must block removal attempts and ask the user to retry.
     */
    private val _sharedGroupsLoadFailed = MutableStateFlow(false)
    val sharedGroupsLoadFailed: StateFlow<Boolean> = _sharedGroupsLoadFailed.asStateFlow()

    /** All groups the current user belongs to — for the "Add to existing group" picker. */
    private val _allGroups = MutableStateFlow<List<Group>>(emptyList())
    val allGroups: StateFlow<List<Group>> = _allGroups.asStateFlow()

    /** Net direct balance with this friend.
     *  Positive = they owe you, Negative = you owe them, null = no expenses yet. */
    private val _directBalance = MutableStateFlow<Double?>(null)
    val directBalance: StateFlow<Double?> = _directBalance.asStateFlow()

    /** Per-currency entries for display — never sum across currencies. */
    private val _balanceEntries = MutableStateFlow<List<Pair<Double, String>>>(emptyList())
    val balanceEntries: StateFlow<List<Pair<Double, String>>> = _balanceEntries.asStateFlow()

    private val _isLoading    = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _recurringExpenses = MutableStateFlow<List<Expense>>(emptyList())
    val recurringExpenses: StateFlow<List<Expense>> = _recurringExpenses.asStateFlow()

    private val _actionState  = MutableStateFlow<FriendSettingsActionState>(FriendSettingsActionState.Idle)
    val actionState: StateFlow<FriendSettingsActionState> = _actionState.asStateFlow()

    init {
        loadData()
        viewModelScope.launch {
            when (val result = getMyProfileUseCase()) {
                is ApiResult.Success -> myEmail = result.data.email
                else -> Unit
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true

            val friendsDeferred        = async { friendRepository.getFriends() }
            val sentDeferred           = async { friendRepository.getSentRequests() }
            // Shared groups via membership query — NOT GroupBalance rows.
            val sharedGroupsDeferred   = async { friendRepository.getSharedGroups(friendId) }
            // Net balance is still from UserBalance — covers group + non-group expenses.
            val netBalanceDeferred     = async { balanceRepository.getNetBalanceWithUser(friendId) }
            val groupsDeferred         = async { getGroupsUseCase() }

            val allFriends = (friendsDeferred.await() as? ApiResult.Success)?.data ?: emptyList()
            val found = allFriends.find { it.id == friendId }

            if (found != null) {
                _friend.value = found
                _friendType.value = when {
                    found.isPlaceholder -> FriendType.Placeholder
                    found.isInvited     -> FriendType.Invited(found.email)
                    else                -> FriendType.Accepted
                }
            } else {
                val sent = (sentDeferred.await() as? ApiResult.Success)
                    ?.data?.find { it.receiverId == friendId }
                if (sent != null) {
                    _friend.value = Friend(
                        id                = sent.receiverId,
                        fullName          = sent.receiverName,
                        email             = "",
                        profilePictureUrl = null,
                    )
                    _friendType.value = FriendType.Pending
                }
            }

            // Shared groups — membership-based via /api/friends/{friendId}/shared-groups.
            // Does NOT depend on GroupBalance rows, so it is correct even for groups
            // where both users are members but have no expenses or balance rows yet.
            val sharedGroupsResult = sharedGroupsDeferred.await()
            when (sharedGroupsResult) {
                is ApiResult.Success -> {
                    _sharedGroups.value = sharedGroupsResult.data
                    _sharedGroupsLoadFailed.value = false
                }
                else -> {
                    // Do not replace an existing list with empty on failure.
                    // _sharedGroupsLoadFailed = true tells the UI to block remove-friend
                    // so an empty list is never misread as "no shared groups."
                    _sharedGroupsLoadFailed.value = true
                }
            }

            // _allGroups powers "Add to existing group" picker — still from getGroupsUseCase
            val groupsResult = groupsDeferred.await()
            if (groupsResult is ApiResult.Success) {
                _allGroups.value = groupsResult.data
            }

            // Net balance guard — from UserBalance (covers group + non-group expenses)
            val netResult = netBalanceDeferred.await()
            if (netResult is ApiResult.Success) {
                val data = netResult.data
                val net = data.sumOf { it.amount }
                _directBalance.value = if (net == 0.0 && data.isEmpty()) null else net
                _balanceEntries.value = data.groupBy { it.currency }
                    .map { (cur, list) -> Pair(list.sumOf { it.amount }, cur) }
                    .filter { it.first != 0.0 }
            }

            _isLoading.value = false
        }
        // Load direct recurring expenses with this friend
        viewModelScope.launch {
            when (val result = expenseRepository.getDirectRecurringExpenses(friendId)) {
                is ApiResult.Success -> _recurringExpenses.value = result.data
                else -> Unit
            }
        }
    }

    fun sendInvite(onDone: () -> Unit) {
        val name  = _friend.value?.fullName ?: return
        val email = _friend.value?.email ?: ""
        viewModelScope.launch {
            when (friendRepository.inviteFriend(email = email, name = name)) {
                is ApiResult.Success -> {
                    _actionState.value = FriendSettingsActionState.Success("Invite sent!")
                    onDone()
                }
                else -> _actionState.value = FriendSettingsActionState.Error("Failed to send invite")
            }
        }
    }

    fun updateContactInfo(newEmail: String) {
        val local   = _friend.value ?: return
        val trimmed = newEmail.trim()

        if (trimmed.isBlank()) {
            _actionState.value = FriendSettingsActionState.Error("Please enter an email address")
            return
        }
        if (trimmed.equals(myEmail, ignoreCase = true)) {
            _actionState.value = FriendSettingsActionState.Error("You can't use your own email address")
            return
        }

        viewModelScope.launch {
            when (friendRepository.inviteFriend(email = trimmed, name = local.fullName)) {
                is ApiResult.Success -> {
                    _friend.value     = local.copy(email = trimmed)
                    _friendType.value = FriendType.Invited(trimmed)
                    _actionState.value = FriendSettingsActionState.Success("Invite sent!")
                }
                else -> _actionState.value = FriendSettingsActionState.Error("Failed to send invite")
            }
        }
    }

    fun removeFriend(onRemoved: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = friendRepository.removeFriend(friendId)) {
                is ApiResult.Success -> {
                    _actionState.value = FriendSettingsActionState.Success("Removed")
                    onRemoved()
                }
                // 409: backend blocked removal (shared groups or unsettled balance).
                // Surface the exact server message so the user knows what to fix.
                is ApiResult.Conflict -> _actionState.value = FriendSettingsActionState.Error(
                    result.message ?: "Cannot remove: shared groups or unsettled balances remain."
                )
                is ApiResult.NetworkError -> _actionState.value = FriendSettingsActionState.Error(
                    result.message ?: "Network error. Please try again."
                )
                else -> _actionState.value = FriendSettingsActionState.Error(
                    "Failed to remove friend. Please try again."
                )
            }
            _isLoading.value = false
        }
    }

    fun blockUser(onBlocked: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            when (friendRepository.blockUser(friendId)) {
                is ApiResult.Success -> {
                    _actionState.value = FriendSettingsActionState.Success("User blocked")
                    onBlocked()
                }
                else -> _actionState.value = FriendSettingsActionState.Error("Failed to block user")
            }
            _isLoading.value = false
        }
    }

    fun showError(message: String) {
        _actionState.value = FriendSettingsActionState.Error(message)
    }

    fun updateDirectSchedule(expenseId: String, newInterval: String) {
        viewModelScope.launch {
            when (expenseRepository.updateExpense(
                expenseId = expenseId, description = null, totalAmount = null,
                currency = null, splitType = null, category = null, notes = null,
                expenseDate = null, payerData = null, splitData = null,
                repeatInterval = newInterval, clearRepeat = null,
            )) {
                is ApiResult.Success -> {
                    _actionState.value = FriendSettingsActionState.Success("Schedule updated")
                    loadData()
                }
                else -> _actionState.value = FriendSettingsActionState.Error("Failed to update schedule")
            }
        }
    }

    fun stopDirectRecurring(expenseId: String) {
        viewModelScope.launch {
            when (expenseRepository.stopRecurring(expenseId)) {
                is ApiResult.Success -> {
                    _recurringExpenses.value = _recurringExpenses.value.filter { it.id != expenseId }
                    _actionState.value = FriendSettingsActionState.Success("Recurring stopped")
                }
                else -> _actionState.value = FriendSettingsActionState.Error("Failed to stop recurring")
            }
        }
    }

    fun resetActionState() { _actionState.value = FriendSettingsActionState.Idle }
}

sealed class FriendSettingsActionState {
    object Idle : FriendSettingsActionState()
    data class Success(val message: String) : FriendSettingsActionState()
    data class Error(val message: String)   : FriendSettingsActionState()
}