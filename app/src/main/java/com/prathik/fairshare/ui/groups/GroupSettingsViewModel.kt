package com.prathik.fairshare.ui.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.data.local.EncryptedTokenStore
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Friend
import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.model.GroupMember
import com.prathik.fairshare.domain.repository.FriendRepository
import com.prathik.fairshare.domain.repository.GroupRepository
import com.prathik.fairshare.domain.repository.ImportRepository
import com.prathik.fairshare.domain.usecase.group.DeleteGroupUseCase
import com.prathik.fairshare.domain.usecase.group.GetGroupBalancesUseCase
import com.prathik.fairshare.domain.usecase.group.LeaveGroupUseCase
import com.prathik.fairshare.domain.usecase.group.GetGroupMembersUseCase
import com.prathik.fairshare.domain.usecase.group.GetGroupUseCase
import com.prathik.fairshare.domain.usecase.group.RemoveMemberUseCase
import com.prathik.fairshare.domain.usecase.group.UpdateGroupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupSettingsViewModel @Inject constructor(
    private val getGroupUseCase         : GetGroupUseCase,
    private val getMembersUseCase       : GetGroupMembersUseCase,
    private val getGroupBalancesUseCase : GetGroupBalancesUseCase,
    private val updateGroupUseCase      : UpdateGroupUseCase,
    private val removeMemberUseCase     : RemoveMemberUseCase,
    private val deleteGroupUseCase      : DeleteGroupUseCase,
    private val leaveGroupUseCase       : LeaveGroupUseCase,
    private val groupRepository         : GroupRepository,
    private val importRepository        : ImportRepository,
    private val friendRepository        : FriendRepository,
    private val tokenStore              : EncryptedTokenStore,
    savedStateHandle                    : SavedStateHandle,
) : ViewModel() {

    val groupId      : String  = checkNotNull(savedStateHandle["groupId"])
    val currentUserId: String? = tokenStore.getUserId()

    // ── UI state ──────────────────────────────────────────────────────────────
    private val _group = MutableStateFlow<Group?>(null)
    val group: StateFlow<Group?> = _group.asStateFlow()

    private val _members = MutableStateFlow<List<GroupMember>>(emptyList())
    val members: StateFlow<List<GroupMember>> = _members.asStateFlow()

    /** Per-currency balances in this group — used for display. Null = no expenses. */
    private val _yourGroupBalances = MutableStateFlow<List<com.prathik.fairshare.domain.model.Balance>>(emptyList())
    val yourGroupBalances: StateFlow<List<com.prathik.fairshare.domain.model.Balance>> = _yourGroupBalances.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _actionState = MutableStateFlow<GroupSettingsActionState>(GroupSettingsActionState.Idle)
    val actionState: StateFlow<GroupSettingsActionState> = _actionState.asStateFlow()

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    private val _claimState = MutableStateFlow<ClaimActionState>(ClaimActionState.Idle)
    val claimState: StateFlow<ClaimActionState> = _claimState.asStateFlow()

    // Editable fields
    private val _editName = MutableStateFlow("")
    val editName: StateFlow<String> = _editName.asStateFlow()

    private val _simplifyDebts = MutableStateFlow(false)
    val simplifyDebts: StateFlow<Boolean> = _simplifyDebts.asStateFlow()

    private val _defaultCurrency = MutableStateFlow("USD")
    val defaultCurrency: StateFlow<String> = _defaultCurrency.asStateFlow()

    private val _muteNotifications = MutableStateFlow(false)
    val muteNotifications: StateFlow<Boolean> = _muteNotifications.asStateFlow()

    init {
        loadData()
        loadFriends()
    }

    fun refreshFriends() {
        viewModelScope.launch {
            when (val r = friendRepository.getFriends()) {
                is ApiResult.Success -> _friends.value = r.data
                else -> Unit
            }
        }
    }

    private fun loadFriends() = refreshFriends()

    fun loadData() {
        viewModelScope.launch { loadDataInternal() }
    }

    private suspend fun loadDataInternal() {
        _isLoading.value = true
        val groupResult   = getGroupUseCase(groupId)
        val membersResult = getMembersUseCase(groupId)
        val balanceResult = getGroupBalancesUseCase(groupId)
        if (groupResult is ApiResult.Success) {
            _group.value = groupResult.data
            _editName.value = groupResult.data.name
            _simplifyDebts.value = groupResult.data.simplifyDebts
            _defaultCurrency.value = groupResult.data.defaultCurrency
        }
        if (membersResult is ApiResult.Success) {
            _members.value = membersResult.data
        }
        if (balanceResult is ApiResult.Success) {
            val data = balanceResult.data
            _yourGroupBalances.value = data
        }
        _isLoading.value = false
    }

    fun onNameChanged(value: String) { _editName.value = value }
    fun onSimplifyDebtsToggled()     { _simplifyDebts.value = !_simplifyDebts.value }

    fun saveDefaultCurrency(currency: String) {
        if (currency == _defaultCurrency.value) return
        _defaultCurrency.value = currency
        viewModelScope.launch {
            updateGroupUseCase(
                groupId         = groupId,
                name            = null,
                description     = null,
                simplifyDebts   = null,
                defaultCurrency = currency,
            )
        }
    }
    fun onMuteNotificationsToggled() { _muteNotifications.value = !_muteNotifications.value }

    fun saveGroupName() {
        val name = _editName.value.trim()
        if (name == _group.value?.name) return
        viewModelScope.launch {
            _actionState.value = GroupSettingsActionState.Loading
            when (val result = updateGroupUseCase(
                groupId       = groupId,
                name          = name,
                description   = null,
                simplifyDebts = null,
            )) {
                is ApiResult.Success -> {
                    _group.value = result.data
                    _actionState.value = GroupSettingsActionState.Success("Group name updated")
                }
                else -> _actionState.value = GroupSettingsActionState.Error("Failed to update name")
            }
        }
    }

    fun saveSimplifyDebts(value: Boolean) {
        // Optimistically update local state immediately for snappy UI
        _simplifyDebts.value = value
        viewModelScope.launch {
            val result = updateGroupUseCase(
                groupId       = groupId,
                name          = null,
                description   = null,
                simplifyDebts = value,
            )
            when (result) {
                is ApiResult.Success -> {
                    // Use the server's confirmed value directly — no extra network call
                    _simplifyDebts.value = result.data.simplifyDebts
                    _group.value = result.data
                    val message = if (value) "Simplify debts turned on" else "Simplify debts turned off"
                    _actionState.value = GroupSettingsActionState.Success(message)
                }
                else -> {
                    // Revert on failure
                    _simplifyDebts.value = !value
                    _actionState.value = GroupSettingsActionState.Error("Failed to update. Please try again.")
                }
            }
        }
    }

    fun removeMember(memberId: String) {
        viewModelScope.launch {
            _actionState.value = GroupSettingsActionState.Loading
            when (removeMemberUseCase(groupId, memberId)) {
                is ApiResult.Success -> {
                    _members.value = _members.value.filter { it.userId != memberId }
                    _actionState.value = GroupSettingsActionState.Success("Member removed")
                }
                is ApiResult.Conflict -> _actionState.value =
                    GroupSettingsActionState.Error("Member has unsettled balances")
                else -> _actionState.value =
                    GroupSettingsActionState.Error("Failed to remove member")
            }
        }
    }

    fun deleteGroup(confirmName: String) {
        viewModelScope.launch {
            _actionState.value = GroupSettingsActionState.Loading
            when (val result = deleteGroupUseCase(groupId, confirmName)) {
                is ApiResult.Success -> _actionState.value = GroupSettingsActionState.GroupDeleted
                is ApiResult.Conflict -> _actionState.value =
                    GroupSettingsActionState.Error(result.message ?: "Failed to delete group")
                is ApiResult.HttpError -> _actionState.value =
                    GroupSettingsActionState.Error(result.message ?: "Failed to delete group")
                is ApiResult.ValidationError -> _actionState.value =
                    GroupSettingsActionState.Error(result.message ?: "Invalid input")
                else -> _actionState.value =
                    GroupSettingsActionState.Error("Failed to delete group. Please try again.")
            }
        }
    }

    fun leaveGroup() {
        viewModelScope.launch {
            _actionState.value = GroupSettingsActionState.Loading
            when (val result = leaveGroupUseCase(groupId)) {
                is ApiResult.Success      -> _actionState.value = GroupSettingsActionState.GroupDeleted
                is ApiResult.Conflict     -> _actionState.value =
                    GroupSettingsActionState.Error("Settle all balances before leaving")
                is ApiResult.ValidationError -> _actionState.value =
                    GroupSettingsActionState.Error("Settle all balances before leaving")
                is ApiResult.HttpError    -> _actionState.value =
                    GroupSettingsActionState.Error("Server error (${result.code}): ${result.message}")
                is ApiResult.NetworkError -> _actionState.value =
                    GroupSettingsActionState.Error("Network error: ${result.message}")
                else -> _actionState.value =
                    GroupSettingsActionState.Error("Failed to leave group: $result")
            }
        }
    }

    fun archiveGroup() {
        viewModelScope.launch {
            _actionState.value = GroupSettingsActionState.Loading
            when (groupRepository.archiveGroup(groupId)) {
                is ApiResult.Success -> {
                    _group.value = _group.value?.copy(isArchived = true)
                    _actionState.value = GroupSettingsActionState.Success("Group archived")
                }
                else -> _actionState.value =
                    GroupSettingsActionState.Error("Failed to archive group")
            }
        }
    }

    fun unarchiveGroup() {
        viewModelScope.launch {
            _actionState.value = GroupSettingsActionState.Loading
            when (groupRepository.unarchiveGroup(groupId)) {
                is ApiResult.Success -> {
                    _group.value = _group.value?.copy(isArchived = false)
                    _actionState.value = GroupSettingsActionState.Success("Group restored")
                }
                else -> _actionState.value =
                    GroupSettingsActionState.Error("Failed to restore group")
            }
        }
    }

    fun claimMember(placeholderUserId: String) {
        viewModelScope.launch {
            _claimState.value = ClaimActionState.Loading
            when (importRepository.claimIdentity(groupId, placeholderUserId)) {
                is ApiResult.Success -> {
                    loadDataInternal()  // await refresh before emitting success
                    _claimState.value = ClaimActionState.Success("You've claimed this identity. Balances updated.")
                }
                is ApiResult.Conflict -> _claimState.value =
                    ClaimActionState.Error("This identity has already been claimed.")
                else -> _claimState.value = ClaimActionState.Error("Failed to claim identity.")
            }
        }
    }

    fun assignMember(placeholderUserId: String, friendUserId: String) {
        viewModelScope.launch {
            _claimState.value = ClaimActionState.Loading
            when (importRepository.assignPlaceholder(groupId, placeholderUserId, friendUserId)) {
                is ApiResult.Success -> {
                    loadDataInternal()  // await refresh before emitting success
                    _claimState.value = ClaimActionState.Success("Member linked successfully.")
                }
                is ApiResult.Conflict -> {
                    // Member already assigned — refresh silently and treat as success
                    loadDataInternal()
                    _claimState.value = ClaimActionState.Success("Member already linked.")
                }
                else -> _claimState.value = ClaimActionState.Error("Failed to assign member.")
            }
        }
    }

    fun resetClaimState() { _claimState.value = ClaimActionState.Idle }
    fun resetActionState() { _actionState.value = GroupSettingsActionState.Idle }
}

sealed class GroupSettingsActionState {
    object Idle         : GroupSettingsActionState()
    object Loading      : GroupSettingsActionState()
    object GroupDeleted : GroupSettingsActionState()
    data class Success(val message: String) : GroupSettingsActionState()
    data class Error(val message: String)   : GroupSettingsActionState()
}

sealed class ClaimActionState {
    object Idle    : ClaimActionState()
    object Loading : ClaimActionState()
    data class Success(val message: String) : ClaimActionState()
    data class Error(val message: String)   : ClaimActionState()
}