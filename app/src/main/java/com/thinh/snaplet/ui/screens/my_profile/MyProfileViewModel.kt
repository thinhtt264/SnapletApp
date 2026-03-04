package com.thinh.snaplet.ui.screens.my_profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.snaplet.R
import com.thinh.snaplet.data.repository.UserRepository
import com.thinh.snaplet.data.repository.auth.AuthRepository
import com.thinh.snaplet.platform.photo_picker.PhotoPickerManager
import com.thinh.snaplet.ui.common.UiText
import com.thinh.snaplet.ui.overlay.OverlayEventBus
import com.thinh.snaplet.ui.overlay.SheetOption
import com.thinh.snaplet.ui.theme.Red
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyProfileViewModel @Inject constructor(
    userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val photoPickerManager: PhotoPickerManager,
) : ViewModel() {

    private val _widgetChainEnabled = MutableStateFlow(true)
    private val _showPhotoPicker = MutableStateFlow(false)
    private val _selectedPhotoUri = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MyProfileUiState> = combine(
        userRepository.observeMyUserProfile(),
        _widgetChainEnabled,
        _showPhotoPicker,
        _selectedPhotoUri,
    ) { profile, chainEnabled, showPicker, selectedUri ->
        MyProfileUiState(
            displayName = profile?.displayName.orEmpty(),
            firstName = profile?.firstName.orEmpty(),
            avatarUrl = profile?.avatarUrl,
            userName = profile?.userName.orEmpty(),
            email = profile?.email.orEmpty(),
            widgetChainEnabled = chainEnabled,
            showPhotoPicker = showPicker,
            selectedPhotoUri = selectedUri,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MyProfileUiState()
    )

    fun onWidgetChainToggle() {
        _widgetChainEnabled.update { !it }
    }

    fun onPhotoPickerLaunched() {
        _showPhotoPicker.value = false
    }

    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch {
            val processedUri = photoPickerManager.processPickedImage(uri)
            if (processedUri != null) {
                _selectedPhotoUri.value = processedUri.toString()
            }
        }
    }

    fun onPhotoPickerDismissed() {
        _showPhotoPicker.value = false
    }

    private fun handlePickFromGallery() {
        _showPhotoPicker.value = true
    }

    private fun handleDeleteAvatar() {
        _selectedPhotoUri.value = null
    }

    private fun handleLogout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun onLogout() {
        val options = listOf(
            SheetOption(
                id = "logout",
                label = UiText.StringResource(R.string.logout),
                color = Red,
                onClick = ::handleLogout
            ), SheetOption(
                id = "cancel", label = UiText.StringResource(R.string.cancel), onClick = { })
        )

        OverlayEventBus.showOptionsSheet(
            options = options,
            title = UiText.StringResource(R.string.profile_logout_confirm_title),
        )
    }

    fun onEditPhoto() {
        val options = listOf(
            SheetOption(
                id = "pick_from_gallery",
                label = UiText.StringResource(R.string.pick_photo_from_gallery),
                onClick = ::handlePickFromGallery,
            ),
            SheetOption(
                id = "delete_avatar",
                label = UiText.StringResource(R.string.delete_avatar),
                color = Red,
                onClick = ::handleDeleteAvatar,
            ), SheetOption(
                id = "cancel", label = UiText.StringResource(R.string.cancel), onClick = { })
        )

        OverlayEventBus.showOptionsSheet(
            options = options,
            title = UiText.StringResource(R.string.profile_change_avatar_title),
        )
    }
}
