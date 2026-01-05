package com.thinh.snaplet.ui.screens.register.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.ui.components.AppText
import com.thinh.snaplet.ui.components.PrimaryButton

@Composable
fun RegisterPageContent(
    title: String,
    subtitle: String,
    buttonText: String,
    isLoading: Boolean,
    onButtonClick: () -> Unit,
    inputField: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subtitleColor: Color = colorScheme.secondary,
    extraContent: @Composable (() -> Unit)? = null,
    bottomContent: @Composable (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.height(96.dp)
        ) {
            AppText(
                text = title,
                typography = typography.displaySmall,
                color = colorScheme.onBackground
            )

            Spacer(Modifier.height(12.dp))

            AppText(
                text = subtitle,
                typography = typography.bodyLarge,
                color = subtitleColor
            )
        }

        Spacer(Modifier.height(48.dp))

        inputField()

        Box(
            modifier = Modifier.height(52.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Column {
                extraContent?.invoke()
            }
        }

        PrimaryButton(
            onClick = onButtonClick,
            title = if (isLoading) "" else buttonText,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 18.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.primary,
                disabledContainerColor = colorScheme.primary.copy(alpha = 0.5f)
            ),
            titleColor = Color.Black
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.Black,
                    strokeWidth = 2.dp
                )
            }
        } else {
            Spacer(Modifier.height(42.dp))
        }

        bottomContent?.invoke()

        Spacer(Modifier.height(40.dp))
    }
}

