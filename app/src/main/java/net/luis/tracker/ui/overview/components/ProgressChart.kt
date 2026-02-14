package net.luis.tracker.ui.overview.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
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

	val modelProducer = remember { CartesianChartModelProducer() }

	val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM") }
	val zone = remember { ZoneId.systemDefault() }

	val dateLabels = remember(progressData) {
		progressData.map { progress ->
			Instant.ofEpochMilli(progress.startTime)
				.atZone(zone)
				.toLocalDate()
				.format(dateFormatter)
		}
	}

	LaunchedEffect(progressData, metric, weightUnit) {
		val values = progressData.map { progress ->
			when (metric) {
				ChartMetric.MAX_WEIGHT -> weightUnit.convertFromKg(progress.maxWeight)
				ChartMetric.TOTAL_VOLUME -> weightUnit.convertFromKg(progress.totalVolume)
				ChartMetric.MAX_REPS -> progress.maxReps.toDouble()
				ChartMetric.SET_COUNT -> progress.setCount.toDouble()
			}
		}

		if (values.isNotEmpty()) {
			modelProducer.runTransaction {
				lineSeries { series(values) }
			}
		}
	}

	CartesianChartHost(
		chart = rememberCartesianChart(
			rememberLineCartesianLayer(),
			startAxis = VerticalAxis.rememberStart(),
			bottomAxis = HorizontalAxis.rememberBottom(
				valueFormatter = { _, value, _ ->
					val index = value.toInt()
					if (index in dateLabels.indices) dateLabels[index] else ""
				}
			)
		),
		modelProducer = modelProducer,
		modifier = modifier
			.fillMaxWidth()
			.height(250.dp)
	)
}
