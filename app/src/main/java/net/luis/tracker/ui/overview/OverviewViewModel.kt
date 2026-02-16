package net.luis.tracker.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.luis.tracker.data.local.dao.CategoryWorkoutCount
import net.luis.tracker.data.local.dao.ExerciseProgress
import net.luis.tracker.data.local.dao.PersonalRecord
import net.luis.tracker.data.local.dao.WorkoutDateInfo
import net.luis.tracker.data.repository.ExerciseRepository
import net.luis.tracker.data.repository.StatsRepository
import net.luis.tracker.domain.model.ChartMetric
import net.luis.tracker.domain.model.Exercise
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

data class OverviewUiState(
	val currentMonth: YearMonth = YearMonth.now(),
	val workoutDays: Set<Int> = emptySet(),
	val workoutDayMap: Map<Int, List<Long>> = emptyMap(),
	val workoutsThisWeek: Int = 0,
	val workoutsThisMonth: Int = 0,
	val currentStreak: Int = 0,
	val averageDuration: Double? = null,
	val exercises: List<Exercise> = emptyList(),
	val selectedExerciseId: Long? = null,
	val selectedMetric: ChartMetric = ChartMetric.MAX_WEIGHT,
	val progressData: List<ExerciseProgress> = emptyList(),
	val isLoading: Boolean = true,
	val totalWorkoutsAllTime: Int = 0,
	val totalVolumeAllTime: Double = 0.0,
	val avgWorkoutsPerWeek: Double = 0.0,
	val longestWorkoutMinutes: Long = 0,
	val personalRecords: List<PersonalRecord> = emptyList(),
	val categoryBreakdown: List<CategoryWorkoutCount> = emptyList(),
	val selectedDayWorkouts: List<WorkoutDateInfo> = emptyList(),
	val showWorkoutPicker: Boolean = false,
	val navigateToWorkoutId: Long? = null
)

