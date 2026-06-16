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
	CATEGORY
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
		/** Sections available on the "This Month" tab, in their default order. */
		val MONTH_SECTIONS = listOf(
			OverviewSection.STREAK,
			OverviewSection.SUMMARY,
			OverviewSection.PROGRESS,
			OverviewSection.CATEGORY
		)

		/** Sections available on the "Lifetime" tab, in their default order. */
		val LIFETIME_SECTIONS = listOf(
			OverviewSection.STREAK,
			OverviewSection.SUMMARY,
			OverviewSection.PROGRESS,
			OverviewSection.RECORDS,
			OverviewSection.CATEGORY
		)

		val DEFAULT = OverviewLayout(
			month = MONTH_SECTIONS.map { OverviewSectionState(it) },
			lifetime = LIFETIME_SECTIONS.map { OverviewSectionState(it) }
		)

		/**
		 * Reconciles a (possibly stale or partial) stored list against the sections allowed for a
		 * tab: keeps stored order and visibility for valid sections, drops unknown/duplicate ones,
		 * and appends any missing allowed sections at the end (visible by default). This keeps
		 * persisted configs forward-compatible when sections are added or removed.
		 */
		fun normalize(
			stored: List<OverviewSectionState>,
			allowed: List<OverviewSection>
		): List<OverviewSectionState> {
			val allowedSet = allowed.toSet()
			val kept = stored.filter { it.section in allowedSet }.distinctBy { it.section }
			val present = kept.map { it.section }.toSet()
			val missing = allowed.filter { it !in present }.map { OverviewSectionState(it) }
			return kept + missing
		}
	}
}
