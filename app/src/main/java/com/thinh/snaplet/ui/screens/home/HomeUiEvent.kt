package com.thinh.snaplet.ui.screens.home

import com.thinh.snaplet.ui.common.UiText
import com.thinh.snaplet.utils.permission.Permission

sealed interface HomeUiEvent {

    data class RequestPermission(val permission: Permission) : HomeUiEvent

    data class ShowError(val message: UiText) : HomeUiEvent

    data class ShowSuccess(val message: UiText) : HomeUiEvent

    object ScrollToFirstPost : HomeUiEvent
}

