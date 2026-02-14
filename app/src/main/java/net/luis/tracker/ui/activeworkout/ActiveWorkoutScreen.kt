package net.luis.tracker.ui.activeworkout

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.luis.tracker.FitnessTrackerApp
import net.luis.tracker.R
import net.luis.tracker.data.repository.ExerciseRepository
import net.luis.tracker.data.repository.WorkoutRepository
import net.luis.tracker.domain.model.Exercise
import net.luis.tracker.domain.model.WeightUnit
import net.luis.tracker.ui.common.components.WeightInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
	app: FitnessTrackerApp,
	weightUnit: WeightUnit,
	onFinished: () -> Unit
) {
	val viewModel: ActiveWorkoutViewModel = viewModel(
		factory = ActiveWorkoutViewModel.Factory(
			ExerciseRepository(app.database.exerciseDao()),
			WorkoutRepository(
				app.database.workoutDao(),
				app.database.workoutExerciseDao(),
				app.database.workoutSetDao()
			)
		)
	)

	val uiState by viewModel.uiState.collectAsState()

	// Keep screen on while workout is active
	val context = LocalContext.current
	DisposableEffect(Unit) {
		val window = (context as? android.app.Activity)?.window
		window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
		onDispose {
			window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
		}
	}

	// Start the workout timer automatically
	LaunchedEffect(Unit) {
		viewModel.startWorkout()
	}

	// Intercept back button to offer discard
	BackHandler(enabled = uiState.isRunning) {
		viewModel.requestDiscard()
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.active_workout)) }
			)
		},
		floatingActionButton = {
			FloatingActionButton(onClick = { viewModel.showExerciseSelector() }) {
				Icon(
					imageVector = Icons.Default.Add,
					contentDescription = stringResource(R.string.add_exercise_to_workout)
				)
			}
		}
	) { innerPadding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(innerPadding)
		) {
			// Timer and controls
			TimerSection(
				elapsedSeconds = uiState.elapsedSeconds,
				isPaused = uiState.isPaused,
				onTogglePause = { viewModel.togglePause() },
				onFinish = { viewModel.finishWorkout(onFinished) }
			)

			HorizontalDivider()

			// Exercise cards
			LazyColumn(
				contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
				verticalArrangement = Arrangement.spacedBy(12.dp),
				modifier = Modifier.fillMaxSize()
			) {
				items(uiState.exercises, key = { it.id }) { entry ->
					ExerciseCard(
						entry = entry,
						weightUnit = weightUnit,
						onChangeExercise = { viewModel.showExerciseSelector(forEntryId = entry.id) },
						onRemoveExercise = { viewModel.removeExercise(entry.id) },
						onAddSet = { weightKg, reps -> viewModel.addSet(entry.id, weightKg, reps) },
						onRemoveSet = { setIndex -> viewModel.removeSet(entry.id, setIndex) }
					)
				}
			}
		}
	}

	// Exercise selection bottom sheet
	if (uiState.showSelectExercise) {
		ExerciseSelectSheet(
			exercises = uiState.availableExercises,
			onSelect = { exercise ->
				val editId = viewModel.editingEntryId
				if (editId != null) {
					viewModel.updateExerciseSelection(editId, exercise)
				} else {
					viewModel.addExercise(exercise)
				}
			},
			onDismiss = { viewModel.hideExerciseSelector() }
		)
	}

	// Discard confirmation dialog
	if (uiState.showDiscardDialog) {
		AlertDialog(
			onDismissRequest = { viewModel.cancelDiscard() },
			title = { Text(stringResource(R.string.discard_workout)) },
			text = { Text(stringResource(R.string.discard_workout_message)) },
			confirmButton = {
				TextButton(onClick = { viewModel.confirmDiscard(onFinished) }) {
					Text(stringResource(R.string.discard))
				}
			},
			dismissButton = {
				TextButton(onClick = { viewModel.cancelDiscard() }) {
					Text(stringResource(R.string.cancel))
				}
			}
		)
	}
}

@Composable
private fun TimerSection(
	elapsedSeconds: Long,
	isPaused: Boolean,
	onTogglePause: () -> Unit,
	onFinish: () -> Unit
) {
	val hours = elapsedSeconds / 3600
	val minutes = (elapsedSeconds % 3600) / 60
	val seconds = elapsedSeconds % 60
	val timerText = "%02d:%02d:%02d".format(hours, minutes, seconds)

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp, vertical = 12.dp),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.SpaceBetween
	) {
		Text(
			text = timerText,
			style = MaterialTheme.typography.headlineLarge,
			fontWeight = FontWeight.Bold
		)

		Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
			Button(onClick = onTogglePause) {
				Icon(
					imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
					contentDescription = null
				)
				Spacer(modifier = Modifier.width(4.dp))
				Text(
					text = stringResource(if (isPaused) R.string.resume else R.string.pause)
				)
			}

			Button(onClick = onFinish) {
				Icon(
					imageVector = Icons.Default.Done,
					contentDescription = null
				)
				Spacer(modifier = Modifier.width(4.dp))
				Text(stringResource(R.string.finish_workout))
			}
		}
	}
}

