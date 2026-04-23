package com.prathik.fairshare.ui.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.repository.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupInviteViewModel @Inject constructor(
    private val groupRepository : GroupRepository,
    savedStateHandle            : SavedStateHandle,
) : ViewModel() {

    private val groupId: String = checkNotNull(savedStateHandle["groupId"])

    private val _inviteCode   = MutableStateFlow<String?>(null)
    val inviteCode: StateFlow<String?> = _inviteCode.asStateFlow()

    private val _groupName    = MutableStateFlow<String?>(null)
    val groupName: StateFlow<String?> = _groupName.asStateFlow()

    private val _isLoading    = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _actionState  = MutableStateFlow<GroupInviteActionState>(GroupInviteActionState.Idle)
    val actionState: StateFlow<GroupInviteActionState> = _actionState.asStateFlow()

    init { loadGroup() }

    private fun loadGroup() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = groupRepository.getGroup(groupId)) {
                is ApiResult.Success -> {
                    _inviteCode.value = result.data.inviteCode
                    _groupName.value  = result.data.name
                }
                else -> _actionState.value = GroupInviteActionState.Error("Failed to load invite code")
            }
            _isLoading.value = false
        }
    }

    fun regenerateCode() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = groupRepository.regenerateInviteCode(groupId)) {
                is ApiResult.Success -> {
                    _inviteCode.value = result.data
                    _actionState.value = GroupInviteActionState.Success("Invite code changed")
                }
                else -> _actionState.value = GroupInviteActionState.Error("Failed to change invite code")
            }
            _isLoading.value = false
        }
    }

    fun retryLoad() { loadGroup() }
    fun resetActionState() { _actionState.value = GroupInviteActionState.Idle }
}

sealed class GroupInviteActionState {
    object Idle                                : GroupInviteActionState()
    data class Success(val message: String)    : GroupInviteActionState()
    data class Error(val message: String)      : GroupInviteActionState()
}