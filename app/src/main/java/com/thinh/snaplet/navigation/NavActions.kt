package com.thinh.snaplet.navigation

import androidx.navigation.NavHostController

class NavActions(
    private val nav: NavHostController
) {
    fun navigateToMyProfile() {
        nav.navigate(NavScreen.MyProfile.route)
    }

    fun popBackStack(): Boolean = nav.popBackStack()

    fun navigateToLoginReplacingOnboarding() {
        nav.navigate(NavScreen.Login.route) {
            popUpTo(NavScreen.Onboarding.route) { inclusive = true }
        }
    }

    fun navigateToRegisterReplacingOnboarding() {
        nav.navigate(NavScreen.Register.route) {
            popUpTo(NavScreen.Onboarding.route) { inclusive = true }
        }
    }

    fun navigateToRegister() {
        nav.navigate(NavScreen.Register.route)
    }

    fun navigateToLogin() {
        nav.navigate(NavScreen.Login.route)
    }
}