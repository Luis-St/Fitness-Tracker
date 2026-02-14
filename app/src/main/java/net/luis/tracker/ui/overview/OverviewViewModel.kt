package net.luis.tracker.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.luis.tracker.data.local.dao.ExerciseProgress
import net.luis.tracker.data.repository.ExerciseRepository
import net.luis.tracker.data.repository.StatsRepository
import net.luis.tracker.domain.model.ChartMetric
import net.luis.tracker.domain.model.Exercise
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class OverviewUiState(
	val currentMonth: YearMonth = YearMonth.now(),
	val workoutDays: Set<Int> = emptySet(),
	val workoutsThisWeek: Int = 0,
	val workoutsThisMonth: Int = 0,
	val currentStreak: Int = 0,
	val averageDuration: Double? = null,
	val exercises: List<Exercise> = emptyList(),
	val selectedExerciseId: Long? = null,
	val selectedMetric: ChartMetric = ChartMetric.MAX_WEIGHT,
	val progressData: List<ExerciseProgress> = emptyList(),
	val isLoading: Boolean = true
)

class OverviewViewModel(
	private val statsRepository: StatsRepository,
	private val exerciseRepository: ExerciseRepository
) : ViewModel() {

	private val _uiState = MutableStateFlow(OverviewUiState())
	val uiState: StateFlow<OverviewUiState> = _uiState.asStateFlow()

	init {
		viewModelScope.launch {
			exerciseRepository.getAllActive().collect { exercises ->
				_uiState.update { it.copy(exercises = exercises) }
			}
		}
		loadMonthData()
		loadProgressData()
	}

	fun changeMonth(yearMonth: YearMonth) {
		_uiState.update { it.copy(currentMonth = yearMonth) }
		loadMonthData()
	}

	fun selectExercise(exerciseId: Long?) {
		_uiState.update { it.copy(selectedExerciseId = exerciseId) }
		loadProgressData()
	}

	fun selectMetric(metric: ChartMetric) {
		_uiState.update { it.copy(selectedMetric = metric) }
	}

	private fun loadMonthData() {
		viewModelScope.launch {
			val state = _uiState.value
			val zone = ZoneId.systemDefault()
			val month = state.currentMonth

			// Month range
			val monthStart = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
			val monthEnd = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

			// Week range (Monday to Sunday)
			val today = LocalDate.now()
			val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
			val weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).plusDays(1)
			val weekStartMillis = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
			val weekEndMillis = weekEnd.atStartOfDay(zone).toInstant().toEpochMilli()

			// Workout days in the current month
			val workoutDates = statsRepository.getWorkoutDatesInRange(monthStart, monthEnd).first()
			val workoutDays = workoutDates.map { millis ->
				Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().dayOfMonth
			}.toSet()

			// Workouts this month
			val workoutsThisMonth = statsRepository.getWorkoutCount(monthStart, monthEnd).first()

			// Workouts this week
			val workoutsThisWeek = statsRepository.getWorkoutCount(weekStartMillis, weekEndMillis).first()

			// Average duration for the current month
			val averageDuration = statsRepository.getAverageDuration(monthStart, monthEnd).first()

			// Current streak calculation
			val currentStreak = calculateStreak(zone)

			_uiState.update {
				it.copy(
					workoutDays = workoutDays,
					workoutsThisWeek = workoutsThisWeek,
					workoutsThisMonth = workoutsThisMonth,
					currentStreak = currentStreak,
					averageDuration = averageDuration,
					isLoading = false
				)
			}
		}
	}

	private suspend fun calculateStreak(zone: ZoneId): Int {
		// Query a broad range to find recent workout dates (last 365 days)
		val today = LocalDate.now()
		val rangeStart = today.minusDays(365).atStartOfDay(zone).toInstant().toEpochMilli()
		val rangeEnd = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

		val workoutDates = statsRepository.getWorkoutDatesInRange(rangeStart, rangeEnd).first()
		val workoutLocalDates = workoutDates.map { millis ->
			Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
		}.toSet()

		var streak = 0
		// Start from today; if no workout today, try yesterday
		var checkDate = today
		if (!workoutLocalDates.contains(checkDate)) {
			checkDate = today.minusDays(1)
		}

		while (workoutLocalDates.contains(checkDate)) {
			streak++
			checkDate = checkDate.minusDays(1)
		}

		return streak
	}

	private fun loadProgressData() {
		viewModelScope.launch {
			val exerciseId = _uiState.value.selectedExerciseId
			val progressFlow = if (exerciseId != null) {
				statsRepository.getExerciseProgress(exerciseId)
			} else {
				statsRepository.getAllExerciseProgress()
			}
			progressFlow.collect { progressData ->
				_uiState.update { it.copy(progressData = progressData) }
			}
		}
	}

	class Factory(
		private val statsRepository: StatsRepository,
		private val exerciseRepository: ExerciseRepository
	) : ViewModelProvider.Factory {
		@Suppress("UNCHECKED_CAST")
		override fun <T : ViewModel> create(modelClass: Class<T>): T {
			return OverviewViewModel(statsRepository, exerciseRepository) as T
		}
	}
}
