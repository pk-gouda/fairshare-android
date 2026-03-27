package com.prathik.fairshare.ui.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.usecase.notification.GetUnreadCountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the main shell — scoped to the entire app session.
 *
 * Manages the unread notification count shown as a badge
 * on the Activity tab in the bottom navigation bar.
 *
 * Polling strategy: fetches count every 30 seconds while app is active.
 * Will be replaced with WebSocket push updates on Day 21.
 */
@HiltViewModel
class MainShellViewModel @Inject constructor(
    private val getUnreadCountUseCase: GetUnreadCountUseCase,
) : ViewModel() {

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    init {
        startPollingUnreadCount()
    }

    /**
     * Polls unread notification count every 30 seconds.
     * Silently ignores errors — badge just stays at last known value.
     */
    private fun startPollingUnreadCount() {
        viewModelScope.launch {
            while (true) {
                fetchUnreadCount()
                delay(30_000L)
            }
        }
    }

    /**
     * Fetches unread count immediately.
     * Called after marking notifications as read on ActivityScreen.
     */
    fun refreshUnreadCount() {
        viewModelScope.launch {
            fetchUnreadCount()
        }
    }

    /**
     * Called by WebSocket events (Day 21) when a new notification arrives
     * while the app is in the foreground — increments badge immediately
     * without a network call.
     */
    fun incrementUnreadCount() {
        _unreadCount.value = _unreadCount.value + 1
    }

    /**
     * Called when user marks all notifications as read.
     * Clears the badge immediately without waiting for next poll.
     */
    fun clearUnreadCount() {
        _unreadCount.value = 0
    }

    private suspend fun fetchUnreadCount() {
        when (val result = getUnreadCountUseCase()) {
            is ApiResult.Success -> _unreadCount.value = result.data
            else -> Unit // Silently ignore — badge stays at last known value
        }
    }
}