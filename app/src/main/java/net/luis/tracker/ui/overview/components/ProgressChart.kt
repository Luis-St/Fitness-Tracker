package net.luis.tracker.ui.overview.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.DotProperties
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.GridProperties
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.Line
import net.luis.tracker.R
import net.luis.tracker.data.local.dao.ExerciseProgress
import net.luis.tracker.domain.model.ChartMetric
import net.luis.tracker.domain.model.WeightUnit
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ProgressChart(
	progressData: List<ExerciseProgress>,
	metric: ChartMetric,
	weightUnit: WeightUnit,
	modifier: Modifier = Modifier
) {
	if (progressData.isEmpty()) {
		Box(
			modifier = modifier
				.fillMaxWidth()
				.height(200.dp),
			contentAlignment = Alignment.Center
		) {
			Text(
				text = stringResource(R.string.no_data_yet),
				style = MaterialTheme.typography.bodyLarge,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
		return
	}

	val primary = MaterialTheme.colorScheme.primary
	val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM") }
	val zone = remember { ZoneId.systemDefault() }

	val values = remember(progressData, metric, weightUnit) {
		progressData.map { progress ->
			when (metric) {
				ChartMetric.MAX_WEIGHT -> weightUnit.convertFromKg(progress.maxWeight)
				ChartMetric.TOTAL_VOLUME -> weightUnit.convertFromKg(progress.totalVolume)
				ChartMetric.MAX_REPS -> progress.maxReps.toDouble()
				ChartMetric.SET_COUNT -> progress.setCount.toDouble()
			}
		}
	}

	val dateLabels = remember(progressData) {
		progressData.map { progress ->
			Instant.ofEpochMilli(progress.startTime)
				.atZone(zone)
				.toLocalDate()
				.format(dateFormatter)
		}
	}

	val lineData = remember(values, primary) {
		listOf(
			Line(
				label = "",
				values = values,
				color = SolidColor(primary),
				firstGradientFillColor = primary.copy(alpha = 0.4f),
				secondGradientFillColor = Color.Transparent,
				drawStyle = DrawStyle.Stroke(width = 2.dp),
				curvedEdges = true,
				dotProperties = DotProperties(
					enabled = true,
					radius = 4.dp,
					color = SolidColor(primary),
					strokeWidth = 2.dp,
					strokeColor = SolidColor(primary)
				)
			)
		)
	}

	LineChart(
		modifier = modifier
			.fillMaxWidth()
			.height(250.dp),
		data = lineData,
		animationMode = AnimationMode.Together(delayBuilder = { it * 100L }),
		labelHelperProperties = LabelHelperProperties(enabled = false),
		labelProperties = LabelProperties(
			enabled = true,
			labels = dateLabels
		),
		indicatorProperties = HorizontalIndicatorProperties(enabled = true),
		gridProperties = GridProperties(enabled = false)
	)
}
