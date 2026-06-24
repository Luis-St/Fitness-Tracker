package net.luis.tracker.domain

import net.luis.tracker.domain.model.WorkoutSet

/**
 * Verdict for a single value (weight or reps) of an entered set versus the matching set number of
 * the previous logged performances.
 *
 * [SINGLE_DROP] is a first dip below last time (the value was stable or higher the session before),
 * while [WORSE] is a regression — a drop that follows an already-dropping trend. [TRADEOFF] is an
 * excused drop (this value fell but the other rose) and shares the neutral badge. [NEUTRAL] means
 * there is no baseline to compare against at all (first time, or a set beyond the last performance's
 * set count) and renders without any badge.
 */
enum class SetComparison {
	BETTER,
	SAME,
	SINGLE_DROP,
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
 * - lower, but the other value went up -> [SetComparison.TRADEOFF] (excused)
 * - lower, and [previous] was itself a drop from [beforePrevious] -> [SetComparison.WORSE]
 *   (a regression: two declines in a row)
 * - lower otherwise -> [SetComparison.SINGLE_DROP] (a first dip)
 *
 * [beforePrevious] is the same set number from the workout before [previous]; without it (or its
 * field) a drop can only ever be a [SetComparison.SINGLE_DROP], since a regression needs two prior
 * points to confirm.
 *
 * Body-weight / reps-only exercises only get a reps verdict; the weight verdict is [NEUTRAL].
 */
fun compareSet(
	current: WorkoutSet,
	previous: WorkoutSet?,
	beforePrevious: WorkoutSet?,
	hasWeight: Boolean
): SetBadges {
	if (previous == null) return SetBadges(SetComparison.NEUTRAL, SetComparison.NEUTRAL)

	val repsCmp = current.reps.compareTo(previous.reps)
	val prevRepsWasDrop = beforePrevious != null && previous.reps < beforePrevious.reps

	if (!hasWeight) {
		return SetBadges(
			weight = SetComparison.NEUTRAL,
			reps = fieldVerdict(repsCmp, otherImproved = false, previousWasDrop = prevRepsWasDrop)
		)
	}

	val weightCmp = weightCompare(current.weightKg, previous.weightKg)
	val prevWeightWasDrop = beforePrevious != null &&
		weightCompare(previous.weightKg, beforePrevious.weightKg) < 0

	return SetBadges(
		weight = fieldVerdict(weightCmp, otherImproved = repsCmp > 0, previousWasDrop = prevWeightWasDrop),
		reps = fieldVerdict(repsCmp, otherImproved = weightCmp > 0, previousWasDrop = prevRepsWasDrop)
	)
}

private fun weightCompare(a: Double, b: Double): Int = when {
	a - b > WEIGHT_EPSILON -> 1
	b - a > WEIGHT_EPSILON -> -1
	else -> 0
}

private fun fieldVerdict(cmp: Int, otherImproved: Boolean, previousWasDrop: Boolean): SetComparison = when {
	cmp > 0 -> SetComparison.BETTER
	cmp == 0 -> SetComparison.SAME
	otherImproved -> SetComparison.TRADEOFF // drop excused by the other value improving
	previousWasDrop -> SetComparison.WORSE  // regression: dropped again after last time's drop
	else -> SetComparison.SINGLE_DROP       // first dip below last time
}
