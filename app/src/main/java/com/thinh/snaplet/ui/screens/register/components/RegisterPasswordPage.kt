package com.thinh.snaplet.ui.screens.register.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.R
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.components.FormTextField

@Composable
fun RegisterPasswordPage(
    username: String,
    password: String,
    passwordError: String?,
    isPasswordVisible: Boolean,
    isLoading: Boolean,
    onPasswordChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onRegister: () -> Unit,
    focusManager: FocusManager = LocalFocusManager.current
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    RegisterPageContent(
        title = stringResource(R.string.register_password_title),
        subtitle = username,
        subtitleColor = colorScheme.primary,
        buttonText = stringResource(R.string.create_account),
        buttonEnabled = !isLoading && passwordError == null,
        isLoading = isLoading,
        onButtonClick = onRegister,
        inputField = {
            FormTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = stringResource(R.string.password),
                placeholder = stringResource(R.string.password_placeholder),
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                onImeAction = {
                    focusManager.clearFocus()
                    onRegister()
                },
                errorMessage = passwordError,
                enabled = !isLoading,
                visualTransformation = if (isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(
                        onClick = onPasswordVisibilityToggle,
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = if (isPasswordVisible) {
                                Icons.Filled.VisibilityOff
                            } else {
                                Icons.Filled.Visibility
                            },
                            contentDescription = "Toggle Password Visibility",
                            tint = colorScheme.secondary
                        )
                    }
                },
                modifier = Modifier.focusRequester(focusRequester)
            )
        },
        extraContent = {
            AnimatedVisibility(
                visible = password.isNotEmpty() && password.length < 8,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                BaseText(
                    text = stringResource(R.string.password_requirement),
                    typography = typography.bodySmall,
                    color = colorScheme.onError,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }
    )
}