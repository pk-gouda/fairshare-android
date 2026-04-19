package com.prathik.fairshare.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.model.Notification
import com.prathik.fairshare.domain.usecase.group.GetDeletedGroupsUseCase
import com.prathik.fairshare.domain.usecase.group.RestoreGroupUseCase
import com.prathik.fairshare.domain.usecase.notification.GetNotificationsUseCase
import com.prathik.fairshare.domain.usecase.notification.MarkAllReadUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val getNotificationsUseCase: GetNotificationsUseCase,
    private val markAllReadUseCase: MarkAllReadUseCase,
    private val getDeletedGroupsUseCase: GetDeletedGroupsUseCase,
    private val restoreGroupUseCase: RestoreGroupUseCase,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _actionState = MutableStateFlow<ActivityActionState>(ActivityActionState.Idle)
    val actionState: StateFlow<ActivityActionState> = _actionState.asStateFlow()

    private val _deletedGroups = MutableStateFlow<List<Group>>(emptyList())
    val deletedGroups: StateFlow<List<Group>> = _deletedGroups.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            // Only show spinner on first load
            if (_notifications.value.isEmpty()) _isLoading.value = true
            when (val result = getNotificationsUseCase()) {
                is ApiResult.Success -> _notifications.value = result.data
                else -> Unit
            }
            when (val result = getDeletedGroupsUseCase()) {
                is ApiResult.Success -> _deletedGroups.value = result.data
                else -> Unit
            }
            _isLoading.value = false
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            when (markAllReadUseCase()) {
                is ApiResult.Success -> {
                    // Mark all as read locally
                    _notifications.value = _notifications.value.map { it.copy(isRead = true) }
                    _actionState.value = ActivityActionState.Success("All marked as read")
                }

                else -> _actionState.value = ActivityActionState.Error("Failed to mark as read")
            }
        }
    }

    fun restoreGroup(groupId: String) {
        viewModelScope.launch {
            _actionState.value = ActivityActionState.Loading
            when (restoreGroupUseCase(groupId)) {
                is ApiResult.Success -> {
                    // Remove from deleted list
                    _deletedGroups.value = _deletedGroups.value.filter { it.id != groupId }
                    _actionState.value = ActivityActionState.GroupRestored
                }
                is ApiResult.Forbidden -> _actionState.value =
                    ActivityActionState.Error("Only the group creator can restore")
                is ApiResult.Conflict -> _actionState.value =
                    ActivityActionState.Error("Restore window has expired (30 days)")
                else -> _actionState.value =
                    ActivityActionState.Error("Failed to restore group")
            }
        }
    }

    fun resetActionState() {
        _actionState.value = ActivityActionState.Idle
    }

    /**
     * Groups notifications into Today / Yesterday / Earlier buckets.
     */
    fun groupedNotifications(): Map<String, List<Notification>> {
        val zoneId = java.time.ZoneId.systemDefault()
        val today = java.time.LocalDate.now(zoneId)
        val yesterday = today.minusDays(1)

        return _notifications.value.groupBy { notification ->
            try {
                // Parse as UTC, convert to device local time for grouping
                val instant = try {
                    java.time.Instant.parse(notification.createdAt)
                } catch (e: Exception) {
                    java.time.LocalDateTime.parse(
                        notification.createdAt,
                        java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
                    ).toInstant(java.time.ZoneOffset.UTC)
                }
                val localDate = instant.atZone(zoneId).toLocalDate()
                when (localDate) {
                    today -> "Today"
                    yesterday -> "Yesterday"
                    else -> "Earlier"
                }
            } catch (e: Exception) {
                "Earlier"
            }
        }
    }

    val hasUnread: Boolean
        get() = _notifications.value.any { !it.isRead }
}

sealed class ActivityActionState {
    object Idle : ActivityActionState()
    object Loading : ActivityActionState()
    object GroupRestored : ActivityActionState()
    data class Success(val message: String) : ActivityActionState()
    data class Error(val message: String) : ActivityActionState()
}