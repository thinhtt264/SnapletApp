package com.thinh.snaplet.ui.screens.friend_request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.snaplet.data.model.RelationshipStatus
import com.thinh.snaplet.data.model.UserProfile
import com.thinh.snaplet.data.repository.UserRepository
import com.thinh.snaplet.ui.overlay.OverlayEventBus
import com.thinh.snaplet.utils.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendRequestViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendRequestUiState(userProfile = null))
    val uiState: StateFlow<FriendRequestUiState> = _uiState.asStateFlow()

    fun loadUser(userProfile: UserProfile?) {
        viewModelScope.launch {
            _uiState.update { it.copy(userProfile = userProfile, isLoading = true) }

            val currentUser = userRepository.getCurrentUserProfile()
            val isCurrentUser = currentUser?.id == userProfile?.id

            val relationship = userRepository.getRelationshipWithUser(userProfile?.id ?: "")
                .fold(onSuccess = { it }, onFailure = { null })

            val isPending = relationship?.status?.equals(
                RelationshipStatus.PENDING.value,
                ignoreCase = true
            ) == true

            _uiState.update {
                it.copy(
                    isCurrentUser = isCurrentUser,
                    isPending = isPending,
                    isLoading = false
                )
            }
        }
    }

    fun sendFriendRequest() {
        val userId = _uiState.value.userProfile?.id ?: return
        if (_uiState.value.isRequesting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isRequesting = true) }
            userRepository.sendFriendRequest(userId).fold(
                onSuccess = {
                    _uiState.update { it.copy(isRequesting = false) }
                },
                onFailure = { error ->
                    Logger.e("❌ Failed to send friend request: ${error.message}")
                    _uiState.update { it.copy(isRequesting = false) }
                }
            )
        }
    }

    fun refreshPending() {
        if (_uiState.value.isRequesting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRequesting = true) }
            delay(1500L)
            _uiState.update { it.copy(isRequesting = false) }
        }
    }

    fun dismiss() {
        OverlayEventBus.dismiss()
    }
}