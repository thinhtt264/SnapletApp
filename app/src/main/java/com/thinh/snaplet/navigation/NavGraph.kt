package com.thinh.snaplet.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.thinh.snaplet.ui.screens.home.Home
import com.thinh.snaplet.ui.screens.image_crop.ImageCrop
import com.thinh.snaplet.ui.screens.login.Login
import com.thinh.snaplet.ui.screens.my_profile.MyProfile
import com.thinh.snaplet.ui.screens.onboarding.Onboarding
import com.thinh.snaplet.ui.screens.register.Register

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
        enterTransition = NavTransitions.Default.enter,
        exitTransition = NavTransitions.Default.exit,
        popEnterTransition = NavTransitions.Default.popEnter,
        popExitTransition = NavTransitions.Default.popExit
    ) {
        authGraph(navController = navController)
        homeGraph(navController = navController)
    }
}

fun NavGraphBuilder.homeGraph(navController: NavHostController) {
    val actions = NavActions(navController)
    navigation(
        startDestination = NavScreen.Home.route,
        route = NavScreen.HomeGraph.route,
        enterTransition = NavTransitions.HomeGraph.enter,
        exitTransition = NavTransitions.HomeGraph.exit
    ) {
        composable(route = NavScreen.Home.route) {
            Home(onProfileClick = actions::navigateToMyProfile)
        }
        composable(
            route = NavScreen.MyProfile.route,
            enterTransition = NavTransitions.MyProfile.enter,
            exitTransition = NavTransitions.MyProfile.exit,
            popEnterTransition = NavTransitions.MyProfile.popEnter,
            popExitTransition = NavTransitions.MyProfile.popExit
        ) {
            MyProfile(
                onBackClick = actions::popBackStack,
                onNavigateToImageCrop = actions::navigateToImageCrop,
            )
        }
        composable(
            route = NavScreen.ImageCrop.route,
            arguments = listOf(
                navArgument("imageUri") { type = NavType.StringType }
            ),
            // enterTransition = NavTransitions.Default.enter,
            // exitTransition = NavTransitions.Default.exit,
            // popEnterTransition = NavTransitions.Default.popEnter,
            // popExitTransition = NavTransitions.Default.popExit
        ) {
            ImageCrop()
        }
    }
}

fun NavGraphBuilder.authGraph(navController: NavHostController) {
    val actions = NavActions(navController)
    navigation(
        startDestination = NavScreen.Onboarding.route,
        route = NavScreen.AuthGraph.route
    ) {
        composable(route = NavScreen.Onboarding.route) {
            Onboarding(
                onNavigateToLogin = actions::navigateToLoginReplacingOnboarding,
                onNavigateToRegister = actions::navigateToRegisterReplacingOnboarding
            )
        }
        composable(route = NavScreen.Login.route) {
            Login(onRegisterClick = actions::navigateToRegister)
        }
        composable(route = NavScreen.Register.route) {
            Register(onLoginClick = actions::navigateToLogin)
        }
    }
}