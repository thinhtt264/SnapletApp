package com.thinh.snaplet.ui.overlay

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object OverlayEventBus {

    private val _events = MutableSharedFlow<OverlayEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    internal val events: Flow<OverlayEvent> = _events.asSharedFlow()

    fun showOptionsSheet(options: List<SheetOption>) {
        _events.tryEmit(
            OverlayEvent.ShowBottomSheet(
                BottomSheetContent.Options(options = options)
            )
        )
    }

    fun showConfirmSheet(content: BottomSheetContent.Confirm) {
        _events.tryEmit(
            OverlayEvent.ShowBottomSheet(content)
        )
    }

    fun showConfirmModal(title: String? = null) {
        _events.tryEmit(OverlayEvent.ShowConfirmModal(title))
    }

    fun dismiss() {
        _events.tryEmit(OverlayEvent.Dismiss)
    }
}
