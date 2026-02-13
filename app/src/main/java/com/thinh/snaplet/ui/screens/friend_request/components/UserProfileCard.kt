package com.thinh.snaplet.ui.screens.friend_request.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.R
import com.thinh.snaplet.ui.components.Avatar
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.components.PrimaryButton
import com.thinh.snaplet.ui.screens.friend_request.FriendRequestUiState

@Composable
internal fun UserProfileCard(
    modifier: Modifier = Modifier,
    state: FriendRequestUiState,
    onSendRequest: () -> Unit,
    onRefreshPending: () -> Unit = {},
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Avatar(
            avatarUrl = state.userProfile?.avatarUrl.orEmpty(),
            firstName = state.userProfile?.firstName.orEmpty(),
            size = 120.dp,
            isConnectedUser = false
        )

        Spacer(modifier = Modifier.height(16.dp))

        BaseText(
            text = state.userProfile?.displayName.orEmpty(),
            typography = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier.height(50.dp), contentAlignment = Alignment.Center
        ) {
            when {
                state.isCurrentUser -> {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.secondary, shape = CircleShape)
                            .width(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BaseText(
                            text = stringResource(R.string.you),
                            typography = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                state.isPending -> {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.secondary, shape = CircleShape)
                            .width(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                else -> {
                    PrimaryButton(
                        modifier = Modifier.fillMaxSize(),
                        onClick = onSendRequest,
                        title = stringResource(R.string.add_friend),
                        typography = MaterialTheme.typography.bodyLarge,
                        isLoading = state.isLoading,
                        contentPadding = PaddingValues(start = 8.dp, end = 16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        titleColor = Color.Black,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = Color.Black
                            )
                        })
                }
            }
        }
    }
}