package com.thinh.snaplet.domain.model

/**
 * Available actions for a post in the feed.
 * UI layer maps these to SheetOption (labels, colors, click handlers).
 */
sealed class PostAction {
    data object Share : PostAction()
    data object Download : PostAction()
    data object Delete : PostAction()
    data object Report : PostAction()
    data object Cancel : PostAction()
}
