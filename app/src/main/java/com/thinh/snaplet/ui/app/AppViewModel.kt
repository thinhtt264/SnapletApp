package com.thinh.snaplet.ui.app

import AuthState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.snaplet.data.repository.UserRepository
import com.thinh.snaplet.data.repository.auth.AuthRepository
import com.thinh.snaplet.data.repository.device.DeviceRepository
import com.thinh.snaplet.navigation.NavScreen
import com.thinh.snaplet.platform.deeplink.DeepLinkEvent
import com.thinh.snaplet.platform.deeplink.DeepLinkManager
import com.thinh.snaplet.ui.overlay.ModalContent
import com.thinh.snaplet.ui.overlay.OverlayEventBus
import com.thinh.snaplet.ui.screens.friend_request.FriendRequestUiState
import com.thinh.snaplet.utils.Logger
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
    private val deviceRepository: DeviceRepository,
    private val deepLinkManager: DeepLinkManager,
    private val userRepository: UserRepository
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
        observeDeepLinkEvents()
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

    private fun observeDeepLinkEvents() {
        viewModelScope.launch {
            deepLinkManager.events.collect { event ->
                if (event is DeepLinkEvent.FriendRequest) {
                    handleFriendRequestDeepLink(event.userName)
                }
            }
        }
    }

    private suspend fun handleFriendRequestDeepLink(userName: String) {
        deviceRepository.getOrCreateFingerprint()
        val profileResult = userRepository.getUserProfile(userName)
        profileResult.fold(
            onSuccess = { userProfile ->
                val state = FriendRequestUiState(userProfile = userProfile)
                OverlayEventBus.showModal(ModalContent.FriendRequest(state = state))
            },
            onFailure = { error ->
                Logger.e("❌ Failed to load user profile: ${error.message}")
            }
        )
    }
}