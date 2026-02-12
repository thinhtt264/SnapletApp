package com.thinh.snaplet.domain.media

import com.thinh.snaplet.data.repository.MediaRepository
import javax.inject.Inject

/**
 * Downloads an image from the given source (URL or file path) to the device.
 * Returns the display name of the saved file on success.
 */
class DownloadPostImageUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    suspend operator fun invoke(imageSource: String): Result<String> {
        return mediaRepository.downloadImage(imageSource)
    }
}
