package com.thinh.snaplet.domain.post

import com.thinh.snaplet.data.repository.MediaRepository
import com.thinh.snaplet.utils.network.ApiResult
import javax.inject.Inject

/**
 * Deletes a post by id.
 * ViewModel handles UI state updates and events based on result.
 */
class DeletePostUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    suspend operator fun invoke(postId: String): ApiResult<Unit> {
        return mediaRepository.deletePost(postId)
    }
}
