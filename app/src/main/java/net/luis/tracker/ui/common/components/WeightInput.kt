package net.luis.tracker.ui.common.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import net.luis.tracker.domain.model.WeightUnit

@Composable
fun WeightInput(
	weightKg: Double,
	onWeightChange: (Double) -> Unit,
	weightUnit: WeightUnit,
	modifier: Modifier = Modifier
) {
	val displayValue = weightUnit.convertFromKg(weightKg)
	val stepKg = 0.5
	val displayText = if (displayValue == displayValue.toLong().toDouble()) {
		displayValue.toLong().toString()
	} else {
		"%.1f".format(displayValue)
	}

	Row(
		verticalAlignment = Alignment.CenterVertically,
		modifier = modifier
	) {
		IconButton(onClick = {
			val newWeight = (weightKg - stepKg).coerceAtLeast(0.0)
			onWeightChange(newWeight)
		}) {
			Icon(Icons.Default.Remove, contentDescription = null)
		}
		OutlinedTextField(
			value = displayText,
			onValueChange = { text ->
				val parsed = text.toDoubleOrNull()
				if (parsed != null && parsed >= 0) {
					onWeightChange(weightUnit.convertToKg(parsed))
				}
			},
			modifier = Modifier.width(100.dp),
			suffix = { Text(weightUnit.name.lowercase()) },
			singleLine = true,
			keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
		)
		IconButton(onClick = {
			onWeightChange(weightKg + stepKg)
		}) {
			Icon(Icons.Default.Add, contentDescription = null)
		}
	}
}
