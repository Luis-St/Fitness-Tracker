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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.luis.tracker.R
import net.luis.tracker.domain.model.WeightUnit
import java.text.NumberFormat
import kotlin.math.roundToInt

private val PositiveGreen = Color(0xFF4CAF50)

/**
 * Compares the selected month against the previous month for workouts, volume and sets, showing
 * each current value with a colored percentage delta.
 */
@Composable
fun MonthComparisonCard(
	workoutsThisMonth: Int,
	prevWorkouts: Int,
	volumeThisMonth: Double,
	prevVolume: Double,
	setsThisMonth: Int,
	prevSets: Int,
	weightUnit: WeightUnit,
	modifier: Modifier = Modifier
) {
	Card(
		modifier = modifier.fillMaxWidth(),
		elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
	) {
		Column(modifier = Modifier.padding(16.dp)) {
			Text(
				text = stringResource(R.string.month_comparison_title),
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.Bold
			)
			Spacer(modifier = Modifier.height(16.dp))
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceEvenly
			) {
				ComparisonStatItem(
					value = formatCount(workoutsThisMonth.toLong()),
					deltaPercent = deltaPercent(workoutsThisMonth.toDouble(), prevWorkouts.toDouble()),
					label = stringResource(R.string.nav_workouts),
					modifier = Modifier.weight(1f)
				)
				ComparisonStatItem(
					value = weightUnit.formatWeightLarge(volumeThisMonth),
					deltaPercent = deltaPercent(volumeThisMonth, prevVolume),
					label = stringResource(R.string.stat_volume),
					modifier = Modifier.weight(1f)
				)
				ComparisonStatItem(
					value = formatCount(setsThisMonth.toLong()),
					deltaPercent = deltaPercent(setsThisMonth.toDouble(), prevSets.toDouble()),
					label = stringResource(R.string.stat_sets),
					modifier = Modifier.weight(1f)
				)
			}
		}
	}
}

/** Percent change from [previous] to [current], or null when there is no previous baseline. */
private fun deltaPercent(current: Double, previous: Double): Int? =
	if (previous <= 0.0) null else ((current - previous) / previous * 100.0).roundToInt()

private fun formatCount(value: Long): String = NumberFormat.getIntegerInstance().format(value)

@Composable
private fun ComparisonStatItem(
	value: String,
	deltaPercent: Int?,
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
		Spacer(modifier = Modifier.height(2.dp))
		Text(
			text = when {
				deltaPercent == null -> "–"
				deltaPercent > 0 -> "▲ $deltaPercent%"
				deltaPercent < 0 -> "▼ ${-deltaPercent}%"
				else -> "±0%"
			},
			style = MaterialTheme.typography.bodySmall,
			fontWeight = FontWeight.Bold,
			color = when {
				deltaPercent == null || deltaPercent == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
				deltaPercent > 0 -> PositiveGreen
				else -> MaterialTheme.colorScheme.error
			},
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
