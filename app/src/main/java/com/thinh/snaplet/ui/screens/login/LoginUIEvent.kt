package com.thinh.snaplet.ui.screens.login

import com.thinh.snaplet.ui.common.UiText

sealed interface LoginUIEvent {

    data object LoginSuccess : LoginUIEvent

    data class ShowErrorPopup(val message: UiText) : LoginUIEvent

    data object NavigateToRegister : LoginUIEvent
}

