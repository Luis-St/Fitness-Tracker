package net.luis.tracker.ui.exercises

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.luis.tracker.FitnessTrackerApp
import net.luis.tracker.R
import net.luis.tracker.data.repository.CategoryRepository
import net.luis.tracker.data.repository.ExerciseRepository
import net.luis.tracker.ui.common.components.ConfirmDeleteDialog
import net.luis.tracker.ui.common.components.EmptyState
import net.luis.tracker.ui.exercises.components.CategoryManagementDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(
	app: FitnessTrackerApp,
	onAddExercise: () -> Unit,
	onExerciseClick: (Long) -> Unit,
	onOpenSettings: () -> Unit = {}
) {
	val factory = remember {
		ExercisesViewModel.Factory(
			exerciseRepository = ExerciseRepository(app.database.exerciseDao()),
			categoryRepository = CategoryRepository(app.database.categoryDao())
		)
	}
	val viewModel: ExercisesViewModel = viewModel(factory = factory)

	val uiState by viewModel.uiState.collectAsState()
	var showCategoryDialog by remember { mutableStateOf(false) }
	var exerciseToDelete by remember { mutableStateOf<Long?>(null) }

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.exercises_title)) },
				actions = {
					IconButton(onClick = { showCategoryDialog = true }) {
						Icon(
							imageVector = Icons.Default.Edit,
							contentDescription = stringResource(R.string.manage_categories)
						)
					}
					IconButton(onClick = onOpenSettings) {
						Icon(
							imageVector = Icons.Default.Settings,
							contentDescription = stringResource(R.string.settings)
						)
					}
				}
			)
		},
		floatingActionButton = {
			FloatingActionButton(onClick = onAddExercise) {
				Icon(
					imageVector = Icons.Default.Add,
					contentDescription = stringResource(R.string.add_exercise)
				)
			}
		}
	) { innerPadding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(innerPadding)
		) {
			// Category filter chips
			LazyRow(
				contentPadding = PaddingValues(horizontal = 16.dp),
				horizontalArrangement = Arrangement.spacedBy(8.dp),
				modifier = Modifier.fillMaxWidth()
			) {
				item {
					FilterChip(
						selected = uiState.selectedCategoryId == null,
						onClick = { viewModel.selectCategory(null) },
						label = { Text(stringResource(R.string.all_categories)) }
					)
				}
				items(uiState.categories, key = { it.id }) { category ->
					FilterChip(
						selected = uiState.selectedCategoryId == category.id,
						onClick = { viewModel.selectCategory(category.id) },
						label = { Text(category.name) }
					)
				}
			}

			Spacer(modifier = Modifier.height(8.dp))

			// Content
			when {
				uiState.isLoading -> {
					Box(
						modifier = Modifier.fillMaxSize(),
						contentAlignment = Alignment.Center
					) {
						CircularProgressIndicator()
					}
				}
				uiState.exercises.isEmpty() -> {
					EmptyState(
						icon = Icons.Default.FitnessCenter,
						message = stringResource(R.string.no_exercises)
					)
				}
				else -> {
					LazyColumn(
						contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
						verticalArrangement = Arrangement.spacedBy(8.dp),
						modifier = Modifier.fillMaxSize()
					) {
						items(uiState.exercises, key = { it.id }) { exercise ->
							ExerciseItem(
								exercise = exercise,
								onClick = { onExerciseClick(exercise.id) },
								onDelete = { exerciseToDelete = exercise.id }
							)
						}
					}
				}
			}
		}
	}

	// Category management dialog
	if (showCategoryDialog) {
		CategoryManagementDialog(
			categories = uiState.categories,
			onAdd = { viewModel.addCategory(it) },
			onUpdate = { viewModel.updateCategory(it) },
			onDelete = { viewModel.deleteCategory(it) },
			onDismiss = { showCategoryDialog = false }
		)
	}

	// Confirm delete exercise dialog
	exerciseToDelete?.let { id ->
		ConfirmDeleteDialog(
			title = stringResource(R.string.delete_exercise),
			message = stringResource(R.string.delete_exercise_confirmation),
			onConfirm = {
				viewModel.deleteExercise(id)
				exerciseToDelete = null
			},
			onDismiss = { exerciseToDelete = null }
		)
	}
}

@Composable
private fun ExerciseItem(
	exercise: net.luis.tracker.domain.model.Exercise,
	onClick: () -> Unit,
	onDelete: () -> Unit
) {
	Card(
		modifier = Modifier
			.fillMaxWidth()
			.clickable(onClick = onClick),
		elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			Column(modifier = Modifier.weight(1f)) {
				Text(
					text = exercise.title,
					style = MaterialTheme.typography.titleMedium
				)
				if (exercise.category != null) {
					Spacer(modifier = Modifier.height(4.dp))
					SuggestionChip(
						onClick = {},
						label = {
							Text(
								text = exercise.category.name,
								style = MaterialTheme.typography.labelSmall
							)
						}
					)
				}
			}
			Spacer(modifier = Modifier.width(8.dp))
			IconButton(onClick = onDelete) {
				Icon(
					imageVector = Icons.Default.Delete,
					contentDescription = stringResource(R.string.delete),
					tint = MaterialTheme.colorScheme.error
				)
			}
		}
	}
}
