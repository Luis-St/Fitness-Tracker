package net.luis.tracker.ui.activeworkout

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.luis.tracker.data.draft.DraftExerciseEntry
import net.luis.tracker.data.draft.DraftWorkoutSet
import net.luis.tracker.data.draft.WorkoutDraft
import net.luis.tracker.data.repository.ActiveWorkoutDraftRepository
import net.luis.tracker.data.repository.ExerciseRepository
import net.luis.tracker.data.repository.WorkoutRepository
import net.luis.tracker.domain.model.Category
import net.luis.tracker.domain.model.Exercise
import net.luis.tracker.domain.model.TimerResumeMode
import net.luis.tracker.domain.model.Workout
import net.luis.tracker.domain.model.WorkoutExercise
import net.luis.tracker.domain.model.WorkoutSet

data class ActiveExerciseEntry(
	val id: Long,
	val exercise: Exercise,
	val sets: List<WorkoutSet> = emptyList(),
	val isGhost: Boolean = false,
	val planWeightKg: Double = 0.0,
	val planSets: Int = 0,
	val planSetsData: List<WorkoutSet> = emptyList()
)

data class ActiveWorkoutUiState(
	val isRunning: Boolean = false,
	val isPaused: Boolean = false,
	val exercises: List<ActiveExerciseEntry> = emptyList()
)

