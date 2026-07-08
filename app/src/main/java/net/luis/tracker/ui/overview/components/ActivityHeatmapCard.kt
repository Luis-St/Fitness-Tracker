package net.luis.tracker.ui.overview.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.luis.tracker.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

private const val WEEKS = 52
private val CELL_SIZE = 16.dp
private val CELL_GAP = 4.dp

/**
 * Yearly activity heatmap where each cell is one week, shaded by how many days that week had a
 * workout (0–7). Assumes at most one workout per day, so the per-week active-day count is the
 * meaningful intensity. Wraps to fill the available width — no horizontal scrolling.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActivityHeatmapCard(
	countByDay: Map<LocalDate, Int>,
	modifier: Modifier = Modifier
) {
	Card(
		modifier = modifier.fillMaxWidth(),
		elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
	) {
		Column(modifier = Modifier.padding(16.dp)) {
			Text(
				text = stringResource(R.string.activity_heatmap),
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.Bold
			)
			Spacer(modifier = Modifier.height(16.dp))

			val today = LocalDate.now()
			val currentWeekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
			val firstMonday = currentWeekMonday.minusWeeks((WEEKS - 1).toLong())

			// Empty cells need clear contrast against the card surface, otherwise the grid
			// looks blank. onSurface at low alpha reads well in both light and dark themes.
			val emptyColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
			val baseColor = MaterialTheme.colorScheme.primary

			FlowRow(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(CELL_GAP),
				verticalArrangement = Arrangement.spacedBy(CELL_GAP)
			) {
				for (week in 0 until WEEKS) {
					val weekStart = firstMonday.plusWeeks(week.toLong())
					val activeDays = (0 until 7).count { d ->
						val date = weekStart.plusDays(d.toLong())
						!date.isAfter(today) && (countByDay[date] ?: 0) > 0
					}
					Box(
						modifier = Modifier
							.size(CELL_SIZE)
							.clip(RoundedCornerShape(3.dp))
							.background(cellColor(activeDays, emptyColor, baseColor))
					)
				}
			}

			Spacer(modifier = Modifier.height(12.dp))

			// Legend: Less [ ][ ][ ][ ] More
			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(CELL_GAP)
			) {
				Text(
					text = stringResource(R.string.heatmap_less),
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
				listOf(0, 2, 4, 7).forEach { activeDays ->
					Box(
						modifier = Modifier
							.size(CELL_SIZE)
							.clip(RoundedCornerShape(3.dp))
							.background(cellColor(activeDays, emptyColor, baseColor))
					)
				}
				Text(
					text = stringResource(R.string.heatmap_more),
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
		}
	}
}

private fun cellColor(activeDays: Int, emptyColor: Color, baseColor: Color): Color =
	if (activeDays <= 0) emptyColor else baseColor.copy(alpha = alphaFor(activeDays))

/** Buckets the weekly active-day count (1–7) into four visible alpha levels. */
private fun alphaFor(activeDays: Int): Float = when {
	activeDays <= 2 -> 0.5f
	activeDays <= 4 -> 0.68f
	activeDays <= 6 -> 0.84f
	else -> 1f
}
