package com.thinh.snaplet.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun <T> StepAnimatedContent(
    currentStep: T,
    stepOrder: (T) -> Int,
    modifier: Modifier = Modifier,
    animationDurationMs: Int = 300,
    content: @Composable (T) -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AnimatedContent(
            targetState = currentStep, transitionSpec = {
                val direction = when {
                    stepOrder(targetState) > stepOrder(initialState) -> 1
                    stepOrder(targetState) < stepOrder(initialState) -> -1
                    else -> 0
                }

                if (direction == 0) {
                    fadeIn() togetherWith fadeOut()
                } else {
                    slideInHorizontally(
                        animationSpec = tween(animationDurationMs)
                    ) { fullWidth -> direction * fullWidth } togetherWith slideOutHorizontally(
                        animationSpec = tween(animationDurationMs)
                    ) { fullWidth -> -direction * fullWidth }
                }
            }, label = "StepAnimatedContent"
        ) { step ->
            content(step)
        }
    }
}