class ActiveWorkoutViewModel(
	private val exerciseRepository: ExerciseRepository,
	private val workoutRepository: WorkoutRepository,
	private val draftRepository: ActiveWorkoutDraftRepository,
	private val resumeWorkoutId: Long = 0L,
	private val planWorkoutId: Long = 0L,
	private val timerResumeMode: TimerResumeMode = TimerResumeMode.RESUME
) : ViewModel() {

	private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
	val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

	private val _elapsedMillis = MutableStateFlow(0L)
	val elapsedMillis: StateFlow<Long> = _elapsedMillis.asStateFlow()

	private val _showDiscardDialog = MutableStateFlow(false)
	val showDiscardDialog: StateFlow<Boolean> = _showDiscardDialog.asStateFlow()

	private val _availableExercises = MutableStateFlow<List<Exercise>?>(null)
	val availableExercises: StateFlow<List<Exercise>?> = _availableExercises.asStateFlow()

	private val _isReady = MutableStateFlow(false)
	val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

	private var timerJob: Job? = null
	private var saveDraftJob: Job? = null
	private var startTimeMillis: Long = 0L
	private var timerBaseElapsed: Long = 0L
	private var timerResumedAt: Long = 0L
	private var entryIdCounter: Long = 0L
	private var resumedFromWorkoutId: Long = 0L
	private var sourcePlanWorkoutId: Long = 0L

	init {
		viewModelScope.launch {
			exerciseRepository.getAllActive().collect { exercises ->
				_availableExercises.value = exercises
			}
		}
		viewModelScope.launch {
			if (resumeWorkoutId > 0L) {
				restoreFromWorkout(resumeWorkoutId)
			} else if (planWorkoutId > 0L) {
				loadPlan(planWorkoutId)
			} else {
				val draft = draftRepository.loadDraft()
				if (draft != null) {
					restoreFromDraft(draft)
				}
			}
			_isReady.value = true
		}
	}

	fun startWorkout() {
		if (_uiState.value.isRunning) return
		startTimeMillis = System.currentTimeMillis()
		timerBaseElapsed = 0L
		_uiState.update { it.copy(isRunning = true, isPaused = false) }
		startTimer()
		saveDraft()
	}

	fun togglePause() {
		val current = _uiState.value
		if (!current.isRunning) return
		if (current.isPaused) {
			_uiState.update { it.copy(isPaused = false) }
			startTimer()
		} else {
			timerBaseElapsed += SystemClock.elapsedRealtime() - timerResumedAt
			_elapsedMillis.value = timerBaseElapsed
			_uiState.update { it.copy(isPaused = true) }
			timerJob?.cancel()
		}
		saveDraft()
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
		saveDraft()
		return newId
	}

	fun removeExerciseIfEmpty(entryId: Long) {
		_uiState.update { state ->
			val entry = state.exercises.find { it.id == entryId }
			if (entry != null && entry.sets.isEmpty() && !entry.isGhost) {
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
		saveDraft()
	}

	fun addSet(entryId: Long, weightKg: Double, reps: Int, dropWeightKg: Double? = null, dropReps: Int? = null) {
		_uiState.update { state ->
			val index = state.exercises.indexOfFirst { it.id == entryId }
			if (index < 0) return@update state

			val entry = state.exercises[index]
			val newSet = WorkoutSet(setNumber = entry.sets.size + 1, weightKg = weightKg, reps = reps, dropWeightKg = dropWeightKg, dropReps = dropReps)
			val updatedEntry = entry.copy(sets = entry.sets + newSet)

			if (!entry.isGhost || entry.sets.isNotEmpty()) {
				state.copy(exercises = state.exercises.map { if (it.id == entryId) updatedEntry else it })
			} else {
				// First real set on a ghost exercise: move it right after the last active exercise
				val list = state.exercises.toMutableList()
				list.removeAt(index)
				val insertAt = list.indexOfLast { it.sets.isNotEmpty() } + 1
				list.add(insertAt, updatedEntry)
				state.copy(exercises = list)
			}
		}
		saveDraft()
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
		saveDraft()
	}

	fun requestDiscard() {
		_showDiscardDialog.value = true
	}

	fun cancelDiscard() {
		_showDiscardDialog.value = false
	}

	fun confirmDiscard(onDiscarded: () -> Unit) {
		timerJob?.cancel()
		saveDraftJob?.cancel()
		_showDiscardDialog.value = false
		_uiState.update { ActiveWorkoutUiState() }
		_elapsedMillis.value = 0L
		timerBaseElapsed = 0L
		onDiscarded()
		viewModelScope.launch {
			withContext(NonCancellable) { draftRepository.clearDraft() }
		}
	}

	fun finishWorkout(onFinished: () -> Unit) {
		val state = _uiState.value
		timerJob?.cancel()
		saveDraftJob?.cancel()

		if (state.exercises.isEmpty() || state.exercises.all { it.sets.isEmpty() }) {
			_uiState.update { ActiveWorkoutUiState() }
			_elapsedMillis.value = 0
			onFinished()
			viewModelScope.launch {
				withContext(NonCancellable) { draftRepository.clearDraft() }
			}
			return
		}

		val endTimeMillis = System.currentTimeMillis()
		val durationSeconds = _elapsedMillis.value / 1000
		val oldWorkoutId = resumedFromWorkoutId

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
			durationSeconds = durationSeconds,
			isFinished = true,
			exercises = workoutExercises
		)

		// Navigate immediately — DB writes complete in the background
		onFinished()

		viewModelScope.launch {
			withContext(NonCancellable) {
				if (oldWorkoutId > 0L) {
					workoutRepository.deleteById(oldWorkoutId)
				}
				workoutRepository.saveFullWorkout(workout)
				draftRepository.clearDraft()
			}
		}
	}

	private fun saveDraft() {
		val state = _uiState.value
		if (!state.isRunning) return
		saveDraftJob?.cancel()
		saveDraftJob = viewModelScope.launch {
			delay(500)
			draftRepository.saveDraft(createDraft())
		}
	}

	private fun saveDraftImmediate() {
		saveDraftJob?.cancel()
		saveDraftJob = viewModelScope.launch {
			draftRepository.saveDraft(createDraft())
		}
	}

	private fun createDraft(): WorkoutDraft {
		val state = _uiState.value
		val currentElapsed = if (state.isPaused) {
			timerBaseElapsed
		} else {
			timerBaseElapsed + (SystemClock.elapsedRealtime() - timerResumedAt)
		}
		return WorkoutDraft(
			startTimeMillis = startTimeMillis,
			elapsedMillis = currentElapsed,
			entryIdCounter = entryIdCounter,
			planWorkoutId = sourcePlanWorkoutId,
			exercises = state.exercises.map { entry ->
				DraftExerciseEntry(
					entryId = entry.id,
					exerciseId = entry.exercise.id,
					title = entry.exercise.title,
					notes = entry.exercise.notes,
					hasWeight = entry.exercise.hasWeight,
					categoryId = entry.exercise.category?.id,
					categoryName = entry.exercise.category?.name,
					sets = entry.sets.map { set ->
						DraftWorkoutSet(
							setNumber = set.setNumber,
							weightKg = set.weightKg,
							reps = set.reps,
							dropWeightKg = set.dropWeightKg,
							dropReps = set.dropReps
						)
					},
					isGhost = entry.isGhost,
					planWeightKg = entry.planWeightKg,
					planSets = entry.planSets,
				planSetsData = entry.planSetsData.map { set ->
					DraftWorkoutSet(
						setNumber = set.setNumber,
						weightKg = set.weightKg,
						reps = set.reps,
						dropWeightKg = set.dropWeightKg,
						dropReps = set.dropReps
					)
				}
				)
			}
		)
	}

	private suspend fun restoreFromDraft(draft: WorkoutDraft) {
		startTimeMillis = draft.startTimeMillis
		timerBaseElapsed = draft.elapsedMillis
		entryIdCounter = draft.entryIdCounter
		sourcePlanWorkoutId = draft.planWorkoutId
		_elapsedMillis.value = draft.elapsedMillis

		val exercises = withContext(Dispatchers.Default) {
			draft.exercises.map { entry ->
				ActiveExerciseEntry(
					id = entry.entryId,
					exercise = Exercise(
						id = entry.exerciseId,
						title = entry.title,
						notes = entry.notes,
						hasWeight = entry.hasWeight,
						category = if (entry.categoryId != null && entry.categoryName != null) {
							Category(id = entry.categoryId, name = entry.categoryName)
						} else {
							null
						}
					),
					sets = entry.sets.map { set ->
						WorkoutSet(
							setNumber = set.setNumber,
							weightKg = set.weightKg,
							reps = set.reps,
							dropWeightKg = set.dropWeightKg,
							dropReps = set.dropReps
						)
					},
					isGhost = entry.isGhost,
					planWeightKg = entry.planWeightKg,
					planSets = entry.planSets,
					planSetsData = entry.planSetsData.map { set ->
						WorkoutSet(
							setNumber = set.setNumber,
							weightKg = set.weightKg,
							reps = set.reps,
							dropWeightKg = set.dropWeightKg,
							dropReps = set.dropReps
						)
					}
				)
			}
		}

		val autoResume = timerResumeMode == TimerResumeMode.RESUME
		_uiState.value = ActiveWorkoutUiState(
			isRunning = true,
			isPaused = !autoResume,
			exercises = exercises
		)
		if (autoResume) startTimer()
	}

	private suspend fun restoreFromWorkout(workoutId: Long) {
		val workout = workoutRepository.getByIdWithExercises(workoutId) ?: return

		resumedFromWorkoutId = workoutId
		sourcePlanWorkoutId = workout.planWorkoutId ?: 0L
		startTimeMillis = workout.startTime
		timerBaseElapsed = workout.durationSeconds * 1000
		_elapsedMillis.value = timerBaseElapsed

		val templateExercises: Map<Long, WorkoutExercise> = if (workout.planWorkoutId != null) {
			workoutRepository.getByIdWithExercises(workout.planWorkoutId)
				?.exercises?.associateBy { it.exercise.id } ?: emptyMap()
		} else {
			emptyMap()
		}

		var nextEntryId = 0L
		val restoredExerciseIds = mutableSetOf<Long>()
		val exercises = workout.exercises.map { we ->
			val entryId = ++nextEntryId
			restoredExerciseIds.add(we.exercise.id)
			ActiveExerciseEntry(
				id = entryId,
				exercise = we.exercise,
				sets = we.sets,
				planSetsData = templateExercises[we.exercise.id]?.sets ?: emptyList()
			)
		}.toMutableList()

		templateExercises.values
			.filter { it.exercise.id !in restoredExerciseIds }
			.forEach { we ->
				val entryId = ++nextEntryId
				exercises.add(
					ActiveExerciseEntry(
						id = entryId,
						exercise = we.exercise,
						isGhost = true,
						planWeightKg = we.sets.maxOfOrNull { it.weightKg } ?: 0.0,
						planSets = we.sets.size,
						planSetsData = we.sets
					)
				)
			}

		entryIdCounter = nextEntryId

		val autoResume = timerResumeMode == TimerResumeMode.RESUME
		_uiState.value = ActiveWorkoutUiState(
			isRunning = true,
			isPaused = !autoResume,
			exercises = exercises
		)
		if (autoResume) startTimer()
	}

	private suspend fun loadPlan(workoutId: Long) {
		val workout = workoutRepository.getByIdWithExercises(workoutId) ?: return
		sourcePlanWorkoutId = workoutId

		var nextEntryId = 0L
		val exercises = workout.exercises.map { we ->
			val entryId = ++nextEntryId
			val maxWeight = we.sets.maxOfOrNull { it.weightKg } ?: 0.0
			ActiveExerciseEntry(
				id = entryId,
				exercise = we.exercise,
				sets = emptyList(),
				isGhost = true,
				planWeightKg = maxWeight,
				planSets = we.sets.size,
				planSetsData = we.sets
			)
		}
		entryIdCounter = nextEntryId

		_uiState.value = ActiveWorkoutUiState(exercises = exercises)
	}

	class Factory(
		private val exerciseRepository: ExerciseRepository,
		private val workoutRepository: WorkoutRepository,
		private val draftRepository: ActiveWorkoutDraftRepository,
		private val resumeWorkoutId: Long = 0L,
		private val planWorkoutId: Long = 0L,
		private val timerResumeMode: TimerResumeMode = TimerResumeMode.RESUME
	) : ViewModelProvider.Factory {
		@Suppress("UNCHECKED_CAST")
		override fun <T : ViewModel> create(modelClass: Class<T>): T {
			return ActiveWorkoutViewModel(exerciseRepository, workoutRepository, draftRepository, resumeWorkoutId, planWorkoutId, timerResumeMode) as T
		}
	}
}
