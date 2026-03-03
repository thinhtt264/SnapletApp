package com.thinh.snaplet.domain.model

sealed interface RelationshipAction {
    /** Viewing own profile */
    data object CurrentUser : RelationshipAction

    /** No relationship yet – can send a friend request */
    data object AddFriend : RelationshipAction

    /** Already friends */
    data object Accepted : RelationshipAction

    /** Blocked */
    data object Blocked : RelationshipAction

    /** Pending – current user sent the request (initiator) */
    data object PendingByMe : RelationshipAction

    /** Pending – other user sent the request; can accept with [relationshipId] */
    data class PendingByOther(val relationshipId: String) : RelationshipAction
}
