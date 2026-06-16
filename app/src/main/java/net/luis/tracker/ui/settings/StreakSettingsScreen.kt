package net.luis.tracker.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.luis.tracker.FitnessTrackerApp
import net.luis.tracker.R
import net.luis.tracker.data.repository.SettingsRepository
import net.luis.tracker.data.repository.StatsRepository
import net.luis.tracker.domain.StreakGap
import net.luis.tracker.domain.model.StreakException
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakSettingsScreen(
	app: FitnessTrackerApp,
	onNavigateBack: () -> Unit
) {
	val factory = remember {
		StreakSettingsViewModel.Factory(
			statsRepository = StatsRepository(app.database.statsDao()),
			settingsRepository = SettingsRepository(app)
		)
	}
	val viewModel: StreakSettingsViewModel = viewModel(factory = factory)
	val uiState by viewModel.uiState.collectAsState()

	val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

	var showPausePicker by remember { mutableStateOf(false) }

	if (showPausePicker) {
		PauseRangePickerScreen(
			onDismiss = { showPausePicker = false },
			onRangeSelected = { start, end ->
				viewModel.addPause(start, end)
				showPausePicker = false
			}
		)
		return
	}

	if (uiState.showRestoreDialog) {
		RestoreGapsDialog(
			gaps = uiState.gaps,
			dateFormatter = dateFormatter,
			onDismiss = { viewModel.dismissRestoreDialog() },
			onConfirm = { selected -> viewModel.restoreGaps(selected) }
		)
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.streak_settings_title)) },
				navigationIcon = {
					IconButton(onClick = onNavigateBack) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = stringResource(R.string.back)
						)
					}
				}
			)
		}
	) { innerPadding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(innerPadding)
				.verticalScroll(rememberScrollState())
				.padding(horizontal = 16.dp)
		) {
			Spacer(modifier = Modifier.height(8.dp))

			// --- Computed streak preview ---
			Text(
				text = stringResource(R.string.current_streak),
				style = MaterialTheme.typography.bodyLarge
			)
			Text(
				text = stringResource(R.string.streak_current_value, uiState.computedStreak),
				style = MaterialTheme.typography.headlineMedium,
				fontWeight = FontWeight.Bold,
				color = MaterialTheme.colorScheme.primary
			)

			Spacer(modifier = Modifier.height(24.dp))

			// --- Weekly goal ---
			SectionHeaderText(stringResource(R.string.weekly_workout_goal))
			Text(
				text = stringResource(R.string.weekly_workout_goal_value, uiState.weeklyGoal),
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
			Slider(
				value = uiState.weeklyGoal.toFloat(),
				onValueChange = { viewModel.setWeeklyGoal(it.roundToInt()) },
				valueRange = 1f..7f,
				steps = 5,
				modifier = Modifier.fillMaxWidth()
			)

			Spacer(modifier = Modifier.height(24.dp))

			// --- Start value (baseline) ---
			SectionHeaderText(stringResource(R.string.streak_start_value))
			Text(
				text = stringResource(R.string.streak_start_value_desc),
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				modifier = Modifier.padding(bottom = 8.dp)
			)

			var baselineText by remember { mutableStateOf(uiState.baseline.toString()) }
			LaunchedEffect(uiState.baseline) {
				if (baselineText.toIntOrNull() != uiState.baseline) {
					baselineText = uiState.baseline.toString()
				}
			}
			OutlinedTextField(
				value = baselineText,
				onValueChange = { input ->
					if (input.isEmpty() || input == "-" || input.matches(Regex("-?\\d+"))) {
						baselineText = input
						input.toIntOrNull()?.let { viewModel.setBaseline(it) }
					}
				},
				label = { Text(stringResource(R.string.streak_start_value)) },
				singleLine = true,
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
				modifier = Modifier.fillMaxWidth()
			)

			Spacer(modifier = Modifier.height(24.dp))

			// --- Pauses & exceptions ---
			SectionHeaderText(stringResource(R.string.streak_pauses_header))

			if (uiState.exceptions.isEmpty()) {
				Text(
					text = stringResource(R.string.streak_no_exceptions),
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					modifier = Modifier.padding(bottom = 8.dp)
				)
			} else {
				val sorted = uiState.exceptions.sortedByDescending { it.start }
				sorted.forEachIndexed { index, exception ->
					ExceptionRow(
						exception = exception,
						dateFormatter = dateFormatter,
						onDelete = { viewModel.deleteException(exception.id) }
					)
					if (index < sorted.lastIndex) HorizontalDivider()
				}
			}

			Spacer(modifier = Modifier.height(8.dp))

			FilledTonalButton(
				onClick = { showPausePicker = true },
				modifier = Modifier.fillMaxWidth()
			) {
				Icon(
					imageVector = Icons.Default.Add,
					contentDescription = null,
					modifier = Modifier.padding(end = 8.dp)
				)
				Text(stringResource(R.string.streak_add_pause))
			}

			Spacer(modifier = Modifier.height(32.dp))

			// --- Danger zone ---
			SectionHeaderText(
				text = stringResource(R.string.streak_danger_zone),
				color = MaterialTheme.colorScheme.error
			)

			OutlinedButton(
				onClick = { viewModel.openRestoreDialog() },
				colors = ButtonDefaults.outlinedButtonColors(
					contentColor = MaterialTheme.colorScheme.error
				),
				border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
				modifier = Modifier.fillMaxWidth()
			) {
				Text(stringResource(R.string.streak_restore_button))
			}

			Spacer(modifier = Modifier.height(24.dp))
		}
	}
}

