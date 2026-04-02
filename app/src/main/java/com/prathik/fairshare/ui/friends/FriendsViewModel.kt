package com.prathik.fairshare.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Balance
import com.prathik.fairshare.domain.model.Friend
import com.prathik.fairshare.domain.model.Friendship
import com.prathik.fairshare.domain.usecase.balance.GetAllBalancesUseCase
import com.prathik.fairshare.domain.usecase.friend.AcceptFriendRequestUseCase
import com.prathik.fairshare.domain.usecase.friend.DeclineFriendRequestUseCase
import com.prathik.fairshare.domain.usecase.friend.GetFriendRequestsUseCase
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
    private val getFriendsUseCase: GetFriendsUseCase,
    private val getFriendRequestsUseCase: GetFriendRequestsUseCase,
    private val getAllBalancesUseCase: GetAllBalancesUseCase,
    private val acceptRequestUseCase: AcceptFriendRequestUseCase,
    private val declineRequestUseCase: DeclineFriendRequestUseCase,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    // Pending received requests — drives the banner visibility
    private val _pendingRequests = MutableStateFlow<List<Friendship>>(emptyList())
    val pendingRequests: StateFlow<List<Friendship>> = _pendingRequests.asStateFlow()

    // Per-friend balance map: friendId → net amount
    private val _balanceMap = MutableStateFlow<Map<String, Double>>(emptyMap())
    val balanceMap: StateFlow<Map<String, Double>> = _balanceMap.asStateFlow()

    // Net totals for the hero card
    private val _owedToYou = MutableStateFlow(0.0)
    val owedToYou: StateFlow<Double> = _owedToYou.asStateFlow()

    private val _youOwe = MutableStateFlow(0.0)
    val youOwe: StateFlow<Double> = _youOwe.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _actionState = MutableStateFlow<FriendsActionState>(FriendsActionState.Idle)
    val actionState: StateFlow<FriendsActionState> = _actionState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            // Only show spinner on first load, not on refreshes
            if (_friends.value.isEmpty()) _isLoading.value = true

            val friendsDeferred = async { getFriendsUseCase() }
            val requestsDeferred = async { getFriendRequestsUseCase() }
            val balancesDeferred = async { getAllBalancesUseCase() }

            // Friends
            when (val result = friendsDeferred.await()) {
                is ApiResult.Success -> _friends.value = result.data
                else -> Unit
            }

            when (val result = requestsDeferred.await()) {
                is ApiResult.Success -> _pendingRequests.value = result.data
                else -> Unit
            }

            when (val result = balancesDeferred.await()) {
                is ApiResult.Success -> {
                    val balances = result.data
                    // Sum all balances per otherUserId across all groups
                    val map = balances.groupBy { it.otherUserId }
                        .mapValues { (_, list) -> list.sumOf { it.amount } }
                    _balanceMap.value = map

                    // Net totals
                    _owedToYou.value = balances.filter { it.amount > 0 }.sumOf { it.amount }
                    _youOwe.value = balances.filter { it.amount < 0 }.sumOf { -it.amount }
                }

                else -> Unit
            }

            _isLoading.value = false
        }
    }

    fun onSearchChanged(query: String) {
        _searchQuery.value = query
    }

    fun filteredFriends(): List<Friend> {
        val q = _searchQuery.value.trim().lowercase()
        return if (q.isBlank()) _friends.value
        else _friends.value.filter {
            it.fullName.lowercase().contains(q) || it.email.lowercase().contains(q)
        }
    }

    fun acceptRequest(friendshipId: String) {
        viewModelScope.launch {
            when (acceptRequestUseCase(friendshipId)) {
                is ApiResult.Success -> {
                    _pendingRequests.value = _pendingRequests.value.filter { it.id != friendshipId }
                    _actionState.value = FriendsActionState.Success("Friend request accepted")
                    loadData() // refresh friends list
                }

                else -> _actionState.value = FriendsActionState.Error("Failed to accept request")
            }
        }
    }

    fun declineRequest(friendshipId: String) {
        viewModelScope.launch {
            when (declineRequestUseCase(friendshipId)) {
                is ApiResult.Success -> {
                    _pendingRequests.value = _pendingRequests.value.filter { it.id != friendshipId }
                    _actionState.value = FriendsActionState.Success("Request declined")
                }

                else -> _actionState.value = FriendsActionState.Error("Failed to decline request")
            }
        }
    }

    fun resetActionState() {
        _actionState.value = FriendsActionState.Idle
    }
}

sealed class FriendsActionState {
    object Idle : FriendsActionState()
    data class Success(val message: String) : FriendsActionState()
    data class Error(val message: String) : FriendsActionState()
}