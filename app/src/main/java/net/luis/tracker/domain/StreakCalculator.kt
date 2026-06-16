package net.luis.tracker.domain

import net.luis.tracker.domain.model.StreakException
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields

/** A completed ISO week that broke the streak (not satisfied and not excused). */
data class StreakGap(
	val weekStart: LocalDate, // Monday
	val weekEnd: LocalDate,   // Sunday (inclusive)
	val workoutCount: Int
)

/**
 * Shared streak computation reused by the overview and streak-settings screens.
 *
 * A streak counts consecutive ISO weeks that are *satisfied*: a week is satisfied when
 * its distinct workout-day count meets the weekly [goal] OR the week is excused by a
 * [StreakException]. The current (in-progress) week only contributes when already
 * satisfied; otherwise it neither counts nor breaks the streak.
 */
object StreakCalculator {

	private val weekFields = WeekFields.ISO

	private fun weekKey(date: LocalDate): Pair<Int, Int> =
		date.get(weekFields.weekBasedYear()) to date.get(weekFields.weekOfWeekBasedYear())

	private fun mondayOf(date: LocalDate): LocalDate =
		date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

	/** Expand each exception's start..end into the ISO week keys it touches. */
	fun excusedWeeks(exceptions: List<StreakException>): Set<Pair<Int, Int>> {
		val result = mutableSetOf<Pair<Int, Int>>()
		for (e in exceptions) {
			if (e.end.isBefore(e.start)) continue
			var d = e.start
			while (!d.isAfter(e.end)) {
				result.add(weekKey(d))
				d = d.plusWeeks(1)
			}
			result.add(weekKey(e.end))
		}
		return result
	}

	private fun workoutsPerWeek(
		workoutDateMillis: List<Long>,
		zone: ZoneId
	): Map<Pair<Int, Int>, Int> =
		workoutDateMillis
			.map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
			.groupBy { weekKey(it) }
			.mapValues { (_, dates) -> dates.toSet().size } // distinct days

	fun calculateStreak(
		workoutDateMillis: List<Long>,
		goal: Int,
		exceptions: List<StreakException>,
		baseline: Int,
		zone: ZoneId = ZoneId.systemDefault(),
		today: LocalDate = LocalDate.now()
	): Int {
		val excused = excusedWeeks(exceptions)
		val perWeek = workoutsPerWeek(workoutDateMillis, zone)

		fun satisfied(key: Pair<Int, Int>): Boolean =
			(perWeek[key] ?: 0) >= goal || key in excused

		// Count the current week only if already satisfied; otherwise it's in progress
		// and is skipped (without breaking the streak).
		var streak = if (satisfied(weekKey(today))) 1 else 0

		var checkDate = today.minusWeeks(1)
		while (satisfied(weekKey(checkDate))) {
			streak++
			checkDate = checkDate.minusWeeks(1)
		}

		return baseline + streak // no clamping — negative allowed
	}

	/**
	 * Completed weeks (first workout → previous week, capped to [maxWeeks]) that broke
	 * the streak: not satisfied by the goal and not excused. Most recent first.
	 */
	fun findGaps(
		workoutDateMillis: List<Long>,
		goal: Int,
		exceptions: List<StreakException>,
		zone: ZoneId = ZoneId.systemDefault(),
		today: LocalDate = LocalDate.now(),
		maxWeeks: Int = 104
	): List<StreakGap> {
		if (workoutDateMillis.isEmpty()) return emptyList()
		val excused = excusedWeeks(exceptions)
		val perWeek = workoutsPerWeek(workoutDateMillis, zone)
		val firstDate = workoutDateMillis.min().let {
			Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
		}

		val currentMonday = mondayOf(today)
		val cap = currentMonday.minusWeeks((maxWeeks - 1).toLong())
		var weekStart = mondayOf(firstDate).let { if (it.isBefore(cap)) cap else it }

		val gaps = mutableListOf<StreakGap>()
		while (weekStart.isBefore(currentMonday)) { // exclude current in-progress week
			val key = weekKey(weekStart)
			val count = perWeek[key] ?: 0
			if (count < goal && key !in excused) {
				gaps.add(StreakGap(weekStart, weekStart.plusDays(6), count))
			}
			weekStart = weekStart.plusWeeks(1)
		}
		return gaps.asReversed()
	}
}