@Composable
private fun ExerciseCard(
	entry: ActiveExerciseEntry,
	weightUnit: WeightUnit,
	onChangeExercise: () -> Unit,
	onRemoveExercise: () -> Unit,
	onAddSet: (weightKg: Double, reps: Int) -> Unit,
	onRemoveSet: (setIndex: Int) -> Unit
) {
	var newWeightKg by remember { mutableDoubleStateOf(0.0) }
	var newReps by remember { mutableIntStateOf(0) }
	var repsText by remember { mutableStateOf("") }

	Card(
		modifier = Modifier.fillMaxWidth(),
		elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
	) {
		Column(modifier = Modifier.padding(16.dp)) {
			// Exercise header
			Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically
			) {
				Text(
					text = entry.exercise.title,
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.Bold,
					modifier = Modifier.weight(1f)
				)
				IconButton(onClick = onChangeExercise) {
					Icon(
						imageVector = Icons.Default.SwapHoriz,
						contentDescription = stringResource(R.string.select_exercise)
					)
				}
				IconButton(onClick = onRemoveExercise) {
					Icon(
						imageVector = Icons.Default.Close,
						contentDescription = stringResource(R.string.remove),
						tint = MaterialTheme.colorScheme.error
					)
				}
			}

			// Sets list
			if (entry.sets.isEmpty()) {
				Text(
					text = stringResource(R.string.no_sets_yet),
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					modifier = Modifier.padding(vertical = 8.dp)
				)
			} else {
				entry.sets.forEachIndexed { index, set ->
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.padding(vertical = 4.dp),
						verticalAlignment = Alignment.CenterVertically
					) {
						Text(
							text = stringResource(R.string.set_number, set.setNumber),
							style = MaterialTheme.typography.bodyMedium,
							modifier = Modifier.width(48.dp)
						)
						Text(
							text = weightUnit.formatWeight(set.weightKg),
							style = MaterialTheme.typography.bodyMedium,
							modifier = Modifier.weight(1f)
						)
						Text(
							text = "${set.reps} ${stringResource(R.string.reps)}",
							style = MaterialTheme.typography.bodyMedium
						)
						IconButton(onClick = { onRemoveSet(index) }) {
							Icon(
								imageVector = Icons.Default.Delete,
								contentDescription = stringResource(R.string.remove),
								tint = MaterialTheme.colorScheme.error
							)
						}
					}
				}
			}

			HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

			// Add set row
			Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(8.dp)
			) {
				WeightInput(
					weightKg = newWeightKg,
					onWeightChange = { newWeightKg = it },
					weightUnit = weightUnit,
					modifier = Modifier.weight(1f)
				)
				OutlinedTextField(
					value = repsText,
					onValueChange = { text ->
						repsText = text
						newReps = text.toIntOrNull() ?: 0
					},
					label = { Text(stringResource(R.string.reps)) },
					modifier = Modifier.width(80.dp),
					singleLine = true,
					keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
				)
				IconButton(
					onClick = {
						if (newReps > 0) {
							onAddSet(newWeightKg, newReps)
							newWeightKg = 0.0
							newReps = 0
							repsText = ""
						}
					}
				) {
					Icon(
						imageVector = Icons.Default.Add,
						contentDescription = stringResource(R.string.add_set)
					)
				}
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseSelectSheet(
	exercises: List<Exercise>,
	onSelect: (Exercise) -> Unit,
	onDismiss: () -> Unit
) {
	val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
	var searchQuery by remember { mutableStateOf("") }

	val filtered = remember(exercises, searchQuery) {
		if (searchQuery.isBlank()) exercises
		else exercises.filter { it.title.contains(searchQuery, ignoreCase = true) }
	}

	ModalBottomSheet(
		onDismissRequest = onDismiss,
		sheetState = sheetState
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp)
		) {
			Text(
				text = stringResource(R.string.select_exercise),
				style = MaterialTheme.typography.titleLarge,
				modifier = Modifier.padding(bottom = 12.dp)
			)
			OutlinedTextField(
				value = searchQuery,
				onValueChange = { searchQuery = it },
				placeholder = { Text(stringResource(R.string.search_exercises)) },
				leadingIcon = {
					Icon(Icons.Default.Search, contentDescription = null)
				},
				modifier = Modifier.fillMaxWidth(),
				singleLine = true
			)
			Spacer(modifier = Modifier.height(8.dp))
			LazyColumn {
				items(filtered, key = { it.id }) { exercise ->
					ListItem(
						headlineContent = { Text(exercise.title) },
						supportingContent = exercise.category?.let {
							{ Text(it.name) }
						},
						modifier = Modifier.fillMaxWidth(),
						trailingContent = {
							IconButton(onClick = { onSelect(exercise) }) {
								Icon(Icons.Default.Add, contentDescription = null)
							}
						}
					)
					HorizontalDivider()
				}
			}
		}
	}
}
