package net.luis.tracker.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.luis.tracker.domain.model.AppLanguage
import net.luis.tracker.domain.model.OverviewLayout
import net.luis.tracker.domain.model.OverviewSection
import net.luis.tracker.domain.model.OverviewSectionState
import net.luis.tracker.domain.model.StreakException
import net.luis.tracker.domain.model.StreakExceptionType
import net.luis.tracker.domain.model.ThemeMode
import net.luis.tracker.domain.model.TimerResumeMode
import net.luis.tracker.domain.model.WeightUnit
import java.time.LocalDate

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

	private object Keys {
		val THEME_MODE = stringPreferencesKey("theme_mode")
		val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
		val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
		val REST_TIMER_SECONDS = intPreferencesKey("rest_timer_seconds")
		val WEEKLY_WORKOUT_GOAL = intPreferencesKey("weekly_workout_goal")
		val TIMER_RESUME_MODE = stringPreferencesKey("timer_resume_mode")
		val APP_LANGUAGE = stringPreferencesKey("app_language")
		val PREFERRED_WEIGHTS_KG = stringPreferencesKey("preferred_weights_kg")
		val OVERVIEW_LAYOUT_MONTH = stringPreferencesKey("overview_layout_month")
		val OVERVIEW_LAYOUT_LIFETIME = stringPreferencesKey("overview_layout_lifetime")
		val STREAK_BASELINE = intPreferencesKey("streak_baseline")
		val STREAK_EXCEPTIONS = stringPreferencesKey("streak_exceptions")
	}

	val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
		ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
	}

	val dynamicColors: Flow<Boolean> = context.dataStore.data.map { prefs ->
		prefs[Keys.DYNAMIC_COLORS] ?: true
	}

	val weightUnit: Flow<WeightUnit> = context.dataStore.data.map { prefs ->
		WeightUnit.valueOf(prefs[Keys.WEIGHT_UNIT] ?: WeightUnit.KG.name)
	}

	val restTimerSeconds: Flow<Int> = context.dataStore.data.map { prefs ->
		prefs[Keys.REST_TIMER_SECONDS] ?: 90
	}

	val weeklyWorkoutGoal: Flow<Int> = context.dataStore.data.map { prefs ->
		prefs[Keys.WEEKLY_WORKOUT_GOAL] ?: 2
	}

	val timerResumeMode: Flow<TimerResumeMode> = context.dataStore.data.map { prefs ->
		TimerResumeMode.valueOf(prefs[Keys.TIMER_RESUME_MODE] ?: TimerResumeMode.RESUME.name)
	}

	suspend fun setThemeMode(mode: ThemeMode) {
		context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
	}

	suspend fun setDynamicColors(enabled: Boolean) {
		context.dataStore.edit { it[Keys.DYNAMIC_COLORS] = enabled }
	}

	suspend fun setWeightUnit(unit: WeightUnit) {
		context.dataStore.edit { it[Keys.WEIGHT_UNIT] = unit.name }
	}

	suspend fun setRestTimerSeconds(seconds: Int) {
		context.dataStore.edit { it[Keys.REST_TIMER_SECONDS] = seconds }
	}

	suspend fun setWeeklyWorkoutGoal(goal: Int) {
		context.dataStore.edit { it[Keys.WEEKLY_WORKOUT_GOAL] = goal }
	}

	suspend fun setTimerResumeMode(mode: TimerResumeMode) {
		context.dataStore.edit { it[Keys.TIMER_RESUME_MODE] = mode.name }
	}

	val appLanguage: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
		AppLanguage.valueOf(prefs[Keys.APP_LANGUAGE] ?: AppLanguage.SYSTEM.name)
	}

	suspend fun setAppLanguage(language: AppLanguage) {
		context.dataStore.edit { it[Keys.APP_LANGUAGE] = language.name }
	}

	val preferredWeightsKg: Flow<List<Double>> = context.dataStore.data.map { prefs ->
		val raw = prefs[Keys.PREFERRED_WEIGHTS_KG] ?: ""
		if (raw.isBlank()) emptyList()
		else raw.split(",").mapNotNull { it.toDoubleOrNull() }
	}

	suspend fun setPreferredWeightsKg(weights: List<Double>) {
		context.dataStore.edit { it[Keys.PREFERRED_WEIGHTS_KG] = weights.joinToString(",") }
	}

	val overviewLayout: Flow<OverviewLayout> = context.dataStore.data.map { prefs ->
		OverviewLayout(
			month = parseSections(prefs[Keys.OVERVIEW_LAYOUT_MONTH], OverviewLayout.MONTH_SECTIONS),
			lifetime = parseSections(prefs[Keys.OVERVIEW_LAYOUT_LIFETIME], OverviewLayout.LIFETIME_SECTIONS)
		)
	}

	suspend fun setOverviewLayout(layout: OverviewLayout) {
		context.dataStore.edit { prefs ->
			prefs[Keys.OVERVIEW_LAYOUT_MONTH] = serializeSections(layout.month)
			prefs[Keys.OVERVIEW_LAYOUT_LIFETIME] = serializeSections(layout.lifetime)
		}
	}

	val streakBaseline: Flow<Int> = context.dataStore.data.map { prefs ->
		prefs[Keys.STREAK_BASELINE] ?: 0
	}

	suspend fun setStreakBaseline(baseline: Int) {
		context.dataStore.edit { it[Keys.STREAK_BASELINE] = baseline }
	}

	val streakExceptions: Flow<List<StreakException>> = context.dataStore.data.map { prefs ->
		parseStreakExceptions(prefs[Keys.STREAK_EXCEPTIONS])
	}

	suspend fun setStreakExceptions(exceptions: List<StreakException>) {
		context.dataStore.edit { it[Keys.STREAK_EXCEPTIONS] = serializeStreakExceptions(exceptions) }
	}

	private fun parseStreakExceptions(raw: String?): List<StreakException> =
		raw?.takeIf { it.isNotBlank() }
			?.split(";")
			?.mapNotNull { token ->
				runCatching {
					val parts = token.split("|")
					StreakException(
						id = parts[0],
						type = StreakExceptionType.valueOf(parts[1]),
						start = LocalDate.ofEpochDay(parts[2].toLong()),
						end = LocalDate.ofEpochDay(parts[3].toLong())
					)
				}.getOrNull()
			}
			?: emptyList()

	private fun serializeStreakExceptions(exceptions: List<StreakException>): String =
		exceptions.joinToString(";") { e ->
			"${e.id}|${e.type.name}|${e.start.toEpochDay()}|${e.end.toEpochDay()}"
		}

	private fun parseSections(raw: String?, allowed: List<OverviewSection>): List<OverviewSectionState> {
		val stored = raw?.takeIf { it.isNotBlank() }
			?.split(",")
			?.mapNotNull { token ->
				val parts = token.split(":")
				val section = parts.getOrNull(0)
					?.let { name -> runCatching { OverviewSection.valueOf(name) }.getOrNull() }
					?: return@mapNotNull null
				OverviewSectionState(section, visible = parts.getOrNull(1) != "0")
			}
			?: emptyList()
		return OverviewLayout.normalize(stored, allowed)
	}

	private fun serializeSections(sections: List<OverviewSectionState>): String =
		sections.joinToString(",") { "${it.section.name}:${if (it.visible) 1 else 0}" }
}
