package net.luis.tracker.domain

import net.luis.tracker.domain.model.WorkoutSet

/**
 * Verdict for a single value (weight or reps) of an entered set versus the matching set number of
 * the last logged performance.
 *
 * [TRADEOFF] is an excused drop (this value fell but the other rose) and still gets a (neutral)
 * badge. [NEUTRAL] means there is no baseline to compare against at all (first time, or a set
 * beyond the last performance's set count) and renders without any badge.
 */
enum class SetComparison {
	BETTER,
	SAME,
	WORSE,
	TRADEOFF,
	NEUTRAL
}

/** Per-field badge verdicts for one set. */
data class SetBadges(
	val weight: SetComparison,
	val reps: SetComparison
)

private const val WEIGHT_EPSILON = 0.0001

/**
 * Compares [current] to [previous] (the matching set number from the last logged workout), judging
 * weight and reps independently:
 * - higher than last -> [SetComparison.BETTER]
 * - equal -> [SetComparison.SAME]
 * - lower -> [SetComparison.WORSE], unless the other value went up (a trade-off), in which case the
 *   drop is excused as [SetComparison.TRADEOFF]
 *
 * Body-weight / reps-only exercises only get a reps verdict; the weight verdict is [NEUTRAL].
 */
fun compareSet(current: WorkoutSet, previous: WorkoutSet?, hasWeight: Boolean): SetBadges {
	if (previous == null) return SetBadges(SetComparison.NEUTRAL, SetComparison.NEUTRAL)

	val repsCmp = current.reps.compareTo(previous.reps)

	if (!hasWeight) {
		return SetBadges(SetComparison.NEUTRAL, fieldVerdict(repsCmp, otherImproved = false))
	}

	val weightCmp = when {
		current.weightKg - previous.weightKg > WEIGHT_EPSILON -> 1
		previous.weightKg - current.weightKg > WEIGHT_EPSILON -> -1
		else -> 0
	}

	return SetBadges(
		weight = fieldVerdict(weightCmp, otherImproved = repsCmp > 0),
		reps = fieldVerdict(repsCmp, otherImproved = weightCmp > 0)
	)
}

private fun fieldVerdict(cmp: Int, otherImproved: Boolean): SetComparison = when {
	cmp > 0 -> SetComparison.BETTER
	cmp == 0 -> SetComparison.SAME
	otherImproved -> SetComparison.TRADEOFF // drop excused by the other value improving
	else -> SetComparison.WORSE
}
