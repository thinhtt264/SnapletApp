package com.thinh.snaplet.domain.user

import com.thinh.snaplet.data.repository.UserRepository
import com.thinh.snaplet.utils.network.ApiResult
import javax.inject.Inject

class GetDisplayableFriendsCountUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): ApiResult<Int?> {
        return when (val result = userRepository.getFriendsCount()) {
            is ApiResult.Success -> ApiResult.Success(
                if (result.data > 0) result.data else null
            )
            is ApiResult.Failure -> result
        }
    }
}