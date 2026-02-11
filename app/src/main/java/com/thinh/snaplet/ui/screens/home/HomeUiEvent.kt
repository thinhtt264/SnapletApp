package com.thinh.snaplet.ui.screens.home

import com.thinh.snaplet.platform.permission.Permission
import com.thinh.snaplet.ui.common.UiText

sealed interface HomeUiEvent {

    data class RequestPermission(val permission: Permission) : HomeUiEvent

    data class ShowError(val message: UiText) : HomeUiEvent

    data class ShowSuccess(val message: UiText) : HomeUiEvent

    object ScrollToFirstPost : HomeUiEvent
}

