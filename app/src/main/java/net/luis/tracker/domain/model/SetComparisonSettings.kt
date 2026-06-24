package net.luis.tracker.domain.model

/**
 * User settings for the active-workout set comparison feature, which badges newly entered sets
 * based on the same set number from the previous logged workouts of the exercise:
 * - green: improvement over last time
 * - yellow: a single drop below last time (could be an off day)
 * - red: a regression (dropped again after already dropping the session before)
 * - grey: unchanged, or a trade-off where the other value rose
 *
 * Colors are stored as packed ARGB ints so they can live in DataStore and be turned into a
 * Compose `Color` directly.
 *
 * Badges are drawn as a subtly tinted background in both themes. [brightenInDark] lightens the
 * tint colors when a dark theme is active so they read a bit more vividly against the dark surface.
 */
data class SetComparisonSettings(
	val enabled: Boolean = true,
	val betterColor: Int = DEFAULT_BETTER,
	val singleDropColor: Int = DEFAULT_SINGLE_DROP,
	val worseColor: Int = DEFAULT_WORSE,
	val neutralColor: Int = DEFAULT_NEUTRAL,
	val brightenInDark: Boolean = true
) {
	companion object {
		const val DEFAULT_BETTER: Int = 0xFF43A047.toInt()       // green
		const val DEFAULT_SINGLE_DROP: Int = 0xFFF9A825.toInt()  // amber / yellow
		const val DEFAULT_WORSE: Int = 0xFFE53935.toInt()        // red
		const val DEFAULT_NEUTRAL: Int = 0xFF9E9E9E.toInt()      // grey (unchanged / trade-off)
	}
}
