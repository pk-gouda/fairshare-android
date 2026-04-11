package com.prathik.fairshare.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.data.model.request.CreateReminderRequest
import com.prathik.fairshare.data.network.api.ReminderApiService
import com.prathik.fairshare.data.network.safeApiCall
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.ReminderFrequency
import com.prathik.fairshare.ui.components.FsPrimaryButton
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Radius
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.Surface2
import com.prathik.fairshare.ui.theme.Surface4
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary
import com.prathik.fairshare.ui.theme.TextTertiary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class CreateReminderViewModel @Inject constructor(
    private val reminderApiService: ReminderApiService,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val groupId: String = checkNotNull(savedStateHandle["groupId"])

    private val _frequency     = MutableStateFlow(ReminderFrequency.WEEKLY)
    val frequency: StateFlow<ReminderFrequency> = _frequency.asStateFlow()

    private val _dayOfWeek     = MutableStateFlow(1) // Monday
    val dayOfWeek: StateFlow<Int> = _dayOfWeek.asStateFlow()

    private val _dayOfMonth    = MutableStateFlow(1)
    val dayOfMonth: StateFlow<Int> = _dayOfMonth.asStateFlow()

    private val _notifyViaApp  = MutableStateFlow(true)
    val notifyViaApp: StateFlow<Boolean> = _notifyViaApp.asStateFlow()

    private val _notifyViaEmail = MutableStateFlow(false)
    val notifyViaEmail: StateFlow<Boolean> = _notifyViaEmail.asStateFlow()

    private val _isLoading     = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _actionState   = MutableStateFlow<CreateReminderActionState>(CreateReminderActionState.Idle)
    val actionState: StateFlow<CreateReminderActionState> = _actionState.asStateFlow()

    fun onFrequencyChanged(f: ReminderFrequency) { _frequency.value = f }
    fun onDayOfWeekChanged(d: Int)               { _dayOfWeek.value = d }
    fun onDayOfMonthChanged(d: Int)              { _dayOfMonth.value = d }
    fun onNotifyViaAppChanged(v: Boolean)        { _notifyViaApp.value = v }
    fun onNotifyViaEmailChanged(v: Boolean)      { _notifyViaEmail.value = v }

    fun create() {
        viewModelScope.launch {
            _isLoading.value = true
            val request = CreateReminderRequest(
                groupId        = groupId,
                frequency      = _frequency.value,
                dayOfWeek      = if (_frequency.value == ReminderFrequency.WEEKLY) _dayOfWeek.value else null,
                dayOfMonth     = if (_frequency.value == ReminderFrequency.MONTHLY) _dayOfMonth.value else null,
                notifyViaApp   = _notifyViaApp.value,
                notifyViaEmail = _notifyViaEmail.value,
            )
            when (safeApiCall { reminderApiService.createReminder(groupId, request) }) {
                is ApiResult.Success -> _actionState.value = CreateReminderActionState.Success
                is ApiResult.NetworkError -> _actionState.value = CreateReminderActionState.Error("No internet connection.")
                else -> _actionState.value = CreateReminderActionState.Error("Failed to create reminder.")
            }
            _isLoading.value = false
        }
    }

    fun resetActionState() { _actionState.value = CreateReminderActionState.Idle }
}

sealed class CreateReminderActionState {
    object Idle    : CreateReminderActionState()
    object Success : CreateReminderActionState()
    data class Error(val message: String) : CreateReminderActionState()
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReminderScreen(
    onBack   : () -> Unit,
    onCreated: () -> Unit,
    viewModel: CreateReminderViewModel = hiltViewModel(),
) {
    val frequency      by viewModel.frequency.collectAsState()
    val dayOfWeek      by viewModel.dayOfWeek.collectAsState()
    val dayOfMonth     by viewModel.dayOfMonth.collectAsState()
    val notifyViaApp   by viewModel.notifyViaApp.collectAsState()
    val notifyViaEmail by viewModel.notifyViaEmail.collectAsState()
    val isLoading      by viewModel.isLoading.collectAsState()
    val actionState    by viewModel.actionState.collectAsState()
    val snackbarHost   = remember { SnackbarHostState() }

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is CreateReminderActionState.Success -> { onCreated(); viewModel.resetActionState() }
            is CreateReminderActionState.Error   -> { snackbarHost.showSnackbar(s.message); viewModel.resetActionState() }
            else -> Unit
        }
    }

    Scaffold(
        containerColor = Surface0,
        snackbarHost   = { SnackbarHost(snackbarHost) },
        topBar         = { FsTopBar(title = "New reminder", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.lg)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(Spacing.md))

            SectionLabel("FREQUENCY")
            Spacer(Modifier.height(Spacing.sm))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.xl))
                    .background(Surface2),
            ) {
                ReminderFrequency.values().forEachIndexed { i, freq ->
                    OptionRow(
                        label     = freq.name.lowercase().replaceFirstChar { it.uppercase() },
                        selected  = frequency == freq,
                        onClick   = { viewModel.onFrequencyChanged(freq) },
                    )
                }
            }

            // Day picker for WEEKLY
            if (frequency == ReminderFrequency.WEEKLY) {
                Spacer(Modifier.height(Spacing.xl))
                SectionLabel("SEND ON")
                Spacer(Modifier.height(Spacing.sm))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.xl))
                        .background(Surface2),
                ) {
                    listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                        .forEachIndexed { i, day ->
                            OptionRow(
                                label    = day,
                                selected = dayOfWeek == i + 1,
                                onClick  = { viewModel.onDayOfWeekChanged(i + 1) },
                            )
                        }
                }
            }

            // Day picker for MONTHLY
            if (frequency == ReminderFrequency.MONTHLY) {
                Spacer(Modifier.height(Spacing.xl))
                SectionLabel("DAY OF MONTH")
                Spacer(Modifier.height(Spacing.sm))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.xl))
                        .background(Surface2),
                ) {
                    listOf(1, 5, 10, 15, 20, 25, 28).forEach { day ->
                        OptionRow(
                            label    = "Day $day",
                            selected = dayOfMonth == day,
                            onClick  = { viewModel.onDayOfMonthChanged(day) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.xl))
            SectionLabel("NOTIFY VIA")
            Spacer(Modifier.height(Spacing.sm))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.xl))
                    .background(Surface2),
            ) {
                ToggleRow("In-app notification", notifyViaApp)   { viewModel.onNotifyViaAppChanged(it) }
                ToggleRow("Email notification",  notifyViaEmail) { viewModel.onNotifyViaEmailChanged(it) }
            }

            Spacer(Modifier.height(Spacing.xxxl))

            FsPrimaryButton(
                text      = "Create reminder",
                onClick   = { viewModel.create() },
                isLoading = isLoading,
                modifier  = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.xxxl))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text          = text,
        fontSize      = 11.sp,
        fontWeight    = FontWeight.SemiBold,
        color         = TextTertiary,
        letterSpacing = 0.8.sp,
    )
}

@Composable
private fun OptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, fontSize = 15.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        if (selected) {
            Text(text = "✓", fontSize = 15.sp, color = Green400, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, fontSize = 15.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
            colors          = SwitchDefaults.colors(
                checkedThumbColor  = Green400,
                checkedTrackColor  = Green400.copy(alpha = 0.3f),
            ),
        )
    }
}