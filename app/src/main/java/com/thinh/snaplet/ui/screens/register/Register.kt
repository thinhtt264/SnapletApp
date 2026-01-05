package com.thinh.snaplet.ui.screens.register

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import animateVisibility
import com.thinh.snaplet.ui.components.StepAnimatedContent
import com.thinh.snaplet.ui.screens.register.components.RegisterEmailPage
import com.thinh.snaplet.ui.screens.register.components.RegisterPasswordPage
import com.thinh.snaplet.ui.screens.register.components.RegisterUsernamePage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Register(
    onRegisterSuccess: () -> Unit,
    onLoginClick: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                RegisterUIEvent.RegisterSuccess -> onRegisterSuccess()
                RegisterUIEvent.NavigateToLogin -> onLoginClick()
            }
        }
    }

    fun onGoBack() {
        when (uiState.currentStep) {
            RegisterStep.USERNAME -> viewModel.onScrollToPage(RegisterStep.EMAIL)
            RegisterStep.PASSWORD -> viewModel.onScrollToPage(RegisterStep.USERNAME)
            RegisterStep.EMAIL -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .imePadding()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            IconButton(
                onClick = ::onGoBack,
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .padding(16.dp)
                    .size(48.dp)
                    .animateVisibility(uiState.currentStep != RegisterStep.EMAIL)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colorScheme.onBackground
                )
            }
        }

        StepAnimatedContent(
            currentStep = uiState.currentStep,
            stepOrder = { step ->
                when (step) {
                    RegisterStep.EMAIL -> 0
                    RegisterStep.USERNAME -> 1
                    RegisterStep.PASSWORD -> 2
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
        ) { step ->
            when (step) {
                RegisterStep.EMAIL -> RegisterEmailPage(
                    email = uiState.email,
                    emailError = uiState.emailError?.asString(context),
                    isLoading = uiState.isLoading,
                    onEmailChange = viewModel::onEmailChange,
                    onContinue = viewModel::onContinueFromEmail,
                    onLoginClick = viewModel::onNavigateToLogin
                )

                RegisterStep.USERNAME -> RegisterUsernamePage(
                    email = uiState.email,
                    username = uiState.username,
                    usernameError = uiState.usernameError?.asString(context),
                    isLoading = uiState.isLoading,
                    onUsernameChange = viewModel::onUsernameChange,
                    onContinue = viewModel::onContinueFromUsername
                )

                RegisterStep.PASSWORD -> RegisterPasswordPage(
                    username = uiState.username,
                    password = uiState.password,
                    passwordError = uiState.passwordError?.asString(context),
                    isPasswordVisible = uiState.isPasswordVisible,
                    errorMessage = uiState.errorMessage?.asString(context),
                    isLoading = uiState.isLoading,
                    onPasswordChange = viewModel::onPasswordChange,
                    onPasswordVisibilityToggle = viewModel::onPasswordVisibilityToggle,
                    onRegister = viewModel::onRegister
                )
            }
        }
    }
}