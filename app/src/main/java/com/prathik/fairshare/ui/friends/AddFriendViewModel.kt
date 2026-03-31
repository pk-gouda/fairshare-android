package com.prathik.fairshare.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.User
import com.prathik.fairshare.domain.usecase.friend.SendFriendRequestUseCase
import com.prathik.fairshare.domain.usecase.user.SearchUserByEmailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddFriendViewModel @Inject constructor(
    private val searchUserByEmailUseCase: SearchUserByEmailUseCase,
    private val sendFriendRequestUseCase: SendFriendRequestUseCase,
) : ViewModel() {

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _searchResult = MutableStateFlow<User?>(null)
    val searchResult: StateFlow<User?> = _searchResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _actionState = MutableStateFlow<AddFriendActionState>(AddFriendActionState.Idle)
    val actionState: StateFlow<AddFriendActionState> = _actionState.asStateFlow()

    fun onEmailChanged(value: String) {
        _email.value = value
        if (value.isBlank()) _searchResult.value = null
    }

    fun searchUser() {
        val query = _email.value.trim()
        if (query.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            _searchResult.value = null
            when (val result = searchUserByEmailUseCase(query)) {
                is ApiResult.Success -> _searchResult.value = result.data
                is ApiResult.NotFound -> _searchResult.value = null
                else -> _actionState.value = AddFriendActionState.Error("Search failed")
            }
            _isLoading.value = false
        }
    }

    fun sendRequest(userId: String) {
        viewModelScope.launch {
            when (sendFriendRequestUseCase(userId)) {
                is ApiResult.Success  -> _actionState.value = AddFriendActionState.Success("Friend request sent!")
                is ApiResult.Conflict -> _actionState.value = AddFriendActionState.Error("Request already sent or already friends")
                else                  -> _actionState.value = AddFriendActionState.Error("Failed to send request")
            }
        }
    }

    fun resetActionState() { _actionState.value = AddFriendActionState.Idle }
}

sealed class AddFriendActionState {
    object Idle : AddFriendActionState()
    data class Success(val message: String) : AddFriendActionState()
    data class Error(val message: String)   : AddFriendActionState()
}