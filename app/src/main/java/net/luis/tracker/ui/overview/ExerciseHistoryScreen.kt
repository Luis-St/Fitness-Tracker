package net.luis.tracker.ui.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.luis.tracker.FitnessTrackerApp
import net.luis.tracker.R
import net.luis.tracker.data.local.dao.ExerciseSetHistory
import net.luis.tracker.data.repository.StatsRepository
import net.luis.tracker.domain.model.WeightUnit
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseHistoryScreen(
	app: FitnessTrackerApp,
	exerciseId: Long,
	exerciseTitle: String,
	weightUnit: WeightUnit,
	onNavigateBack: () -> Unit
) {
	val statsRepository = remember { StatsRepository(app.database.statsDao()) }
	val history by statsRepository.getExerciseSetHistory(exerciseId).collectAsState(initial = emptyList())
	val dateFormatter = remember {
		DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
	}

	val groupedByWorkout: List<Pair<String, List<ExerciseSetHistory>>> = remember(history) {
		history.groupBy { it.workoutId }
			.values
			.map { sets ->
				val date = Instant.ofEpochMilli(sets.first().workoutDate)
					.atZone(ZoneId.systemDefault())
					.toLocalDate()
					.format(dateFormatter)
				date to sets
			}
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(exerciseTitle) },
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
			items(groupedByWorkout) { (date, sets) ->
				Spacer(modifier = Modifier.height(12.dp))
				Text(
					text = date,
					style = MaterialTheme.typography.titleSmall,
					fontWeight = FontWeight.Bold,
					color = MaterialTheme.colorScheme.primary
				)
				Spacer(modifier = Modifier.height(8.dp))
				sets.forEach { set ->
					SetRow(set = set, weightUnit = weightUnit)
					Spacer(modifier = Modifier.height(4.dp))
				}
				HorizontalDivider(
					modifier = Modifier.padding(top = 4.dp),
					color = MaterialTheme.colorScheme.outlineVariant
				)
			}
		}
	}
}

@Composable
private fun SetRow(
	set: ExerciseSetHistory,
	weightUnit: WeightUnit
) {
	Row(
		modifier = Modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.SpaceBetween
	) {
		Text(
			text = stringResource(R.string.set_detail_format, set.setNumber),
			style = MaterialTheme.typography.bodyMedium
		)
		Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
			if (set.weightKg > 0) {
				Text(
					text = weightUnit.formatWeight(set.weightKg),
					style = MaterialTheme.typography.bodyMedium,
					fontWeight = FontWeight.Medium
				)
			}
			Text(
				text = "${set.reps} ${stringResource(R.string.reps)}",
				style = MaterialTheme.typography.bodyMedium
			)
		}
	}
}
