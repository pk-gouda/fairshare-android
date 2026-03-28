package com.prathik.fairshare.ui.expense

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.data.local.EncryptedTokenStore
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.usecase.expense.DeleteExpenseUseCase
import com.prathik.fairshare.domain.usecase.expense.GetExpenseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExpenseDetailViewModel @Inject constructor(
    private val getExpenseUseCase   : GetExpenseUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
    private val tokenStore          : EncryptedTokenStore,
    savedStateHandle                : SavedStateHandle,
) : ViewModel() {

    val expenseId: String = savedStateHandle.get<String>("expenseId") ?: ""
    val currentUserId: String? = tokenStore.getUserId()

    private val _expenseState = MutableStateFlow<ExpenseDetailUiState>(ExpenseDetailUiState.Loading)
    val expenseState: StateFlow<ExpenseDetailUiState> = _expenseState.asStateFlow()

    private val _actionState = MutableStateFlow<ExpenseActionState>(ExpenseActionState.Idle)
    val actionState: StateFlow<ExpenseActionState> = _actionState.asStateFlow()

    init { loadExpense() }

    fun loadExpense() {
        viewModelScope.launch {
            _expenseState.value = ExpenseDetailUiState.Loading
            when (val result = getExpenseUseCase(expenseId)) {
                is ApiResult.Success      -> _expenseState.value = ExpenseDetailUiState.Success(result.data)
                is ApiResult.NetworkError -> _expenseState.value = ExpenseDetailUiState.Error("No internet connection.", true)
                else                      -> _expenseState.value = ExpenseDetailUiState.Error("Failed to load expense.", false)
            }
        }
    }

    fun deleteExpense() {
        viewModelScope.launch {
            _actionState.value = ExpenseActionState.Loading
            when (val result = deleteExpenseUseCase(expenseId)) {
                is ApiResult.Success      -> _actionState.value = ExpenseActionState.Deleted
                is ApiResult.NetworkError -> _actionState.value = ExpenseActionState.Error("No internet connection.")
                else                      -> _actionState.value = ExpenseActionState.Error("Failed to delete expense.")
            }
        }
    }

    fun resetActionState() { _actionState.value = ExpenseActionState.Idle }
}

sealed class ExpenseDetailUiState {
    object Loading : ExpenseDetailUiState()
    data class Success(val expense: Expense) : ExpenseDetailUiState()
    data class Error(val message: String, val isNetwork: Boolean) : ExpenseDetailUiState()
}

sealed class ExpenseActionState {
    object Idle    : ExpenseActionState()
    object Loading : ExpenseActionState()
    object Deleted : ExpenseActionState()
    data class Error(val message: String) : ExpenseActionState()
}