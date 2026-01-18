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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

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

                _startDestination.value =
                    if (isAuthenticated) {
                        NavScreen.HomeGraph.route
                    } else {
                        NavScreen.AuthGraph.route
                    }
            } catch (_: Exception) {
                _startDestination.value = NavScreen.AuthGraph.route
            } finally {
                _isLoading.value = false
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