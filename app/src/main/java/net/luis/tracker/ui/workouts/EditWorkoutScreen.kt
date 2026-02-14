package net.luis.tracker.ui.workouts

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.luis.tracker.FitnessTrackerApp
import net.luis.tracker.R
import net.luis.tracker.data.repository.ExerciseRepository
import net.luis.tracker.data.repository.WorkoutRepository
import net.luis.tracker.domain.model.Exercise
import net.luis.tracker.domain.model.WeightUnit
import net.luis.tracker.domain.model.Workout
import net.luis.tracker.domain.model.WorkoutExercise
import net.luis.tracker.domain.model.WorkoutSet
import net.luis.tracker.ui.common.components.WeightInput

private data class EditExerciseEntry(
	val id: Long,
	val exercise: Exercise,
	val sets: List<WorkoutSet> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWorkoutScreen(
	app: FitnessTrackerApp,
	workoutId: Long,
	weightUnit: WeightUnit,
	onNavigateBack: () -> Unit
) {
	val workoutRepository = remember {
		WorkoutRepository(
			app.database,
			app.database.workoutDao(),
			app.database.workoutExerciseDao(),
			app.database.workoutSetDao()
		)
	}
	val exerciseRepository = remember {
		ExerciseRepository(app.database.exerciseDao())
	}

	var originalWorkout by remember { mutableStateOf<Workout?>(null) }
	var exercises by remember { mutableStateOf<List<EditExerciseEntry>>(emptyList()) }
	var notes by remember { mutableStateOf("") }
	var isLoading by remember { mutableStateOf(true) }
	var isSaving by remember { mutableStateOf(false) }
	var availableExercises by remember { mutableStateOf<List<Exercise>>(emptyList()) }
	var showExerciseSelector by remember { mutableStateOf(false) }
	var entryIdCounter by remember { mutableStateOf(0L) }
	val expandedEntries = remember { mutableStateMapOf<Long, Boolean>() }
	val lazyListState = rememberLazyListState()

	val scope = rememberCoroutineScope()

	LaunchedEffect(workoutId) {
		val (workout, allExercises) = withContext(Dispatchers.IO) {
			val w = workoutRepository.getByIdWithExercises(workoutId)
			val e = exerciseRepository.getAllActive().first()
			Pair(w, e)
		}
		availableExercises = allExercises

		if (workout != null) {
			originalWorkout = workout
			notes = workout.notes
			exercises = workout.exercises.map { we ->
				entryIdCounter++
				EditExerciseEntry(
					id = entryIdCounter,
					exercise = we.exercise,
					sets = we.sets
				)
			}
		}
		isLoading = false
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.edit_workout)) },
				navigationIcon = {
					IconButton(onClick = onNavigateBack) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = stringResource(R.string.back)
						)
					}
				},
				actions = {
					IconButton(
						onClick = {
							val workout = originalWorkout ?: return@IconButton
							isSaving = true
							val updatedExercises = exercises.mapIndexed { index, entry ->
								WorkoutExercise(
									exercise = entry.exercise,
									orderIndex = index,
									sets = entry.sets.mapIndexed { setIdx, set ->
										set.copy(setNumber = setIdx + 1)
									}
								)
							}
							val updatedWorkout = workout.copy(
								notes = notes,
								exercises = updatedExercises
							)
							scope.launch {
								workoutRepository.updateFullWorkout(updatedWorkout)
								isSaving = false
								onNavigateBack()
							}
						},
						enabled = !isSaving && originalWorkout != null
					) {
						Icon(
							imageVector = Icons.Default.Save,
							contentDescription = stringResource(R.string.save)
						)
					}
				}
			)
		},
		floatingActionButton = {
			if (!isLoading && originalWorkout != null) {
				FloatingActionButton(onClick = {
					showExerciseSelector = true
				}) {
					Icon(
						imageVector = Icons.Default.Add,
						contentDescription = stringResource(R.string.add_exercise_to_workout)
					)
				}
			}
		}
	) { innerPadding ->
		when {
			isLoading -> {
				Box(
					modifier = Modifier
						.fillMaxSize()
						.padding(innerPadding),
					contentAlignment = Alignment.Center
				) {
					CircularProgressIndicator()
				}
			}
			originalWorkout == null -> {
				Box(
					modifier = Modifier
						.fillMaxSize()
						.padding(innerPadding),
					contentAlignment = Alignment.Center
				) {
					Text(
						text = stringResource(R.string.workout_not_found),
						style = MaterialTheme.typography.bodyLarge,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
			}
			else -> {
				LazyColumn(
					state = lazyListState,
					contentPadding = PaddingValues(
						start = 16.dp,
						end = 16.dp,
						top = innerPadding.calculateTopPadding() + 8.dp,
						bottom = innerPadding.calculateBottomPadding() + 80.dp
					),
					verticalArrangement = Arrangement.spacedBy(12.dp),
					modifier = Modifier.fillMaxSize()
				) {
					// Notes field
					item {
						OutlinedTextField(
							value = notes,
							onValueChange = { notes = it },
							label = { Text(stringResource(R.string.notes)) },
							modifier = Modifier.fillMaxWidth(),
							minLines = 2,
							maxLines = 4
						)
					}

					// Exercise cards
					items(exercises, key = { it.id }) { entry ->
						val isExpanded = expandedEntries[entry.id] ?: false
						EditExerciseCard(
							entry = entry,
							weightUnit = weightUnit,
							isExpanded = isExpanded,
							onToggleExpanded = {
								expandedEntries[entry.id] = !isExpanded
							},
							onRemoveExercise = {
								exercises = exercises.filter { it.id != entry.id }
							},
							onAddSet = { weightKg, reps ->
								exercises = exercises.map { e ->
									if (e.id == entry.id) {
										val nextSetNumber = e.sets.size + 1
										val newSet = WorkoutSet(
											setNumber = nextSetNumber,
											weightKg = weightKg,
											reps = reps
										)
										e.copy(sets = e.sets + newSet)
									} else {
										e
									}
								}
							},
							onRemoveSet = { setIndex ->
								exercises = exercises.map { e ->
									if (e.id == entry.id) {
										val updatedSets = e.sets
											.filterIndexed { index, _ -> index != setIndex }
											.mapIndexed { index, set -> set.copy(setNumber = index + 1) }
										e.copy(sets = updatedSets)
									} else {
										e
									}
								}
							}
						)
					}
				}
			}
		}
	}

	// Exercise selection bottom sheet
	if (showExerciseSelector) {
		EditExerciseSelectSheet(
			exercises = availableExercises,
			onSelect = { exercise ->
				entryIdCounter++
				val newId = entryIdCounter
				val newEntry = EditExerciseEntry(
					id = newId,
					exercise = exercise
				)
				exercises = exercises + newEntry
				expandedEntries[newId] = true
				showExerciseSelector = false
				scope.launch {
					val index = exercises.size // notes item is at 0, so exercises start at 1
					lazyListState.animateScrollToItem(index)
				}
			},
			onDismiss = {
				showExerciseSelector = false
			}
		)
	}
}

@Composable
private fun EditExerciseCard(
	entry: EditExerciseEntry,
	weightUnit: WeightUnit,
	isExpanded: Boolean,
	onToggleExpanded: () -> Unit,
	onRemoveExercise: () -> Unit,
	onAddSet: (weightKg: Double, reps: Int) -> Unit,
	onRemoveSet: (setIndex: Int) -> Unit
) {
	var weightText by remember { mutableStateOf("") }
	var newReps by remember { mutableIntStateOf(0) }
	var repsText by remember { mutableStateOf("") }

	Card(
		modifier = Modifier
			.fillMaxWidth()
			.animateContentSize(),
		elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
	) {
		Column(modifier = Modifier.padding(16.dp)) {
			// Exercise header — clickable to toggle expand/collapse
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.clickable(onClick = onToggleExpanded),
				verticalAlignment = Alignment.CenterVertically
			) {
				Column(modifier = Modifier.weight(1f)) {
					Text(
						text = entry.exercise.title,
						style = MaterialTheme.typography.titleMedium,
						fontWeight = FontWeight.Bold
					)
					if (!isExpanded) {
						Text(
							text = stringResource(R.string.n_sets, entry.sets.size),
							style = MaterialTheme.typography.bodyMedium,
							color = MaterialTheme.colorScheme.onSurfaceVariant
						)
					}
				}
				IconButton(onClick = onRemoveExercise) {
					Icon(
						imageVector = Icons.Default.Close,
						contentDescription = stringResource(R.string.remove),
						tint = MaterialTheme.colorScheme.error
					)
				}
				IconButton(onClick = onToggleExpanded) {
					Icon(
						imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp
							else Icons.Default.KeyboardArrowDown,
						contentDescription = stringResource(
							if (isExpanded) R.string.collapse else R.string.expand
						)
					)
				}
			}

			if (isExpanded) {
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
						text = weightText,
						onTextChange = { weightText = it },
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
								val weightKg = weightUnit.convertToKg(weightText.toDoubleOrNull() ?: 0.0)
								onAddSet(weightKg, newReps)
								weightText = ""
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditExerciseSelectSheet(
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
