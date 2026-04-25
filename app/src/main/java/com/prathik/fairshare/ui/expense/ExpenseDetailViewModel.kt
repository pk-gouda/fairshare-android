package com.prathik.fairshare.ui.expense

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.data.local.EncryptedTokenStore
import com.prathik.fairshare.data.network.api.ExpenseApiService
import com.prathik.fairshare.data.network.safeApiCall
import com.prathik.fairshare.data.model.mapper.toDomain
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.model.ExpenseChangeLog
import com.prathik.fairshare.domain.model.ExpenseItem
import com.prathik.fairshare.domain.usecase.expense.DeleteExpenseUseCase
import com.prathik.fairshare.domain.usecase.expense.GetExpenseUseCase
import com.prathik.fairshare.domain.usecase.expense.RestoreExpenseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExpenseDetailViewModel @Inject constructor(
    private val getExpenseUseCase    : GetExpenseUseCase,
    private val deleteExpenseUseCase  : DeleteExpenseUseCase,
    private val restoreExpenseUseCase : RestoreExpenseUseCase,
    private val expenseApiService    : ExpenseApiService,
    private val tokenStore           : EncryptedTokenStore,
    savedStateHandle                 : SavedStateHandle,
) : ViewModel() {

    val expenseId    : String  = savedStateHandle.get<String>("expenseId") ?: ""
    val currentUserId: String? = tokenStore.getUserId()

    private val _expenseState = MutableStateFlow<ExpenseDetailUiState>(ExpenseDetailUiState.Loading)
    val expenseState: StateFlow<ExpenseDetailUiState> = _expenseState.asStateFlow()

    private val _actionState = MutableStateFlow<ExpenseActionState>(ExpenseActionState.Idle)
    val actionState: StateFlow<ExpenseActionState> = _actionState.asStateFlow()

    private val _items = MutableStateFlow<List<ExpenseItem>>(emptyList())
    val items: StateFlow<List<ExpenseItem>> = _items.asStateFlow()

    private val _itemsLoading = MutableStateFlow(false)
    val itemsLoading: StateFlow<Boolean> = _itemsLoading.asStateFlow()

    private val _changeLog = MutableStateFlow<List<ExpenseChangeLog>>(emptyList())
    val changeLog: StateFlow<List<ExpenseChangeLog>> = _changeLog.asStateFlow()

    private val _changeLogLoading = MutableStateFlow(false)
    val changeLogLoading: StateFlow<Boolean> = _changeLogLoading.asStateFlow()

    init { loadExpense() }

    private var hasLoadedOnce = false

    fun loadExpense() {
        viewModelScope.launch {
            if (!hasLoadedOnce) _expenseState.value = ExpenseDetailUiState.Loading
            when (val result = getExpenseUseCase(expenseId)) {
                is ApiResult.Success -> {
                    _expenseState.value = ExpenseDetailUiState.Success(result.data)
                    hasLoadedOnce = true
                    // Always load items — itemCount may be 0 due to lazy loading on backend
                    if (_items.value.isEmpty()) {
                        loadItems()
                    }
                    loadChangeLog()
                }
                is ApiResult.NotFound -> _expenseState.value = ExpenseDetailUiState.Deleted
                is ApiResult.NetworkError -> {
                    if (!hasLoadedOnce) _expenseState.value = ExpenseDetailUiState.Error("No internet connection.", true)
                }
                else -> {
                    if (!hasLoadedOnce) _expenseState.value = ExpenseDetailUiState.Error("Failed to load expense.", false)
                }
            }
        }
    }

    /**
     * Forces a full reload of the expense — called after editing item assignments
     * so the item breakdown reflects the latest saved state.
     */
    fun forceRefresh() {
        hasLoadedOnce = false
        _items.value = emptyList()
        loadExpense()
    }

    fun loadItems() {
        viewModelScope.launch {
            _itemsLoading.value = true
            val result = safeApiCall { expenseApiService.getExpenseItems(expenseId) }
            if (result is ApiResult.Success) {
                _items.value = result.data.map { it.toDomain() }
            }
            _itemsLoading.value = false
        }
    }

    fun loadChangeLog() {
        viewModelScope.launch {
            _changeLogLoading.value = true
            val result = safeApiCall { expenseApiService.getChangeLog(expenseId) }
            if (result is ApiResult.Success) {
                _changeLog.value = result.data.map { entry ->
                    ExpenseChangeLog(
                        changedById   = entry.changedById,
                        changedByName = entry.changedByName,
                        changedAt     = entry.changedAt,
                        changes       = entry.changes.map { fc ->
                            ExpenseChangeLog.FieldChange(
                                fieldName = fc.fieldName,
                                oldValue  = fc.oldValue,
                                newValue  = fc.newValue,
                            )
                        }
                    )
                }
            }
            _changeLogLoading.value = false
        }
    }

    fun deleteExpense() {
        viewModelScope.launch {
            _actionState.value = ExpenseActionState.Loading
            when (val result = deleteExpenseUseCase(expenseId)) {
                is ApiResult.Success    -> _actionState.value = ExpenseActionState.Deleted
                is ApiResult.NetworkError -> _actionState.value = ExpenseActionState.Error("No internet connection.")
                else                    -> _actionState.value = ExpenseActionState.Error("Failed to delete expense.")
            }
        }
    }

    fun restoreExpense() {
        viewModelScope.launch {
            _actionState.value = ExpenseActionState.Loading
            when (restoreExpenseUseCase(expenseId)) {
                is ApiResult.Success    -> _actionState.value = ExpenseActionState.Restored
                is ApiResult.NetworkError -> _actionState.value = ExpenseActionState.Error("No internet connection.")
                else                    -> _actionState.value = ExpenseActionState.Error("Failed to restore expense.")
            }
        }
    }

    fun resetActionState() { _actionState.value = ExpenseActionState.Idle }
}

sealed class ExpenseDetailUiState {
    object Loading : ExpenseDetailUiState()
    object Deleted : ExpenseDetailUiState()
    data class Success(val expense: Expense) : ExpenseDetailUiState()
    data class Error(val message: String, val isNetwork: Boolean) : ExpenseDetailUiState()
}

sealed class ExpenseActionState {
    object Idle    : ExpenseActionState()
    object Loading : ExpenseActionState()
    object Deleted : ExpenseActionState()
    object Restored : ExpenseActionState()
    data class Error(val message: String) : ExpenseActionState()
}