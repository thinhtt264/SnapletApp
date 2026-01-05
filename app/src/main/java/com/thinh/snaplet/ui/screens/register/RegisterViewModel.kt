package com.thinh.snaplet.ui.screens.register

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.snaplet.R
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.UiText
import com.thinh.snaplet.utils.ValidationConstants
import com.thinh.snaplet.utils.safeMessage
import com.thinh.snaplet.data.repository.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<RegisterUIEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    val uiEvent: SharedFlow<RegisterUIEvent> = _uiEvent.asSharedFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, errorMessage = null) }
    }

    fun onUsernameChange(username: String) {
        _uiState.update { it.copy(username = username, usernameError = null, errorMessage = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null, errorMessage = null) }
    }

    fun onPasswordVisibilityToggle() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onScrollToPage(step: RegisterStep) {
        val currentState = _uiState.value
        if (currentState.currentStep != step && !currentState.isLoading) {
            when (step) {
                RegisterStep.EMAIL -> {
                    _uiState.update {
                        it.copy(
                            currentStep = RegisterStep.EMAIL,
                            username = "",
                            usernameError = null,
                            password = "",
                            passwordError = null,
                            errorMessage = null
                        )
                    }
                }

                RegisterStep.USERNAME -> {
                    _uiState.update {
                        it.copy(
                            currentStep = RegisterStep.USERNAME,
                            password = "",
                            passwordError = null,
                            errorMessage = null
                        )
                    }
                }

                RegisterStep.PASSWORD -> {
                    _uiState.update {
                        it.copy(
                            currentStep = RegisterStep.PASSWORD,
                            errorMessage = null
                        )
                    }
                }
            }
        }
    }

    fun onContinueFromEmail() {
        viewModelScope.launch {
            val currentState = _uiState.value

            if (currentState.currentStep != RegisterStep.EMAIL || currentState.isLoading) {
                return@launch
            }

            val emailError = validateEmail(currentState.email)
            if (emailError != null) {
                _uiState.update { it.copy(emailError = emailError) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, emailError = null, errorMessage = null) }

            try {
                val stateAfterDelay = _uiState.value
                if (stateAfterDelay.currentStep != RegisterStep.EMAIL) {
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }

                val result = authRepository.checkEmailAvailability(currentState.email)
                
                result.onSuccess { isAvailable ->
                    if (!isAvailable) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                emailError = UiText.StringResource(R.string.email_already_taken)
                            )
                        }
                        return@launch
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentStep = RegisterStep.USERNAME
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            emailError = UiText.DynamicString(error.safeMessage)
                        )
                    }
                }
            } catch (e: Exception) {
                Logger.e("❌ Email check failed: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        emailError = UiText.DynamicString(e.safeMessage)
                    )
                }
            }
        }
    }

    fun onContinueFromUsername() {
        viewModelScope.launch {
            val currentState = _uiState.value

            Logger.d("onContinueFromUsername called: step=${currentState.currentStep}, isLoading=${currentState.isLoading}")

            // Kiểm tra nếu đã không ở step USERNAME hoặc đang loading thì không xử lý
            if (currentState.currentStep != RegisterStep.USERNAME || currentState.isLoading) {
                Logger.d("onContinueFromUsername: Skipping - step=${currentState.currentStep}, isLoading=${currentState.isLoading}")
                return@launch
            }

            val usernameError = validateUsername(currentState.username)
            if (usernameError != null) {
                _uiState.update { it.copy(usernameError = usernameError) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, usernameError = null, errorMessage = null) }

            try {
                // Mock API call to check username availability
                delay(1000) // Simulate network delay

                // Kiểm tra lại step sau delay để đảm bảo vẫn ở USERNAME step
                val stateAfterDelay = _uiState.value
                if (stateAfterDelay.currentStep != RegisterStep.USERNAME) {
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }

                val isUsernameAvailable = checkUsernameAvailable(currentState.username)

                if (!isUsernameAvailable) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            usernameError = UiText.StringResource(R.string.username_already_taken)
                        )
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentStep = RegisterStep.PASSWORD
                    )
                }
            } catch (e: Exception) {
                Logger.e("❌ Username check failed: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        usernameError = UiText.DynamicString(e.safeMessage)
                    )
                }
            }
        }
    }

    fun onRegister() {
        viewModelScope.launch {
            val currentState = _uiState.value

            // Validate password
            val passwordError = validatePassword(currentState.password)
            if (passwordError != null) {
                _uiState.update { it.copy(passwordError = passwordError) }
                return@launch
            }

            // Start loading
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // Mock API call to register
                delay(1500) // Simulate network delay

                // Mock registration logic
                Logger.d("✅ Registration successful for: ${currentState.email}")

                _uiState.update { it.copy(isLoading = false) }
                _uiEvent.emit(RegisterUIEvent.RegisterSuccess)

            } catch (e: Exception) {
                Logger.e("❌ Registration exception: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = UiText.DynamicString(e.safeMessage)
                    )
                }
            }
        }
    }

    fun onNavigateToLogin() {
        viewModelScope.launch {
            _uiEvent.emit(RegisterUIEvent.NavigateToLogin)
        }
    }

    private fun validateEmail(email: String): UiText? {
        return when {
            email.isBlank() -> UiText.StringResource(R.string.email_required)
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                UiText.StringResource(R.string.email_invalid)

            else -> null
        }
    }

    private fun validateUsername(username: String): UiText? {
        return when {
            username.isBlank() -> UiText.StringResource(R.string.username_required)
            username.length < ValidationConstants.USERNAME_MIN_LENGTH -> UiText.StringResource(R.string.username_requirement1)
            username.length > ValidationConstants.USERNAME_MAX_LENGTH -> UiText.StringResource(R.string.username_requirement2)
            !username.matches(ValidationConstants.USERNAME_PATTERN) -> UiText.StringResource(R.string.username_invalid_characters)
            else -> null
        }
    }

    private fun validatePassword(password: String): UiText? {
        return when {
            password.isBlank() -> UiText.StringResource(R.string.password_required)
            password.length < ValidationConstants.PASSWORD_MIN_LENGTH -> UiText.StringResource(R.string.password_requirement)
            else -> null
        }
    }


    private suspend fun checkUsernameAvailable(username: String): Boolean {
        // Mock: return false for specific usernames to simulate taken usernames
        val takenUsernames = listOf("admin", "test", "user")
        return !takenUsernames.contains(username.lowercase())
    }
}

