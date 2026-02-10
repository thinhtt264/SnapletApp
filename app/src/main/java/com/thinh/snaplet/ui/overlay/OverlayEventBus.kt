package com.thinh.snaplet.ui.overlay

import com.thinh.snaplet.ui.common.UiText
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
                BottomSheetContent.Options(options)
            )
        )
    }

    fun showConfirmSheet(content: BottomSheetContent.Confirm) {
        _events.tryEmit(
            OverlayEvent.ShowBottomSheet(content)
        )
    }

    fun showConfirmDialog(
        title: UiText,
        message: UiText,
        confirmText: UiText,
        cancelText: UiText? = null,
        onConfirm: () -> Unit
    ) {
        _events.tryEmit(
            OverlayEvent.ShowModal(
                ModalContent.ConfirmDialog(
                    title = title,
                    message = message,
                    confirmText = confirmText,
                    cancelText = cancelText,
                    onConfirm = onConfirm
                )
            )
        )
    }

    fun dismiss() {
        _events.tryEmit(OverlayEvent.Dismiss)
    }
}
