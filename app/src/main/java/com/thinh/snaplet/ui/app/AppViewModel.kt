package com.thinh.snaplet.ui.app

import AuthState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.snaplet.data.repository.auth.AuthRepository
import com.thinh.snaplet.data.repository.device.DeviceRepository
import com.thinh.snaplet.navigation.NavScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    private val _uiState: MutableStateFlow<AppUiState> = MutableStateFlow(AppUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<AppUiEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    val uiEvent = _uiEvent.asSharedFlow()

    private var isInitialized = false

    init {
        initializeApp()
    }

    private fun initializeApp() {
        viewModelScope.launch {
            try {
                deviceRepository.getOrCreateFingerprint()

                val isAuthenticated = authRepository.isAuthenticated()

                _uiState.update {
                    it.copy(
                        startDestination = if (isAuthenticated) {
                            NavScreen.HomeGraph.route
                        } else {
                            NavScreen.AuthGraph.route
                        }
                    )
                }

            } catch (_: Exception) {
                _uiState.update { it.copy(startDestination = NavScreen.AuthGraph.route) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
                isInitialized = true
            }
        }

        observeAuthState()
    }

    private fun observeAuthState() {
        authRepository.authState
            .onEach { authState ->
                if (!isInitialized) return@onEach

                when (authState) {
                    is AuthState.Authenticated -> {
                        _uiEvent.emit(AppUiEvent.NavigateToHomeGraph)
                    }

                    is AuthState.Unauthenticated -> {
                        _uiEvent.emit(AppUiEvent.NavigateToAuthGraph)
                    }
                }
            }
            .catch { e ->
                if (isInitialized) {
                    _uiEvent.emit(AppUiEvent.NavigateToAuthGraph)
                }
            }
            .launchIn(viewModelScope)
    }
}