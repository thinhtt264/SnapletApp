package com.thinh.snaplet.ui.screens.register

sealed class RegisterUIEvent {

    data object RegisterSuccess : RegisterUIEvent()
    
    data class ShowErrorPopup(val message: String) : RegisterUIEvent()

    data object NavigateToLogin : RegisterUIEvent()
}

