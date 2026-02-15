package net.luis.tracker.ui.activeworkout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.luis.tracker.R

@Composable
fun RestTimerScreen(
	durationSeconds: Int,
	onFinished: () -> Unit,
	onSkip: () -> Unit
) {
	var progress by remember { mutableFloatStateOf(1f) }
	var remainingSeconds by remember { mutableIntStateOf(durationSeconds) }

	LaunchedEffect(Unit) {
		val startNanos = withFrameNanos { it }
		val totalNanos = durationSeconds * 1_000_000_000L
		while (true) {
			val elapsed = withFrameNanos { it } - startNanos
			val fraction = 1f - (elapsed.toFloat() / totalNanos)
			if (fraction <= 0f) {
				progress = 0f
				remainingSeconds = 0
				break
			}
			progress = fraction
			val newSeconds = (fraction * durationSeconds).let { raw ->
				if (raw > 0f && raw < durationSeconds.toFloat()) raw.toInt() + 1 else raw.toInt()
			}
			remainingSeconds = newSeconds
		}
		onFinished()
	}

	val minutes = remainingSeconds / 60
	val seconds = remainingSeconds % 60

	val primaryColor = MaterialTheme.colorScheme.primary
	val trackColor = MaterialTheme.colorScheme.surfaceVariant

	Column(
		modifier = Modifier.fillMaxSize(),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		Box(
			contentAlignment = Alignment.Center,
			modifier = Modifier
				.size(280.dp)
				.drawBehind {
					val strokeWidth = 12.dp.toPx()
					val arcRect = Rect(
						strokeWidth / 2,
						strokeWidth / 2,
						size.width - strokeWidth / 2,
						size.height - strokeWidth / 2
					)
					drawArc(
						color = trackColor,
						startAngle = 0f,
						sweepAngle = 360f,
						useCenter = false,
						topLeft = arcRect.topLeft,
						size = arcRect.size,
						style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
					)
					val sweep = progress * 360f
					drawArc(
						color = primaryColor,
						startAngle = -90f,
						sweepAngle = sweep,
						useCenter = false,
						topLeft = arcRect.topLeft,
						size = arcRect.size,
						style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
					)
				}
		) {
			Text(
				text = stringResource(R.string.rest_timer_format, minutes, seconds),
				style = MaterialTheme.typography.displayLarge
			)
		}

		Spacer(modifier = Modifier.height(32.dp))

		OutlinedButton(onClick = onSkip) {
			Text(stringResource(R.string.skip))
		}
	}
}
