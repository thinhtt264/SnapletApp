package com.thinh.snaplet.domain.model

/**
 * Result of the upload post flow (request upload -> upload file -> confirm -> create post).
 */
sealed class UploadPostResult {
    data object Success : UploadPostResult()
    data class Failed(val message: String) : UploadPostResult()
}
