package com.thinh.snaplet.domain.feed

import javax.inject.Inject

/**
 * Decides whether to trigger load-more based on scroll position and loading state.
 * Business rule: trigger when user is within [THRESHOLD] items from the end.
 */
class ShouldTriggerLoadMoreUseCase @Inject constructor() {

    companion object {
        /** Trigger load more when this many items away from the end (e.g. 3 = when 3rd from last visible). */
        private const val THRESHOLD = 3
    }

    /**
     * @param currentIndex 0-based index of the currently visible item
     * @param totalItems total number of items
     * @param canLoadMore true if nextCursor != null && !isLoadingMore && !isLoadingPosts
     */
    operator fun invoke(
        currentIndex: Int,
        totalItems: Int,
        canLoadMore: Boolean
    ): Boolean {
        if (!canLoadMore || totalItems == 0) return false
        val loadMoreThreshold = (totalItems - THRESHOLD).coerceAtLeast(0)
        return currentIndex >= loadMoreThreshold
    }
}
