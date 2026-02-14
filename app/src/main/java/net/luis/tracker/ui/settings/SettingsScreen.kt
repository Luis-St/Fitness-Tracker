package net.luis.tracker.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.luis.tracker.FitnessTrackerApp
import net.luis.tracker.R
import net.luis.tracker.data.export.DataExporter
import net.luis.tracker.data.export.DataImporter
import net.luis.tracker.data.repository.SettingsRepository
import net.luis.tracker.domain.model.ThemeMode
import net.luis.tracker.domain.model.WeightUnit
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
	app: FitnessTrackerApp,
	onNavigateBack: () -> Unit
) {
	val viewModel: SettingsViewModel = viewModel(
		factory = SettingsViewModel.Factory(
			settingsRepository = SettingsRepository(app)
		)
	)

	val uiState by viewModel.uiState.collectAsState()
	val context = LocalContext.current
	val scope = rememberCoroutineScope()
	val snackbarHostState = remember { SnackbarHostState() }

	val exportSuccessMsg = stringResource(R.string.export_success)
	val exportErrorMsg = stringResource(R.string.export_error)
	val importSuccessMsg = stringResource(R.string.import_success)
	val importErrorMsg = stringResource(R.string.import_error)

	val exportLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.CreateDocument("application/json")
	) { uri: Uri? ->
		uri?.let {
			scope.launch {
				try {
					withContext(Dispatchers.IO) {
						context.contentResolver.openOutputStream(it)?.use { outputStream ->
							DataExporter.export(app.database, outputStream)
						}
					}
					snackbarHostState.showSnackbar(exportSuccessMsg)
				} catch (e: Exception) {
					snackbarHostState.showSnackbar(exportErrorMsg)
				}
			}
		}
	}

	val importLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.OpenDocument()
	) { uri: Uri? ->
		uri?.let {
			scope.launch {
				try {
					withContext(Dispatchers.IO) {
						context.contentResolver.openInputStream(it)?.use { inputStream ->
							DataImporter.`import`(app.database, inputStream)
						}
					}
					snackbarHostState.showSnackbar(importSuccessMsg)
				} catch (e: Exception) {
					snackbarHostState.showSnackbar(importErrorMsg)
				}
			}
		}
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.settings)) },
				navigationIcon = {
					IconButton(onClick = onNavigateBack) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = stringResource(R.string.back)
						)
					}
				}
			)
		},
		snackbarHost = { SnackbarHost(snackbarHostState) }
	) { innerPadding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(innerPadding)
				.verticalScroll(rememberScrollState())
				.padding(horizontal = 16.dp)
		) {
			// --- Appearance Section ---
			SectionHeader(text = stringResource(R.string.appearance))

			Text(
				text = stringResource(R.string.theme),
				style = MaterialTheme.typography.bodyLarge
			)
			Spacer(modifier = Modifier.height(8.dp))

			val themeModes = ThemeMode.entries
			val themeLabels = listOf(
				stringResource(R.string.light),
				stringResource(R.string.dark),
				stringResource(R.string.system_default)
			)
			SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
				themeModes.forEachIndexed { index, mode ->
					SegmentedButton(
						selected = uiState.themeMode == mode,
						onClick = { viewModel.setThemeMode(mode) },
						shape = SegmentedButtonDefaults.itemShape(
							index = index,
							count = themeModes.size
						)
					) {
						Text(themeLabels[index])
					}
				}
			}

			Spacer(modifier = Modifier.height(16.dp))

			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically
			) {
				Column(modifier = Modifier.weight(1f)) {
					Text(
						text = stringResource(R.string.dynamic_colors),
						style = MaterialTheme.typography.bodyLarge
					)
					Text(
						text = stringResource(R.string.dynamic_colors_desc),
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
				Switch(
					checked = uiState.dynamicColors,
					onCheckedChange = { viewModel.setDynamicColors(it) }
				)
			}

			Spacer(modifier = Modifier.height(24.dp))

			// --- Units Section ---
			SectionHeader(text = stringResource(R.string.units))

			Text(
				text = stringResource(R.string.weight_unit),
				style = MaterialTheme.typography.bodyLarge
			)
			Spacer(modifier = Modifier.height(8.dp))

			val weightUnits = WeightUnit.entries
			val weightLabels = weightUnits.map { it.name.lowercase() }
			SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
				weightUnits.forEachIndexed { index, unit ->
					SegmentedButton(
						selected = uiState.weightUnit == unit,
						onClick = { viewModel.setWeightUnit(unit) },
						shape = SegmentedButtonDefaults.itemShape(
							index = index,
							count = weightUnits.size
						)
					) {
						Text(weightLabels[index])
					}
				}
			}

			Spacer(modifier = Modifier.height(24.dp))

			// --- Workout Section ---
			SectionHeader(text = stringResource(R.string.workout_settings))

			Text(
				text = stringResource(R.string.default_rest_timer),
				style = MaterialTheme.typography.bodyLarge
			)
			Spacer(modifier = Modifier.height(4.dp))

			val minutes = uiState.restTimerSeconds / 60
			val seconds = uiState.restTimerSeconds % 60
			Text(
				text = stringResource(R.string.rest_timer_format, minutes, seconds),
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)

			Slider(
				value = uiState.restTimerSeconds.toFloat(),
				onValueChange = { value ->
					// Snap to nearest step of 15
					val snapped = (value / 15f).roundToInt() * 15
					viewModel.setRestTimerSeconds(snapped.coerceIn(30, 300))
				},
				valueRange = 30f..300f,
				steps = (300 - 30) / 15 - 1, // intermediate steps between min and max
				modifier = Modifier.fillMaxWidth()
			)

			Spacer(modifier = Modifier.height(24.dp))

			// --- Data Section ---
			SectionHeader(text = stringResource(R.string.data))

			FilledTonalButton(
				onClick = { exportLauncher.launch("fitness_tracker_backup.json") },
				modifier = Modifier.fillMaxWidth()
			) {
				Icon(
					imageVector = Icons.Default.FileDownload,
					contentDescription = null,
					modifier = Modifier.padding(end = 8.dp)
				)
				Text(stringResource(R.string.export_data))
			}

			Spacer(modifier = Modifier.height(8.dp))

			FilledTonalButton(
				onClick = { importLauncher.launch(arrayOf("application/json")) },
				modifier = Modifier.fillMaxWidth()
			) {
				Icon(
					imageVector = Icons.Default.FileUpload,
					contentDescription = null,
					modifier = Modifier.padding(end = 8.dp)
				)
				Text(stringResource(R.string.import_data))
			}

			Spacer(modifier = Modifier.height(24.dp))

			// --- About Section ---
			SectionHeader(text = stringResource(R.string.about))

			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				Text(
					text = stringResource(R.string.app_version),
					style = MaterialTheme.typography.bodyLarge
				)
				Text(
					text = "1.0",
					style = MaterialTheme.typography.bodyLarge,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}

			Spacer(modifier = Modifier.height(24.dp))
		}
	}
}

@Composable
private fun SectionHeader(text: String) {
	Text(
		text = text,
		style = MaterialTheme.typography.titleSmall,
		color = MaterialTheme.colorScheme.primary,
		modifier = Modifier.padding(bottom = 12.dp)
	)
}
