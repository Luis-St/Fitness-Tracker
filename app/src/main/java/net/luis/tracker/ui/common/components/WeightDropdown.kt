package net.luis.tracker.ui.common.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import net.luis.tracker.R
import net.luis.tracker.domain.model.WeightUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightDropdown(
	text: String,
	onTextChange: (String) -> Unit,
	weightUnit: WeightUnit,
	preferredWeightsKg: List<Double>,
	modifier: Modifier = Modifier,
	isError: Boolean = false,
	label: String? = null
) {
	if (preferredWeightsKg.isEmpty()) {
		WeightInput(text, onTextChange, weightUnit, modifier, isError, label)
		return
	}

	var expanded by remember { mutableStateOf(false) }
	var isCustom by remember { mutableStateOf(false) }
	var hasFocused by remember { mutableStateOf(false) }
	val focusRequester = remember { FocusRequester() }

	if (isCustom) {
		WeightInput(
			text = text,
			onTextChange = onTextChange,
			weightUnit = weightUnit,
			modifier = modifier
				.focusRequester(focusRequester)
				.onFocusChanged {
					if (it.isFocused) {
						hasFocused = true
					} else if (hasFocused && text.isEmpty()) {
						isCustom = false
						hasFocused = false
					}
				},
			isError = isError,
			label = label
		)
		LaunchedEffect(Unit) {
			focusRequester.requestFocus()
		}
		return
	}

	val sortedWeightsKg = remember(preferredWeightsKg) { preferredWeightsKg.sorted() }

	ExposedDropdownMenuBox(
		expanded = expanded,
		onExpandedChange = { expanded = it },
		modifier = modifier
	) {
		OutlinedTextField(
			value = text,
			onValueChange = {},
			readOnly = true,
			label = { Text(label ?: stringResource(R.string.weight), maxLines = 1, overflow = TextOverflow.Ellipsis) },
			suffix = { if (text.isNotEmpty()) Text(weightUnit.name.lowercase()) },
			trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
			isError = isError,
			singleLine = true,
			modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
		)
		ExposedDropdownMenu(
			expanded = expanded,
			onDismissRequest = { expanded = false }
		) {
			sortedWeightsKg.forEach { weightKg ->
				val converted = weightUnit.convertFromKg(weightKg)
				val numberText = if (converted == converted.toLong().toDouble()) {
					converted.toLong().toString()
				} else {
					"%.1f".format(converted)
				}
				DropdownMenuItem(
					text = { Text("$numberText ${weightUnit.name.lowercase()}") },
					onClick = {
						onTextChange(numberText)
						expanded = false
					},
					contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
				)
			}
			DropdownMenuItem(
				text = { Text(stringResource(R.string.custom_weight)) },
				onClick = {
					onTextChange("")
					isCustom = true
					expanded = false
				},
				contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
			)
		}
	}
}
