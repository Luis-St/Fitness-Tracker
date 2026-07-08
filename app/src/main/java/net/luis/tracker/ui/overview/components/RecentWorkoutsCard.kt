package net.luis.tracker.ui.overview.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.luis.tracker.R
import net.luis.tracker.data.local.dao.RecentWorkout
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun RecentWorkoutsCard(
	workouts: List<RecentWorkout>,
	onWorkoutClick: (Long) -> Unit,
	modifier: Modifier = Modifier
) {
	Card(
		modifier = modifier.fillMaxWidth(),
		elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
	) {
		Column(modifier = Modifier.padding(16.dp)) {
			Text(
				text = stringResource(R.string.recent_workouts),
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.Bold
			)
			Spacer(modifier = Modifier.height(8.dp))

			if (workouts.isEmpty()) {
				Text(
					text = stringResource(R.string.no_data_yet),
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
				return@Column
			}

			val zone = remember { ZoneId.systemDefault() }
			val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

			workouts.forEachIndexed { index, workout ->
				val date = Instant.ofEpochMilli(workout.startTime).atZone(zone).toLocalDate()
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.clickable { onWorkoutClick(workout.workoutId) }
						.padding(vertical = 12.dp),
					horizontalArrangement = Arrangement.SpaceBetween,
					verticalAlignment = Alignment.CenterVertically
				) {
					Text(
						text = date.format(dateFormatter),
						style = MaterialTheme.typography.bodyLarge
					)
					Text(
						text = formatWorkoutDuration(workout.durationSeconds),
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
				if (index < workouts.lastIndex) HorizontalDivider()
			}
		}
	}
}

/** Compact workout duration: "1h 05m" / "45m". */
@Composable
private fun formatWorkoutDuration(totalSeconds: Long): String {
	val totalMinutes = totalSeconds / 60
	val hours = totalMinutes / 60
	val minutes = totalMinutes % 60
	val hourUnit = stringResource(R.string.unit_hour_short)
	val minuteUnit = stringResource(R.string.unit_minute_short)
	return if (hours > 0) {
		"$hours$hourUnit ${minutes.toString().padStart(2, '0')}$minuteUnit"
	} else {
		"$minutes$minuteUnit"
	}
}
