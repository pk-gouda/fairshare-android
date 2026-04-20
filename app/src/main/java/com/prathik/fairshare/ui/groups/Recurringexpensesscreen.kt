package com.prathik.fairshare.ui.groups

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import com.prathik.fairshare.domain.repository.ExpenseRepository
import com.prathik.fairshare.domain.model.Expense
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
import com.prathik.fairshare.ui.theme.Surface3
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary
import com.prathik.fairshare.ui.theme.TextTertiary
import com.prathik.fairshare.util.MoneyUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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

    fun updateSchedule(expenseId: String, newInterval: String, nextRepeatDate: String?) {
        viewModelScope.launch {
            when (expenseRepository.updateExpense(
                expenseId      = expenseId,
                description    = null, totalAmount = null, currency = null,
                splitType      = null, category    = null, notes    = null,
                expenseDate    = null, payerData   = null, splitData = null,
                repeatInterval = newInterval,
                clearRepeat    = null,
                nextRepeatDate = nextRepeatDate,
            )) {
                is ApiResult.Success -> { _actionState.value = RecurringExpensesActionState.Updated; load() }
                else -> _actionState.value = RecurringExpensesActionState.Error("Failed to update schedule.")
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

data class EditScheduleState(
    val expense: Expense,
    val currentInterval: String,
)

sealed class RecurringExpensesActionState {
    object Idle    : RecurringExpensesActionState()
    object Stopped : RecurringExpensesActionState()
    object Updated : RecurringExpensesActionState()
    data class Error(val message: String) : RecurringExpensesActionState()
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringExpensesScreen(
    onBack                  : () -> Unit,
    onNavigateToEditExpense : (String) -> Unit = {},
    viewModel               : RecurringExpensesViewModel = hiltViewModel(),
) {
    val state       by viewModel.state.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    var stopTarget        by remember { mutableStateOf<Expense?>(null) }
    var editScheduleTarget by remember { mutableStateOf<Expense?>(null) }

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is RecurringExpensesActionState.Stopped -> { snackbarHost.showSnackbar("Recurring stopped") ; viewModel.resetActionState() }
            is RecurringExpensesActionState.Updated -> { snackbarHost.showSnackbar("Schedule updated") ; viewModel.resetActionState() }
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


    // ── Edit Schedule Sheet ───────────────────────────────────────────────────
    editScheduleTarget?.let { expense ->
        val frequencies = listOf("DAILY", "WEEKLY", "MONTHLY")
        val labels = mapOf("DAILY" to "Daily", "WEEKLY" to "Weekly", "MONTHLY" to "Monthly")
        var selected by remember(expense.id) { mutableStateOf(expense.repeatInterval ?: "MONTHLY") }
        var showNextDatePicker by remember(expense.id) { mutableStateOf(false) }

        // Parse current nextRepeatDate string ("yyyy-MM-dd") into epoch millis for the picker
        val initialMillis: Long? = remember(expense.nextRepeatDate) {
            expense.nextRepeatDate?.let {
                runCatching {
                    LocalDate.parse(it).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
                }.getOrNull()
            }
        }
        // selectedNextDate tracks the user's picked date as "yyyy-MM-dd" string
        var selectedNextDate by remember(expense.id) {
            mutableStateOf<String?>(expense.nextRepeatDate)
        }

        // Human-readable label for the next date button
        val nextDateLabel: String = selectedNextDate?.let { dateStr ->
            try {
                LocalDate.parse(dateStr).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
            } catch (e: Exception) {
                dateStr
            }
        } ?: "Pick date"

        if (showNextDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
            DatePickerDialog(
                onDismissRequest = { showNextDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC")).toLocalDate()
                            selectedNextDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                        }
                        showNextDatePicker = false
                    }) { Text("OK", color = Green400) }
                },
                dismissButton = {
                    TextButton(onClick = { showNextDatePicker = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
            ) { DatePicker(state = datePickerState) }
        }

        AlertDialog(
            onDismissRequest = { editScheduleTarget = null },
            containerColor   = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            title   = { Text("Edit schedule — ${expense.description}",
                fontWeight = FontWeight.SemiBold, color = TextPrimary) },
            text    = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    // ── Frequency chips ──────────────────────────────────────
                    Text("How often should this repeat?",
                        fontSize = 14.sp, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        frequencies.forEach { freq ->
                            val isSelected = selected == freq
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) Green400.copy(alpha = 0.15f) else Surface2)
                                    .border(1.dp, if (isSelected) Green400 else Surface3, RoundedCornerShape(20.dp))
                                    .clickable { selected = freq }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Text(labels[freq] ?: freq, fontSize = 13.sp,
                                    color = if (isSelected) Green400 else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                            }
                        }
                    }

                    // ── Next occurrence date ─────────────────────────────────
                    Spacer(Modifier.height(4.dp))
                    Text("Next occurrence", fontSize = 14.sp, color = TextSecondary)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Surface2)
                            .border(1.dp, Surface3, RoundedCornerShape(12.dp))
                            .clickable { showNextDatePicker = true }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Text(nextDateLabel, fontSize = 14.sp, color = TextPrimary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateSchedule(expense.id, selected, selectedNextDate)
                    editScheduleTarget = null
                }) { Text("Save", color = Green400, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { editScheduleTarget = null }) {
                    Text("Cancel", color = TextSecondary)
                }
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
                                expense             = expense,
                                onNavigateToEdit    = { onNavigateToEditExpense(expense.id) },
                                onEditSchedule      = { editScheduleTarget = expense },
                                onStop              = { stopTarget = expense },
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
    expense             : Expense,
    onNavigateToEdit    : () -> Unit,
    onEditSchedule      : () -> Unit,
    onStop              : () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.xl))
            .background(Surface2)
            .clickable(onClick = onNavigateToEdit)
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
                    text       = MoneyUtils.format(expense.totalAmount, expense.currency),
                    fontSize   = 14.sp,
                    color      = Green400,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            TextButton(onClick = onEditSchedule) {
                Text("Edit Schedule", color = TextSecondary, fontSize = 13.sp)
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