package net.luis.tracker.ui.overview.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.luis.tracker.R
import net.luis.tracker.domain.model.WeightUnit

@Composable
fun StatsSummaryCard(
	workoutsThisWeek: Int,
	workoutsThisMonth: Int,
	currentStreak: Int,
	averageDuration: Double?,
	@Suppress("UNUSED_PARAMETER") weightUnit: WeightUnit,
	modifier: Modifier = Modifier
) {
	Card(
		modifier = modifier.fillMaxWidth(),
		elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
	) {
		Column(
			modifier = Modifier.padding(16.dp)
		) {
			Text(
				text = stringResource(R.string.stats_summary),
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.Bold
			)
			Spacer(modifier = Modifier.height(16.dp))
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceEvenly
			) {
				StatItem(
					value = workoutsThisWeek.toString(),
					label = stringResource(R.string.this_week),
					modifier = Modifier.weight(1f)
				)
				StatItem(
					value = workoutsThisMonth.toString(),
					label = stringResource(R.string.this_month),
					modifier = Modifier.weight(1f)
				)
			}
			Spacer(modifier = Modifier.height(12.dp))
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceEvenly
			) {
				StatItem(
					value = "$currentStreak ${stringResource(R.string.days)}",
					label = stringResource(R.string.current_streak),
					modifier = Modifier.weight(1f)
				)
				StatItem(
					value = formatDuration(averageDuration),
					label = stringResource(R.string.avg_duration),
					modifier = Modifier.weight(1f)
				)
			}
		}
	}
}

@Composable
private fun StatItem(
	value: String,
	label: String,
	modifier: Modifier = Modifier
) {
	Column(
		modifier = modifier,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Text(
			text = value,
			style = MaterialTheme.typography.headlineSmall,
			fontWeight = FontWeight.Bold,
			color = MaterialTheme.colorScheme.primary,
			textAlign = TextAlign.Center
		)
		Spacer(modifier = Modifier.height(4.dp))
		Text(
			text = label,
			style = MaterialTheme.typography.bodySmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			textAlign = TextAlign.Center
		)
	}
}

@Composable
private fun formatDuration(seconds: Double?): String {
	if (seconds == null) return "--"
	val minutes = (seconds / 60).toInt()
	return "$minutes ${stringResource(R.string.minutes_short)}"
}
