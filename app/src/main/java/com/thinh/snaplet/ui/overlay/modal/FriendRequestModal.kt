package com.thinh.snaplet.ui.overlay.modal

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import com.thinh.snaplet.ui.overlay.ModalContent
import com.thinh.snaplet.ui.screens.friend_request.FriendRequestOverlayContent

@Composable
internal fun FriendRequestModal(
    content: ModalContent.FriendRequest,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
    ) {
        FriendRequestOverlayContent(state = content.state)
    }
}