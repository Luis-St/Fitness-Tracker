package net.luis.tracker.domain.model

/**
 * User settings for the active-workout set comparison feature, which badges newly entered sets
 * green/yellow/red based on the last logged performance of the same exercise.
 *
 * Colors are stored as packed ARGB ints so they can live in DataStore and be turned into a
 * Compose `Color` directly.
 */
data class SetComparisonSettings(
	val enabled: Boolean = true,
	val betterColor: Int = DEFAULT_BETTER,
	val sameColor: Int = DEFAULT_SAME,
	val worseColor: Int = DEFAULT_WORSE,
	val neutralColor: Int = DEFAULT_NEUTRAL
) {
	companion object {
		const val DEFAULT_BETTER: Int = 0xFF43A047.toInt()  // green
		const val DEFAULT_SAME: Int = 0xFFF9A825.toInt()    // amber / yellow
		const val DEFAULT_WORSE: Int = 0xFFE53935.toInt()   // red
		const val DEFAULT_NEUTRAL: Int = 0xFF9E9E9E.toInt()  // grey (trade-off)
	}
}
