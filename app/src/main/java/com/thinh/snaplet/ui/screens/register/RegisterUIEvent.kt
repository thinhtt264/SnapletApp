package com.thinh.snaplet.ui.screens.register

sealed class RegisterUIEvent {

    data object RegisterSuccess : RegisterUIEvent()
    

    data object NavigateToLogin : RegisterUIEvent()
}

