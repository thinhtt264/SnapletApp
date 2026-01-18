package com.thinh.snaplet.utils.network

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Failure(val error: ApiError) : ApiResult<Nothing>()
    
    /**
     * Transform ApiResult using fold pattern
     * @param onSuccess callback for Success case
     * @param onFailure callback for Failure case
     * @return result of the transformation
     */
    inline fun <R> fold(
        onSuccess: (T) -> R,
        onFailure: (ApiError) -> R
    ): R = when (this) {
        is Success -> onSuccess(data)
        is Failure -> onFailure(error)
    }
}