class OverviewViewModel(
	private val statsRepository: StatsRepository,
	private val exerciseRepository: ExerciseRepository,
	private val weeklyWorkoutGoal: Int
) : ViewModel() {

	private val _uiState = MutableStateFlow(OverviewUiState())
	val uiState: StateFlow<OverviewUiState> = _uiState.asStateFlow()

	private var progressJob: Job? = null
	private var monthDataJob: Job? = null

	init {
		viewModelScope.launch {
			exerciseRepository.getAllActive().collect { exercises ->
				_uiState.update { it.copy(exercises = exercises) }
			}
		}
		loadGlobalStats()
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

	fun onDayClick(dayNumber: Int) {
		val workoutIds = _uiState.value.workoutDayMap[dayNumber] ?: return
		if (workoutIds.size == 1) {
			_uiState.update { it.copy(navigateToWorkoutId = workoutIds.first()) }
		} else {
			// Multiple workouts - load details for bottom sheet
			val zone = ZoneId.systemDefault()
			val month = _uiState.value.currentMonth
			val dayStart = month.atDay(dayNumber).atStartOfDay(zone).toInstant().toEpochMilli()
			val dayEnd = month.atDay(dayNumber).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
			viewModelScope.launch {
				val workouts = statsRepository.getWorkoutIdsInRange(dayStart, dayEnd).first()
				_uiState.update {
					it.copy(
						selectedDayWorkouts = workouts,
						showWorkoutPicker = true
					)
				}
			}
		}
	}

	fun dismissWorkoutPicker() {
		_uiState.update { it.copy(showWorkoutPicker = false, selectedDayWorkouts = emptyList()) }
	}

	fun onNavigationHandled() {
		_uiState.update { it.copy(navigateToWorkoutId = null) }
	}

	private fun loadGlobalStats() {
		viewModelScope.launch {
			statsRepository.getPersonalRecords().collect { records ->
				_uiState.update { it.copy(personalRecords = records) }
			}
		}
		viewModelScope.launch {
			statsRepository.getCategoryBreakdown().collect { breakdown ->
				_uiState.update { it.copy(categoryBreakdown = breakdown) }
			}
		}
		viewModelScope.launch {
			statsRepository.getTotalWorkoutCount().collect { count ->
				_uiState.update { it.copy(totalWorkoutsAllTime = count) }
			}
		}
		viewModelScope.launch {
			statsRepository.getTotalVolume().collect { volume ->
				_uiState.update { it.copy(totalVolumeAllTime = volume ?: 0.0) }
			}
		}
		viewModelScope.launch {
			statsRepository.getLongestWorkoutDuration().collect { durationSeconds ->
				_uiState.update { it.copy(longestWorkoutMinutes = (durationSeconds ?: 0) / 60) }
			}
		}
		viewModelScope.launch {
			@OptIn(ExperimentalCoroutinesApi::class)
			statsRepository.getFirstWorkoutDate()
				.flatMapLatest { firstDate ->
					statsRepository.getTotalWorkoutCount().map { count ->
						if (firstDate != null && count > 0) {
							val firstLocalDate = Instant.ofEpochMilli(firstDate)
								.atZone(ZoneId.systemDefault()).toLocalDate()
							val daysSinceFirst = ChronoUnit.DAYS.between(firstLocalDate, LocalDate.now()).coerceAtLeast(1)
							val weeks = daysSinceFirst / 7.0
							if (weeks > 0) count / weeks else count.toDouble()
						} else {
							0.0
						}
					}
				}
				.collect { avg ->
					_uiState.update { it.copy(avgWorkoutsPerWeek = avg) }
				}
		}
	}

	private fun loadMonthData() {
		monthDataJob?.cancel()
		monthDataJob = viewModelScope.launch {
			val zone = ZoneId.systemDefault()
			val month = _uiState.value.currentMonth

			// Month range
			val monthStart = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
			val monthEnd = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

			// Week range (Monday to Sunday)
			val today = LocalDate.now()
			val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
			val weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).plusDays(1)
			val weekStartMillis = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
			val weekEndMillis = weekEnd.atStartOfDay(zone).toInstant().toEpochMilli()

			// Streak range (last 365 days)
			val streakRangeStart = today.minusDays(365).atStartOfDay(zone).toInstant().toEpochMilli()
			val streakRangeEnd = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

			launch {
				statsRepository.getWorkoutIdsInRange(monthStart, monthEnd).collect { workoutInfoList ->
					val workoutDayMap = workoutInfoList.groupBy { info ->
						Instant.ofEpochMilli(info.startTime).atZone(zone).toLocalDate().dayOfMonth
					}.mapValues { entry -> entry.value.map { it.workoutId } }
					_uiState.update {
						it.copy(
							workoutDays = workoutDayMap.keys,
							workoutDayMap = workoutDayMap,
							isLoading = false
						)
					}
				}
			}
			launch {
				statsRepository.getWorkoutCount(monthStart, monthEnd).collect { count ->
					_uiState.update { it.copy(workoutsThisMonth = count) }
				}
			}
			launch {
				statsRepository.getWorkoutCount(weekStartMillis, weekEndMillis).collect { count ->
					_uiState.update { it.copy(workoutsThisWeek = count) }
				}
			}
			launch {
				statsRepository.getAverageDuration(monthStart, monthEnd).collect { avg ->
					_uiState.update { it.copy(averageDuration = avg) }
				}
			}
			launch {
				statsRepository.getWorkoutDatesInRange(streakRangeStart, streakRangeEnd).collect { workoutDates ->
					val streak = calculateStreak(workoutDates, zone)
					_uiState.update { it.copy(currentStreak = streak) }
				}
			}
		}
	}

	private fun calculateStreak(workoutDateMillis: List<Long>, zone: ZoneId): Int {
		if (workoutDateMillis.isEmpty()) return 0

		val workoutDates = workoutDateMillis.map { millis ->
			Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
		}

		// Group by ISO week: pair of (week-based year, week number)
		val weekFields = java.time.temporal.WeekFields.ISO
		val workoutsPerWeek = workoutDates.groupBy { date ->
			val weekYear = date.get(weekFields.weekBasedYear())
			val weekNum = date.get(weekFields.weekOfWeekBasedYear())
			weekYear to weekNum
		}.mapValues { (_, dates) -> dates.toSet().size } // count distinct days

		val today = LocalDate.now()
		val currentWeekYear = today.get(weekFields.weekBasedYear())
		val currentWeekNum = today.get(weekFields.weekOfWeekBasedYear())

		// Determine starting point: if current week already meets the goal, count it;
		// otherwise skip it (it's still in progress) and start from the previous week
		var checkDate = today.with(weekFields.weekOfWeekBasedYear(), currentWeekNum.toLong())
			.with(weekFields.weekBasedYear(), currentWeekYear.toLong())
		val currentWeekKey = currentWeekYear to currentWeekNum
		val currentWeekCount = workoutsPerWeek[currentWeekKey] ?: 0

		var streak = 0
		if (currentWeekCount >= weeklyWorkoutGoal) {
			streak = 1
			checkDate = checkDate.minusWeeks(1)
		} else {
			// Current week doesn't meet goal yet, start checking from previous week
			checkDate = checkDate.minusWeeks(1)
		}

		// Count consecutive previous weeks that meet the goal
		while (true) {
			val weekYear = checkDate.get(weekFields.weekBasedYear())
			val weekNum = checkDate.get(weekFields.weekOfWeekBasedYear())
			val key = weekYear to weekNum
			val count = workoutsPerWeek[key] ?: 0
			if (count >= weeklyWorkoutGoal) {
				streak++
				checkDate = checkDate.minusWeeks(1)
			} else {
				break
			}
		}

		return streak
	}

	private fun loadProgressData() {
		progressJob?.cancel()
		progressJob = viewModelScope.launch {
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
		private val exerciseRepository: ExerciseRepository,
		private val weeklyWorkoutGoal: Int
	) : ViewModelProvider.Factory {
		@Suppress("UNCHECKED_CAST")
		override fun <T : ViewModel> create(modelClass: Class<T>): T {
			return OverviewViewModel(statsRepository, exerciseRepository, weeklyWorkoutGoal) as T
		}
	}
}
