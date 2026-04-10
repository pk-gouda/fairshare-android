package com.prathik.fairshare.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Friend
import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.model.GroupType
import com.prathik.fairshare.domain.usecase.friend.GetFriendsUseCase
import com.prathik.fairshare.domain.usecase.group.AddMemberUseCase
import com.prathik.fairshare.domain.usecase.group.CreateGroupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val createGroupUseCase: CreateGroupUseCase,
    private val getFriendsUseCase: GetFriendsUseCase,
    private val addMemberUseCase: AddMemberUseCase,
) : ViewModel() {

    // ── Step ──────────────────────────────────────────────────────────────────
    private val _step = MutableStateFlow(1)
    val step: StateFlow<Int> = _step.asStateFlow()

    // ── Step 1: Group details ─────────────────────────────────────────────────
    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _selectedType = MutableStateFlow(GroupType.TRIP)
    val selectedType: StateFlow<GroupType> = _selectedType.asStateFlow()

    private val _showAllTypes = MutableStateFlow(false)
    val showAllTypes: StateFlow<Boolean> = _showAllTypes.asStateFlow()

    private val _nameError = MutableStateFlow<String?>(null)
    val nameError: StateFlow<String?> = _nameError.asStateFlow()

    // ── Step 2: Add members ───────────────────────────────────────────────────
    private val _createdGroup = MutableStateFlow<Group?>(null)
    val createdGroup: StateFlow<Group?> = _createdGroup.asStateFlow()

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    private val _friendSearchQuery = MutableStateFlow("")
    val friendSearchQuery: StateFlow<String> = _friendSearchQuery.asStateFlow()

    private val _selectedFriendIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedFriendIds: StateFlow<Set<String>> = _selectedFriendIds.asStateFlow()

    private val _friendsLoading = MutableStateFlow(false)
    val friendsLoading: StateFlow<Boolean> = _friendsLoading.asStateFlow()

    // ── Shared ────────────────────────────────────────────────────────────────
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _actionState = MutableStateFlow<CreateGroupActionState>(CreateGroupActionState.Idle)
    val actionState: StateFlow<CreateGroupActionState> = _actionState.asStateFlow()

    // ── Step 1 actions ────────────────────────────────────────────────────────

    fun onNameChanged(value: String) {
        if (value.length <= 25) {
            _name.value = value
            _nameError.value = null
        }
    }

    fun onDescriptionChanged(value: String) {
        if (value.length <= 100) _description.value = value
    }

    fun onTypeSelected(type: GroupType) { _selectedType.value = type }

    fun toggleShowAllTypes() { _showAllTypes.value = !_showAllTypes.value }

    /** Validates and creates the group, then moves to step 2. */
    fun proceed() {
        val name = _name.value.trim()
        if (name.isBlank()) { _nameError.value = "Group name is required"; return }
        if (name.length < 2) { _nameError.value = "Name must be at least 2 characters"; return }

        viewModelScope.launch {
            _isLoading.value = true
            when (val result = createGroupUseCase(
                name        = name,
                type        = _selectedType.value.name,
                description = _description.value.trim().ifBlank { null },
            )) {
                is ApiResult.Success -> {
                    _createdGroup.value = result.data
                    loadFriends()
                    _step.value = 2
                }
                is ApiResult.ValidationError -> _nameError.value = result.message
                else -> _actionState.value = CreateGroupActionState.Error("Failed to create group")
            }
            _isLoading.value = false
        }
    }

    // ── Step 2 actions ────────────────────────────────────────────────────────

    private fun loadFriends() {
        viewModelScope.launch {
            _friendsLoading.value = true
            when (val result = getFriendsUseCase()) {
                is ApiResult.Success -> _friends.value = result.data.filter { it.isActive }
                else -> { /* silent fail — list stays empty */ }
            }
            _friendsLoading.value = false
        }
    }

    fun onFriendSearchChanged(query: String) { _friendSearchQuery.value = query }

    fun toggleFriend(friendId: String) {
        val current = _selectedFriendIds.value.toMutableSet()
        if (current.contains(friendId)) current.remove(friendId) else current.add(friendId)
        _selectedFriendIds.value = current
    }

    fun filteredFriends(): List<Friend> {
        val query = _friendSearchQuery.value.trim()
        return if (query.isBlank()) _friends.value
        else _friends.value.filter {
            it.fullName.contains(query, ignoreCase = true) ||
                    it.email.contains(query, ignoreCase = true)
        }
    }

    /** Adds selected members then emits Success. */
    fun finishWithMembers() {
        val groupId = _createdGroup.value?.id ?: return
        val selected = _selectedFriendIds.value.toList()
        if (selected.isEmpty()) {
            _actionState.value = CreateGroupActionState.Success(groupId)
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            selected.forEach { friendId ->
                addMemberUseCase(groupId, friendId) // best-effort; ignore individual failures
            }
            _isLoading.value = false
            _actionState.value = CreateGroupActionState.Success(groupId)
        }
    }

    /** Skip adding members — navigate straight to group detail. */
    fun skipMembers() {
        val groupId = _createdGroup.value?.id ?: return
        _actionState.value = CreateGroupActionState.Success(groupId)
    }

    fun resetActionState() { _actionState.value = CreateGroupActionState.Idle }
}

sealed class CreateGroupActionState {
    object Idle : CreateGroupActionState()
    data class Success(val groupId: String) : CreateGroupActionState()
    data class Error(val message: String)   : CreateGroupActionState()
}