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
import net.luis.tracker.domain.model.ThemeMode
import net.luis.tracker.domain.model.WeightUnit

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

	private object Keys {
		val THEME_MODE = stringPreferencesKey("theme_mode")
		val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
		val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
		val REST_TIMER_SECONDS = intPreferencesKey("rest_timer_seconds")
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
}
