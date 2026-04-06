package com.prathik.fairshare.ui.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Friend
import com.prathik.fairshare.domain.model.GroupMember
import com.prathik.fairshare.domain.usecase.friend.GetFriendsUseCase
import com.prathik.fairshare.domain.usecase.group.AddMemberUseCase
import com.prathik.fairshare.domain.usecase.group.GetGroupMembersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for AddMemberScreen.
 *
 * Loads the user's friends list and the group's current members.
 * Friends already in the group are shown with a checkmark and can't be added again.
 * Supports search/filter by name or email.
 */
@HiltViewModel
class AddMemberViewModel @Inject constructor(
    private val getFriendsUseCase: GetFriendsUseCase,
    private val getGroupMembersUseCase: GetGroupMembersUseCase,
    private val addMemberUseCase: AddMemberUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val groupId: String = checkNotNull(savedStateHandle["groupId"])

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    private val _members = MutableStateFlow<List<GroupMember>>(emptyList())
    val members: StateFlow<List<GroupMember>> = _members.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _actionState = MutableStateFlow<AddMemberActionState>(AddMemberActionState.Idle)
    val actionState: StateFlow<AddMemberActionState> = _actionState.asStateFlow()

    // Track which friends are currently being added (show spinner per row)
    private val _addingIds = MutableStateFlow<Set<String>>(emptySet())
    val addingIds: StateFlow<Set<String>> = _addingIds.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true

            // Load friends and members in parallel
            val friendsResult = getFriendsUseCase()
            val membersResult = getGroupMembersUseCase(groupId)

            if (friendsResult is ApiResult.Success) {
                _friends.value = friendsResult.data
            }
            if (membersResult is ApiResult.Success) {
                _members.value = membersResult.data
            }

            _isLoading.value = false
        }
    }

    fun onSearchChanged(query: String) {
        _searchQuery.value = query
    }

    /**
     * Returns friends filtered by search query.
     * Each friend is paired with a boolean indicating if they're already in the group.
     */
    fun filteredFriends(): List<Pair<Friend, Boolean>> {
        val query = _searchQuery.value.lowercase()
        val memberUserIds = _members.value.map { it.userId }.toSet()

        return _friends.value
            .filter { friend ->
                if (query.isBlank()) true
                else friend.fullName.lowercase().contains(query)
                        || friend.email.lowercase().contains(query)
            }
            .map { friend ->
                friend to memberUserIds.contains(friend.id)
            }
            .sortedWith(compareBy(
                { it.second },  // non-members first
                { it.first.fullName.lowercase() }
            ))
    }

    fun addMember(friendId: String) {
        viewModelScope.launch {
            _addingIds.value = _addingIds.value + friendId
            when (val result = addMemberUseCase(groupId, friendId)) {
                is ApiResult.Success -> {
                    // Add to local members list so UI updates immediately
                    _members.value = _members.value + result.data
                    _actionState.value = AddMemberActionState.Success(
                        _friends.value.find { it.id == friendId }?.fullName ?: "Member"
                    )
                }
                is ApiResult.Conflict -> _actionState.value =
                    AddMemberActionState.Error(result.message)
                is ApiResult.NetworkError -> _actionState.value =
                    AddMemberActionState.Error("No internet connection.")
                else -> _actionState.value =
                    AddMemberActionState.Error(
                        (result as? ApiResult.HttpError)?.message
                            ?: (result as? ApiResult.ValidationError)?.message
                            ?: "Failed to add member."
                    )
            }
            _addingIds.value = _addingIds.value - friendId
        }
    }

    fun resetActionState() {
        _actionState.value = AddMemberActionState.Idle
    }
}

sealed class AddMemberActionState {
    object Idle : AddMemberActionState()
    data class Success(val name: String) : AddMemberActionState()
    data class Error(val message: String) : AddMemberActionState()
}