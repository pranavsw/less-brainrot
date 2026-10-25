package dev.lessbrainrot

/**
 * The plant-health scale shown in the floating counter, replicated from the
 * "Reels Usage Plant Health Scale" reference image. States grade the day's
 * scroll count against the daily limit; with no limit set, a default reference
 * keeps the plant meaningful.
 */
enum class PlantState(val drawableRes: Int) {
    VERY_LESS(R.drawable.ic_plant_very_less),
    LESS(R.drawable.ic_plant_less),
    AT_LIMIT(R.drawable.ic_plant_at_limit),
    OVER_LIMIT(R.drawable.ic_plant_over_limit),
    WAY_OVER(R.drawable.ic_plant_way_over);

    companion object {
        // Used for grading when the user has not set a daily limit.
        const val DEFAULT_REFERENCE = 100

        fun forCount(count: Int, limit: Int): PlantState {
            val reference = if (limit > 0) limit else DEFAULT_REFERENCE
            val ratio = count.toDouble() / reference
            return when {
                ratio >= 1.75 -> WAY_OVER
                ratio >= 1.25 -> OVER_LIMIT
                ratio >= 1.0 -> AT_LIMIT
                ratio >= 0.3 -> LESS
                else -> VERY_LESS
            }
        }
    }
}
