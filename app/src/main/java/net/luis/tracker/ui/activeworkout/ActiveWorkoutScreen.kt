package net.luis.tracker.ui.activeworkout

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import net.luis.tracker.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
	viewModel: ActiveWorkoutViewModel,
	onFinished: () -> Unit,
	onEditExercise: (entryId: Long) -> Unit,
	onAddExercise: () -> Unit
) {
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
			FloatingActionButton(onClick = onAddExercise) {
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
				elapsedMillisFlow = viewModel.elapsedMillis,
				isPaused = uiState.isPaused,
				onTogglePause = { viewModel.togglePause() },
				onFinish = { viewModel.finishWorkout(onFinished) }
			)

			HorizontalDivider()

			Spacer(modifier = Modifier.height(8.dp))

			// Compact exercise list
			LazyColumn(
				contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp),
				modifier = Modifier.weight(1f).fillMaxWidth()
			) {
				items(uiState.exercises, key = { it.id }) { entry ->
					ListItem(
						headlineContent = {
							Text(entry.exercise.title)
						},
						supportingContent = {
							Text(stringResource(R.string.n_sets, entry.sets.size))
						},
						trailingContent = {
							Row {
								IconButton(onClick = { onEditExercise(entry.id) }) {
									Icon(
										imageVector = Icons.Default.Edit,
										contentDescription = stringResource(R.string.edit_exercise_sets)
									)
								}
								IconButton(onClick = { viewModel.removeExercise(entry.id) }) {
									Icon(
										imageVector = Icons.Default.Close,
										contentDescription = stringResource(R.string.remove),
										tint = MaterialTheme.colorScheme.error
									)
								}
							}
						}
					)
					HorizontalDivider()
				}
			}
		}
	}

	// Discard confirmation dialog (separate state to avoid recomposing the Scaffold)
	val showDiscardDialog by viewModel.showDiscardDialog.collectAsStateWithLifecycle()
	if (showDiscardDialog) {
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
	elapsedMillisFlow: StateFlow<Long>,
	isPaused: Boolean,
	onTogglePause: () -> Unit,
	onFinish: () -> Unit
) {
	val elapsedMillis by elapsedMillisFlow.collectAsStateWithLifecycle()
	val totalSeconds = elapsedMillis / 1000
	val hours = totalSeconds / 3600
	val minutes = (totalSeconds % 3600) / 60
	val seconds = totalSeconds % 60
	val timerText = "%02d:%02d:%02d".format(hours, minutes, seconds)

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp, vertical = 8.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Text(
			text = timerText,
			style = MaterialTheme.typography.titleLarge,
			fontWeight = FontWeight.Bold,
			modifier = Modifier.weight(1f)
		)
		IconButton(onClick = onTogglePause) {
			Icon(
				imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
				contentDescription = stringResource(
					if (isPaused) R.string.resume else R.string.pause
				)
			)
		}
		IconButton(onClick = onFinish) {
			Icon(
				imageVector = Icons.Default.Stop,
				contentDescription = stringResource(R.string.finish_workout),
				tint = MaterialTheme.colorScheme.error
			)
		}
	}
}
