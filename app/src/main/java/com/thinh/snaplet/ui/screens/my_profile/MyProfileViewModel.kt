package com.thinh.snaplet.ui.screens.my_profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.snaplet.R
import com.thinh.snaplet.data.repository.UserRepository
import com.thinh.snaplet.data.repository.auth.AuthRepository
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
) : ViewModel() {

    private val _widgetChainEnabled = MutableStateFlow(true)

    val uiState: StateFlow<MyProfileUiState> = combine(
        userRepository.observeMyUserProfile(), _widgetChainEnabled
    ) { profile, chainEnabled ->
        MyProfileUiState(
            displayName = profile?.displayName.orEmpty(),
            firstName = profile?.firstName.orEmpty(),
            avatarUrl = profile?.avatarUrl,
            userName = profile?.userName.orEmpty(),
            email = profile?.email.orEmpty(),
            widgetChainEnabled = chainEnabled,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MyProfileUiState()
    )

    fun onWidgetChainToggle() {
        _widgetChainEnabled.update { !it }
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
}