package net.luis.tracker.ui.overview.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.luis.tracker.R
import net.luis.tracker.data.local.dao.TopExercise

@Composable
fun TopExercisesCard(
	exercises: List<TopExercise>,
	modifier: Modifier = Modifier
) {
	Card(
		modifier = modifier.fillMaxWidth(),
		elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
	) {
		Column(modifier = Modifier.padding(16.dp)) {
			Text(
				text = stringResource(R.string.top_exercises),
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.Bold
			)
			Spacer(modifier = Modifier.height(8.dp))

			if (exercises.isEmpty()) {
				Text(
					text = stringResource(R.string.no_data_yet),
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
				return@Column
			}

			exercises.forEachIndexed { index, exercise ->
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(vertical = 10.dp),
					horizontalArrangement = Arrangement.spacedBy(12.dp),
					verticalAlignment = Alignment.CenterVertically
				) {
					Text(
						text = "${index + 1}",
						style = MaterialTheme.typography.titleMedium,
						fontWeight = FontWeight.Bold,
						color = MaterialTheme.colorScheme.primary,
						modifier = Modifier.width(20.dp)
					)
					Text(
						text = exercise.exerciseTitle,
						style = MaterialTheme.typography.bodyLarge,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis,
						modifier = Modifier.weight(1f)
					)
					Text(
						text = stringResource(R.string.top_exercises_count, exercise.workoutCount),
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
				if (index < exercises.lastIndex) HorizontalDivider()
			}
		}
	}
}
