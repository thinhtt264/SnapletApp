package com.thinh.snaplet.ui.screens.login

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.snaplet.R
import com.thinh.snaplet.data.model.UserProfile
import com.thinh.snaplet.data.repository.UserRepository
import com.thinh.snaplet.data.repository.auth.AuthRepository
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.UiText
import com.thinh.snaplet.utils.safeMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    userRepository: UserRepository
) : ViewModel() {

    private val _currentUserProfile: Flow<UserProfile?> = userRepository.observeMyUserProfile()
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        observeUserProfile()
    }

    private fun observeUserProfile() {
        viewModelScope.launch {
            _currentUserProfile.collect { profile ->
                profile?.let { _uiState.update { it.copy(email = profile.email) } }
            }
        }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, errorMessage = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null, errorMessage = null) }
    }

    fun onPasswordVisibilityToggle() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onBackToEmailStep() {
        _uiState.update {
            it.copy(
                currentStep = LoginStep.EMAIL,
                password = "",
                passwordError = null,
                errorMessage = null
            )
        }
    }

    fun onContinueFromEmail() {
        viewModelScope.launch {
            val currentState = _uiState.value

            val emailError = validateEmail(currentState.email)
            if (emailError != null) {
                _uiState.update { it.copy(emailError = emailError) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                _uiState.update {
                    it.copy(
                        isLoading = false, currentStep = LoginStep.PASSWORD
                    )
                }

            } catch (e: Exception) {
                Logger.e("❌ Email validation failed: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false, emailError = UiText.DynamicString(e.safeMessage)
                    )
                }
            }
        }
    }

    fun onLogin(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val currentState = _uiState.value

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val result = authRepository.login(
                    email = currentState.email,
                    password = currentState.password
                )

                if (result.isSuccess) {
                    val userProfile: UserProfile = result.getOrThrow()
                    Logger.d("✅ Login successful for: ${userProfile.displayName}")

                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = UiText.DynamicString(
                                result.exceptionOrNull()?.message ?: "Unknown error"
                            )
                        )
                    }
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = UiText.DynamicString(e.safeMessage)
                    )
                }
            }
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
}