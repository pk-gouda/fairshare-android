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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.prathik.fairshare.data.model.mapper.toDomain
import com.prathik.fairshare.data.model.request.UpdateReminderRequest
import com.prathik.fairshare.data.network.safeApiCall
import com.prathik.fairshare.data.network.api.ReminderApiService
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Reminder
import com.prathik.fairshare.domain.model.ReminderFrequency
import com.prathik.fairshare.ui.components.FsEmptyState
import com.prathik.fairshare.ui.components.FsErrorScreen
import com.prathik.fairshare.ui.components.FsIconButton
import com.prathik.fairshare.ui.components.FsSkeletonBlock
import com.prathik.fairshare.ui.components.FsSkeletonTimelineRow
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Radius
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.Surface2
import com.prathik.fairshare.ui.theme.Surface3
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
class RemindersViewModel @Inject constructor(
    private val reminderApiService: ReminderApiService,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val groupId: String = checkNotNull(savedStateHandle["groupId"])

    private val _state = MutableStateFlow<RemindersUiState>(RemindersUiState.Loading)
    val state: StateFlow<RemindersUiState> = _state.asStateFlow()

    private val _actionState = MutableStateFlow<RemindersActionState>(RemindersActionState.Idle)
    val actionState: StateFlow<RemindersActionState> = _actionState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = RemindersUiState.Loading
            when (val result = safeApiCall { reminderApiService.getGroupReminders(groupId) }) {
                is ApiResult.Success      -> _state.value = RemindersUiState.Success(
                    result.data.map { it.toDomain() }
                )
                is ApiResult.NetworkError -> _state.value = RemindersUiState.Error("No internet connection.")
                else                      -> _state.value = RemindersUiState.Error("Failed to load reminders.")
            }
        }
    }

    fun toggleActive(reminder: Reminder) {
        viewModelScope.launch {
            safeApiCall {
                reminderApiService.updateReminder(
                    reminder.id,
                    UpdateReminderRequest(isActive = !reminder.isActive)
                )
            }
            load()
        }
    }

    fun delete(reminderId: String) {
        viewModelScope.launch {
            when (safeApiCall { reminderApiService.deleteReminder(reminderId) }) {
                is ApiResult.Success -> {
                    _actionState.value = RemindersActionState.Deleted
                    load()
                }
                else -> _actionState.value = RemindersActionState.Error("Failed to delete reminder.")
            }
        }
    }

    fun resetActionState() { _actionState.value = RemindersActionState.Idle }
}

sealed class RemindersUiState {
    object Loading : RemindersUiState()
    data class Success(val reminders: List<Reminder>) : RemindersUiState()
    data class Error(val message: String) : RemindersUiState()
}

sealed class RemindersActionState {
    object Idle    : RemindersActionState()
    object Deleted : RemindersActionState()
    data class Error(val message: String) : RemindersActionState()
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    onBack            : () -> Unit,
    onNavigateToCreate: () -> Unit,
    viewModel         : RemindersViewModel = hiltViewModel(),
) {
    val state       by viewModel.state.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    var deleteTarget by remember { mutableStateOf<Reminder?>(null) }

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is RemindersActionState.Deleted -> { snackbarHost.showSnackbar("Reminder deleted") ; viewModel.resetActionState() }
            is RemindersActionState.Error   -> { snackbarHost.showSnackbar(s.message) ; viewModel.resetActionState() }
            else -> Unit
        }
    }

    deleteTarget?.let { reminder ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title   = { Text("Delete reminder?") },
            text    = { Text("This will stop sending ${reminder.frequency.name.lowercase()} settlement reminders to this group.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(reminder.id) ; deleteTarget = null }) {
                    Text("Delete", color = com.prathik.fairshare.ui.theme.Negative)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        containerColor = Surface0,
        snackbarHost   = { SnackbarHost(snackbarHost) },
        topBar         = { FsTopBar(title = "Reminders", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(
                onClick          = onNavigateToCreate,
                containerColor   = Green400,
                contentColor     = Surface0,
                shape            = RoundedCornerShape(Radius.full),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add reminder", modifier = Modifier.size(24.dp))
            }
        },
    ) { innerPadding ->
        when (val s = state) {
            is RemindersUiState.Loading -> RemindersSkeleton(Modifier.padding(innerPadding))
            is RemindersUiState.Error   -> FsErrorScreen(message = s.message, onRetry = { viewModel.load() })
            is RemindersUiState.Success -> {
                if (s.reminders.isEmpty()) {
                    FsEmptyState(
                        title    = "No reminders",
                        subtitle = "Add a reminder to nudge group members to settle up",
                        ctaText  = "Add reminder",
                        onCta    = onNavigateToCreate,
                    )
                } else {
                    LazyColumn(
                        modifier            = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        items(s.reminders, key = { it.id }) { reminder ->
                            ReminderRow(
                                reminder     = reminder,
                                onToggle     = { viewModel.toggleActive(reminder) },
                                onDelete     = { deleteTarget = reminder },
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderRow(
    reminder : Reminder,
    onToggle : () -> Unit,
    onDelete : () -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.xl))
            .background(Surface2)
            .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Green400.copy(alpha = 0.12f)),
        ) {
            Icon(Icons.Outlined.NotificationsActive, null, tint = Green400, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = reminder.frequency.name.lowercase()
                    .replaceFirstChar { it.uppercase() } + " reminder",
                fontSize   = 15.sp,
                fontWeight = FontWeight.Medium,
                color      = TextPrimary,
            )
            val detail = when (reminder.frequency) {
                ReminderFrequency.WEEKLY  -> reminder.dayOfWeek?.let { dayName(it) } ?: ""
                ReminderFrequency.MONTHLY -> reminder.dayOfMonth?.let { "Day $it" } ?: ""
                else -> ""
            }
            if (detail.isNotBlank()) {
                Text(text = detail, fontSize = 12.sp, color = TextSecondary)
            }
            val channels = buildList {
                if (reminder.notifyViaApp)   add("In-app")
                if (reminder.notifyViaEmail) add("Email")
            }.joinToString(" · ")
            if (channels.isNotBlank()) {
                Text(text = channels, fontSize = 12.sp, color = TextTertiary)
            }
        }
        Switch(
            checked         = reminder.isActive,
            onCheckedChange = { onToggle() },
            colors          = SwitchDefaults.colors(checkedThumbColor = Green400, checkedTrackColor = Green400.copy(alpha = 0.3f)),
        )
        Spacer(Modifier.width(Spacing.sm))
        FsIconButton(
            icon               = Icons.Outlined.Delete,
            contentDescription = "Delete",
            onClick            = onDelete,
            tint               = TextTertiary,
        )
    }
}

private fun dayName(dayOfWeek: Int): String = when (dayOfWeek) {
    1 -> "Monday" ; 2 -> "Tuesday" ; 3 -> "Wednesday" ; 4 -> "Thursday"
    5 -> "Friday" ; 6 -> "Saturday" ; 7 -> "Sunday" ; else -> ""
}

// ── Reminders skeleton ────────────────────────────────────────────────────────

@androidx.compose.runtime.Composable
private fun RemindersSkeleton(modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
    ) {
        repeat(4) { com.prathik.fairshare.ui.components.FsSkeletonTimelineRow() }
    }
}