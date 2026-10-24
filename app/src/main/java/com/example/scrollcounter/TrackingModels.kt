package com.example.scrollcounter

enum class FeedState { FEED_CONFIRMED, FEED_NOT_CONFIRMED, FEED_UNKNOWN }

data class FeedClassification(
    val state: FeedState,
    val reason: String,
    val evidence: Int,
    val confirmedContainerClass: String? = null
)

data class ScrollObservation(
    val packageName: String,
    val eventType: Int,
    val className: String?,
    val viewId: String?,
    val scrollable: Boolean?,
    val deltaX: Int,
    val deltaY: Int,
    val fromIndex: Int,
    val toIndex: Int,
    val itemCount: Int,
    val classification: FeedClassification
) {
    val hasValidIndices: Boolean
        get() = fromIndex >= 0 && toIndex >= 0

    val hasVerticalMovement: Boolean
        get() = (deltaY != 0 && deltaY != -1) ||
            (hasValidIndices && fromIndex != toIndex)

    val isYouTubeFallbackEvent: Boolean
        get() = packageName == SupportedPackages.YOUTUBE &&
            eventType == android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            !hasVerticalMovement
}

data class GestureDecision(
    val counted: Boolean,
    val reason: String
)
