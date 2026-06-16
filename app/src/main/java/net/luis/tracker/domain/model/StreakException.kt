package net.luis.tracker.domain.model

import java.time.LocalDate

/** Distinguishes the origin of a streak exception for UI labelling only. */
enum class StreakExceptionType { PAUSE, RESTORE }

/**
 * A range of weeks that are "excused" from the streak goal. Weeks overlapping the
 * range neither break the streak nor need to meet the weekly goal; the streak
 * bridges across them.
 *
 * A RESTORE entry is just a single-week range (Mon–Sun) of a gap being filled.
 * Both types behave identically in computation.
 */
data class StreakException(
	val id: String,
	val start: LocalDate,
	val end: LocalDate, // inclusive
	val type: StreakExceptionType
)
