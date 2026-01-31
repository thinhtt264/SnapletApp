package com.thinh.snaplet.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.thinh.snaplet.ui.screens.home.Home
import com.thinh.snaplet.ui.screens.login.Login
import com.thinh.snaplet.ui.screens.onboarding.Onboarding
import com.thinh.snaplet.ui.screens.register.Register

private const val NAV_ANIM_DURATION = 250
private const val FADE_DURATION_DIVISOR = 1
private const val ENTER_OFFSET_PERCENT = 0.3f
private const val EXIT_OFFSET_PERCENT = 0.15f

@Composable
fun NavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: String = NavScreen.AuthGraph.route,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> (fullWidth * ENTER_OFFSET_PERCENT).toInt() },
                animationSpec = tween(
                    durationMillis = NAV_ANIM_DURATION, easing = FastOutSlowInEasing
                )
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = NAV_ANIM_DURATION / FADE_DURATION_DIVISOR,
                    easing = FastOutSlowInEasing
                )
            )
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -(fullWidth * EXIT_OFFSET_PERCENT).toInt() },
                animationSpec = tween(
                    durationMillis = NAV_ANIM_DURATION, easing = FastOutSlowInEasing
                )
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = NAV_ANIM_DURATION / FADE_DURATION_DIVISOR,
                    easing = FastOutSlowInEasing
                )
            )
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -(fullWidth * EXIT_OFFSET_PERCENT).toInt() },
                animationSpec = tween(
                    durationMillis = NAV_ANIM_DURATION, easing = FastOutSlowInEasing
                )
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = NAV_ANIM_DURATION / FADE_DURATION_DIVISOR,
                    easing = FastOutSlowInEasing
                )
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> (fullWidth * ENTER_OFFSET_PERCENT).toInt() },
                animationSpec = tween(
                    durationMillis = NAV_ANIM_DURATION, easing = FastOutSlowInEasing
                )
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = NAV_ANIM_DURATION / FADE_DURATION_DIVISOR,
                    easing = FastOutSlowInEasing
                )
            )
        }) {
        authGraph(navController = navController)
        homeGraph(navController = navController)
    }
}

fun NavGraphBuilder.homeGraph(navController: NavHostController) {
    navigation(
        startDestination = NavScreen.Home.route,
        route = NavScreen.HomeGraph.route,
        enterTransition = {
            fadeIn(tween(120)) + scaleIn(
                initialScale = 0.92f, animationSpec = tween(120, easing = FastOutSlowInEasing)
            )
        },

        exitTransition = {
            fadeOut(tween(90)) + scaleOut(
                targetScale = 0.95f, animationSpec = tween(90)
            )
        }

    ) {
        composable(route = NavScreen.Home.route) {
            Home()
        }
    }
}

fun NavGraphBuilder.authGraph(navController: NavHostController) {
    navigation(
        startDestination = NavScreen.Onboarding.route, route = NavScreen.AuthGraph.route
    ) {
        composable(route = NavScreen.Onboarding.route) {
            Onboarding(onNavigateToLogin = {
                navController.navigate(NavScreen.Login.route) {
                    popUpTo(NavScreen.Onboarding.route) { inclusive = true }
                }
            }, onNavigateToRegister = {
                navController.navigate(NavScreen.Register.route) {
                    popUpTo(NavScreen.Onboarding.route) { inclusive = true }
                }
            })
        }

        composable(route = NavScreen.Login.route) {
            Login(onRegisterClick = {
                navController.navigate(NavScreen.Register.route)
            })
        }

        composable(route = NavScreen.Register.route) {
            Register(onLoginClick = {
                navController.navigate(NavScreen.Login.route)
            })
        }
    }
}