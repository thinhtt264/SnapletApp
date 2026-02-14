package com.thinh.snaplet.domain.user

import com.thinh.snaplet.data.model.RelationshipStatus
import com.thinh.snaplet.data.repository.UserRepository
import com.thinh.snaplet.domain.model.RelationshipAction
import javax.inject.Inject

class GetRelationshipActionUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(targetUserId: String): RelationshipAction {
        val currentUser = userRepository.getCurrentUserProfile()
        val isCurrentUser = currentUser?.id == targetUserId
        if (isCurrentUser) return RelationshipAction.CurrentUser

        val relationship = userRepository.getRelationshipWithUser(targetUserId)
            .fold(onSuccess = { it }, onFailure = { null })
        val status = RelationshipStatus.from(relationship?.status ?: "")

        return when (status) {
            RelationshipStatus.ACCEPTED -> RelationshipAction.Accepted
            RelationshipStatus.BLOCKED -> RelationshipAction.Blocked
            RelationshipStatus.PENDING -> if (relationship?.initiator == currentUser?.id) {
                RelationshipAction.PendingByMe
            } else {
                RelationshipAction.PendingByOther(relationship?.id ?: "")
            }
            null -> RelationshipAction.AddFriend
        }
    }
}
