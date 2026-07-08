package net.luis.tracker.domain.model

/**
 * Configurable sections of the overview/home screen. The calendar is intentionally not part of
 * this list — it stays pinned below the tab row. Section order and visibility are configured
 * independently per overview tab (see [OverviewLayout]).
 */
enum class OverviewSection {
	STREAK,
	SUMMARY,
	PROGRESS,
	RECORDS,
	CATEGORY,
	TOTALS,
	RECENT_WORKOUTS,
	CONSISTENCY,
	TOP_EXERCISES,
	HEATMAP
}

/** A single section together with its current visibility. */
data class OverviewSectionState(
	val section: OverviewSection,
	val visible: Boolean = true
)

/**
 * Per-tab configuration of the overview sections. Each tab keeps its own ordered list of
 * [OverviewSectionState]s. The lists are always normalized so they contain exactly the sections
 * that are valid for that tab (see [normalize]).
 */
data class OverviewLayout(
	val month: List<OverviewSectionState>,
	val lifetime: List<OverviewSectionState>
) {
	companion object {
		/**
		 * Sections available on the "This Month" tab, in their default order and default
		 * visibility. Newly introduced sections should default to hidden so existing users aren't
		 * surprised by extra cards appearing on their overview.
		 */
		val MONTH_SECTIONS = listOf(
			OverviewSectionState(OverviewSection.STREAK),
			OverviewSectionState(OverviewSection.SUMMARY),
			OverviewSectionState(OverviewSection.PROGRESS),
			OverviewSectionState(OverviewSection.CATEGORY),
			OverviewSectionState(OverviewSection.RECENT_WORKOUTS, visible = false),
			OverviewSectionState(OverviewSection.TOP_EXERCISES, visible = false)
		)

		/** Sections available on the "Lifetime" tab, in their default order and default visibility. */
		val LIFETIME_SECTIONS = listOf(
			OverviewSectionState(OverviewSection.STREAK),
			OverviewSectionState(OverviewSection.SUMMARY),
			OverviewSectionState(OverviewSection.PROGRESS),
			OverviewSectionState(OverviewSection.RECORDS),
			OverviewSectionState(OverviewSection.CATEGORY),
			OverviewSectionState(OverviewSection.TOTALS, visible = false),
			OverviewSectionState(OverviewSection.RECENT_WORKOUTS, visible = false),
			OverviewSectionState(OverviewSection.CONSISTENCY, visible = false),
			OverviewSectionState(OverviewSection.TOP_EXERCISES, visible = false),
			OverviewSectionState(OverviewSection.HEATMAP, visible = false)
		)

		val DEFAULT = OverviewLayout(
			month = MONTH_SECTIONS,
			lifetime = LIFETIME_SECTIONS
		)

		/**
		 * Reconciles a (possibly stale or partial) stored list against the [defaults] allowed for a
		 * tab: keeps stored order and visibility for valid sections, drops unknown/duplicate ones,
		 * and appends any missing sections using their default visibility (so newly added,
		 * default-hidden sections stay hidden for existing users). This keeps persisted configs
		 * forward-compatible when sections are added or removed.
		 */
		fun normalize(
			stored: List<OverviewSectionState>,
			defaults: List<OverviewSectionState>
		): List<OverviewSectionState> {
			val allowed = defaults.map { it.section }.toSet()
			val kept = stored.filter { it.section in allowed }.distinctBy { it.section }
			val present = kept.map { it.section }.toSet()
			val missing = defaults.filter { it.section !in present }
			return kept + missing
		}
	}
}
