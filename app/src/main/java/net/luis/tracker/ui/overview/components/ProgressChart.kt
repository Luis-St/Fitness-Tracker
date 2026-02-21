package net.luis.tracker.ui.overview.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.*
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
	val textColor = MaterialTheme.colorScheme.onSurface
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

	val firstLabel = remember(progressData) {
		Instant.ofEpochMilli(progressData.first().startTime)
			.atZone(zone).toLocalDate().format(dateFormatter)
	}
	val lastLabel = remember(progressData) {
		Instant.ofEpochMilli(progressData.last().startTime)
			.atZone(zone).toLocalDate().format(dateFormatter)
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

	val textMeasurer = rememberTextMeasurer()
	val density = LocalDensity.current
	val indicatorStartPadding = remember(values, density) {
		val min = values.min()
		val max = values.max()
		val range = max - min
		val indicatorValues = if (range == 0.0) {
			listOf(min)
		} else {
			(0 until 5).map { i -> min + range * i / 4.0 }
		}
		val indicatorStyle = TextStyle(fontSize = 12.sp)
		val maxTextWidthPx = indicatorValues.maxOf { value ->
			textMeasurer.measure("%.1f".format(value), indicatorStyle).size.width
		}
		val paddingPx = with(density) { 12.dp.toPx() }
		with(density) { (maxTextWidthPx + paddingPx).toDp() } + 4.dp
	}

	Column(modifier = modifier.fillMaxWidth()) {
		LineChart(
			modifier = Modifier
				.fillMaxWidth()
				.height(250.dp),
			data = lineData,
			animationMode = AnimationMode.Together(delayBuilder = { it * 100L }),
			labelHelperProperties = LabelHelperProperties(enabled = false),
			labelProperties = LabelProperties(enabled = false),
			indicatorProperties = HorizontalIndicatorProperties(
				enabled = true,
				textStyle = TextStyle(color = textColor)
			),
			gridProperties = GridProperties(enabled = false)
		)
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(start = indicatorStartPadding, top = 8.dp),
			horizontalArrangement = Arrangement.SpaceBetween
		) {
			Text(
				text = firstLabel,
				style = TextStyle(fontSize = 12.sp),
				color = textColor
			)
			Text(
				text = lastLabel,
				style = TextStyle(fontSize = 12.sp),
				color = textColor
			)
		}
	}
}
