package com.prathik.fairshare.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.data.local.InvitedFriendDao
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Friend
import com.prathik.fairshare.domain.model.InvitedFriend
import com.prathik.fairshare.domain.usecase.balance.GetAllBalancesUseCase
import com.prathik.fairshare.domain.usecase.friend.GetFriendsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val getFriendsUseCase   : GetFriendsUseCase,
    private val getAllBalancesUseCase: GetAllBalancesUseCase,
    private val invitedFriendDao    : InvitedFriendDao,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    private val _invitedFriends = MutableStateFlow<List<InvitedFriend>>(emptyList())
    val invitedFriends: StateFlow<List<InvitedFriend>> = _invitedFriends.asStateFlow()

    private val _balanceMap = MutableStateFlow<Map<String, Double>>(emptyMap())
    val balanceMap: StateFlow<Map<String, Double>> = _balanceMap.asStateFlow()

    private val _owedToYou = MutableStateFlow(0.0)
    val owedToYou: StateFlow<Double> = _owedToYou.asStateFlow()

    private val _youOwe = MutableStateFlow(0.0)
    val youOwe: StateFlow<Double> = _youOwe.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _actionState = MutableStateFlow<FriendsActionState>(FriendsActionState.Idle)
    val actionState: StateFlow<FriendsActionState> = _actionState.asStateFlow()

    init { loadData() }

    fun loadData() {
        viewModelScope.launch {
            if (_friends.value.isEmpty()) _isLoading.value = true

            val friendsDeferred  = async { getFriendsUseCase() }
            val balancesDeferred = async { getAllBalancesUseCase() }

            when (val result = friendsDeferred.await()) {
                is ApiResult.Success -> _friends.value = result.data
                else -> Unit
            }

            when (val result = balancesDeferred.await()) {
                is ApiResult.Success -> {
                    val balances = result.data
                    _balanceMap.value = balances.groupBy { it.otherUserId }
                        .mapValues { (_, list) -> list.sumOf { it.amount } }
                    _owedToYou.value = balances.filter { it.amount > 0 }.sumOf { it.amount }
                    _youOwe.value    = balances.filter { it.amount < 0 }.sumOf { -it.amount }
                }
                else -> Unit
            }

            _invitedFriends.value = invitedFriendDao.getAll().map {
                InvitedFriend(
                    id            = it.id,
                    displayName   = it.displayName,
                    emailOrPhone  = it.emailOrPhone,
                    isPlaceholder = it.isPlaceholder,
                    invitedAt     = it.invitedAt,
                )
            }

            _isLoading.value = false
        }
    }

    fun onSearchChanged(query: String) { _searchQuery.value = query }

    fun filteredFriends(): List<Friend> {
        val q = _searchQuery.value.trim().lowercase()
        return if (q.isBlank()) _friends.value
        else _friends.value.filter {
            it.fullName.lowercase().contains(q) || it.email.lowercase().contains(q)
        }
    }

    fun resetActionState() { _actionState.value = FriendsActionState.Idle }
}

sealed class FriendsActionState {
    object Idle : FriendsActionState()
    data class Success(val message: String) : FriendsActionState()
    data class Error(val message: String)   : FriendsActionState()
}