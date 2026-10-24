package dev.lessbrainrot

class GestureCounter(
    private val debounceMs: Long = DEFAULT_DEBOUNCE_MS,
    private val warmupMs: Long = DEFAULT_WARMUP_MS,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private var lastCountAt = Long.MIN_VALUE
    private var lastPackage: String? = null
    private var warmupUntil = Long.MIN_VALUE

    fun reset() {
        lastCountAt = Long.MIN_VALUE
        lastPackage = null
        warmupUntil = clock() + warmupMs
    }

    fun decide(observation: ScrollObservation): GestureDecision {
        val now = clock()
        val standardScroll =
            observation.eventType == android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SCROLLED
        if (!standardScroll && !observation.isYouTubeFallbackEvent) {
            return GestureDecision(false, "Ignored: event is not TYPE_VIEW_SCROLLED")
        }
        if (observation.classification.state != FeedState.FEED_CONFIRMED) {
            return GestureDecision(false, "Ignored: feed context is not confirmed")
        }
        if (observation.packageName != lastPackage) {
            lastPackage = observation.packageName
            lastCountAt = Long.MIN_VALUE
            warmupUntil = now + warmupMs
        }
        if (now < warmupUntil) {
            return GestureDecision(false, "Ignored: startup/package-transition warm-up active")
        }
        if (!observation.hasVerticalMovement) {
            if (observation.isYouTubeFallbackEvent) {
                return acceptFallback(observation)
            }
            return GestureDecision(false, "Ignored: no usable vertical delta or valid index transition")
        }
        if (
            (observation.deltaY != 0 &&
                observation.deltaY != -1 &&
                observation.deltaX != 0 &&
                observation.deltaX != -1 &&
                kotlin.math.abs(observation.deltaY) <= kotlin.math.abs(observation.deltaX))
        ) {
            return GestureDecision(false, "Ignored: horizontal movement is as large as vertical movement")
        }
        if (lastCountAt != Long.MIN_VALUE && now - lastCountAt < debounceMs) {
            return GestureDecision(false, "Ignored: within ${debounceMs}ms debounce interval")
        }
        lastCountAt = now
        return GestureDecision(true, "Accepted: confirmed feed and usable vertical movement")
    }

    private fun acceptFallback(observation: ScrollObservation): GestureDecision {
        val now = clock()
        if (lastCountAt != Long.MIN_VALUE && now - lastCountAt < debounceMs) {
            return GestureDecision(false, "Ignored: YouTube fallback event is within debounce interval")
        }
        lastCountAt = now
        return GestureDecision(
            true,
            "Accepted approximate YouTube fallback event; Shorts context is not distinguishable from metadata"
        )
    }

    fun consider(observation: ScrollObservation): Boolean = decide(observation).counted

    companion object {
        const val DEFAULT_DEBOUNCE_MS = 1_000L
        const val DEFAULT_WARMUP_MS = 3_000L
    }
}
