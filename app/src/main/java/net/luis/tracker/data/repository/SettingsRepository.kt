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
import net.luis.tracker.domain.model.ThemeMode
import net.luis.tracker.domain.model.TimerResumeMode
import net.luis.tracker.domain.model.WeightUnit

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
}
