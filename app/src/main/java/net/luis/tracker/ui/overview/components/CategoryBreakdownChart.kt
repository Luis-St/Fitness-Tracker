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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.ehsannarmani.compose_charts.PieChart
import ir.ehsannarmani.compose_charts.models.Pie
import net.luis.tracker.R
import net.luis.tracker.data.local.dao.CategoryWorkoutCount

private val categoryColors = listOf(
	Color(0xFF4CAF50),
	Color(0xFF2196F3),
	Color(0xFFFF9800),
	Color(0xFFE91E63),
	Color(0xFF9C27B0),
	Color(0xFF00BCD4),
	Color(0xFFFF5722),
	Color(0xFF607D8B),
	Color(0xFF795548),
	Color(0xFF3F51B5)
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryBreakdownChart(
	categoryBreakdown: List<CategoryWorkoutCount>,
	modifier: Modifier = Modifier
) {
	if (categoryBreakdown.isEmpty()) return

	val noCategoryLabel = stringResource(R.string.no_category_label)

	Card(
		modifier = modifier.fillMaxWidth(),
		elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
	) {
		Column(
			modifier = Modifier.padding(16.dp),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Text(
				text = stringResource(R.string.category_breakdown),
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.Bold,
				modifier = Modifier.fillMaxWidth()
			)
			Spacer(modifier = Modifier.height(16.dp))

			var pieData by remember(categoryBreakdown) {
				mutableStateOf(
					categoryBreakdown.mapIndexed { index, category ->
						Pie(
							label = if (category.categoryName == "Uncategorized") noCategoryLabel else category.categoryName,
							data = category.count.toDouble(),
							color = categoryColors[index % categoryColors.size],
							selectedColor = categoryColors[index % categoryColors.size].copy(alpha = 0.8f)
						)
					}
				)
			}

			PieChart(
				modifier = Modifier.size(200.dp),
				data = pieData,
				onPieClick = { clickedPie ->
					val pieIndex = pieData.indexOf(clickedPie)
					pieData = pieData.mapIndexed { mapIndex, pie ->
						pie.copy(selected = pieIndex == mapIndex && !pie.selected)
					}
				},
				selectedScale = 1.1f,
				style = Pie.Style.Stroke(width = 48.dp)
			)

			Spacer(modifier = Modifier.height(16.dp))

			// Legend
			FlowRow(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(16.dp),
				verticalArrangement = Arrangement.spacedBy(8.dp)
			) {
				categoryBreakdown.forEachIndexed { index, category ->
					Row(verticalAlignment = Alignment.CenterVertically) {
						Box(
							modifier = Modifier
								.size(12.dp)
								.clip(CircleShape)
								.background(categoryColors[index % categoryColors.size])
						)
						Spacer(modifier = Modifier.width(6.dp))
						val displayName = if (category.categoryName == "Uncategorized") noCategoryLabel else category.categoryName
						Text(
							text = "$displayName (${category.count})",
							style = MaterialTheme.typography.bodySmall
						)
					}
				}
			}
		}
	}
}
