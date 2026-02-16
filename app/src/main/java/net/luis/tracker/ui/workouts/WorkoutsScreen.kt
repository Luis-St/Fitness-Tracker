package net.luis.tracker.ui.workouts

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.luis.tracker.FitnessTrackerApp
import net.luis.tracker.R
import net.luis.tracker.data.repository.WorkoutRepository
import net.luis.tracker.domain.model.WeightUnit
import net.luis.tracker.domain.model.Workout
import net.luis.tracker.ui.common.components.ConfirmDeleteDialog
import net.luis.tracker.ui.common.components.EmptyState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsScreen(
	app: FitnessTrackerApp,
	weightUnit: WeightUnit,
	onStartWorkout: () -> Unit,
	onWorkoutClick: (Long) -> Unit,
	onOpenSettings: () -> Unit = {}
) {
	val factory = remember {
		WorkoutsViewModel.Factory(
			workoutRepository = WorkoutRepository(
				app.database,
				app.database.workoutDao(),
				app.database.workoutExerciseDao(),
				app.database.workoutSetDao()
			)
		)
	}
	val viewModel: WorkoutsViewModel = viewModel(factory = factory)

	val uiState by viewModel.uiState.collectAsState()
	var workoutToDelete by remember { mutableStateOf<Workout?>(null) }

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.workouts_title)) },
				actions = {
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
			FloatingActionButton(onClick = onStartWorkout) {
				Icon(
					imageVector = Icons.Default.Add,
					contentDescription = stringResource(R.string.start_workout)
				)
			}
		},
		contentWindowInsets = WindowInsets(0)
	) { innerPadding ->
		when {
			uiState.isLoading -> {
				Box(
					modifier = Modifier
						.fillMaxSize()
						.padding(innerPadding),
					contentAlignment = Alignment.Center
				) {
					CircularProgressIndicator()
				}
			}
			uiState.workouts.isEmpty() -> {
				EmptyState(
					icon = Icons.Default.History,
					message = stringResource(R.string.no_workouts),
					modifier = Modifier.padding(innerPadding)
				)
			}
			else -> {
				LazyColumn(
					contentPadding = PaddingValues(
						start = 16.dp,
						end = 16.dp,
						top = innerPadding.calculateTopPadding() + 8.dp,
						bottom = innerPadding.calculateBottomPadding() + 16.dp
					),
					verticalArrangement = Arrangement.spacedBy(12.dp),
					modifier = Modifier.fillMaxSize()
				) {
					items(uiState.workouts, key = { it.id }) { workout ->
						WorkoutCard(
							workout = workout,
							weightUnit = weightUnit,
							onClick = { onWorkoutClick(workout.id) },
							onDelete = { workoutToDelete = workout }
						)
					}
				}
			}
		}
	}

	workoutToDelete?.let { workout ->
		ConfirmDeleteDialog(
			title = stringResource(R.string.delete_workout),
			message = stringResource(R.string.delete_workout_confirmation),
			onConfirm = {
				viewModel.deleteWorkout(workout)
				workoutToDelete = null
			},
			onDismiss = { workoutToDelete = null }
		)
	}
}

@Composable
private fun WorkoutCard(
	workout: Workout,
	weightUnit: WeightUnit,
	onClick: () -> Unit,
	onDelete: () -> Unit
) {
	val dateFormatter = remember {
		DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
	}

	val formattedDate = remember(workout.startTime) {
		Instant.ofEpochMilli(workout.startTime)
			.atZone(ZoneId.systemDefault())
			.format(dateFormatter)
	}

	val durationMinutes = workout.durationSeconds / 60

	Card(
		modifier = Modifier
			.fillMaxWidth()
			.clickable(onClick = onClick),
		elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
	) {
		Column(modifier = Modifier.padding(16.dp)) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically
			) {
				Text(
					text = formattedDate,
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.Bold,
					modifier = Modifier.weight(1f)
				)
				IconButton(onClick = onDelete) {
					Icon(
						imageVector = Icons.Default.Delete,
						contentDescription = stringResource(R.string.delete_workout),
						tint = MaterialTheme.colorScheme.error
					)
				}
			}

			Spacer(modifier = Modifier.height(8.dp))

			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(16.dp)
			) {
				Row(verticalAlignment = Alignment.CenterVertically) {
					Icon(
						imageVector = Icons.Default.Schedule,
						contentDescription = null,
						tint = MaterialTheme.colorScheme.onSurfaceVariant
					)
					Spacer(modifier = Modifier.width(4.dp))
					Text(
						text = stringResource(R.string.duration_format, durationMinutes),
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}

				Row(verticalAlignment = Alignment.CenterVertically) {
					Icon(
						imageVector = Icons.Default.FitnessCenter,
						contentDescription = null,
						tint = MaterialTheme.colorScheme.onSurfaceVariant
					)
					Spacer(modifier = Modifier.width(4.dp))
					Text(
						text = stringResource(R.string.exercises_count, workout.exerciseCount),
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
			}

		}
	}
}
