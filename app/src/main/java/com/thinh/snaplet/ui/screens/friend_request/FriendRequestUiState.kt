package com.thinh.snaplet.ui.screens.friend_request

import com.thinh.snaplet.data.model.UserProfile

data class FriendRequestUiState(
    val userProfile: UserProfile? = null,
    val isCurrentUser: Boolean = false,
    val isPending: Boolean = false,
    val isRequesting: Boolean = false,
    val isLoading: Boolean = true,
)