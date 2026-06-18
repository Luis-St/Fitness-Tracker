package net.luis.tracker.ui.activeworkout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import net.luis.tracker.R
import net.luis.tracker.data.local.dao.SetHistoryEntry
import net.luis.tracker.domain.SetComparison
import net.luis.tracker.domain.compareSet
import net.luis.tracker.domain.model.SetComparisonSettings
import net.luis.tracker.domain.model.WeightUnit
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import net.luis.tracker.ui.common.components.WeightDropdown
import net.luis.tracker.ui.common.components.WeightInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutExerciseScreen(
	viewModel: ActiveWorkoutViewModel,
	entryId: Long,
	weightUnit: WeightUnit,
	preferredWeightsKg: List<Double> = emptyList(),
	comparisonSettings: SetComparisonSettings = SetComparisonSettings(),
	onNavigateBack: () -> Unit,
	onRest: () -> Unit,
	onFinishWithTimer: () -> Unit
) {
	val entry by remember(entryId) {
		viewModel.uiState
			.map { state -> state.exercises.find { it.id == entryId } }
			.distinctUntilChanged()
	}.collectAsStateWithLifecycle(initialValue = null)

	var weightText by remember { mutableStateOf("") }
	var repsText by remember { mutableStateOf("") }
	var weightError by remember { mutableStateOf(false) }
	var repsError by remember { mutableStateOf(false) }
	var isDropSet by remember { mutableStateOf(false) }
	var dropWeightText by remember { mutableStateOf("") }
	var dropRepsText by remember { mutableStateOf("") }
	var dropWeightError by remember { mutableStateOf(false) }
	var dropRepsError by remember { mutableStateOf(false) }
	var showNotesDialog by remember { mutableStateOf(false) }
	var historySet by remember { mutableStateOf<Int?>(null) }

	historySet?.let { setNumber ->
		entry?.let { e ->
			SetHistoryDialog(
				exerciseTitle = e.exercise.title,
				setNumber = setNumber,
				hasWeight = e.exercise.hasWeight,
				weightUnit = weightUnit,
				loadHistory = { viewModel.getSetHistory(e.exercise.id, setNumber) },
				onDismiss = { historySet = null }
			)
		}
	}

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

	val tryAddPendingSet = {
		val currentEntry = entry
		if (currentEntry != null) {
			val reps = repsText.toIntOrNull() ?: 0
			val weightValid = !currentEntry.exercise.hasWeight || currentEntry.exercise.allowsZeroWeight || (weightText.toDoubleOrNull() ?: 0.0) > 0.0
			val repsValid = reps > 0
			val hasDropWeight = isDropSet && currentEntry.exercise.hasWeight && dropWeightText.isNotBlank()
			val hasDropReps = isDropSet && dropRepsText.isNotBlank()
			val wantsDropSet = hasDropWeight || hasDropReps
			val dropWeightValue = dropWeightText.toDoubleOrNull() ?: 0.0
			val mainWeightValue = weightText.toDoubleOrNull() ?: 0.0
			val dropWeightValid = !wantsDropSet || !currentEntry.exercise.hasWeight ||
				(hasDropWeight && dropWeightValue > 0.0 && dropWeightValue < mainWeightValue)
			val dropRepsValid = !wantsDropSet || (hasDropReps && (dropRepsText.toIntOrNull() ?: 0) > 0)
			if (repsValid && weightValid && dropWeightValid && dropRepsValid) {
				val weightKg = if (currentEntry.exercise.hasWeight) {
					weightUnit.convertToKg(weightText.toDoubleOrNull() ?: 0.0)
				} else {
					0.0
				}
				val dropWeightKg = if (wantsDropSet && currentEntry.exercise.hasWeight) {
					weightUnit.convertToKg(dropWeightValue)
				} else if (wantsDropSet) {
					0.0
				} else {
					null
				}
				viewModel.addSet(entryId, weightKg, reps, dropWeightKg, if (wantsDropSet) dropRepsText.toIntOrNull() else null)
			}
		}
		weightText = ""
		repsText = ""
		dropWeightText = ""
		dropRepsText = ""
		weightError = false
		repsError = false
		dropWeightError = false
		dropRepsError = false
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
		},
		bottomBar = {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 16.dp, vertical = 12.dp)
					.height(IntrinsicSize.Min),
				horizontalArrangement = Arrangement.spacedBy(8.dp)
			) {
				FilledTonalButton(
					onClick = { tryAddPendingSet(); onRest() },
					modifier = Modifier
						.weight(1f)
						.fillMaxHeight()
				) {
					Icon(
						imageVector = Icons.Default.Timer,
						contentDescription = null,
						modifier = Modifier.padding(end = 8.dp)
					)
					Text(stringResource(R.string.rest))
				}
				Button(
					onClick = { tryAddPendingSet(); onFinishWithTimer() },
					enabled = entry?.sets?.isNotEmpty() == true,
					modifier = Modifier
						.weight(1f)
						.fillMaxHeight()
				) {
					Icon(
						imageVector = Icons.Default.Timer,
						contentDescription = null,
						modifier = Modifier.padding(end = 8.dp)
					)
					Text(stringResource(R.string.finish_and_rest))
				}
			}
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
			val realSets = currentEntry.sets
			val lastPerformance = currentEntry.lastPerformanceSets
			val comparisonActive = comparisonSettings.enabled && lastPerformance.isNotEmpty()
			// Ghost/reference rows only ever come from a template, never from logged history.
			val ghostSets = currentEntry.planSetsData
			val totalRows = maxOf(realSets.size, ghostSets.size)

			if (totalRows == 0) {
				Text(
					text = stringResource(R.string.no_sets_yet),
					style = MaterialTheme.typography.bodyLarge,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					modifier = Modifier.padding(vertical = 16.dp)
				)
			} else {
				val hasWeight = currentEntry.exercise.hasWeight
				LazyColumn(
					modifier = Modifier.weight(1f, fill = false),
					contentPadding = PaddingValues(vertical = 8.dp),
					verticalArrangement = Arrangement.spacedBy(4.dp)
				) {
					items(totalRows) { index ->
						val realSet = realSets.getOrNull(index)
						val ghostSet = ghostSets.getOrNull(index)
						if (realSet != null) {
							val badges = if (comparisonActive) {
								compareSet(realSet, lastPerformance.getOrNull(index), hasWeight)
							} else {
								null
							}
							// When the feature is on, every entered value carries a badge — the
							// neutral color is the fallback when there's nothing to compare against.
							val neutralBadge = if (comparisonSettings.enabled) {
								Color(comparisonSettings.neutralColor)
							} else {
								null
							}
							SetItem(
								setNumber = realSet.setNumber,
								weightKg = realSet.weightKg,
								reps = realSet.reps,
								hasWeight = hasWeight,
								weightUnit = weightUnit,
								showDivider = index < totalRows - 1,
								dropWeightKg = realSet.dropWeightKg,
								dropReps = realSet.dropReps,
								weightBadgeColor = badges?.weight.toBadgeColor(comparisonSettings) ?: neutralBadge,
								repsBadgeColor = badges?.reps.toBadgeColor(comparisonSettings) ?: neutralBadge,
								onValueClick = { historySet = realSet.setNumber },
								onRemove = { viewModel.removeSet(entryId, index) }
							)
						} else if (ghostSet != null) {
							SetItem(
								setNumber = index + 1,
								weightKg = ghostSet.weightKg,
								reps = ghostSet.reps,
								hasWeight = hasWeight,
								weightUnit = weightUnit,
								showDivider = index < totalRows - 1,
								dropWeightKg = ghostSet.dropWeightKg,
								dropReps = ghostSet.dropReps,
								isGhost = true
							)
						}
					}
				}
			}

			HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

			Text(
				text = stringResource(R.string.add_set),
				style = MaterialTheme.typography.titleMedium,
				modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
			)

			Row(
				modifier = Modifier
					.fillMaxWidth()
					.height(IntrinsicSize.Min),
				horizontalArrangement = Arrangement.spacedBy(12.dp),
				verticalAlignment = Alignment.Top
			) {
				Column(
					modifier = Modifier.weight(1f),
					verticalArrangement = Arrangement.spacedBy(8.dp)
				) {
					if (currentEntry.exercise.hasWeight) {
						Row(
							modifier = Modifier.fillMaxWidth(),
							horizontalArrangement = Arrangement.spacedBy(8.dp)
						) {
							WeightDropdown(
								text = weightText,
								onTextChange = { weightText = it; weightError = false },
								weightUnit = weightUnit,
								preferredWeightsKg = preferredWeightsKg,
								isError = weightError,
								allowsZeroWeight = currentEntry.exercise.allowsZeroWeight,
								modifier = Modifier.weight(1f)
							)
							if (isDropSet) {
								WeightDropdown(
									text = dropWeightText,
									onTextChange = { dropWeightText = it; dropWeightError = false },
									weightUnit = weightUnit,
									preferredWeightsKg = preferredWeightsKg,
									isError = dropWeightError,
									label = stringResource(R.string.drop_weight),
									allowsZeroWeight = currentEntry.exercise.allowsZeroWeight,
									modifier = Modifier.weight(1f)
								)
							}
						}
					}
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.spacedBy(8.dp)
					) {
						OutlinedTextField(
							value = repsText,
							onValueChange = { repsText = it; repsError = false },
							label = { Text(stringResource(R.string.reps)) },
							modifier = Modifier.weight(1f),
							singleLine = true,
							isError = repsError,
							keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
						)
						if (isDropSet) {
							OutlinedTextField(
								value = dropRepsText,
								onValueChange = { dropRepsText = it; dropRepsError = false },
								label = { Text(stringResource(R.string.drop_reps)) },
								modifier = Modifier.weight(1f),
								singleLine = true,
								isError = dropRepsError,
								keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
							)
						}
					}
				}
				if (currentEntry.exercise.hasWeight) {
					FilledTonalButton(
						onClick = {
							isDropSet = !isDropSet
							if (!isDropSet) {
								dropWeightText = ""
								dropRepsText = ""
								dropWeightError = false
								dropRepsError = false
							}
						},
						modifier = Modifier
							.padding(top = 8.dp)
							.fillMaxHeight()
							.width(40.dp),
						contentPadding = PaddingValues(0.dp)
					) {
						Icon(
							imageVector = if (isDropSet) Icons.Default.Close else Icons.Default.ArrowDownward,
							contentDescription = stringResource(R.string.drop_set)
						)
					}
				}
			}

			Spacer(modifier = Modifier.height(12.dp))

			Button(
				onClick = {
					val reps = repsText.toIntOrNull() ?: 0
					val weightValid = !currentEntry.exercise.hasWeight || currentEntry.exercise.allowsZeroWeight || (weightText.toDoubleOrNull() ?: 0.0) > 0.0
					val repsValid = reps > 0
					val hasDropWeight = isDropSet && currentEntry.exercise.hasWeight && dropWeightText.isNotBlank()
					val hasDropReps = isDropSet && dropRepsText.isNotBlank()
					val wantsDropSet = hasDropWeight || hasDropReps
					val dropWeightValue = dropWeightText.toDoubleOrNull() ?: 0.0
					val mainWeightValue = weightText.toDoubleOrNull() ?: 0.0
					val dropWeightValid = !wantsDropSet || !currentEntry.exercise.hasWeight ||
						(hasDropWeight && dropWeightValue > 0.0 && dropWeightValue < mainWeightValue)
					val dropRepsValid = !wantsDropSet || (hasDropReps && (dropRepsText.toIntOrNull() ?: 0) > 0)
					weightError = !weightValid
					repsError = !repsValid
					dropWeightError = wantsDropSet && !dropWeightValid
					dropRepsError = wantsDropSet && !dropRepsValid
					if (repsValid && weightValid && dropWeightValid && dropRepsValid) {
						val weightKg = if (currentEntry.exercise.hasWeight) {
							weightUnit.convertToKg(weightText.toDoubleOrNull() ?: 0.0)
						} else {
							0.0
						}
						val dropWeightKg = if (wantsDropSet && currentEntry.exercise.hasWeight) {
							weightUnit.convertToKg(dropWeightValue)
						} else if (wantsDropSet) {
							0.0
						} else {
							null
						}
						viewModel.addSet(entryId, weightKg, reps, dropWeightKg, if (wantsDropSet) dropRepsText.toIntOrNull() else null)
						weightText = ""
						repsText = ""
						dropWeightText = ""
						dropRepsText = ""
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

@Composable
private fun SetItem(
	setNumber: Int,
	weightKg: Double,
	reps: Int,
	hasWeight: Boolean,
	weightUnit: WeightUnit,
	showDivider: Boolean,
	dropWeightKg: Double? = null,
	dropReps: Int? = null,
	isGhost: Boolean = false,
	weightBadgeColor: Color? = null,
	repsBadgeColor: Color? = null,
	onValueClick: (() -> Unit)? = null,
	onRemove: (() -> Unit)? = null
) {
	val contentAlpha = if (isGhost) 0.5f else 1f
	Column(modifier = Modifier.fillMaxWidth()) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(vertical = 4.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			Text(
				text = stringResource(R.string.set_number, setNumber),
				style = MaterialTheme.typography.bodyLarge,
				fontWeight = FontWeight.Medium,
				modifier = Modifier.width(48.dp).alpha(contentAlpha)
			)
			// Zero-weight sets (allowed via allowsZeroWeight) display reps-only
			val showWeight = hasWeight && weightKg > 0.0
			if (showWeight) {
				val weightText = if (dropWeightKg != null && dropWeightKg > 0.0) {
					weightUnit.formatWeightPair(weightKg, dropWeightKg)
				} else {
					weightUnit.formatWeight(weightKg)
				}
				Row(modifier = Modifier.weight(1f)) {
					BadgeText(
						text = weightText,
						badgeColor = weightBadgeColor,
						contentAlpha = contentAlpha,
						onClick = if (isGhost) null else onValueClick
					)
				}
			} else {
				Spacer(modifier = Modifier.weight(1f))
			}
			val repsText = if (dropReps != null) "$reps / $dropReps ${stringResource(R.string.reps)}"
			else "$reps ${stringResource(R.string.reps)}"
			BadgeText(
				text = repsText,
				badgeColor = repsBadgeColor,
				contentAlpha = contentAlpha,
				onClick = if (isGhost) null else onValueClick
			)
			IconButton(
				onClick = onRemove ?: {},
				enabled = !isGhost
			) {
				Icon(
					imageVector = Icons.Default.Delete,
					contentDescription = stringResource(R.string.remove),
					tint = MaterialTheme.colorScheme.error.copy(alpha = if (isGhost) 0.38f else 1f)
				)
			}
		}
	}
	if (showDivider) {
		HorizontalDivider()
	}
}

/** Resolves a per-field [SetComparison] to its configured badge color, or null for no badge. */
private fun SetComparison?.toBadgeColor(settings: SetComparisonSettings): Color? = when (this) {
	SetComparison.BETTER -> Color(settings.betterColor)
	SetComparison.SAME -> Color(settings.sameColor)
	SetComparison.WORSE -> Color(settings.worseColor)
	SetComparison.TRADEOFF -> Color(settings.neutralColor)
	SetComparison.NEUTRAL, null -> null
}

/**
 * A set value (weight or reps). When [badgeColor] is non-null the value gets a subtle, lightly
 * tinted rounded mark in that color; otherwise it renders as plain text honoring [contentAlpha]
 * (used to fade ghost/reference rows). [onClick] opens the set's full history.
 */
@Composable
private fun BadgeText(
	text: String,
	badgeColor: Color?,
	contentAlpha: Float,
	onClick: (() -> Unit)? = null
) {
	val shape = RoundedCornerShape(6.dp)
	val base = Modifier
		.clip(shape)
		.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
	if (badgeColor != null) {
		Text(
			text = text,
			style = MaterialTheme.typography.bodyLarge,
			modifier = base
				.background(badgeColor.copy(alpha = 0.18f))
				.padding(horizontal = 6.dp, vertical = 2.dp)
		)
	} else {
		Text(
			text = text,
			style = MaterialTheme.typography.bodyLarge,
			modifier = base
				.padding(horizontal = 6.dp, vertical = 2.dp)
				.alpha(contentAlpha)
		)
	}
}

@Composable
private fun SetHistoryDialog(
	exerciseTitle: String,
	setNumber: Int,
	hasWeight: Boolean,
	weightUnit: WeightUnit,
	loadHistory: suspend () -> List<SetHistoryEntry>,
	onDismiss: () -> Unit
) {
	var history by remember(setNumber) { mutableStateOf<List<SetHistoryEntry>?>(null) }
	LaunchedEffect(setNumber) {
		history = loadHistory()
	}
	val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text("$exerciseTitle — ${stringResource(R.string.set_history_title, setNumber)}") },
		text = {
			val entries = history
			when {
				entries == null -> Box(
					modifier = Modifier.fillMaxWidth(),
					contentAlignment = Alignment.Center
				) {
					CircularProgressIndicator()
				}
				entries.isEmpty() -> Text(stringResource(R.string.set_history_empty))
				else -> LazyColumn(
					modifier = Modifier.heightIn(max = 360.dp),
					verticalArrangement = Arrangement.spacedBy(8.dp)
				) {
					items(entries) { item ->
						val date = Instant.ofEpochMilli(item.workoutDate)
							.atZone(ZoneId.systemDefault())
							.toLocalDate()
							.format(dateFormatter)
						val weightText = if (hasWeight && item.weightKg > 0.0) {
							if (item.dropWeightKg != null && item.dropWeightKg > 0.0) {
								weightUnit.formatWeightPair(item.weightKg, item.dropWeightKg)
							} else {
								weightUnit.formatWeight(item.weightKg)
							}
						} else {
							null
						}
						val repsText = if (item.dropReps != null) {
							"${item.reps} / ${item.dropReps} ${stringResource(R.string.reps)}"
						} else {
							"${item.reps} ${stringResource(R.string.reps)}"
						}
						Row(
							modifier = Modifier.fillMaxWidth(),
							horizontalArrangement = Arrangement.spacedBy(12.dp),
							verticalAlignment = Alignment.CenterVertically
						) {
							Text(
								text = date,
								style = MaterialTheme.typography.bodyMedium,
								color = MaterialTheme.colorScheme.onSurfaceVariant,
								modifier = Modifier.weight(1f)
							)
							if (weightText != null) {
								Text(
									text = weightText,
									style = MaterialTheme.typography.bodyMedium,
									fontWeight = FontWeight.Medium
								)
							}
							Text(
								text = repsText,
								style = MaterialTheme.typography.bodyMedium
							)
						}
					}
				}
			}
		},
		confirmButton = {
			Button(onClick = onDismiss) {
				Text(stringResource(R.string.close))
			}
		}
	)
}
