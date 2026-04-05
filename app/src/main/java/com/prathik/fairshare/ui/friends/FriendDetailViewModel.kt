package com.prathik.fairshare.ui.friends

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.model.Friend
import com.prathik.fairshare.domain.repository.FriendRepository
import com.prathik.fairshare.domain.repository.ExpenseRepository
import com.prathik.fairshare.domain.usecase.balance.GetAllBalancesUseCase
import com.prathik.fairshare.domain.usecase.expense.GetGroupExpensesUseCase
import com.prathik.fairshare.domain.usecase.group.GetGroupsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val friendRepository: FriendRepository,
    private val expenseRepository: ExpenseRepository,
    private val getAllBalancesUseCase: GetAllBalancesUseCase,
    private val getGroupsUseCase: GetGroupsUseCase,
    private val getGroupExpensesUseCase: GetGroupExpensesUseCase,
) : ViewModel() {

    val friendId: String = checkNotNull(savedStateHandle["friendId"])

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _friend = MutableStateFlow<Friend?>(null)
    val friend: StateFlow<Friend?> = _friend.asStateFlow()

    // null = active friend, "pending" | "invited" | "placeholder" otherwise
    private val _friendStatus = MutableStateFlow<String?>(null)
    val friendStatus: StateFlow<String?> = _friendStatus.asStateFlow()

    private val _netBalance = MutableStateFlow(0.0)
    val netBalance: StateFlow<Double> = _netBalance.asStateFlow()

    private val _currency = MutableStateFlow("USD")
    val currency: StateFlow<String> = _currency.asStateFlow()

    private val _expensesState = MutableStateFlow<FriendExpensesState>(FriendExpensesState.Loading)
    val expensesState: StateFlow<FriendExpensesState> = _expensesState.asStateFlow()

    private val _actionState =
        MutableStateFlow<FriendDetailActionState>(FriendDetailActionState.Idle)
    val actionState: StateFlow<FriendDetailActionState> = _actionState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            if (_expensesState.value !is FriendExpensesState.Success) {
                _expensesState.value = FriendExpensesState.Loading
            }
            _isLoading.value = true

            val friendsDeferred = async { friendRepository.getFriends() }
            val sentDeferred = async { friendRepository.getSentRequests() }
            val balancesDeferred = async { getAllBalancesUseCase() }
            val groupsDeferred = async { getGroupsUseCase() }

            // Resolve friend — backend now returns ACTIVE, PLACEHOLDER, and INVITED
            val allFriends = (friendsDeferred.await() as? ApiResult.Success)?.data ?: emptyList()
            val found = allFriends.find { it.id == friendId }

            if (found != null) {
                _friend.value = found
                _friendStatus.value = when {
                    found.isPlaceholder -> "placeholder"
                    found.isInvited -> "invited"
                    else -> null // active
                }
            } else {
                // Fall back to sent requests (pending user who hasn't accepted yet)
                val sent = (sentDeferred.await() as? ApiResult.Success)
                    ?.data?.find { it.receiverId == friendId }
                if (sent != null) {
                    _friend.value = Friend(sent.receiverId, sent.receiverName, "", null)
                    _friendStatus.value = "pending"
                }
            }

            // Net balance
            when (val result = balancesDeferred.await()) {
                is ApiResult.Success -> {
                    val friendBalances = result.data.filter { it.otherUserId == friendId }
                    _netBalance.value = friendBalances.sumOf { it.amount }
                    _currency.value = friendBalances.firstOrNull()?.currency ?: "USD"
                }

                else -> Unit
            }

            // Fetch shared expenses — direct expenses for ALL friend types,
            // group expenses only for active friends (placeholder/invited can't be in groups)
            val directDeferred =
                async { expenseRepository.getDirectExpensesWithFriend(friendId) }

            val groupExpenses = if (_friendStatus.value == null) {
                // Active friend — also check group expenses where they're a participant
                val groupExpensesDeferred = async {
                    when (val groupsResult = groupsDeferred.await()) {
                        is ApiResult.Success -> groupsResult.data.map { group ->
                            async {
                                (getGroupExpensesUseCase(group.id) as? ApiResult.Success)?.data
                                    ?: emptyList()
                            }
                        }.awaitAll().flatten().filter { expense ->
                            expense.payers.any { it.userId == friendId } ||
                                    expense.splits.any { it.userId == friendId }
                        }

                        else -> emptyList()
                    }
                }
                groupExpensesDeferred.await()
            } else {
                emptyList()
            }

            val directExpenses =
                (directDeferred.await() as? ApiResult.Success)?.data ?: emptyList()

            // Merge, deduplicate by id, sort newest first
            val allExpenses = (directExpenses + groupExpenses)
                .distinctBy { it.id }
                .sortedByDescending { it.expenseDate }

            _expensesState.value = FriendExpensesState.Success(allExpenses)

            _isLoading.value = false
        }
    }

    /**
     * Lightweight refresh — re-fetches expenses + balance without
     * reloading the friend profile. Called on every screen resume.
     */
    fun refreshExpenses() {
        viewModelScope.launch {
            // Refresh balance
            when (val result = getAllBalancesUseCase()) {
                is ApiResult.Success -> {
                    val friendBalances = result.data.filter { it.otherUserId == friendId }
                    _netBalance.value = friendBalances.sumOf { it.amount }
                    _currency.value = friendBalances.firstOrNull()?.currency ?: "USD"
                }
                else -> Unit
            }

            // Refresh expenses — direct for all friend types, group only for active
            val directDeferred =
                async { expenseRepository.getDirectExpensesWithFriend(friendId) }

            val groupExpenses = if (_friendStatus.value == null) {
                val groupExpensesDeferred = async {
                    when (val groupsResult = getGroupsUseCase()) {
                        is ApiResult.Success -> groupsResult.data.map { group ->
                            async {
                                (getGroupExpensesUseCase(group.id) as? ApiResult.Success)?.data
                                    ?: emptyList()
                            }
                        }.awaitAll().flatten().filter { expense ->
                            expense.payers.any { it.userId == friendId } ||
                                    expense.splits.any { it.userId == friendId }
                        }
                        else -> emptyList()
                    }
                }
                groupExpensesDeferred.await()
            } else {
                emptyList()
            }

            val directExpenses =
                (directDeferred.await() as? ApiResult.Success)?.data ?: emptyList()

            val allExpenses = (directExpenses + groupExpenses)
                .distinctBy { it.id }
                .sortedByDescending { it.expenseDate }

            _expensesState.value = FriendExpensesState.Success(allExpenses)
        }
    }

    fun resetActionState() {
        _actionState.value = FriendDetailActionState.Idle
    }
}

sealed class FriendExpensesState {
    object Loading : FriendExpensesState()
    data class Success(val expenses: List<Expense>) : FriendExpensesState()
    data class Error(val message: String) : FriendExpensesState()
}

sealed class FriendDetailActionState {
    object Idle : FriendDetailActionState()
    data class Success(val message: String) : FriendDetailActionState()
    data class Error(val message: String) : FriendDetailActionState()
}