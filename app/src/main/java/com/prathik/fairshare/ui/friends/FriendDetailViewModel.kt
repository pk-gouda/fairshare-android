package com.prathik.fairshare.ui.friends

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.data.local.InvitedFriendDao
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.model.Friend
import com.prathik.fairshare.domain.repository.FriendRepository
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
    savedStateHandle         : SavedStateHandle,
    private val friendRepository    : FriendRepository,
    private val getAllBalancesUseCase: GetAllBalancesUseCase,
    private val getGroupsUseCase    : GetGroupsUseCase,
    private val getGroupExpensesUseCase: GetGroupExpensesUseCase,
    private val invitedFriendDao    : InvitedFriendDao,
) : ViewModel() {

    val friendId: String = checkNotNull(savedStateHandle["friendId"])

    private val _isLoading    = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _friend       = MutableStateFlow<Friend?>(null)
    val friend: StateFlow<Friend?> = _friend.asStateFlow()

    // null = accepted, "pending" | "invited" | "placeholder" otherwise
    private val _friendStatus = MutableStateFlow<String?>(null)
    val friendStatus: StateFlow<String?> = _friendStatus.asStateFlow()

    private val _netBalance   = MutableStateFlow(0.0)
    val netBalance: StateFlow<Double> = _netBalance.asStateFlow()

    private val _currency     = MutableStateFlow("USD")
    val currency: StateFlow<String> = _currency.asStateFlow()

    // Expenses shared with this friend, sorted newest first
    private val _expensesState = MutableStateFlow<FriendExpensesState>(FriendExpensesState.Loading)
    val expensesState: StateFlow<FriendExpensesState> = _expensesState.asStateFlow()

    private val _actionState  = MutableStateFlow<FriendDetailActionState>(FriendDetailActionState.Idle)
    val actionState: StateFlow<FriendDetailActionState> = _actionState.asStateFlow()

    init { loadData() }

    fun loadData() {
        viewModelScope.launch {
            if (_expensesState.value !is FriendExpensesState.Success) {
                _expensesState.value = FriendExpensesState.Loading
            }
            _isLoading.value = true

            val friendsDeferred  = async { friendRepository.getFriends() }
            val sentDeferred     = async { friendRepository.getSentRequests() }
            val balancesDeferred = async { getAllBalancesUseCase() }
            val groupsDeferred   = async { getGroupsUseCase() }

            // Resolve who this friend is
            val accepted = (friendsDeferred.await() as? ApiResult.Success)
                ?.data?.find { it.id == friendId }

            if (accepted != null) {
                _friend.value       = accepted
                _friendStatus.value = null
            } else {
                val sent = (sentDeferred.await() as? ApiResult.Success)
                    ?.data?.find { it.receiverId == friendId }
                if (sent != null) {
                    _friend.value       = Friend(sent.receiverId, sent.receiverName, "", null)
                    _friendStatus.value = "pending"
                } else {
                    val local = invitedFriendDao.getAll()
                        .find { it.id.replace("-", "") == friendId.replace("-", "") }
                    if (local != null) {
                        _friend.value = Friend(local.id, local.displayName, local.emailOrPhone, null)
                        _friendStatus.value = if (local.isPlaceholder) "placeholder" else "invited"
                    }
                }
            }

            // Net balance with this friend
            when (val result = balancesDeferred.await()) {
                is ApiResult.Success -> {
                    val friendBalances = result.data.filter { it.otherUserId == friendId }
                    _netBalance.value = friendBalances.sumOf { it.amount }
                    _currency.value   = friendBalances.firstOrNull()?.currency ?: "USD"
                }
                else -> Unit
            }

            // Fetch shared expenses — only for accepted friends
            if (_friendStatus.value == null) {
                when (val groupsResult = groupsDeferred.await()) {
                    is ApiResult.Success -> {
                        val groups = groupsResult.data
                        // Fetch all group expenses in parallel
                        val allExpenses = groups.map { group ->
                            async {
                                (getGroupExpensesUseCase(group.id) as? ApiResult.Success)?.data
                                    ?: emptyList()
                            }
                        }.awaitAll().flatten()

                        // Filter expenses where friend is a payer or split participant
                        val friendExpenses = allExpenses.filter { expense ->
                            expense.payers.any { it.userId == friendId } ||
                                    expense.splits.any { it.userId == friendId }
                        }.sortedByDescending { it.expenseDate }

                        _expensesState.value = FriendExpensesState.Success(friendExpenses)
                    }
                    else -> _expensesState.value = FriendExpensesState.Error("Failed to load expenses")
                }
            } else {
                _expensesState.value = FriendExpensesState.Success(emptyList())
            }

            _isLoading.value = false
        }
    }

    fun resetActionState() { _actionState.value = FriendDetailActionState.Idle }
}

sealed class FriendExpensesState {
    object Loading : FriendExpensesState()
    data class Success(val expenses: List<Expense>) : FriendExpensesState()
    data class Error(val message: String) : FriendExpensesState()
}

sealed class FriendDetailActionState {
    object Idle : FriendDetailActionState()
    data class Success(val message: String) : FriendDetailActionState()
    data class Error(val message: String)   : FriendDetailActionState()
}