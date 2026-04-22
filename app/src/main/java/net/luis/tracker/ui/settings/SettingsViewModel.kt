package net.luis.tracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.luis.tracker.data.repository.SettingsRepository
import net.luis.tracker.domain.model.AppLanguage
import net.luis.tracker.domain.model.ThemeMode
import net.luis.tracker.domain.model.TimerResumeMode
import net.luis.tracker.domain.model.WeightUnit

data class SettingsUiState(
	val themeMode: ThemeMode = ThemeMode.SYSTEM,
	val dynamicColors: Boolean = true,
	val weightUnit: WeightUnit = WeightUnit.KG,
	val restTimerSeconds: Int = 90,
	val weeklyWorkoutGoal: Int = 2,
	val timerResumeMode: TimerResumeMode = TimerResumeMode.RESUME,
	val appLanguage: AppLanguage = AppLanguage.SYSTEM
)

class SettingsViewModel(
	private val settingsRepository: SettingsRepository
) : ViewModel() {

	private val _uiState = MutableStateFlow(SettingsUiState())
	val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

	init {
		viewModelScope.launch {
			combine(
				settingsRepository.themeMode,
				settingsRepository.dynamicColors,
				settingsRepository.weightUnit,
				combine(
					settingsRepository.restTimerSeconds,
					settingsRepository.weeklyWorkoutGoal,
					settingsRepository.timerResumeMode
				) { rest, goal, timerMode -> Triple(rest, goal, timerMode) },
				settingsRepository.appLanguage
			) { mode, dynamicColors, unit, (seconds, goal, timerMode), lang ->
				SettingsUiState(
					themeMode = mode,
					dynamicColors = dynamicColors,
					weightUnit = unit,
					restTimerSeconds = seconds,
					weeklyWorkoutGoal = goal,
					timerResumeMode = timerMode,
					appLanguage = lang
				)
			}.collect { state ->
				_uiState.value = state
			}
		}
	}

	fun setThemeMode(mode: ThemeMode) {
		viewModelScope.launch {
			settingsRepository.setThemeMode(mode)
		}
	}

	fun setDynamicColors(enabled: Boolean) {
		viewModelScope.launch {
			settingsRepository.setDynamicColors(enabled)
		}
	}

	fun setWeightUnit(unit: WeightUnit) {
		viewModelScope.launch {
			settingsRepository.setWeightUnit(unit)
		}
	}

	fun setRestTimerSeconds(seconds: Int) {
		viewModelScope.launch {
			settingsRepository.setRestTimerSeconds(seconds)
		}
	}

	fun setWeeklyWorkoutGoal(goal: Int) {
		viewModelScope.launch {
			settingsRepository.setWeeklyWorkoutGoal(goal)
		}
	}

	fun setTimerResumeMode(mode: TimerResumeMode) {
		viewModelScope.launch {
			settingsRepository.setTimerResumeMode(mode)
		}
	}

	fun setAppLanguage(language: AppLanguage) {
		viewModelScope.launch {
			settingsRepository.setAppLanguage(language)
		}
	}

	class Factory(
		private val settingsRepository: SettingsRepository
	) : ViewModelProvider.Factory {
		@Suppress("UNCHECKED_CAST")
		override fun <T : ViewModel> create(modelClass: Class<T>): T {
			return SettingsViewModel(settingsRepository) as T
		}
	}
}
