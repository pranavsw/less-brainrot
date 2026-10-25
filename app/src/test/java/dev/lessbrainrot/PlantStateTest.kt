package dev.lessbrainrot

import org.junit.Assert.assertEquals
import org.junit.Test

class PlantStateTest {
    @Test
    fun gradesAgainstTheDailyLimit() {
        assertEquals(PlantState.VERY_LESS, PlantState.forCount(count = 10, limit = 100))
        assertEquals(PlantState.VERY_LESS, PlantState.forCount(count = 29, limit = 100))
        assertEquals(PlantState.LESS, PlantState.forCount(count = 30, limit = 100))
        assertEquals(PlantState.LESS, PlantState.forCount(count = 99, limit = 100))
        assertEquals(PlantState.AT_LIMIT, PlantState.forCount(count = 100, limit = 100))
        assertEquals(PlantState.AT_LIMIT, PlantState.forCount(count = 124, limit = 100))
        assertEquals(PlantState.OVER_LIMIT, PlantState.forCount(count = 125, limit = 100))
        assertEquals(PlantState.OVER_LIMIT, PlantState.forCount(count = 174, limit = 100))
        assertEquals(PlantState.WAY_OVER, PlantState.forCount(count = 175, limit = 100))
    }

    @Test
    fun zeroCountIsAlwaysTheHealthiestState() {
        assertEquals(PlantState.VERY_LESS, PlantState.forCount(count = 0, limit = 2))
    }

    @Test
    fun withoutALimitGradesAgainstDefaultReference() {
        assertEquals(PlantState.VERY_LESS, PlantState.forCount(count = 0, limit = 0))
        assertEquals(PlantState.LESS, PlantState.forCount(count = 50, limit = 0))
        assertEquals(PlantState.AT_LIMIT, PlantState.forCount(count = 110, limit = 0))
        assertEquals(PlantState.WAY_OVER, PlantState.forCount(count = 300, limit = 0))
    }

    @Test
    fun worksWithSmallLimits() {
        assertEquals(PlantState.VERY_LESS, PlantState.forCount(count = 0, limit = 2))
        assertEquals(PlantState.LESS, PlantState.forCount(count = 1, limit = 2))
        assertEquals(PlantState.AT_LIMIT, PlantState.forCount(count = 2, limit = 2))
        assertEquals(PlantState.WAY_OVER, PlantState.forCount(count = 4, limit = 2))
    }
}
