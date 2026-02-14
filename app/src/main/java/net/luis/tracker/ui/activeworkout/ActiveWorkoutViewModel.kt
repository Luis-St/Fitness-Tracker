package net.luis.tracker.ui.activeworkout

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
	val elapsedSeconds: Long = 0,
	val exercises: List<ActiveExerciseEntry> = emptyList(),
	val availableExercises: List<Exercise> = emptyList(),
	val showSelectExercise: Boolean = false,
	val showDiscardDialog: Boolean = false
)

class ActiveWorkoutViewModel(
	private val exerciseRepository: ExerciseRepository,
	private val workoutRepository: WorkoutRepository
) : ViewModel() {

	private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
	val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

	private var timerJob: Job? = null
	private var startTimeMillis: Long = 0L
	private var entryIdCounter: Long = 0L

	/** The entry ID currently being edited when the exercise selector is open for swapping. */
	var editingEntryId: Long? = null
		private set

	init {
		viewModelScope.launch {
			exerciseRepository.getAllActive().collect { exercises ->
				_uiState.update { it.copy(availableExercises = exercises) }
			}
		}
	}

	fun startWorkout() {
		if (_uiState.value.isRunning) return
		startTimeMillis = System.currentTimeMillis()
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
			_uiState.update { it.copy(isPaused = true) }
			timerJob?.cancel()
		}
	}

	private fun startTimer() {
		timerJob?.cancel()
		timerJob = viewModelScope.launch {
			while (isActive) {
				delay(1000)
				_uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
			}
		}
	}

	fun showExerciseSelector(forEntryId: Long? = null) {
		editingEntryId = forEntryId
		_uiState.update { it.copy(showSelectExercise = true) }
	}

	fun hideExerciseSelector() {
		editingEntryId = null
		_uiState.update { it.copy(showSelectExercise = false) }
	}

	fun addExercise(exercise: Exercise) {
		val newEntry = ActiveExerciseEntry(
			id = ++entryIdCounter,
			exercise = exercise
		)
		_uiState.update { it.copy(
			exercises = it.exercises + newEntry,
			showSelectExercise = false
		) }
		editingEntryId = null
	}

	fun removeExercise(entryId: Long) {
		_uiState.update { state ->
			state.copy(exercises = state.exercises.filter { it.id != entryId })
		}
	}

	fun updateExerciseSelection(entryId: Long, exercise: Exercise) {
		_uiState.update { state ->
			state.copy(
				exercises = state.exercises.map { entry ->
					if (entry.id == entryId) entry.copy(exercise = exercise) else entry
				},
				showSelectExercise = false
			)
		}
		editingEntryId = null
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
		_uiState.update { it.copy(showDiscardDialog = true) }
	}

	fun cancelDiscard() {
		_uiState.update { it.copy(showDiscardDialog = false) }
	}

	fun confirmDiscard(onDiscarded: () -> Unit) {
		timerJob?.cancel()
		_uiState.update { ActiveWorkoutUiState() }
		onDiscarded()
	}

	fun finishWorkout(onFinished: () -> Unit) {
		val state = _uiState.value
		timerJob?.cancel()

		val endTimeMillis = System.currentTimeMillis()

		val workoutExercises = state.exercises.mapIndexed { index, entry ->
			WorkoutExercise(
				exercise = entry.exercise,
				orderIndex = index,
				sets = entry.sets
			)
		}

		val workout = Workout(
			startTime = startTimeMillis,
			endTime = endTimeMillis,
			durationSeconds = state.elapsedSeconds,
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
