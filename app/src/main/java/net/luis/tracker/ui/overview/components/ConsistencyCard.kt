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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.luis.tracker.R
import java.time.DayOfWeek
import java.time.format.TextStyle

@Composable
fun ConsistencyCard(
	avgGapDays: Double?,
	longestGapDays: Int,
	mostActiveWeekday: DayOfWeek?,
	modifier: Modifier = Modifier
) {
	val dayUnit = stringResource(R.string.unit_day_short)
	val locale = LocalConfiguration.current.locales[0]
	val placeholder = "--"

	Card(
		modifier = modifier.fillMaxWidth(),
		elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
	) {
		Column(modifier = Modifier.padding(16.dp)) {
			Text(
				text = stringResource(R.string.consistency_title),
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.Bold
			)
			Spacer(modifier = Modifier.height(16.dp))
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceEvenly
			) {
				StatItem(
					value = avgGapDays?.let { "%.1f".format(it) + dayUnit } ?: placeholder,
					label = stringResource(R.string.consistency_avg_gap),
					modifier = Modifier.weight(1f)
				)
				StatItem(
					value = if (longestGapDays > 0) "$longestGapDays$dayUnit" else placeholder,
					label = stringResource(R.string.consistency_longest_gap),
					modifier = Modifier.weight(1f)
				)
				StatItem(
					value = mostActiveWeekday?.getDisplayName(TextStyle.SHORT, locale) ?: placeholder,
					label = stringResource(R.string.consistency_most_active),
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
