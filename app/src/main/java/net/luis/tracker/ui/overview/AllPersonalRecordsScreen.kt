package net.luis.tracker.ui.overview

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.luis.tracker.FitnessTrackerApp
import net.luis.tracker.R
import net.luis.tracker.data.repository.StatsRepository
import net.luis.tracker.domain.model.WeightUnit
import net.luis.tracker.ui.overview.components.PersonalRecordRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllPersonalRecordsScreen(
	app: FitnessTrackerApp,
	weightUnit: WeightUnit,
	onNavigateBack: () -> Unit
) {
	val statsRepository = remember { StatsRepository(app.database.statsDao()) }
	val personalRecords by statsRepository.getPersonalRecords().collectAsState(initial = emptyList())

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.all_personal_records)) },
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
		contentWindowInsets = WindowInsets(0)
	) { innerPadding ->
		LazyColumn(
			modifier = Modifier
				.fillMaxSize()
				.padding(innerPadding)
				.padding(horizontal = 16.dp)
		) {
			itemsIndexed(personalRecords, key = { _, record -> record.exerciseId }) { index, record ->
				PersonalRecordRow(record = record, weightUnit = weightUnit)
				if (index < personalRecords.lastIndex) {
					HorizontalDivider(
						modifier = Modifier.padding(vertical = 8.dp),
						color = MaterialTheme.colorScheme.outlineVariant
					)
				}
			}
		}
	}
}
