package dev.lessbrainrot

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class CounterStoreTest {
    private lateinit var prefs: FakeSharedPreferences
    private lateinit var clock: MutableClock
    private lateinit var store: CounterStore

    @Before
    fun setUp() {
        prefs = FakeSharedPreferences()
        clock = MutableClock("2026-10-25T10:00:00Z")
        store = CounterStore(prefs, clock)
    }

    @Test
    fun incrementsTotalAndPerApp() {
        store.increment(SupportedPackages.INSTAGRAM)
        store.increment(SupportedPackages.INSTAGRAM)
        store.increment(SupportedPackages.YOUTUBE)

        assertEquals(3, store.total())
        assertEquals(2, store.appTotal(SupportedPackages.INSTAGRAM))
        assertEquals(1, store.appTotal(SupportedPackages.YOUTUBE))
    }

    @Test
    fun dayRolloverArchivesYesterdayAndResetsToday() {
        store.increment(SupportedPackages.INSTAGRAM)
        store.increment(SupportedPackages.YOUTUBE)
        clock.advanceDays(1)

        assertEquals(0, store.total())
        assertEquals(0, store.appTotal(SupportedPackages.INSTAGRAM))

        val yesterday = store.history(2).first()
        assertEquals(clock.today().minusDays(1).toString(), yesterday.date)
        assertEquals(2, yesterday.total)
        assertEquals(1, yesterday.perApp[SupportedPackages.INSTAGRAM])
        assertEquals(1, yesterday.perApp[SupportedPackages.YOUTUBE])
    }

    @Test
    fun historyIncludesTodayLiveCounts() {
        store.increment(SupportedPackages.YOUTUBE)
        val days = store.history(3)

        assertEquals(3, days.size)
        assertEquals(clock.today().toString(), days.last().date)
        assertEquals(1, days.last().total)
        assertEquals(0, days.first().total)
    }

    @Test
    fun manualResetKeepsArchivedDays() {
        store.increment(SupportedPackages.INSTAGRAM)
        clock.advanceDays(1)
        store.increment(SupportedPackages.INSTAGRAM)
        store.reset()

        assertEquals(0, store.total())
        val yesterday = store.history(2).first()
        assertEquals(1, yesterday.total)
    }

    @Test
    fun multipleRolloversKeepSeparateArchives() {
        store.increment(SupportedPackages.INSTAGRAM)
        clock.advanceDays(1)
        store.increment(SupportedPackages.YOUTUBE)
        store.increment(SupportedPackages.YOUTUBE)
        clock.advanceDays(1)

        val days = store.history(3)
        assertEquals(1, days[0].total)
        assertEquals(2, days[1].total)
        assertEquals(0, days[2].total)
    }

    @Test
    fun limitNotifierDecision() {
        fun notify(total: Int, limit: Int, reminders: Boolean = true, notified: String? = null) =
            LimitNotifier.shouldNotify(total, limit, reminders, notified, "2026-10-25")

        assertTrue(notify(total = 100, limit = 100))
        assertTrue(notify(total = 105, limit = 100))
        assertFalse(notify(total = 99, limit = 100))
        assertFalse(notify(total = 100, limit = 0))
        assertFalse(notify(total = 100, limit = 100, reminders = false))
        assertFalse(notify(total = 100, limit = 100, notified = "2026-10-25"))
        assertTrue(notify(total = 100, limit = 100, notified = "2026-10-24"))
    }
}

/** In-memory SharedPreferences for JVM tests. */
class FakeSharedPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any>()

    override fun getAll(): Map<String, Any?> = values.toMap()

    override fun getString(key: String, defValue: String?): String? = values[key] as? String ?: defValue

    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? =
        values[key] as? Set<String> ?: defValues

    override fun getInt(key: String, defValue: Int): Int = values[key] as? Int ?: defValue

    override fun getLong(key: String, defValue: Long): Long = values[key] as? Long ?: defValue

    override fun getFloat(key: String, defValue: Float): Float = values[key] as? Float ?: defValue

    override fun getBoolean(key: String, defValue: Boolean): Boolean =
        values[key] as? Boolean ?: defValue

    override fun contains(key: String): Boolean = values.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) = Unit

    private inner class FakeEditor : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        private val cleared = false

        override fun putString(key: String, value: String?): SharedPreferences.Editor {
            pending[key] = value
            return this
        }

        override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor {
            pending[key] = values
            return this
        }

        override fun putInt(key: String, value: Int): SharedPreferences.Editor {
            pending[key] = value
            return this
        }

        override fun putLong(key: String, value: Long): SharedPreferences.Editor {
            pending[key] = value
            return this
        }

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
            pending[key] = value
            return this
        }

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
            pending[key] = value
            return this
        }

        override fun remove(key: String): SharedPreferences.Editor {
            pending[key] = REMOVE
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            values.clear()
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            for ((key, value) in pending) {
                if (value === REMOVE) values.remove(key) else values[key] = value!!
            }
        }
    }

    companion object {
        private object REMOVE
    }
}

/** Clock that can be moved forward day by day to simulate date rollover. */
class MutableClock(initialIso: String) : Clock() {
    private var instant = Instant.parse(initialIso)
    private val zone = ZoneId.of("UTC")

    fun advanceDays(days: Long) {
        instant = instant.plusSeconds(days * 24 * 3600)
    }

    fun today() = java.time.LocalDate.ofInstant(instant, zone)

    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = this

    override fun instant(): Instant = instant
}
