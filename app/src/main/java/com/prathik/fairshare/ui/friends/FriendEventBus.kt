package com.prathik.fairshare.ui.friends

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton event bus that signals the FriendsScreen to reload its list.
 * Emitted by AddFriendViewModel after successfully adding any friend.
 * Collected by FriendsViewModel to trigger loadData().
 */
@Singleton
class FriendEventBus @Inject constructor() {
    private val _friendAdded = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val friendAdded = _friendAdded.asSharedFlow()

    fun notifyFriendAdded() {
        _friendAdded.tryEmit(Unit)
    }
}
