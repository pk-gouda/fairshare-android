package com.prathik.fairshare.ui.friends

import com.prathik.fairshare.domain.model.Friend
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

data class FriendAddedEvent(
    val updatedList : List<Friend>,
    val addedCount  : Int,
)

@Singleton
class FriendEventBus @Inject constructor() {
    private val _friendAdded = MutableSharedFlow<FriendAddedEvent>(extraBufferCapacity = 1)
    val friendAdded = _friendAdded.asSharedFlow()

    fun notifyFriendAdded(updatedList: List<Friend>, addedCount: Int) {
        _friendAdded.tryEmit(FriendAddedEvent(updatedList, addedCount))
    }
}