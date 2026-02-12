package com.thinh.snaplet.domain.media

import com.thinh.snaplet.domain.model.CaptureReadiness
import javax.inject.Inject

/**
 * Validates whether the app is ready to capture a photo.
 * Encapsulates permission and camera-ready rules.
 */
class ValidateCaptureReadinessUseCase @Inject constructor() {

    /**
     * @param hasCameraPermission whether camera permission is granted
     * @param isCameraActive whether ImageCapture is bound and ready
     */
    operator fun invoke(
        hasCameraPermission: Boolean,
        isCameraActive: Boolean
    ): CaptureReadiness = when {
        !hasCameraPermission -> CaptureReadiness.NeedPermission
        !isCameraActive -> CaptureReadiness.CameraNotReady
        else -> CaptureReadiness.Ready
    }
}
