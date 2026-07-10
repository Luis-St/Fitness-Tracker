package net.luis.tracker.ui.settings

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.luis.tracker.FitnessTrackerApp
import net.luis.tracker.R
import net.luis.tracker.data.repository.SettingsRepository
import net.luis.tracker.domain.model.AppLanguage
import net.luis.tracker.domain.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
	app: FitnessTrackerApp,
	onNavigateBack: () -> Unit
) {
	val factory = remember {
		SettingsViewModel.Factory(settingsRepository = SettingsRepository(app))
	}
	val viewModel: SettingsViewModel = viewModel(factory = factory)
	val uiState by viewModel.uiState.collectAsState()

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.appearance)) },
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

			Text(
				text = stringResource(R.string.language),
				style = MaterialTheme.typography.bodyLarge
			)
			Spacer(modifier = Modifier.height(8.dp))

			val languages = AppLanguage.entries
			val languageLabels = listOf(
				stringResource(R.string.system_default),
				"English",
				"Deutsch"
			)
			SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
				languages.forEachIndexed { index, lang ->
					SegmentedButton(
						selected = uiState.appLanguage == lang,
						onClick = { viewModel.setAppLanguage(lang) },
						shape = SegmentedButtonDefaults.itemShape(
							index = index,
							count = languages.size
						)
					) {
						Text(languageLabels[index])
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
		}
	}
}
