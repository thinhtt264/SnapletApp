package com.thinh.snaplet.domain.post

import com.thinh.snaplet.data.model.media.ImageTransform
import com.thinh.snaplet.data.repository.MediaRepository
import com.thinh.snaplet.domain.model.UploadPostResult
import com.thinh.snaplet.utils.network.onFailure
import com.thinh.snaplet.utils.network.onSuccess
import javax.inject.Inject

/**
 * Executes the full upload flow: request upload URL -> upload file -> confirm -> create post.
 * All business and network logic lives here; ViewModel only orchestrates and updates UI state.
 */
class UploadPostUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {

    suspend operator fun invoke(
        imagePath: String,
        transform: ImageTransform,
        caption: String?
    ): UploadPostResult {
        return runCatching {
            val uploadRequestData = mediaRepository.requestUpload(
                items = listOf(imagePath),
                transforms = listOf(transform)
            ).fold(
                onSuccess = { it },
                onFailure = { return UploadPostResult.Failed("Upload request failed: ${it.message}") }
            )

            if (uploadRequestData.data.isEmpty()) {
                return UploadPostResult.Failed("No upload URLs received")
            }

            val uploadItem = uploadRequestData.data.first()

            mediaRepository.uploadMedia(uploadItem.uploadUrl, imagePath).onFailure { error ->
                return UploadPostResult.Failed("Upload failed: ${error.message}")
            }

            mediaRepository.confirmUpload(listOf(uploadItem.mediaId)).fold(
                onSuccess = { confirmData ->
                    mediaRepository.createPost(
                        mediaIds = confirmData.media.map { it.id },
                        caption = caption,
                        visibility = "friend-only"
                    ).fold(
                        onSuccess = { UploadPostResult.Success },
                        onFailure = { UploadPostResult.Failed("Upload failed: ${it.message}") }
                    )
                },
                onFailure = { UploadPostResult.Failed("Upload confirmation failed: ${it.message}") }
            )
        }.getOrElse { e ->
            UploadPostResult.Failed(e.message ?: "Unknown error")
        }
    }
}
