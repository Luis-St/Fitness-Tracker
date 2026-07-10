package net.luis.tracker.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.luis.tracker.FitnessTrackerApp
import net.luis.tracker.R
import net.luis.tracker.data.repository.SettingsRepository
import net.luis.tracker.domain.model.TimerResumeMode
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSettingsScreen(
	app: FitnessTrackerApp,
	onNavigateBack: () -> Unit,
	onOpenStreakSettings: () -> Unit = {}
) {
	val factory = remember {
		SettingsViewModel.Factory(settingsRepository = SettingsRepository(app))
	}
	val viewModel: SettingsViewModel = viewModel(factory = factory)
	val uiState by viewModel.uiState.collectAsState()

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.workout_settings)) },
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

			Spacer(modifier = Modifier.height(8.dp))

			SettingsNavigationRow(
				title = stringResource(R.string.streak_settings_title),
				description = stringResource(R.string.streak_settings_desc),
				onClick = onOpenStreakSettings
			)

			HorizontalDivider()
			Spacer(modifier = Modifier.height(16.dp))

			Text(
				text = stringResource(R.string.timer_on_resume),
				style = MaterialTheme.typography.bodyLarge
			)
			Spacer(modifier = Modifier.height(8.dp))

			val timerResumeModes = TimerResumeMode.entries
			val timerResumeLabels = listOf(
				stringResource(R.string.resume),
				stringResource(R.string.stopped)
			)
			SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
				timerResumeModes.forEachIndexed { index, mode ->
					SegmentedButton(
						selected = uiState.timerResumeMode == mode,
						onClick = { viewModel.setTimerResumeMode(mode) },
						shape = SegmentedButtonDefaults.itemShape(
							index = index,
							count = timerResumeModes.size
						)
					) {
						Text(timerResumeLabels[index])
					}
				}
			}

			Spacer(modifier = Modifier.height(24.dp))
		}
	}
}
