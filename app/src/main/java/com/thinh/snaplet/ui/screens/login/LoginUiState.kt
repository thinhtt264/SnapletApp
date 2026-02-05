package com.thinh.snaplet.ui.screens.login

import com.thinh.snaplet.ui.screens.common.UiText

enum class LoginStep {
    EMAIL,
    PASSWORD
}

data class LoginUiState(
    val currentStep: LoginStep = LoginStep.EMAIL,
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
    val emailError: UiText? = null,
    val passwordError: UiText? = null,
    val isPasswordVisible: Boolean = false
)