package net.luis.tracker.ui.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.luis.tracker.data.repository.CategoryRepository
import net.luis.tracker.data.repository.ExerciseRepository
import net.luis.tracker.domain.model.Category
import net.luis.tracker.domain.model.Exercise

data class ExercisesUiState(
	val exercises: List<Exercise> = emptyList(),
	val categories: List<Category> = emptyList(),
	val selectedCategoryId: Long? = null, // null = all
	val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class ExercisesViewModel(
	private val exerciseRepository: ExerciseRepository,
	private val categoryRepository: CategoryRepository
) : ViewModel() {

	private val _uiState = MutableStateFlow(ExercisesUiState())
	val uiState: StateFlow<ExercisesUiState> = _uiState.asStateFlow()

	private val selectedCategory = MutableStateFlow<Long?>(null)

	init {
		// Combine categories flow with selected category to load exercises
		viewModelScope.launch {
			categoryRepository.getAll().collect { categories ->
				_uiState.update { it.copy(categories = categories) }
			}
		}
		viewModelScope.launch {
			selectedCategory.flatMapLatest { categoryId ->
				if (categoryId == null) {
					exerciseRepository.getAllActive()
				} else {
					exerciseRepository.getByCategory(categoryId)
				}
			}.collect { exercises ->
				_uiState.update { it.copy(exercises = exercises, isLoading = false) }
			}
		}
	}

	fun selectCategory(categoryId: Long?) {
		selectedCategory.value = categoryId
		_uiState.update { it.copy(selectedCategoryId = categoryId) }
	}

	fun deleteExercise(id: Long) {
		viewModelScope.launch {
			exerciseRepository.softDelete(id)
		}
	}

	// Category management
	fun addCategory(name: String) {
		viewModelScope.launch {
			categoryRepository.insert(Category(name = name))
		}
	}

	fun updateCategory(category: Category) {
		viewModelScope.launch {
			categoryRepository.update(category)
		}
	}

	fun deleteCategory(category: Category) {
		viewModelScope.launch {
			categoryRepository.delete(category)
			if (_uiState.value.selectedCategoryId == category.id) {
				selectCategory(null)
			}
		}
	}

	class Factory(
		private val exerciseRepository: ExerciseRepository,
		private val categoryRepository: CategoryRepository
	) : ViewModelProvider.Factory {
		@Suppress("UNCHECKED_CAST")
		override fun <T : ViewModel> create(modelClass: Class<T>): T {
			return ExercisesViewModel(exerciseRepository, categoryRepository) as T
		}
	}
}