@Composable
private fun ExceptionRow(
	exception: StreakException,
	dateFormatter: DateTimeFormatter,
	onDelete: () -> Unit
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(vertical = 4.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Text(
			text = if (exception.end == exception.start) {
				exception.start.format(dateFormatter)
			} else {
				stringResource(
					R.string.streak_date_range,
					exception.start.format(dateFormatter),
					exception.end.format(dateFormatter)
				)
			},
			style = MaterialTheme.typography.bodyLarge,
			modifier = Modifier.weight(1f)
		)
		IconButton(onClick = onDelete) {
			Icon(
				imageVector = Icons.Default.Delete,
				contentDescription = stringResource(R.string.remove),
				tint = MaterialTheme.colorScheme.error
			)
		}
	}
}

/** Quick-select presets offered in the pause picker's dropdown. */
private enum class PausePreset { CUSTOM, SINGLE_DAY, THIS_WEEK, LAST_WEEK }

/** Which custom field a single-date picker is currently editing. */
private enum class DateField { START, END }

/**
 * Full-screen pause picker matching the app's other screens (Scaffold + TopAppBar).
 * A quick-select dropdown offers common ranges; choosing "Custom" reveals Start and
 * End fields, each opening a single-date picker. Non-custom presets hide the fields.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PauseRangePickerScreen(
	onDismiss: () -> Unit,
	onRangeSelected: (start: LocalDate, end: LocalDate) -> Unit
) {
	BackHandler(onBack = onDismiss)

	val today = remember { LocalDate.now() }
	val locale = LocalConfiguration.current.locales[0]
	val dateFormatter = remember(locale) {
		DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
	}

	var preset by remember { mutableStateOf(PausePreset.CUSTOM) }
	var startDate by remember { mutableStateOf<LocalDate?>(null) }
	var endDate by remember { mutableStateOf<LocalDate?>(null) }
	var editingField by remember { mutableStateOf<DateField?>(null) }

	// The effective range from the active preset, or the custom Start/End fields.
	val range: Pair<LocalDate, LocalDate>? = when (preset) {
		PausePreset.SINGLE_DAY -> today to today
		PausePreset.THIS_WEEK -> {
			val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
			monday to monday.plusDays(6)
		}
		PausePreset.LAST_WEEK -> {
			val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1)
			monday to monday.plusDays(6)
		}
		PausePreset.CUSTOM -> startDate?.let { s ->
			val e = endDate ?: s
			minOf(s, e) to maxOf(s, e)
		}
	}

	if (editingField != null) {
		val current = if (editingField == DateField.START) startDate else endDate
		SingleDatePickerDialog(
			initialDate = current ?: today,
			onDismiss = { editingField = null },
			onDateSelected = { picked ->
				if (editingField == DateField.START) startDate = picked else endDate = picked
				editingField = null
			}
		)
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.streak_pick_range)) },
				navigationIcon = {
					IconButton(onClick = onDismiss) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = stringResource(R.string.back)
						)
					}
				}
			)
		},
		bottomBar = {
			Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
				Text(
					text = range?.let { (s, e) ->
						if (s == e) s.format(dateFormatter)
						else stringResource(
							R.string.streak_date_range,
							s.format(dateFormatter),
							e.format(dateFormatter)
						)
					} ?: stringResource(R.string.streak_no_selection),
					style = MaterialTheme.typography.bodyLarge,
					modifier = Modifier.padding(bottom = 8.dp)
				)
				FilledTonalButton(
					onClick = { range?.let { (s, e) -> onRangeSelected(s, e) } },
					enabled = range != null,
					modifier = Modifier.fillMaxWidth()
				) {
					Text(stringResource(R.string.save))
				}
			}
		}
	) { innerPadding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(innerPadding)
				.verticalScroll(rememberScrollState())
				.padding(horizontal = 16.dp)
		) {
			Spacer(modifier = Modifier.height(8.dp))
			PausePresetDropdown(
				selected = preset,
				onPresetSelected = { preset = it },
				modifier = Modifier.fillMaxWidth()
			)

			if (preset == PausePreset.CUSTOM) {
				Spacer(modifier = Modifier.height(16.dp))
				DateInputField(
					label = stringResource(R.string.streak_start_date),
					value = startDate?.format(dateFormatter).orEmpty(),
					onClick = { editingField = DateField.START }
				)
				Spacer(modifier = Modifier.height(12.dp))
				DateInputField(
					label = stringResource(R.string.streak_end_date),
					value = endDate?.format(dateFormatter).orEmpty(),
					onClick = { editingField = DateField.END }
				)
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PausePresetDropdown(
	selected: PausePreset,
	onPresetSelected: (PausePreset) -> Unit,
	modifier: Modifier = Modifier
) {
	var expanded by remember { mutableStateOf(false) }
	val options = listOf(
		PausePreset.CUSTOM to stringResource(R.string.streak_quick_custom),
		PausePreset.SINGLE_DAY to stringResource(R.string.streak_quick_single_day),
		PausePreset.THIS_WEEK to stringResource(R.string.streak_quick_this_week),
		PausePreset.LAST_WEEK to stringResource(R.string.streak_quick_last_week)
	)
	val selectedLabel = options.first { it.first == selected }.second

	ExposedDropdownMenuBox(
		expanded = expanded,
		onExpandedChange = { expanded = it },
		modifier = modifier
	) {
		OutlinedTextField(
			value = selectedLabel,
			onValueChange = {},
			readOnly = true,
			label = { Text(stringResource(R.string.streak_quick_select)) },
			trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
			modifier = Modifier
				.menuAnchor(MenuAnchorType.PrimaryNotEditable)
				.fillMaxWidth()
		)
		ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
			options.forEach { (preset, label) ->
				DropdownMenuItem(
					text = { Text(label) },
					onClick = {
						onPresetSelected(preset)
						expanded = false
					}
				)
			}
		}
	}
}

/** Read-only text field that opens a date picker when tapped anywhere on it. */
@Composable
private fun DateInputField(
	label: String,
	value: String,
	onClick: () -> Unit,
	modifier: Modifier = Modifier
) {
	Box(modifier = modifier.fillMaxWidth()) {
		OutlinedTextField(
			value = value,
			onValueChange = {},
			readOnly = true,
			label = { Text(label) },
			trailingIcon = {
				Icon(imageVector = Icons.Default.DateRange, contentDescription = null)
			},
			modifier = Modifier.fillMaxWidth()
		)
		// A read-only OutlinedTextField swallows clicks, so overlay a transparent
		// click target to open the picker from anywhere on the field.
		Box(
			modifier = Modifier
				.matchParentSize()
				.clickable(onClick = onClick)
		)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleDatePickerDialog(
	initialDate: LocalDate,
	onDismiss: () -> Unit,
	onDateSelected: (LocalDate) -> Unit
) {
	// Re-provide the in-app locale overrides: a Dialog hosts its content in its own
	// AndroidComposeView, which otherwise resets them to the window's default locale.
	val localizedContext = LocalContext.current
	val localizedConfiguration = LocalConfiguration.current
	val saveLabel = stringResource(R.string.save)
	val cancelLabel = stringResource(R.string.cancel)

	val state = rememberDatePickerState(
		initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
	)

	DatePickerDialog(
		onDismissRequest = onDismiss,
		confirmButton = {
			TextButton(
				enabled = state.selectedDateMillis != null,
				onClick = {
					val millis = state.selectedDateMillis ?: return@TextButton
					onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
				}
			) { Text(saveLabel) }
		},
		dismissButton = {
			TextButton(onClick = onDismiss) { Text(cancelLabel) }
		}
	) {
		CompositionLocalProvider(
			LocalContext provides localizedContext,
			LocalConfiguration provides localizedConfiguration
		) {
			DatePicker(state = state)
		}
	}
}

@Composable
private fun RestoreGapsDialog(
	gaps: List<StreakGap>,
	dateFormatter: DateTimeFormatter,
	onDismiss: () -> Unit,
	onConfirm: (List<StreakGap>) -> Unit
) {
	val selected = remember(gaps) { mutableStateListOf<StreakGap>() }
	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(stringResource(R.string.streak_restore_dialog_title)) },
		text = {
			if (gaps.isEmpty()) {
				Text(stringResource(R.string.streak_restore_dialog_empty))
			} else {
				Column(
					modifier = Modifier
						.fillMaxWidth()
						.heightIn(max = 360.dp)
						.verticalScroll(rememberScrollState())
				) {
					gaps.forEach { gap ->
						val checked = gap in selected
						Row(
							modifier = Modifier
								.fillMaxWidth()
								.clickable {
									if (checked) selected.remove(gap) else selected.add(gap)
								}
								.padding(vertical = 4.dp),
							verticalAlignment = Alignment.CenterVertically
						) {
							Checkbox(
								checked = checked,
								onCheckedChange = {
									if (checked) selected.remove(gap) else selected.add(gap)
								}
							)
							Column(modifier = Modifier.weight(1f)) {
								Text(
									text = stringResource(
										R.string.streak_date_range,
										gap.weekStart.format(dateFormatter),
										gap.weekEnd.format(dateFormatter)
									),
									style = MaterialTheme.typography.bodyMedium
								)
								Text(
									text = stringResource(R.string.streak_gap_workouts, gap.workoutCount),
									style = MaterialTheme.typography.bodySmall,
									color = MaterialTheme.colorScheme.onSurfaceVariant
								)
							}
						}
					}
				}
			}
		},
		confirmButton = {
			TextButton(
				onClick = { onConfirm(selected.toList()) },
				enabled = gaps.isNotEmpty() && selected.isNotEmpty()
			) {
				Text(stringResource(R.string.streak_restore_confirm))
			}
		},
		dismissButton = {
			TextButton(onClick = onDismiss) {
				Text(stringResource(R.string.cancel))
			}
		}
	)
}

@Composable
private fun SectionHeaderText(
	text: String,
	color: Color = MaterialTheme.colorScheme.primary
) {
	Text(
		text = text,
		style = MaterialTheme.typography.titleSmall,
		color = color,
		modifier = Modifier.padding(bottom = 8.dp)
	)
}
