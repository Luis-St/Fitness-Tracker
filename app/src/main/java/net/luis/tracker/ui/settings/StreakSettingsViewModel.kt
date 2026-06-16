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
import net.luis.tracker.data.repository.StatsRepository
import net.luis.tracker.domain.StreakCalculator
import net.luis.tracker.domain.StreakGap
import net.luis.tracker.domain.model.StreakException
import net.luis.tracker.domain.model.StreakExceptionType
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

data class StreakSettingsUiState(
	val computedStreak: Int = 0,
	val weeklyGoal: Int = 2,
	val baseline: Int = 0,
	val exceptions: List<StreakException> = emptyList(),
	val gaps: List<StreakGap> = emptyList(),
	val showRestoreDialog: Boolean = false
)

class StreakSettingsViewModel(
	private val statsRepository: StatsRepository,
	private val settingsRepository: SettingsRepository
) : ViewModel() {

	private val _uiState = MutableStateFlow(StreakSettingsUiState())
	val uiState: StateFlow<StreakSettingsUiState> = _uiState.asStateFlow()

	private val zone: ZoneId = ZoneId.systemDefault()
	private var latestDates: List<Long> = emptyList()

	init {
		viewModelScope.launch {
			val today = LocalDate.now()
			val rangeStart = today.minusDays(730).atStartOfDay(zone).toInstant().toEpochMilli()
			val rangeEnd = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

			combine(
				statsRepository.getWorkoutDatesInRange(rangeStart, rangeEnd),
				settingsRepository.weeklyWorkoutGoal,
				settingsRepository.streakExceptions,
				settingsRepository.streakBaseline
			) { dates, goal, exceptions, baseline ->
				val streak = StreakCalculator.calculateStreak(dates, goal, exceptions, baseline, zone, today)
				latestDates = dates
				StreakSettingsUiState(
					computedStreak = streak,
					weeklyGoal = goal,
					baseline = baseline,
					exceptions = exceptions
				)
			}.collect { state ->
				_uiState.update {
					// Preserve any open restore dialog and its gaps.
					state.copy(gaps = it.gaps, showRestoreDialog = it.showRestoreDialog)
				}
			}
		}
	}

	fun setWeeklyGoal(goal: Int) {
		viewModelScope.launch { settingsRepository.setWeeklyWorkoutGoal(goal) }
	}

	fun setBaseline(baseline: Int) {
		viewModelScope.launch { settingsRepository.setStreakBaseline(baseline) }
	}

	fun addPause(start: LocalDate, end: LocalDate) {
		val (from, to) = if (end.isBefore(start)) end to start else start to end
		val updated = _uiState.value.exceptions +
			StreakException(UUID.randomUUID().toString(), from, to, StreakExceptionType.PAUSE)
		viewModelScope.launch { settingsRepository.setStreakExceptions(updated) }
	}

	fun deleteException(id: String) {
		val updated = _uiState.value.exceptions.filterNot { it.id == id }
		viewModelScope.launch { settingsRepository.setStreakExceptions(updated) }
	}

	fun openRestoreDialog() {
		val gaps = StreakCalculator.findGaps(
			latestDates,
			_uiState.value.weeklyGoal,
			_uiState.value.exceptions,
			zone
		)
		_uiState.update { it.copy(gaps = gaps, showRestoreDialog = true) }
	}

	fun dismissRestoreDialog() {
		_uiState.update { it.copy(showRestoreDialog = false, gaps = emptyList()) }
	}

	fun restoreGaps(selected: List<StreakGap>) {
		if (selected.isNotEmpty()) {
			val newExceptions = selected.map { gap ->
				StreakException(
					id = UUID.randomUUID().toString(),
					start = gap.weekStart,
					end = gap.weekEnd,
					type = StreakExceptionType.RESTORE
				)
			}
			val updated = _uiState.value.exceptions + newExceptions
			viewModelScope.launch { settingsRepository.setStreakExceptions(updated) }
		}
		dismissRestoreDialog()
	}

	class Factory(
		private val statsRepository: StatsRepository,
		private val settingsRepository: SettingsRepository
	) : ViewModelProvider.Factory {
		@Suppress("UNCHECKED_CAST")
		override fun <T : ViewModel> create(modelClass: Class<T>): T {
			return StreakSettingsViewModel(statsRepository, settingsRepository) as T
		}
	}
}
