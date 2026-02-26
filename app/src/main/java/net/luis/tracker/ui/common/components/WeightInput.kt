package net.luis.tracker.ui.common.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import net.luis.tracker.R
import net.luis.tracker.domain.model.WeightUnit

@Composable
fun WeightInput(
	text: String,
	onTextChange: (String) -> Unit,
	weightUnit: WeightUnit,
	modifier: Modifier = Modifier,
	isError: Boolean = false
) {
	OutlinedTextField(
		value = text,
		onValueChange = { input ->
			if (input.isEmpty() || input.toDoubleOrNull()?.let { it >= 0 } == true) {
				onTextChange(input)
			}
		},
		modifier = modifier,
		label = { Text(stringResource(R.string.weight)) },
		suffix = { Text(weightUnit.name.lowercase()) },
		singleLine = true,
		isError = isError,
		keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
	)
}
