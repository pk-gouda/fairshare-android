package com.prathik.fairshare.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.model.Notification
import com.prathik.fairshare.domain.usecase.group.GetDeletedGroupsUseCase
import com.prathik.fairshare.domain.usecase.group.RestoreGroupUseCase
import com.prathik.fairshare.domain.repository.NotificationRepository
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

enum class ActivityFilter { ALL, EXPENSES, SETTLEMENTS, GROUPS }

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val getNotificationsUseCase: GetNotificationsUseCase,
    private val notificationRepository: NotificationRepository,
    private val markAllReadUseCase: MarkAllReadUseCase,
    private val getDeletedGroupsUseCase: GetDeletedGroupsUseCase,
    private val restoreGroupUseCase: RestoreGroupUseCase,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _manualRefreshing = MutableStateFlow(false)
    val manualRefreshing: StateFlow<Boolean> = _manualRefreshing.asStateFlow()

    private val _activityLoaded = MutableStateFlow(false)
    val activityLoaded: StateFlow<Boolean> = _activityLoaded.asStateFlow()

    private val _activityLoadFailed = MutableStateFlow(false)
    val activityLoadFailed: StateFlow<Boolean> = _activityLoadFailed.asStateFlow()

    private var initialLoadDone = false

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _selectedFilter = MutableStateFlow(ActivityFilter.ALL)
    val selectedFilter: StateFlow<ActivityFilter> = _selectedFilter.asStateFlow()

    fun setFilter(filter: ActivityFilter) { _selectedFilter.value = filter }

    private val _actionState = MutableStateFlow<ActivityActionState>(ActivityActionState.Idle)
    val actionState: StateFlow<ActivityActionState> = _actionState.asStateFlow()

    private val _deletedGroups = MutableStateFlow<List<Group>>(emptyList())
    val deletedGroups: StateFlow<List<Group>> = _deletedGroups.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            initialLoadDone = false

            // Step 1: Render cached notifications immediately.
            val cached = notificationRepository.getCachedNotifications().stableSorted()
            if (cached.isNotEmpty()) {
                _notifications.value = cached
                _activityLoaded.value = true
                _activityLoadFailed.value = false
            } else {
                _isLoading.value = true
            }

            // Step 2: Network refresh.
            when (val result = getNotificationsUseCase()) {
                is ApiResult.Success -> {
                    _notifications.value = result.data.stableSorted()
                    _activityLoaded.value = true
                    _activityLoadFailed.value = false
                }
                is ApiResult.NetworkError -> {
                    if (!_activityLoaded.value) _activityLoadFailed.value = true
                }
                else -> {
                    if (!_activityLoaded.value) _activityLoadFailed.value = true
                }
            }
            when (val result = getDeletedGroupsUseCase()) {
                is ApiResult.Success -> _deletedGroups.value = result.data
                else -> Unit
            }
            _isLoading.value = false
            initialLoadDone = true
        }
    }

    fun refresh(manual: Boolean = false) {
        if (!initialLoadDone) return
        viewModelScope.launch {
            if (manual) _manualRefreshing.value = true
            try {
                when (val result = getNotificationsUseCase()) {
                    is ApiResult.Success -> {
                        _notifications.value = result.data.stableSorted()
                        _activityLoaded.value = true
                        _activityLoadFailed.value = false
                    }
                    else -> Unit  // keep cached data visible
                }
                when (val result = getDeletedGroupsUseCase()) {
                    is ApiResult.Success -> _deletedGroups.value = result.data
                    else -> Unit
                }
            } finally {
                if (manual) _manualRefreshing.value = false
            }
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
                    ActivityActionState.Error("You must be a member of this group to restore it")
                is ApiResult.Conflict -> _actionState.value =
                    ActivityActionState.Error("Restore window has expired (60 days)")
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
    /**
     * Groups a provided notification list into Today / Yesterday / Earlier buckets.
     * Accepting the list explicitly (rather than reading _notifications.value internally)
     * lets Compose key recomposition on the list change — avoids stale grouped result.
     */
    fun groupedNotifications(
        notifications: List<Notification>,
        filter: ActivityFilter = ActivityFilter.ALL,
    ): Map<String, List<Notification>> {
        val zoneId = java.time.ZoneId.systemDefault()
        val today = java.time.LocalDate.now(zoneId)
        val yesterday = today.minusDays(1)

        val filtered = when (filter) {
            ActivityFilter.ALL -> notifications
            ActivityFilter.EXPENSES -> notifications.filter { it.type.name.startsWith("EXPENSE") }
            ActivityFilter.SETTLEMENTS -> notifications.filter { it.type.name.startsWith("SETTLEMENT") }
            ActivityFilter.GROUPS -> notifications.filter {
                it.type.name.startsWith("GROUP") || it.type.name.startsWith("PLACEHOLDER")
            }
        }

        return filtered.groupBy { notification ->
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
}

// ── Stable sort for activity rows ────────────────────────────────────────────

/** Sort notifications by createdAt DESC then id ASC — deterministic across refreshes. */
private fun List<Notification>.stableSorted(): List<Notification> =
    sortedWith(
        compareByDescending<Notification> { it.createdAt }
            .thenBy { it.id }
    )

sealed class ActivityActionState {
    object Idle : ActivityActionState()
    object Loading : ActivityActionState()
    object GroupRestored : ActivityActionState()
    data class Success(val message: String) : ActivityActionState()
    data class Error(val message: String) : ActivityActionState()
}