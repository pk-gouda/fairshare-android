package com.prathik.fairshare.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.repository.ExpenseRepository
import com.prathik.fairshare.ui.components.FsEmptyState
import com.prathik.fairshare.ui.components.FsErrorScreen
import com.prathik.fairshare.ui.components.FsLoadingScreen
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Negative
import com.prathik.fairshare.ui.theme.Radius
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.Surface2
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary
import com.prathik.fairshare.ui.theme.TextTertiary
import com.prathik.fairshare.util.MoneyUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class RecurringExpensesViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val groupId: String = checkNotNull(savedStateHandle["groupId"])

    private val _state = MutableStateFlow<RecurringExpensesUiState>(RecurringExpensesUiState.Loading)
    val state: StateFlow<RecurringExpensesUiState> = _state.asStateFlow()

    private val _actionState = MutableStateFlow<RecurringExpensesActionState>(RecurringExpensesActionState.Idle)
    val actionState: StateFlow<RecurringExpensesActionState> = _actionState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = RecurringExpensesUiState.Loading
            when (val result = expenseRepository.getRecurringExpenses(groupId)) {
                is ApiResult.Success      -> _state.value = RecurringExpensesUiState.Success(result.data)
                is ApiResult.NetworkError -> _state.value = RecurringExpensesUiState.Error("No internet connection.")
                else                      -> _state.value = RecurringExpensesUiState.Error("Failed to load recurring expenses.")
            }
        }
    }

    fun stopRecurring(expenseId: String) {
        viewModelScope.launch {
            when (expenseRepository.stopRecurring(expenseId)) {
                is ApiResult.Success -> {
                    _actionState.value = RecurringExpensesActionState.Stopped
                    load()
                }
                else -> _actionState.value = RecurringExpensesActionState.Error("Failed to stop recurring expense.")
            }
        }
    }

    fun resetActionState() { _actionState.value = RecurringExpensesActionState.Idle }
}

sealed class RecurringExpensesUiState {
    object Loading : RecurringExpensesUiState()
    data class Success(val expenses: List<Expense>) : RecurringExpensesUiState()
    data class Error(val message: String) : RecurringExpensesUiState()
}

sealed class RecurringExpensesActionState {
    object Idle    : RecurringExpensesActionState()
    object Stopped : RecurringExpensesActionState()
    data class Error(val message: String) : RecurringExpensesActionState()
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringExpensesScreen(
    onBack             : () -> Unit,
    onNavigateToExpense: (String) -> Unit,
    viewModel          : RecurringExpensesViewModel = hiltViewModel(),
) {
    val state       by viewModel.state.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    var stopTarget  by remember { mutableStateOf<Expense?>(null) }

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is RecurringExpensesActionState.Stopped -> { snackbarHost.showSnackbar("Recurring stopped") ; viewModel.resetActionState() }
            is RecurringExpensesActionState.Error   -> { snackbarHost.showSnackbar(s.message) ; viewModel.resetActionState() }
            else -> Unit
        }
    }

    stopTarget?.let { expense ->
        AlertDialog(
            onDismissRequest = { stopTarget = null },
            title   = { Text("Stop recurring?") },
            text    = { Text("\"${expense.description}\" will no longer generate new entries automatically.") },
            confirmButton = {
                TextButton(onClick = { viewModel.stopRecurring(expense.id) ; stopTarget = null }) {
                    Text("Stop", color = Negative)
                }
            },
            dismissButton = {
                TextButton(onClick = { stopTarget = null }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        containerColor = Surface0,
        snackbarHost   = { SnackbarHost(snackbarHost) },
        topBar         = { FsTopBar(title = "Recurring expenses", onBack = onBack) },
    ) { innerPadding ->
        when (val s = state) {
            is RecurringExpensesUiState.Loading -> FsLoadingScreen()
            is RecurringExpensesUiState.Error   -> FsErrorScreen(message = s.message, onRetry = { viewModel.load() })
            is RecurringExpensesUiState.Success -> {
                if (s.expenses.isEmpty()) {
                    FsEmptyState(
                        title    = "No recurring expenses",
                        subtitle = "Expenses set to repeat automatically will appear here",
                    )
                } else {
                    LazyColumn(
                        modifier            = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        items(s.expenses, key = { it.id }) { expense ->
                            RecurringExpenseRow(
                                expense   = expense,
                                onClick   = { onNavigateToExpense(expense.id) },
                                onStop    = { stopTarget = expense },
                            )
                        }
                        item { Spacer(Modifier.height(Spacing.xxxl)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecurringExpenseRow(
    expense : Expense,
    onClick : () -> Unit,
    onStop  : () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.xl))
            .background(Surface2)
            .padding(Spacing.md),
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = expense.description,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color      = TextPrimary,
                )
                Text(
                    text     = MoneyUtils.format(expense.totalAmount, expense.currency),
                    fontSize = 14.sp,
                    color    = Green400,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            TextButton(onClick = onStop) {
                Text("Stop", color = Negative, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text     = "Paid by ${expense.payers.firstOrNull()?.fullName ?: "Unknown"} · ${expense.splitType.name.lowercase().replaceFirstChar { it.uppercase() }} split",
            fontSize = 12.sp,
            color    = TextTertiary,
        )
    }
}