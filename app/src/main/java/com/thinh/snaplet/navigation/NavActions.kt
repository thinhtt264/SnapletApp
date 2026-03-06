package com.thinh.snaplet.navigation

import android.net.Uri
import android.util.Base64
import androidx.navigation.NavHostController
import java.nio.charset.StandardCharsets

class NavActions(
    private val nav: NavHostController
) {
    fun navigateToMyProfile() {
        nav.navigate(NavScreen.MyProfile.route)
    }

    fun navigateToImageCrop(uri: Uri) {
        val encoded = Base64.encodeToString(
            uri.toString().toByteArray(StandardCharsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP
        )
        nav.navigate("image_crop/$encoded")
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