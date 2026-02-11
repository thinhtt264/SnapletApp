package com.thinh.snaplet.domain.model

/**
 * Result of validating whether the app is ready to capture a photo.
 * Domain model – no Android dependencies.
 */
sealed class CaptureReadiness {
    data object Ready : CaptureReadiness()
    data object NeedPermission : CaptureReadiness()
    data object CameraNotReady : CaptureReadiness()
}
