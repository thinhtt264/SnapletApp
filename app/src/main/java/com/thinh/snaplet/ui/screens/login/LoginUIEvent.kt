package com.thinh.snaplet.ui.screens.login

import com.thinh.snaplet.utils.UiText

sealed class LoginUIEvent {

    data object LoginSuccess : LoginUIEvent()
    
    data class ShowErrorPopup(val message: UiText) : LoginUIEvent()

    data object NavigateToRegister : LoginUIEvent()
}

