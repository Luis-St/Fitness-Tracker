package net.luis.tracker.ui.workouts

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.luis.tracker.FitnessTrackerApp
import net.luis.tracker.R
import net.luis.tracker.data.repository.WorkoutRepository
import net.luis.tracker.domain.model.WeightUnit
import net.luis.tracker.domain.model.Workout
import net.luis.tracker.ui.common.components.ConfirmDeleteDialog
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
	app: FitnessTrackerApp,
	workoutId: Long,
	weightUnit: WeightUnit,
	onNavigateBack: () -> Unit,
	onEdit: () -> Unit
) {
	val workoutRepository = remember {
		WorkoutRepository(
			app.database,
			app.database.workoutDao(),
			app.database.workoutExerciseDao(),
			app.database.workoutSetDao()
		)
	}

	var workout by remember { mutableStateOf<Workout?>(null) }
	var isLoading by remember { mutableStateOf(true) }
	var showDeleteDialog by remember { mutableStateOf(false) }
	val scope = rememberCoroutineScope()

	LaunchedEffect(workoutId) {
		workout = workoutRepository.getByIdWithExercises(workoutId)
		isLoading = false
	}

	val dateFormatter = remember {
		DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.workout_detail)) },
				navigationIcon = {
					IconButton(onClick = onNavigateBack) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = stringResource(R.string.back)
						)
					}
				},
				actions = {
					IconButton(onClick = onEdit) {
						Icon(
							imageVector = Icons.Default.Edit,
							contentDescription = stringResource(R.string.edit)
						)
					}
					IconButton(onClick = { showDeleteDialog = true }) {
						Icon(
							imageVector = Icons.Default.Delete,
							contentDescription = stringResource(R.string.delete_workout),
							tint = MaterialTheme.colorScheme.error
						)
					}
				}
			)
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
			workout == null -> {
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
				val w = workout!!
				val formattedDate = remember(w.startTime) {
					Instant.ofEpochMilli(w.startTime)
						.atZone(ZoneId.systemDefault())
						.format(dateFormatter)
				}
				val durationMinutes = w.durationSeconds / 60

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
					// Workout summary card
					item {
						Card(
							modifier = Modifier.fillMaxWidth(),
							elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
						) {
							Column(modifier = Modifier.padding(16.dp)) {
								Text(
									text = formattedDate,
									style = MaterialTheme.typography.titleLarge,
									fontWeight = FontWeight.Bold
								)

								Spacer(modifier = Modifier.height(12.dp))

								Row(
									modifier = Modifier.fillMaxWidth(),
									horizontalArrangement = Arrangement.spacedBy(24.dp)
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
											style = MaterialTheme.typography.bodyMedium
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
											text = stringResource(R.string.exercises_count, w.exerciseCount),
											style = MaterialTheme.typography.bodyMedium
										)
									}
								}

								Spacer(modifier = Modifier.height(8.dp))

								Text(
									text = stringResource(
										R.string.total_volume,
										weightUnit.formatWeight(w.totalVolume)
									),
									style = MaterialTheme.typography.titleMedium,
									color = MaterialTheme.colorScheme.primary
								)

								if (w.notes.isNotEmpty()) {
									Spacer(modifier = Modifier.height(8.dp))
									HorizontalDivider()
									Spacer(modifier = Modifier.height(8.dp))
									Text(
										text = stringResource(R.string.notes),
										style = MaterialTheme.typography.labelMedium,
										color = MaterialTheme.colorScheme.onSurfaceVariant
									)
									Spacer(modifier = Modifier.height(4.dp))
									Text(
										text = w.notes,
										style = MaterialTheme.typography.bodyMedium
									)
								}
							}
						}
					}

					// Exercise cards
					items(w.exercises, key = { it.id }) { workoutExercise ->
						Card(
							modifier = Modifier.fillMaxWidth(),
							elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
						) {
							Column(modifier = Modifier.padding(16.dp)) {
								Text(
									text = workoutExercise.exercise.title,
									style = MaterialTheme.typography.titleMedium,
									fontWeight = FontWeight.Bold
								)

								Spacer(modifier = Modifier.height(8.dp))

								// Column headers
								Row(
									modifier = Modifier.fillMaxWidth(),
									horizontalArrangement = Arrangement.SpaceBetween
								) {
									Text(
										text = stringResource(R.string.set_label),
										style = MaterialTheme.typography.labelMedium,
										color = MaterialTheme.colorScheme.onSurfaceVariant,
										modifier = Modifier.width(48.dp)
									)
									Text(
										text = stringResource(R.string.weight),
										style = MaterialTheme.typography.labelMedium,
										color = MaterialTheme.colorScheme.onSurfaceVariant,
										modifier = Modifier.weight(1f)
									)
									Text(
										text = stringResource(R.string.reps),
										style = MaterialTheme.typography.labelMedium,
										color = MaterialTheme.colorScheme.onSurfaceVariant
									)
								}

								HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

								workoutExercise.sets.forEach { set ->
									Row(
										modifier = Modifier
											.fillMaxWidth()
											.padding(vertical = 4.dp),
										horizontalArrangement = Arrangement.SpaceBetween,
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
									}
								}
							}
						}
					}
				}
			}
		}
	}

	if (showDeleteDialog) {
		ConfirmDeleteDialog(
			title = stringResource(R.string.delete_workout),
			message = stringResource(R.string.delete_workout_confirmation),
			onConfirm = {
				workout?.let { w ->
					scope.launch {
						workoutRepository.delete(w)
					}
				}
				showDeleteDialog = false
				onNavigateBack()
			},
			onDismiss = { showDeleteDialog = false }
		)
	}
}
