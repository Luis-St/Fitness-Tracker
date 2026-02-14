package net.luis.tracker.ui.activeworkout

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import net.luis.tracker.R
import net.luis.tracker.domain.model.WeightUnit
import net.luis.tracker.ui.common.components.WeightInput
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutExerciseScreen(
	viewModel: ActiveWorkoutViewModel,
	entryId: Long,
	weightUnit: WeightUnit,
	onNavigateBack: () -> Unit
) {
	val entry by remember(entryId) {
		viewModel.uiState
			.map { state -> state.exercises.find { it.id == entryId } }
			.distinctUntilChanged()
	}.collectAsStateWithLifecycle(initialValue = viewModel.uiState.collectAsState().value.exercises.find { it.id == entryId })

	var weightText by remember { mutableStateOf("") }
	var repsText by remember { mutableStateOf("") }
	var showNotesDialog by remember { mutableStateOf(false) }

	if (showNotesDialog) {
		AlertDialog(
			onDismissRequest = { showNotesDialog = false },
			title = { Text(stringResource(R.string.notes)) },
			text = { Text(entry?.exercise?.notes ?: "") },
			confirmButton = {
				Button(onClick = { showNotesDialog = false }) {
					Text(stringResource(R.string.close))
				}
			}
		)
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = {
					Text(entry?.exercise?.title ?: stringResource(R.string.edit_exercise_sets))
				},
				navigationIcon = {
					IconButton(onClick = onNavigateBack) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = stringResource(R.string.back)
						)
					}
				},
				actions = {
					val notes = entry?.exercise?.notes
					if (!notes.isNullOrBlank()) {
						IconButton(onClick = { showNotesDialog = true }) {
							Icon(
								imageVector = Icons.Outlined.Info,
								contentDescription = stringResource(R.string.notes)
							)
						}
					}
				}
			)
		}
	) { innerPadding ->
		val currentEntry = entry
		if (currentEntry == null) {
			// Entry was removed while on this screen
			Text(
				text = stringResource(R.string.exercise_not_found),
				modifier = Modifier.padding(innerPadding).padding(16.dp)
			)
			return@Scaffold
		}

		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(innerPadding)
				.padding(horizontal = 16.dp)
		) {
			// Sets list
			if (currentEntry.sets.isEmpty()) {
				Text(
					text = stringResource(R.string.no_sets_yet),
					style = MaterialTheme.typography.bodyLarge,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					modifier = Modifier.padding(vertical = 16.dp)
				)
			} else {
				LazyColumn(
					modifier = Modifier.weight(1f, fill = false),
					contentPadding = PaddingValues(vertical = 8.dp),
					verticalArrangement = Arrangement.spacedBy(4.dp)
				) {
					itemsIndexed(currentEntry.sets) { index, set ->
						Row(
							modifier = Modifier
								.fillMaxWidth()
								.padding(vertical = 4.dp),
							verticalAlignment = Alignment.CenterVertically
						) {
							Text(
								text = stringResource(R.string.set_number, set.setNumber),
								style = MaterialTheme.typography.bodyLarge,
								fontWeight = FontWeight.Medium,
								modifier = Modifier.width(48.dp)
							)
							if (currentEntry.exercise.hasWeight) {
								Text(
									text = weightUnit.formatWeight(set.weightKg),
									style = MaterialTheme.typography.bodyLarge,
									modifier = Modifier.weight(1f)
								)
							} else {
								Spacer(modifier = Modifier.weight(1f))
							}
							Text(
								text = "${set.reps} ${stringResource(R.string.reps)}",
								style = MaterialTheme.typography.bodyLarge
							)
							IconButton(onClick = { viewModel.removeSet(entryId, index) }) {
								Icon(
									imageVector = Icons.Default.Delete,
									contentDescription = stringResource(R.string.remove),
									tint = MaterialTheme.colorScheme.error
								)
							}
						}
						if (index < currentEntry.sets.lastIndex) {
							HorizontalDivider()
						}
					}
				}
			}

			HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

			// Add set form — full width
			Text(
				text = stringResource(R.string.add_set),
				style = MaterialTheme.typography.titleMedium,
				modifier = Modifier.padding(bottom = 8.dp)
			)

			if (currentEntry.exercise.hasWeight) {
				WeightInput(
					text = weightText,
					onTextChange = { weightText = it },
					weightUnit = weightUnit,
					modifier = Modifier.fillMaxWidth()
				)

				Spacer(modifier = Modifier.height(8.dp))
			}

			OutlinedTextField(
				value = repsText,
				onValueChange = { repsText = it },
				label = { Text(stringResource(R.string.reps)) },
				modifier = Modifier.fillMaxWidth(),
				singleLine = true,
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
			)

			Spacer(modifier = Modifier.height(12.dp))

			Button(
				onClick = {
					val reps = repsText.toIntOrNull() ?: 0
					if (reps > 0) {
						val weightKg = if (currentEntry.exercise.hasWeight) {
							weightUnit.convertToKg(weightText.toDoubleOrNull() ?: 0.0)
						} else {
							0.0
						}
						viewModel.addSet(entryId, weightKg, reps)
						weightText = ""
						repsText = ""
					}
				},
				modifier = Modifier.fillMaxWidth()
			) {
				Icon(
					imageVector = Icons.Default.Add,
					contentDescription = null,
					modifier = Modifier.padding(end = 8.dp)
				)
				Text(stringResource(R.string.add_set))
			}

			Spacer(modifier = Modifier.height(16.dp))
		}
	}
}
