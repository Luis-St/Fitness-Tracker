package net.luis.tracker.ui.settings

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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.luis.tracker.FitnessTrackerApp
import net.luis.tracker.R
import net.luis.tracker.data.repository.SettingsRepository
import net.luis.tracker.domain.model.OverviewSection
import net.luis.tracker.ui.overview.OverviewTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewSettingsScreen(
	app: FitnessTrackerApp,
	onNavigateBack: () -> Unit
) {
	val factory = remember {
		SettingsViewModel.Factory(
			settingsRepository = SettingsRepository(app)
		)
	}
	val viewModel: SettingsViewModel = viewModel(factory = factory)
	val uiState by viewModel.uiState.collectAsState()

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.overview_sections)) },
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
				text = stringResource(R.string.overview_sections_desc),
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				modifier = Modifier.padding(bottom = 16.dp)
			)

			var selectedOverviewTab by remember { mutableStateOf(OverviewTab.MONTH) }
			val overviewTabs = OverviewTab.entries
			SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
				overviewTabs.forEachIndexed { index, tab ->
					SegmentedButton(
						selected = selectedOverviewTab == tab,
						onClick = { selectedOverviewTab = tab },
						shape = SegmentedButtonDefaults.itemShape(index = index, count = overviewTabs.size)
					) {
						Text(
							when (tab) {
								OverviewTab.MONTH -> stringResource(R.string.overview_tab_month)
								OverviewTab.LIFETIME -> stringResource(R.string.overview_tab_lifetime)
							}
						)
					}
				}
			}

			Spacer(modifier = Modifier.height(8.dp))

			val overviewSections = when (selectedOverviewTab) {
				OverviewTab.MONTH -> uiState.overviewLayout.month
				OverviewTab.LIFETIME -> uiState.overviewLayout.lifetime
			}
			overviewSections.forEachIndexed { index, sectionState ->
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(vertical = 4.dp),
					verticalAlignment = Alignment.CenterVertically
				) {
					Text(
						text = stringResource(overviewSectionLabel(sectionState.section)),
						style = MaterialTheme.typography.bodyLarge,
						color = if (sectionState.visible) {
							MaterialTheme.colorScheme.onSurface
						} else {
							MaterialTheme.colorScheme.onSurfaceVariant
						},
						modifier = Modifier.weight(1f)
					)
					IconButton(onClick = {
						viewModel.toggleOverviewSection(selectedOverviewTab, sectionState.section)
					}) {
						Icon(
							imageVector = if (sectionState.visible) {
								Icons.Default.Visibility
							} else {
								Icons.Default.VisibilityOff
							},
							contentDescription = stringResource(
								if (sectionState.visible) R.string.hide_section else R.string.show_section
							),
							tint = if (sectionState.visible) {
								MaterialTheme.colorScheme.primary
							} else {
								MaterialTheme.colorScheme.onSurfaceVariant
							}
						)
					}
					IconButton(
						onClick = { viewModel.moveOverviewSection(selectedOverviewTab, index, -1) },
						enabled = index > 0
					) {
						Icon(
							imageVector = Icons.Default.KeyboardArrowUp,
							contentDescription = stringResource(R.string.move_up)
						)
					}
					IconButton(
						onClick = { viewModel.moveOverviewSection(selectedOverviewTab, index, 1) },
						enabled = index < overviewSections.lastIndex
					) {
						Icon(
							imageVector = Icons.Default.KeyboardArrowDown,
							contentDescription = stringResource(R.string.move_down)
						)
					}
				}
				if (index < overviewSections.lastIndex) HorizontalDivider()
			}

			Spacer(modifier = Modifier.height(24.dp))
		}
	}
}

internal fun overviewSectionLabel(section: OverviewSection): Int = when (section) {
	OverviewSection.STREAK -> R.string.current_streak
	OverviewSection.SUMMARY -> R.string.stats_summary
	OverviewSection.PROGRESS -> R.string.progress
	OverviewSection.RECORDS -> R.string.personal_records
	OverviewSection.CATEGORY -> R.string.category_breakdown
	OverviewSection.TOTALS -> R.string.stats_totals
	OverviewSection.RECENT_WORKOUTS -> R.string.recent_workouts
	OverviewSection.CONSISTENCY -> R.string.consistency_title
	OverviewSection.TOP_EXERCISES -> R.string.top_exercises
	OverviewSection.HEATMAP -> R.string.activity_heatmap
}
