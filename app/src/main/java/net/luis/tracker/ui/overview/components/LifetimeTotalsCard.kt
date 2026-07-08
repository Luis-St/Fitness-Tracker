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
import java.text.NumberFormat

@Composable
fun LifetimeTotalsCard(
	totalSets: Int,
	totalReps: Long,
	totalTimeSeconds: Long,
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
				text = stringResource(R.string.stats_totals),
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.Bold
			)
			Spacer(modifier = Modifier.height(16.dp))
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceEvenly
			) {
				StatItem(
					value = formatCount(totalSets.toLong()),
					label = stringResource(R.string.total_sets),
					modifier = Modifier.weight(1f)
				)
				StatItem(
					value = formatCount(totalReps),
					label = stringResource(R.string.total_reps),
					modifier = Modifier.weight(1f)
				)
				StatItem(
					value = formatTotalTime(totalTimeSeconds),
					label = stringResource(R.string.total_time),
					modifier = Modifier.weight(1f)
				)
			}
		}
	}
}

/** Formats a possibly very large count with locale-aware thousands grouping (e.g. 12,345). */
private fun formatCount(value: Long): String = NumberFormat.getIntegerInstance().format(value)

/** Compact, overflow-safe duration: "12d 6h" / "6h 5m" / "5m". */
@Composable
private fun formatTotalTime(totalSeconds: Long): String {
	val totalMinutes = totalSeconds / 60
	val days = totalMinutes / (60 * 24)
	val hours = (totalMinutes % (60 * 24)) / 60
	val minutes = totalMinutes % 60
	val dayUnit = stringResource(R.string.unit_day_short)
	val hourUnit = stringResource(R.string.unit_hour_short)
	val minuteUnit = stringResource(R.string.unit_minute_short)
	return when {
		days > 0 -> "${formatCount(days)}$dayUnit $hours$hourUnit"
		hours > 0 -> "$hours$hourUnit $minutes$minuteUnit"
		else -> "$minutes$minuteUnit"
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
