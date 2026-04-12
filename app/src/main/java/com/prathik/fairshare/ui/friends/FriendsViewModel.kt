package com.prathik.fairshare.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.AccountStatus
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Friend
import com.prathik.fairshare.domain.usecase.balance.GetAllBalancesUseCase
import com.prathik.fairshare.domain.usecase.friend.GetFriendsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val getFriendsUseCase   : GetFriendsUseCase,
    private val getAllBalancesUseCase: GetAllBalancesUseCase,
    private val friendEventBus      : FriendEventBus,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // All friends — active, invited, placeholder — all from backend
    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

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

    init {
        loadData()
        viewModelScope.launch {
            friendEventBus.friendAdded.collect { event ->
                _friends.value = event.updatedList
                val msg = if (event.addedCount == 1) "1 friend added" else "${event.addedCount} friends added"
                _actionState.value = FriendsActionState.Success(msg)
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            if (_friends.value.isEmpty()) _isLoading.value = true

            // Friends first — instant from Room cache after first launch
            when (val result = getFriendsUseCase()) {
                is ApiResult.Success -> _friends.value = result.data
                else -> Unit
            }
            // Stop showing loader as soon as friends are ready
            _isLoading.value = false

            // Balances load independently — updates amounts when ready without blocking display
            launch {
                when (val result = getAllBalancesUseCase()) {
                    is ApiResult.Success -> {
                        val balances = result.data
                        _balanceMap.value = balances.groupBy { it.otherUserId }
                            .mapValues { (_, list) -> list.sumOf { it.amount } }
                        _owedToYou.value = balances.filter { it.amount > 0 }.sumOf { it.amount }
                        _youOwe.value    = balances.filter { it.amount < 0 }.sumOf { -it.amount }
                    }
                    else -> Unit
                }
            }
        }
    }

    fun onSearchChanged(query: String) { _searchQuery.value = query }

    // Active friends only — show in main list with balances
    fun filteredActiveFriends(): List<Friend> {
        val q = _searchQuery.value.trim().lowercase()
        return _friends.value
            .filter { it.isActive }
            .let { list ->
                if (q.isBlank()) list
                else list.filter {
                    it.fullName.lowercase().contains(q) ||
                            it.email.lowercase().contains(q)
                }
            }
    }

    // Invited + placeholder — shown separately with status badge
    fun filteredNonActiveFriends(): List<Friend> {
        val q = _searchQuery.value.trim().lowercase()
        return _friends.value
            .filter { it.isPlaceholder || it.isInvited }
            .let { list ->
                if (q.isBlank()) list
                else list.filter { it.fullName.lowercase().contains(q) }
            }
    }

    fun resetActionState() { _actionState.value = FriendsActionState.Idle }
}

sealed class FriendsActionState {
    object Idle : FriendsActionState()
    data class Success(val message: String) : FriendsActionState()
    data class Error(val message: String)   : FriendsActionState()
}