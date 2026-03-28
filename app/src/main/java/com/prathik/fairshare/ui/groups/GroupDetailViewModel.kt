package com.prathik.fairshare.ui.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Balance
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.usecase.group.GetGroupBalancesUseCase
import com.prathik.fairshare.domain.usecase.expense.GetGroupExpensesUseCase
import com.prathik.fairshare.domain.usecase.group.GetGroupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for GroupDetailScreen.
 *
 * Loads group info, expenses, and balances in parallel.
 * groupId is extracted from SavedStateHandle (navigation argument).
 *
 * Uses GetGroupBalancesUseCase instead of GetAllBalancesUseCase —
 * fetches only this group's balances from the server, no client-side filtering.
 */
@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    private val getGroupUseCase: GetGroupUseCase,
    private val getGroupExpensesUseCase: GetGroupExpensesUseCase,
    private val getGroupBalancesUseCase: GetGroupBalancesUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val groupId: String = checkNotNull(savedStateHandle["groupId"])

    // ── Group state ───────────────────────────────────────────────────────────
    private val _groupState = MutableStateFlow<GroupDetailUiState>(GroupDetailUiState.Loading)
    val groupState: StateFlow<GroupDetailUiState> = _groupState.asStateFlow()

    // ── Expenses state ────────────────────────────────────────────────────────
    private val _expensesState = MutableStateFlow<ExpensesUiState>(ExpensesUiState.Loading)
    val expensesState: StateFlow<ExpensesUiState> = _expensesState.asStateFlow()

    // ── Group balances state ──────────────────────────────────────────────────
    private val _balances = MutableStateFlow<List<Balance>>(emptyList())
    val balances: StateFlow<List<Balance>> = _balances.asStateFlow()

    // ── Your balance in this group ────────────────────────────────────────────
    private val _yourBalance = MutableStateFlow(0.0)
    val yourBalance: StateFlow<Double> = _yourBalance.asStateFlow()

    init {
        loadData()
    }

    /**
     * Loads group, expenses and balances in parallel.
     */
    fun loadData() {
        viewModelScope.launch {
            _groupState.value = GroupDetailUiState.Loading
            _expensesState.value = ExpensesUiState.Loading

            val groupDeferred = async { getGroupUseCase(groupId) }
            val expensesDeferred = async { getGroupExpensesUseCase(groupId) }
            val balancesDeferred = async { getGroupBalancesUseCase(groupId) }

            // Group
            when (val result = groupDeferred.await()) {
                is ApiResult.Success -> _groupState.value = GroupDetailUiState.Success(result.data)
                is ApiResult.NetworkError -> _groupState.value = GroupDetailUiState.Error(
                    message = result.message, isNetwork = true
                )

                else -> _groupState.value = GroupDetailUiState.Error(
                    message = "Failed to load group.", isNetwork = false
                )
            }

            // Expenses
            when (val result = expensesDeferred.await()) {
                is ApiResult.Success -> _expensesState.value = ExpensesUiState.Success(result.data)
                is ApiResult.NetworkError -> _expensesState.value = ExpensesUiState.Error(
                    message = result.message, isNetwork = true
                )

                else -> _expensesState.value = ExpensesUiState.Error(
                    message = "Failed to load expenses.", isNetwork = false
                )
            }

            // Balances — group-scoped, no client-side filter needed
            when (val result = balancesDeferred.await()) {
                is ApiResult.Success -> {
                    _balances.value = result.data
                    _yourBalance.value = result.data.sumOf { it.amount }
                }

                else -> Unit
            }
        }
    }

    /**
     * Refreshes only the expenses — called after adding/editing/deleting an expense.
     */
    fun refreshExpenses() {
        viewModelScope.launch {
            when (val result = getGroupExpensesUseCase(groupId)) {
                is ApiResult.Success -> _expensesState.value = ExpensesUiState.Success(result.data)
                else -> Unit
            }
        }
    }
}

// ── UI States ─────────────────────────────────────────────────────────────────

sealed class GroupDetailUiState {
    object Loading : GroupDetailUiState()
    data class Success(val group: Group) : GroupDetailUiState()
    data class Error(val message: String, val isNetwork: Boolean) : GroupDetailUiState()
}

sealed class ExpensesUiState {
    object Loading : ExpensesUiState()
    data class Success(val expenses: List<Expense>) : ExpensesUiState()
    data class Error(val message: String, val isNetwork: Boolean) : ExpensesUiState()
}