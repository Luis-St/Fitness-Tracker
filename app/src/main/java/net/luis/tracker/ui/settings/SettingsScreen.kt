package net.luis.tracker.ui.settings

import net.luis.tracker.BuildConfig
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.luis.tracker.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
	onNavigateBack: () -> Unit,
	onOpenAppearance: () -> Unit = {},
	onOpenUnits: () -> Unit = {},
	onOpenWorkout: () -> Unit = {},
	onOpenSetComparison: () -> Unit = {},
	onOpenOverviewSettings: () -> Unit = {},
	onOpenData: () -> Unit = {}
) {
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

			SettingsNavigationRow(
				title = stringResource(R.string.appearance),
				description = stringResource(R.string.settings_category_appearance_desc),
				onClick = onOpenAppearance
			)
			HorizontalDivider()

			SettingsNavigationRow(
				title = stringResource(R.string.units),
				description = stringResource(R.string.settings_category_units_desc),
				onClick = onOpenUnits
			)
			HorizontalDivider()

			SettingsNavigationRow(
				title = stringResource(R.string.workout_settings),
				description = stringResource(R.string.settings_category_workout_desc),
				onClick = onOpenWorkout
			)
			HorizontalDivider()

			SettingsNavigationRow(
				title = stringResource(R.string.set_comparison_title),
				description = stringResource(R.string.settings_category_set_comparison_desc),
				onClick = onOpenSetComparison
			)
			HorizontalDivider()

			SettingsNavigationRow(
				title = stringResource(R.string.overview_sections),
				description = stringResource(R.string.overview_sections_desc),
				onClick = onOpenOverviewSettings
			)
			HorizontalDivider()

			SettingsNavigationRow(
				title = stringResource(R.string.data),
				description = stringResource(R.string.settings_category_data_desc),
				onClick = onOpenData
			)

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
					text = BuildConfig.VERSION_NAME,
					style = MaterialTheme.typography.bodyLarge,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}

			Spacer(modifier = Modifier.height(24.dp))
		}
	}
}
