package com.prathik.fairshare.ui.friends

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Friend
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
) : ViewModel() {

    val friendId: String = checkNotNull(savedStateHandle["friendId"])

    private var myEmail: String = ""

    private val _friend       = MutableStateFlow<Friend?>(null)
    val friend: StateFlow<Friend?> = _friend.asStateFlow()

    private val _friendType   = MutableStateFlow<FriendType>(FriendType.Accepted)
    val friendType: StateFlow<FriendType> = _friendType.asStateFlow()

    private val _sharedGroups = MutableStateFlow<List<Group>>(emptyList())
    val sharedGroups: StateFlow<List<Group>> = _sharedGroups.asStateFlow()

    /** All groups the current user belongs to — for the "Add to existing group" picker. */
    private val _allGroups = MutableStateFlow<List<Group>>(emptyList())
    val allGroups: StateFlow<List<Group>> = _allGroups.asStateFlow()

    /** Net direct balance with this friend.
     *  Positive = they owe you, Negative = you owe them, null = no expenses yet. */
    private val _directBalance = MutableStateFlow<Double?>(null)
    val directBalance: StateFlow<Double?> = _directBalance.asStateFlow()

    private val _isLoading    = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

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

            val friendsDeferred   = async { friendRepository.getFriends() }
            val sentDeferred      = async { friendRepository.getSentRequests() }
            val breakdownDeferred = async { balanceRepository.getBreakdownWithUser(friendId) }
            // getNetBalanceWithUser queries UserBalance — catches ALL expenses including non-group.
            // This is the authoritative source for the removal guard.
            val netBalanceDeferred = async { balanceRepository.getNetBalanceWithUser(friendId) }
            val groupsDeferred    = async { getGroupsUseCase() }

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

            // Shared groups — from GroupBalance breakdown (per-group rows)
            val breakdownResult = breakdownDeferred.await()
            val groupsResult    = groupsDeferred.await()
            if (breakdownResult is ApiResult.Success && groupsResult is ApiResult.Success) {
                val sharedGroupIds = breakdownResult.data.mapNotNull { it.groupId }.toSet()
                _sharedGroups.value = groupsResult.data.filter { it.id in sharedGroupIds }
                _allGroups.value    = groupsResult.data   // for "Add to existing group" picker
            }

            // Net balance guard — from UserBalance (covers group + non-group expenses)
            val netResult = netBalanceDeferred.await()
            if (netResult is ApiResult.Success) {
                val net = netResult.data.sumOf { it.amount }
                _directBalance.value = if (net == 0.0 && netResult.data.isEmpty()) null else net
            }

            _isLoading.value = false
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
        // Backend soft-deletes all direct expenses and clears UserBalance records,
        // so no frontend balance guard is needed here.
        viewModelScope.launch {
            _isLoading.value = true
            when (friendRepository.removeFriend(friendId)) {
                is ApiResult.Success -> {
                    _actionState.value = FriendSettingsActionState.Success("Removed")
                    onRemoved()
                }
                else -> _actionState.value = FriendSettingsActionState.Error("Failed to remove")
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

    fun resetActionState() { _actionState.value = FriendSettingsActionState.Idle }
}

sealed class FriendSettingsActionState {
    object Idle : FriendSettingsActionState()
    data class Success(val message: String) : FriendSettingsActionState()
    data class Error(val message: String)   : FriendSettingsActionState()
}