package com.prathik.fairshare.ui.friends

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.data.local.InvitedFriendDao
import com.prathik.fairshare.data.local.InvitedFriendEntity
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

// What kind of friend this is — drives the UI
sealed class FriendType {
    object Accepted : FriendType()
    object Pending : FriendType()
    data class Invited(val email: String) : FriendType()
    object Placeholder : FriendType()
}

@HiltViewModel
class FriendSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val friendRepository: FriendRepository,
    private val invitedFriendDao: InvitedFriendDao,
    private val getGroupsUseCase: GetGroupsUseCase,
    private val getAllBalancesUseCase: GetAllBalancesUseCase,
    private val getMyProfileUseCase: GetMyProfileUseCase,
) : ViewModel() {

    val friendId: String = checkNotNull(savedStateHandle["friendId"])

    private var myEmail: String = ""

    private val _friend = MutableStateFlow<Friend?>(null)
    val friend: StateFlow<Friend?> = _friend.asStateFlow()

    private val _friendType = MutableStateFlow<FriendType>(FriendType.Accepted)
    val friendType: StateFlow<FriendType> = _friendType.asStateFlow()

    private val _sharedGroups = MutableStateFlow<List<Group>>(emptyList())
    val sharedGroups: StateFlow<List<Group>> = _sharedGroups.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _actionState =
        MutableStateFlow<FriendSettingsActionState>(FriendSettingsActionState.Idle)
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

            val friendsDeferred = async { friendRepository.getFriends() }
            val sentDeferred = async { friendRepository.getSentRequests() }
            val balancesDeferred = async { getAllBalancesUseCase() }
            val groupsDeferred = async { getGroupsUseCase() }

            // 1. Accepted friend
            val accepted = (friendsDeferred.await() as? ApiResult.Success)
                ?.data?.find { it.id == friendId }

            if (accepted != null) {
                _friend.value = accepted
                _friendType.value = FriendType.Accepted
            } else {
                // 2. Sent request (pending existing user)
                val sent = (sentDeferred.await() as? ApiResult.Success)
                    ?.data?.find { it.receiverId == friendId }

                if (sent != null) {
                    _friend.value = Friend(
                        id = sent.receiverId,
                        fullName = sent.receiverName,
                        email = "",
                        profilePictureUrl = null,
                    )
                    _friendType.value = FriendType.Pending
                } else {
                    // 3. Local invited / placeholder
                    val local = invitedFriendDao.getAll()
                        .find { it.id.replace("-", "") == friendId.replace("-", "") }
                    if (local != null) {
                        _friend.value = Friend(
                            id = local.id,
                            fullName = local.displayName,
                            email = local.emailOrPhone,
                            profilePictureUrl = null,
                        )
                        _friendType.value = if (local.isPlaceholder)
                            FriendType.Placeholder
                        else
                            FriendType.Invited(local.emailOrPhone)
                    }
                }
            }

            // Shared groups — only for accepted friends
            val balancesResult = balancesDeferred.await()
            val groupsResult = groupsDeferred.await()
            if (balancesResult is ApiResult.Success && groupsResult is ApiResult.Success) {
                val friendGroupIds = balancesResult.data
                    .filter { it.otherUserId == friendId }
                    .mapNotNull { it.groupId }
                    .toSet()
                _sharedGroups.value = groupsResult.data.filter { it.id in friendGroupIds }
            }

            _isLoading.value = false
        }
    }

    fun sendInvite(onDone: () -> Unit) {
        val name = _friend.value?.fullName ?: return
        val email = _friend.value?.email ?: ""
        viewModelScope.launch {
            when (friendRepository.inviteFriend(email = email, name = name)) {
                is ApiResult.Success -> {
                    _actionState.value = FriendSettingsActionState.Success("Invite sent!")
                    onDone()
                }

                else -> _actionState.value =
                    FriendSettingsActionState.Error("Failed to send invite")
            }
        }
    }

    fun updateContactInfo(newEmail: String) {
        val local = _friend.value ?: return
        val trimmed = newEmail.trim()

        if (trimmed.isBlank()) {
            _actionState.value =
                FriendSettingsActionState.Error("Please enter an email or phone number")
            return
        }
        if (trimmed.equals(myEmail, ignoreCase = true)) {
            _actionState.value =
                FriendSettingsActionState.Error("You can't use your own email address")
            return
        }

        viewModelScope.launch {
            // Check if another invited friend already has this email
            val existing = invitedFriendDao.findByEmail(trimmed)
            if (existing != null && existing.id != local.id) {
                _actionState.value =
                    FriendSettingsActionState.Error("$trimmed has already been invited")
                return@launch
            }

            invitedFriendDao.insert(
                InvitedFriendEntity(
                    id = local.id,
                    displayName = local.fullName,
                    emailOrPhone = trimmed,
                    isPlaceholder = false,
                )
            )
            _friend.value = local.copy(email = trimmed)
            _friendType.value = FriendType.Invited(trimmed)
            _actionState.value = FriendSettingsActionState.Success("Contact updated")
        }
    }

    fun removeFriend(onRemoved: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val type = _friendType.value
            val result = when (type) {
                is FriendType.Accepted, FriendType.Pending -> friendRepository.removeFriend(friendId)
                is FriendType.Invited, FriendType.Placeholder -> {
                    invitedFriendDao.deleteById(friendId)
                    ApiResult.Success(Unit)
                }
            }
            when (result) {
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

    fun resetActionState() {
        _actionState.value = FriendSettingsActionState.Idle
    }
}

sealed class FriendSettingsActionState {
    object Idle : FriendSettingsActionState()
    data class Success(val message: String) : FriendSettingsActionState()
    data class Error(val message: String) : FriendSettingsActionState()
}