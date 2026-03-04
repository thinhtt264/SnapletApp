package com.thinh.snaplet.ui.screens.my_profile

data class MyProfileUiState(
    val displayName: String = "",
    val firstName: String = "",
    val avatarUrl: String? = null,
    val userName: String = "",
    val email: String = "",
    val widgetChainEnabled: Boolean = true,
)
