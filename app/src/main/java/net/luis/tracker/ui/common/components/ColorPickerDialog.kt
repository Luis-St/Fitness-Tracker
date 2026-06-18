package net.luis.tracker.ui.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.luis.tracker.R
import kotlin.math.roundToInt

private val presetColors = listOf(
	Color(0xFF43A047), // green
	Color(0xFF7CB342), // light green
	Color(0xFFF9A825), // amber
	Color(0xFFFB8C00), // orange
	Color(0xFFE53935), // red
	Color(0xFF8E24AA), // purple
	Color(0xFF1E88E5), // blue
	Color(0xFF00ACC1), // cyan
	Color(0xFF6D4C41), // brown
	Color(0xFF546E7A)  // blue grey
)

/**
 * A small color picker presented as a dialog: a live preview, a row of preset swatches for quick
 * selection, and RGB sliders for full customization.
 */
@Composable
fun ColorPickerDialog(
	title: String,
	initialColor: Color,
	onConfirm: (Color) -> Unit,
	onDismiss: () -> Unit
) {
	var red by remember { mutableFloatStateOf(initialColor.red) }
	var green by remember { mutableFloatStateOf(initialColor.green) }
	var blue by remember { mutableFloatStateOf(initialColor.blue) }

	val current = Color(red, green, blue)

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(title) },
		text = {
			Column {
				Spacer(
					modifier = Modifier
						.fillMaxWidth()
						.height(48.dp)
						.background(current, RoundedCornerShape(8.dp))
						.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
				)
				Spacer(modifier = Modifier.height(16.dp))
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.horizontalScroll(rememberScrollState()),
					horizontalArrangement = Arrangement.spacedBy(8.dp)
				) {
					presetColors.forEach { preset ->
						val selected = preset.red == red && preset.green == green && preset.blue == blue
						Spacer(
							modifier = Modifier
								.size(32.dp)
								.background(preset, CircleShape)
								.border(
									width = if (selected) 3.dp else 1.dp,
									color = if (selected) {
										MaterialTheme.colorScheme.primary
									} else {
										MaterialTheme.colorScheme.outline
									},
									shape = CircleShape
								)
								.clickable {
									red = preset.red
									green = preset.green
									blue = preset.blue
								}
						)
					}
				}
				Spacer(modifier = Modifier.height(16.dp))
				ColorChannelSlider("R", red, Color(0xFFE53935)) { red = it }
				ColorChannelSlider("G", green, Color(0xFF43A047)) { green = it }
				ColorChannelSlider("B", blue, Color(0xFF1E88E5)) { blue = it }
			}
		},
		confirmButton = {
			Button(onClick = { onConfirm(current) }) {
				Text(stringResource(R.string.save))
			}
		},
		dismissButton = {
			TextButton(onClick = onDismiss) {
				Text(stringResource(R.string.cancel))
			}
		}
	)
}

@Composable
private fun ColorChannelSlider(
	label: String,
	value: Float,
	thumbColor: Color,
	onValueChange: (Float) -> Unit
) {
	Row(verticalAlignment = Alignment.CenterVertically) {
		Text(
			text = label,
			style = MaterialTheme.typography.bodyMedium,
			modifier = Modifier.width(20.dp)
		)
		Slider(
			value = value,
			onValueChange = onValueChange,
			valueRange = 0f..1f,
			colors = SliderDefaults.colors(
				thumbColor = thumbColor,
				activeTrackColor = thumbColor
			),
			modifier = Modifier.weight(1f)
		)
		Text(
			text = (value * 255).roundToInt().toString(),
			style = MaterialTheme.typography.bodyMedium,
			textAlign = TextAlign.End,
			modifier = Modifier.width(36.dp)
		)
	}
}
