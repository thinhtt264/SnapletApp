package com.thinh.snaplet.ui.overlay

sealed interface OverlayEvent {
    data class ShowBottomSheet(
        val content: BottomSheetContent
    ) : OverlayEvent

    data class ShowConfirmModal(val title: String?) : OverlayEvent
    object Dismiss : OverlayEvent
}
