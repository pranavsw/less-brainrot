package dev.lessbrainrot

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import android.view.accessibility.AccessibilityEvent

class GestureCounterTest {
    @Test
    fun acceptsFirstInstagramScrollWhenIndexesAreUnchanged() {
        val counter = GestureCounter(warmupMs = 0L) { 1_000L }
        val classification = FeedClassification(FeedState.FEED_CONFIRMED, "pager", 2)
        val observation = ScrollObservation(
            SupportedPackages.INSTAGRAM, AccessibilityEvent.TYPE_VIEW_SCROLLED, "androidx.viewpager.widget.ViewPager", null,
            true, 0, 971, 18, 18, 25, classification
        )

        assertTrue(counter.consider(observation))
    }

    @Test
    fun debouncesRepeatedEventsAndAcceptsNewPosition() {
        var now = 1_000L
        val counter = GestureCounter(500L, 0L) { now }
        val classification = FeedClassification(FeedState.FEED_CONFIRMED, "test", 2)
        fun observation(to: Int) = ScrollObservation(
            SupportedPackages.INSTAGRAM, AccessibilityEvent.TYPE_VIEW_SCROLLED, "RecyclerView", "reel_feed",
            true, 0, 100, 0, to, 10, classification
        )
        assertTrue(counter.consider(observation(1)))
        assertFalse(counter.consider(observation(2)))
        now += 501
        assertTrue(counter.consider(observation(2)))
    }

    @Test
    fun countsLaterYouTubeSwipeWhenReportedIndexDoesNotChange() {
        var now = 1_000L
        val counter = GestureCounter(500L, 0L) { now }
        val classification = FeedClassification(FeedState.FEED_CONFIRMED, "shorts pager", 2)
        val observation = ScrollObservation(
            SupportedPackages.YOUTUBE, AccessibilityEvent.TYPE_VIEW_SCROLLED, "androidx.viewpager.widget.ViewPager", null,
            true, 0, 900, 4, 4, 10, classification
        )

        assertTrue(counter.consider(observation))
        now += 501
        assertTrue(counter.consider(observation))
    }

    @Test
    fun rejectsContentChangedEventEvenWhenFeedContextIsConfirmed() {
        val counter = GestureCounter(warmupMs = 0L) { 1_000L }
        val classification = FeedClassification(FeedState.FEED_CONFIRMED, "context", 2)
        val observation = ScrollObservation(
            SupportedPackages.INSTAGRAM,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            "androidx.viewpager.widget.ViewPager",
            null,
            true,
            -1,
            -1,
            -1,
            -1,
            -1,
            classification
        )

        assertFalse(counter.consider(observation))
    }

    @Test
    fun rejectsInvalidIndicesWithoutDelta() {
        val counter = GestureCounter(warmupMs = 0L) { 1_000L }
        val classification = FeedClassification(FeedState.FEED_CONFIRMED, "context", 2)
        val observation = ScrollObservation(
            SupportedPackages.YOUTUBE,
            AccessibilityEvent.TYPE_VIEW_SCROLLED,
            "androidx.viewpager.widget.ViewPager",
            null,
            true,
            -1,
            -1,
            -1,
            -1,
            -1,
            classification
        )

        assertFalse(counter.consider(observation))
    }

    @Test
    fun countsYouTubeFallbackContentChangeOncePerDebounceWindow() {
        var now = 1_000L
        val counter = GestureCounter(GestureCounter.DEFAULT_DEBOUNCE_MS, 0L) { now }
        val classification = FeedClassification(
            FeedState.FEED_CONFIRMED,
            "YouTube fallback",
            1
        )
        val observation = ScrollObservation(
            SupportedPackages.YOUTUBE,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            "android.widget.FrameLayout",
            null,
            false,
            -1,
            -1,
            -1,
            -1,
            -1,
            classification
        )

        assertTrue(counter.consider(observation))
        assertFalse(counter.consider(observation))
        now += GestureCounter.DEFAULT_DEBOUNCE_MS + 1
        assertTrue(counter.consider(observation))
    }

    @Test
    fun rejectsSecondGestureBeforeOneSecond() {
        var now = 1_000L
        val counter = GestureCounter(warmupMs = 0L, clock = { now })
        val classification = FeedClassification(FeedState.FEED_CONFIRMED, "pager", 2)
        val observation = ScrollObservation(
            SupportedPackages.INSTAGRAM, AccessibilityEvent.TYPE_VIEW_SCROLLED,
            "androidx.viewpager.widget.ViewPager", null, true,
            0, 800, -1, -1, -1, classification
        )

        assertTrue(counter.consider(observation))
        now += 999
        assertFalse(counter.consider(observation))
        now += 1
        assertTrue(counter.consider(observation))
    }
}
