package com.thinh.snaplet.ui.overlay

import androidx.compose.ui.graphics.Color
import com.thinh.snaplet.R
import com.thinh.snaplet.ui.common.UiText

sealed interface OverlayState {

    data object None : OverlayState

    sealed interface Visible : OverlayState {
        data class Modal(val content: ModalContent) : Visible

        data class BottomSheet(val content: BottomSheetContent) : Visible
    }
}

sealed interface ModalContent {

    data class ConfirmDialog(
        val title: UiText,
        val message: UiText,
        val confirmText: UiText,
        val cancelText: UiText?,
        val onConfirm: () -> Unit
    ) : ModalContent
}

sealed interface BottomSheetContent {

    data class Options(
        val options: List<SheetOption>
    ) : BottomSheetContent

    data class Confirm(
        val title: String,
        val message: String? = null,
        val confirmText: String,
        val cancelText: UiText = UiText.StringResource(R.string.cancel)
    ) : BottomSheetContent

    data class Info(
        val title: String, val description: String
    ) : BottomSheetContent
}

data class SheetOption(
    val id: String,
    val label: UiText,
    val color: Color? = null,
    val onClick: () -> Unit,
    val enabled: Boolean = true
)