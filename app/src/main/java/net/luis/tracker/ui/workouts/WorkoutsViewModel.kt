package net.luis.tracker.ui.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.luis.tracker.data.repository.WorkoutRepository
import net.luis.tracker.domain.model.Workout

data class WorkoutsUiState(
	val workouts: List<Workout> = emptyList(),
	val isLoading: Boolean = true
)

class WorkoutsViewModel(
	private val workoutRepository: WorkoutRepository
) : ViewModel() {

	private val _uiState = MutableStateFlow(WorkoutsUiState())
	val uiState: StateFlow<WorkoutsUiState> = _uiState.asStateFlow()

	init {
		viewModelScope.launch {
			workoutRepository.getAllWithExercises().collect { workouts ->
				_uiState.update {
					it.copy(
						workouts = workouts.sortedByDescending { w -> w.startTime },
						isLoading = false
					)
				}
			}
		}
	}

	fun deleteWorkout(workout: Workout) {
		viewModelScope.launch {
			workoutRepository.delete(workout)
		}
	}

	class Factory(
		private val workoutRepository: WorkoutRepository
	) : ViewModelProvider.Factory {
		@Suppress("UNCHECKED_CAST")
		override fun <T : ViewModel> create(modelClass: Class<T>): T {
			return WorkoutsViewModel(workoutRepository) as T
		}
	}
}
