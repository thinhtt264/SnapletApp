package com.thinh.snaplet.ui.screens.home

import com.thinh.snaplet.utils.permission.Permission

sealed interface HomeUiEvent {
    /**
     * Request permission from user
     * View will show Android system permission dialog
     */
    data class RequestPermission(val permission: Permission) : HomeUiEvent

    /**
     * Show error message to user
     */
    data class ShowError(val message: String) : HomeUiEvent

    /**
     * Show success message to user
     */
    data class ShowSuccess(val message: String) : HomeUiEvent

    /**
     * Scroll to first post (after camera page)
     * Used when temp post is created to show it immediately
     */
    object ScrollToFirstPost : HomeUiEvent
}

