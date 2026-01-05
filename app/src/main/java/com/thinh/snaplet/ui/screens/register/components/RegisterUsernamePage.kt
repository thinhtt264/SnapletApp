package com.thinh.snaplet.ui.screens.register.components

import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.thinh.snaplet.R
import com.thinh.snaplet.ui.components.AuthPageContent
import com.thinh.snaplet.ui.components.FormTextField

@Composable
fun RegisterUsernamePage(
    email: String,
    username: String,
    usernameError: String?,
    isLoading: Boolean,
    onUsernameChange: (String) -> Unit,
    onContinue: () -> Unit,
    focusManager: FocusManager = LocalFocusManager.current
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AuthPageContent(
        title = stringResource(R.string.register_username_title),
        subtitle = email,
        subtitleColor = colorScheme.primary,
        buttonText = stringResource(R.string.continue_text),
        isLoading = isLoading,
        onButtonClick = onContinue,
        inputField = {
            FormTextField(
                value = username,
                onValueChange = onUsernameChange,
                label = stringResource(R.string.username),
                placeholder = stringResource(R.string.username_placeholder),
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
                onImeAction = {
                    focusManager.clearFocus()
                    onContinue()
                },
                errorMessage = usernameError,
                enabled = !isLoading,
                modifier = Modifier.focusRequester(focusRequester)
            )
        }
    )
}

