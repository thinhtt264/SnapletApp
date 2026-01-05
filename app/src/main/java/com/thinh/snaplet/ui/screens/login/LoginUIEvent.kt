package com.thinh.snaplet.ui.screens.login

sealed class LoginUIEvent {

    data object LoginSuccess : LoginUIEvent()
    
    data class ShowErrorPopup(val message: String) : LoginUIEvent()

    data object NavigateToRegister : LoginUIEvent()
}

