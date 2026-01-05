package com.thinh.snaplet.navigation

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

@Composable
fun NavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: String = NavScreen.AuthGraph.route,
) {
    NavHost(
        navController = navController, startDestination = startDestination, modifier = modifier
    ) {
        authGraph(navController = navController)
        homeGraph(navController = navController)
    }
}

fun NavGraphBuilder.homeGraph(navController: NavHostController) {
    navigation(
        startDestination = NavScreen.Home.route, route = NavScreen.HomeGraph.route
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
            Onboarding(
                onNavigateToLogin = {
                    navController.navigate(NavScreen.Login.route) {
                        popUpTo(NavScreen.Onboarding.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(NavScreen.Register.route) {
                        popUpTo(NavScreen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(route = NavScreen.Login.route) {
            Login(onLoginSuccess = {
                navController.navigate(NavScreen.HomeGraph.route) {
                    popUpTo(NavScreen.AuthGraph.route) { inclusive = true }
                }
            }, onRegisterClick = {
                navController.navigate(NavScreen.Register.route)
            })
        }

        composable(route = NavScreen.Register.route) {
            Register(onRegisterSuccess = {
                navController.navigate(NavScreen.HomeGraph.route) {
                    popUpTo(NavScreen.AuthGraph.route) { inclusive = true }
                }
            }, onLoginClick = {
                navController.navigate(NavScreen.Login.route)
            })
        }
    }
}