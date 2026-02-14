package net.luis.tracker.ui.activeworkout

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.luis.tracker.data.repository.ExerciseRepository
import net.luis.tracker.data.repository.WorkoutRepository
import net.luis.tracker.domain.model.Exercise
import net.luis.tracker.domain.model.Workout
import net.luis.tracker.domain.model.WorkoutExercise
import net.luis.tracker.domain.model.WorkoutSet

data class ActiveExerciseEntry(
	val id: Long,
	val exercise: Exercise,
	val sets: List<WorkoutSet> = emptyList()
)

data class ActiveWorkoutUiState(
	val isRunning: Boolean = false,
	val isPaused: Boolean = false,
	val exercises: List<ActiveExerciseEntry> = emptyList()
)

class ActiveWorkoutViewModel(
	private val exerciseRepository: ExerciseRepository,
	private val workoutRepository: WorkoutRepository
) : ViewModel() {

	private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
	val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

	private val _elapsedMillis = MutableStateFlow(0L)
	val elapsedMillis: StateFlow<Long> = _elapsedMillis.asStateFlow()

	private val _showDiscardDialog = MutableStateFlow(false)
	val showDiscardDialog: StateFlow<Boolean> = _showDiscardDialog.asStateFlow()

	private val _availableExercises = MutableStateFlow<List<Exercise>?>(null)
	val availableExercises: StateFlow<List<Exercise>?> = _availableExercises.asStateFlow()

	private var timerJob: Job? = null
	private var startTimeMillis: Long = 0L
	private var timerBaseElapsed: Long = 0L
	private var timerResumedAt: Long = 0L
	private var entryIdCounter: Long = 0L

	init {
		viewModelScope.launch {
			exerciseRepository.getAllActive().collect { exercises ->
				_availableExercises.value = exercises
			}
		}
	}

	fun startWorkout() {
		if (_uiState.value.isRunning) return
		startTimeMillis = System.currentTimeMillis()
		timerBaseElapsed = 0L
		_uiState.update { it.copy(isRunning = true, isPaused = false) }
		startTimer()
	}

	fun togglePause() {
		val current = _uiState.value
		if (!current.isRunning) return
		if (current.isPaused) {
			_uiState.update { it.copy(isPaused = false) }
			startTimer()
		} else {
			timerBaseElapsed += SystemClock.elapsedRealtime() - timerResumedAt
			_uiState.update { it.copy(isPaused = true) }
			timerJob?.cancel()
		}
	}

	private fun startTimer() {
		timerJob?.cancel()
		timerResumedAt = SystemClock.elapsedRealtime()
		timerJob = viewModelScope.launch {
			while (isActive) {
				val now = SystemClock.elapsedRealtime()
				_elapsedMillis.value = timerBaseElapsed + (now - timerResumedAt)
				delay(250)
			}
		}
	}

	fun addExercise(exercise: Exercise): Long {
		val newId = ++entryIdCounter
		val newEntry = ActiveExerciseEntry(
			id = newId,
			exercise = exercise
		)
		_uiState.update { it.copy(exercises = it.exercises + newEntry) }
		return newId
	}

	fun removeExerciseIfEmpty(entryId: Long) {
		_uiState.update { state ->
			val entry = state.exercises.find { it.id == entryId }
			if (entry != null && entry.sets.isEmpty()) {
				state.copy(exercises = state.exercises.filter { it.id != entryId })
			} else {
				state
			}
		}
	}

	fun removeExercise(entryId: Long) {
		_uiState.update { state ->
			state.copy(exercises = state.exercises.filter { it.id != entryId })
		}
	}

	fun addSet(entryId: Long, weightKg: Double, reps: Int) {
		_uiState.update { state ->
			state.copy(
				exercises = state.exercises.map { entry ->
					if (entry.id == entryId) {
						val nextSetNumber = entry.sets.size + 1
						val newSet = WorkoutSet(
							setNumber = nextSetNumber,
							weightKg = weightKg,
							reps = reps
						)
						entry.copy(sets = entry.sets + newSet)
					} else {
						entry
					}
				}
			)
		}
	}

	fun removeSet(entryId: Long, setIndex: Int) {
		_uiState.update { state ->
			state.copy(
				exercises = state.exercises.map { entry ->
					if (entry.id == entryId) {
						val updatedSets = entry.sets
							.filterIndexed { index, _ -> index != setIndex }
							.mapIndexed { index, set -> set.copy(setNumber = index + 1) }
						entry.copy(sets = updatedSets)
					} else {
						entry
					}
				}
			)
		}
	}

	fun requestDiscard() {
		_showDiscardDialog.value = true
	}

	fun cancelDiscard() {
		_showDiscardDialog.value = false
	}

	fun confirmDiscard(onDiscarded: () -> Unit) {
		timerJob?.cancel()
		_showDiscardDialog.value = false
		_uiState.update { ActiveWorkoutUiState() }
		_elapsedMillis.value = 0L
		timerBaseElapsed = 0L
		onDiscarded()
	}

	fun finishWorkout(onFinished: () -> Unit) {
		val state = _uiState.value
		timerJob?.cancel()

		if (state.exercises.isEmpty() || state.exercises.all { it.sets.isEmpty() }) {
			_uiState.update { ActiveWorkoutUiState() }
			_elapsedMillis.value = 0
			onFinished()
			return
		}

		val endTimeMillis = System.currentTimeMillis()

		val workoutExercises = state.exercises
			.filter { it.sets.isNotEmpty() }
			.mapIndexed { index, entry ->
				WorkoutExercise(
					exercise = entry.exercise,
					orderIndex = index,
					sets = entry.sets
				)
			}

		val workout = Workout(
			startTime = startTimeMillis,
			endTime = endTimeMillis,
			durationSeconds = _elapsedMillis.value / 1000,
			exercises = workoutExercises
		)

		viewModelScope.launch {
			workoutRepository.saveFullWorkout(workout)
			onFinished()
		}
	}

	class Factory(
		private val exerciseRepository: ExerciseRepository,
		private val workoutRepository: WorkoutRepository
	) : ViewModelProvider.Factory {
		@Suppress("UNCHECKED_CAST")
		override fun <T : ViewModel> create(modelClass: Class<T>): T {
			return ActiveWorkoutViewModel(exerciseRepository, workoutRepository) as T
		}
	}
}
