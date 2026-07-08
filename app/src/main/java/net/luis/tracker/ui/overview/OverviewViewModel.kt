package net.luis.tracker.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.luis.tracker.data.local.dao.CategoryWorkoutCount
import net.luis.tracker.data.local.dao.ExerciseProgress
import net.luis.tracker.data.local.dao.PersonalRecord
import net.luis.tracker.data.local.dao.RecentWorkout
import net.luis.tracker.data.local.dao.TopExercise
import net.luis.tracker.data.local.dao.WorkoutDateInfo
import net.luis.tracker.data.repository.ExerciseRepository
import net.luis.tracker.data.repository.SettingsRepository
import net.luis.tracker.data.repository.StatsRepository
import net.luis.tracker.domain.StreakCalculator
import net.luis.tracker.domain.model.ChartMetric
import net.luis.tracker.domain.model.Exercise
import net.luis.tracker.domain.model.OverviewLayout
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class OverviewTab { MONTH, LIFETIME }

data class OverviewUiState(
	val selectedTab: OverviewTab = OverviewTab.MONTH,
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
	val monthProgressData: List<ExerciseProgress> = emptyList(),
	val isLoading: Boolean = true,
	val totalWorkoutsAllTime: Int = 0,
	val maxWorkoutVolume: Double = 0.0,
	val avgWorkoutsPerWeek: Double = 0.0,
	val longestWorkoutMinutes: Long = 0,
	val totalSets: Int = 0,
	val totalReps: Long = 0,
	val totalTimeSeconds: Long = 0,
	val recentWorkouts: List<RecentWorkout> = emptyList(),
	val topExercises: List<TopExercise> = emptyList(),
	val avgWorkoutGapDays: Double? = null,
	val longestWorkoutGapDays: Int = 0,
	val mostActiveWeekday: DayOfWeek? = null,
	val workoutCountByDay: Map<LocalDate, Int> = emptyMap(),
	val personalRecords: List<PersonalRecord> = emptyList(),
	val categoryBreakdown: List<CategoryWorkoutCount> = emptyList(),
	val monthCategoryBreakdown: List<CategoryWorkoutCount> = emptyList(),
	val selectedDayWorkouts: List<WorkoutDateInfo> = emptyList(),
	val showWorkoutPicker: Boolean = false,
	val navigateToWorkoutId: Long? = null,
	val overviewLayout: OverviewLayout = OverviewLayout.DEFAULT
)

