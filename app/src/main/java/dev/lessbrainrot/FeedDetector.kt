package dev.lessbrainrot

import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class FeedDetector {
    private var activePackage: String? = null
    private var confirmedContainerClass: String? = null
    private var lastClassification =
        FeedClassification(FeedState.FEED_UNKNOWN, "No structural evidence observed", 0)

    fun reset(reason: String) {
        confirmedContainerClass = null
        lastClassification = FeedClassification(FeedState.FEED_UNKNOWN, reason, 0)
    }

    fun classify(event: AccessibilityEvent, root: AccessibilityNodeInfo?): FeedClassification {
        val packageName = event.packageName?.toString()
        if (packageName != activePackage) {
            activePackage = packageName
            reset("Active package changed")
        }
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            reset("Window state changed; feed context must be re-established")
        }
        if (packageName !in SupportedPackages.all) {
            return FeedClassification(FeedState.FEED_NOT_CONFIRMED, "Package is not enabled for tracking", 0)
        }

        val metadata = AccessibilityMetadata.from(event, root)
        val classification = classifyMetadata(packageName.orEmpty(), metadata)
        lastClassification = classification
        if (classification.state == FeedState.FEED_CONFIRMED) {
            confirmedContainerClass = metadata.className ?: confirmedContainerClass
        }
        return classification
    }

    fun observation(
        event: AccessibilityEvent,
        root: AccessibilityNodeInfo?,
        classification: FeedClassification
    ): ScrollObservation {
        val metadata = AccessibilityMetadata.from(event, root)
        return ScrollObservation(
            packageName = event.packageName?.toString().orEmpty(),
            eventType = event.eventType,
            className = metadata.className,
            viewId = metadata.viewId,
            scrollable = metadata.scrollable,
            deltaX = metadata.deltaX,
            deltaY = metadata.deltaY,
            fromIndex = event.fromIndex,
            toIndex = event.toIndex,
            itemCount = event.itemCount,
            classification = classification
        )
    }

    fun current(): FeedClassification = lastClassification

    fun isFeedContextConfirmed(packageName: String): Boolean =
        packageName == activePackage && confirmedContainerClass != null

    private fun classifyMetadata(
        packageName: String,
        metadata: AccessibilityMetadata
    ): FeedClassification {
        val idSignal = metadata.viewId?.lowercase()?.let { "reel" in it || "short" in it } == true
        val pagerSignal = metadata.className?.contains("ViewPager", ignoreCase = true) == true
        val listSignal = metadata.className?.contains("RecyclerView", ignoreCase = true) == true
        val structuralSignal = pagerSignal || listSignal
        val verticalMovement = metadata.hasVerticalMovement
        val scrollContainerSignal = metadata.scrollable == true || metadata.hasPositionMetadata
        val evidence = listOf(idSignal, structuralSignal, scrollContainerSignal, verticalMovement).count { it }

        val contextConfirmed = when {
            packageName == SupportedPackages.YOUTUBE &&
                pagerSignal &&
                metadata.scrollable == true -> FeedClassification(
                FeedState.FEED_CONFIRMED,
                "Observed YouTube pager container; movement must be verified separately",
                evidence,
                metadata.className
            )
            packageName == SupportedPackages.INSTAGRAM &&
                pagerSignal &&
                metadata.scrollable == true -> FeedClassification(
                FeedState.FEED_CONFIRMED,
                "Observed Instagram pager container; movement must be verified separately",
                evidence,
                metadata.className
            )
            else -> null
        }
        val confirmed = when {
            packageName == SupportedPackages.YOUTUBE &&
                metadata.className == "android.widget.FrameLayout" &&
                !metadata.hasVerticalMovement -> FeedClassification(
                FeedState.FEED_CONFIRMED,
                "YouTube fallback mode: generic content-change metadata; count is approximate",
                evidence,
                metadata.className
            )
            idSignal && verticalMovement -> FeedClassification(
                FeedState.FEED_CONFIRMED,
                "Observed feed-specific resource ID and valid vertical movement",
                evidence,
                metadata.className
            )
            packageName == SupportedPackages.YOUTUBE &&
                pagerSignal &&
                verticalMovement &&
                scrollContainerSignal -> FeedClassification(
                FeedState.FEED_CONFIRMED,
                "Observed YouTube pager with valid vertical movement",
                evidence,
                metadata.className
            )
            packageName == SupportedPackages.INSTAGRAM &&
                structuralSignal &&
                verticalMovement &&
                scrollContainerSignal -> FeedClassification(
                FeedState.FEED_CONFIRMED,
                "Observed Instagram feed container with valid vertical movement",
                evidence,
                metadata.className
            )
            confirmedContainerClass != null &&
                packageName == activePackage &&
                verticalMovement &&
                metadata.className == confirmedContainerClass -> FeedClassification(
                FeedState.FEED_CONFIRMED,
                "Continuing previously confirmed feed container",
                evidence,
                confirmedContainerClass
            )
            contextConfirmed != null -> contextConfirmed
            else -> null
        }
        return confirmed ?: when {
            !verticalMovement && confirmedContainerClass != null &&
                metadata.className == confirmedContainerClass -> FeedClassification(
                FeedState.FEED_CONFIRMED,
                "Continuing confirmed feed context; this event has no movement evidence",
                evidence,
                confirmedContainerClass
            )
            !verticalMovement -> FeedClassification(
                FeedState.FEED_UNKNOWN,
                "No valid vertical movement metadata",
                evidence
            )
            else -> FeedClassification(
                FeedState.FEED_UNKNOWN,
                "Insufficient structural evidence; package alone is not enough",
                evidence
            )
        }
    }
}

private data class AccessibilityMetadata(
    val className: String?,
    val viewId: String?,
    val scrollable: Boolean?,
    val deltaX: Int,
    val deltaY: Int,
    val fromIndex: Int,
    val toIndex: Int
) {
    val hasPositionMetadata: Boolean
        get() = fromIndex >= 0 || toIndex >= 0

    val hasValidIndices: Boolean
        get() = fromIndex >= 0 && toIndex >= 0

    val hasVerticalMovement: Boolean
        get() = (deltaY != 0 && deltaY != -1) ||
            (hasValidIndices && fromIndex != toIndex)

    companion object {
        fun from(event: AccessibilityEvent, root: AccessibilityNodeInfo?): AccessibilityMetadata {
            val deltas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                event.scrollDeltaX to event.scrollDeltaY
            } else {
                0 to 0
            }
            return AccessibilityMetadata(
                className = root?.className?.toString() ?: event.className?.toString(),
                viewId = root?.viewIdResourceName,
                scrollable = root?.isScrollable ?: event.isScrollable,
                deltaX = deltas.first,
                deltaY = deltas.second,
                fromIndex = event.fromIndex,
                toIndex = event.toIndex
            )
        }
    }
}

object SupportedPackages {
    const val INSTAGRAM = "com.instagram.android"
    const val YOUTUBE = "com.google.android.youtube"
    val all = setOf(INSTAGRAM, YOUTUBE)
}