class OverviewViewModel(
	private val statsRepository: StatsRepository,
	private val exerciseRepository: ExerciseRepository,
	private val settingsRepository: SettingsRepository
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
		viewModelScope.launch {
			settingsRepository.overviewLayout.collect { layout ->
				_uiState.update { it.copy(overviewLayout = layout) }
			}
		}
		loadStreak()
		loadGlobalStats()
		loadMonthData()
		loadProgressData()
	}

	private fun loadStreak() {
		viewModelScope.launch {
			val zone = ZoneId.systemDefault()
			val today = LocalDate.now()
			val rangeStart = today.minusDays(730).atStartOfDay(zone).toInstant().toEpochMilli()
			val rangeEnd = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

			combine(
				statsRepository.getWorkoutDatesInRange(rangeStart, rangeEnd),
				settingsRepository.weeklyWorkoutGoal,
				settingsRepository.streakExceptions,
				settingsRepository.streakBaseline
			) { dates, goal, exceptions, baseline ->
				StreakCalculator.calculateStreak(dates, goal, exceptions, baseline, zone, today)
			}.collect { streak ->
				_uiState.update { it.copy(currentStreak = streak) }
			}
		}
	}

	fun selectTab(tab: OverviewTab) {
		_uiState.update { it.copy(selectedTab = tab) }
	}

	fun changeMonth(yearMonth: YearMonth) {
		_uiState.update { it.copy(currentMonth = yearMonth) }
		loadMonthData()
		loadProgressData()
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
		if (workoutIds.isEmpty()) return
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
			statsRepository.getMaxWorkoutVolume().collect { volume ->
				_uiState.update { it.copy(maxWorkoutVolume = volume ?: 0.0) }
			}
		}
		viewModelScope.launch {
			statsRepository.getLongestWorkoutDuration().collect { durationSeconds ->
				_uiState.update { it.copy(longestWorkoutMinutes = (durationSeconds ?: 0) / 60) }
			}
		}
		viewModelScope.launch {
			statsRepository.getTotalSetCount().collect { count ->
				_uiState.update { it.copy(totalSets = count) }
			}
		}
		viewModelScope.launch {
			statsRepository.getTotalReps().collect { reps ->
				_uiState.update { it.copy(totalReps = reps) }
			}
		}
		viewModelScope.launch {
			statsRepository.getTotalDurationSeconds().collect { seconds ->
				_uiState.update { it.copy(totalTimeSeconds = seconds) }
			}
		}
		viewModelScope.launch {
			statsRepository.getRecentWorkouts(RECENT_WORKOUTS_LIMIT).collect { workouts ->
				_uiState.update { it.copy(recentWorkouts = workouts) }
			}
		}
		viewModelScope.launch {
			statsRepository.getTopExercises(TOP_EXERCISES_LIMIT).collect { exercises ->
				_uiState.update { it.copy(topExercises = exercises) }
			}
		}
		viewModelScope.launch {
			statsRepository.getAllWorkoutStartTimes().collect { startTimes ->
				val zone = ZoneId.systemDefault()
				val countByDay = startTimes.groupingBy {
					Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
				}.eachCount()
				val distinctDays = countByDay.keys.sorted()
				val gaps = distinctDays.zipWithNext { a, b -> ChronoUnit.DAYS.between(a, b) }
				val avgGap = if (gaps.isNotEmpty()) gaps.average() else null
				val longestGap = (gaps.maxOrNull() ?: 0L).toInt()
				val mostActive = distinctDays
					.groupingBy { it.dayOfWeek }
					.eachCount()
					.maxByOrNull { it.value }
					?.key
				_uiState.update {
					it.copy(
						workoutCountByDay = countByDay,
						avgWorkoutGapDays = avgGap,
						longestWorkoutGapDays = longestGap,
						mostActiveWeekday = mostActive
					)
				}
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

	private fun monthRange(month: YearMonth): Pair<Long, Long> {
		val zone = ZoneId.systemDefault()
		val monthStart = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
		val monthEnd = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
		return monthStart to monthEnd
	}

	private fun loadMonthData() {
		monthDataJob?.cancel()
		monthDataJob = viewModelScope.launch {
			val zone = ZoneId.systemDefault()
			val month = _uiState.value.currentMonth

			// Month range
			val (monthStart, monthEnd) = monthRange(month)

			// Week range (Monday to Sunday)
			val today = LocalDate.now()
			val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
			val weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).plusDays(1)
			val weekStartMillis = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
			val weekEndMillis = weekEnd.atStartOfDay(zone).toInstant().toEpochMilli()

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
				statsRepository.getCategoryBreakdown(monthStart, monthEnd).collect { breakdown ->
					_uiState.update { it.copy(monthCategoryBreakdown = breakdown) }
				}
			}
		}
	}

	private fun loadProgressData() {
		progressJob?.cancel()
		progressJob = viewModelScope.launch {
			val exerciseId = _uiState.value.selectedExerciseId
			val (monthStart, monthEnd) = monthRange(_uiState.value.currentMonth)

			// All-time progress (Lifetime tab)
			launch {
				val progressFlow = if (exerciseId != null) {
					statsRepository.getExerciseProgress(exerciseId)
				} else {
					statsRepository.getAllExerciseProgress()
				}
				progressFlow.collect { progressData ->
					_uiState.update { it.copy(progressData = progressData) }
				}
			}

			// Month-scoped progress (This Month tab)
			launch {
				val monthFlow = if (exerciseId != null) {
					statsRepository.getExerciseProgress(exerciseId, monthStart, monthEnd)
				} else {
					statsRepository.getAllExerciseProgress(monthStart, monthEnd)
				}
				monthFlow.collect { progressData ->
					_uiState.update { it.copy(monthProgressData = progressData) }
				}
			}
		}
	}

	class Factory(
		private val statsRepository: StatsRepository,
		private val exerciseRepository: ExerciseRepository,
		private val settingsRepository: SettingsRepository
	) : ViewModelProvider.Factory {
		@Suppress("UNCHECKED_CAST")
		override fun <T : ViewModel> create(modelClass: Class<T>): T {
			return OverviewViewModel(statsRepository, exerciseRepository, settingsRepository) as T
		}
	}

	companion object {
		private const val RECENT_WORKOUTS_LIMIT = 5
		private const val TOP_EXERCISES_LIMIT = 5
	}
}
