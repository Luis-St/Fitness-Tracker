package net.luis.tracker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.luis.tracker.FitnessTrackerApp
import net.luis.tracker.R
import net.luis.tracker.data.repository.SettingsRepository
import net.luis.tracker.domain.model.WeightUnit
import net.luis.tracker.util.isAcceptableWeightInput
import net.luis.tracker.util.toWeightDoubleOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitsSettingsScreen(
	app: FitnessTrackerApp,
	onNavigateBack: () -> Unit
) {
	val factory = remember {
		SettingsViewModel.Factory(settingsRepository = SettingsRepository(app))
	}
	val viewModel: SettingsViewModel = viewModel(factory = factory)
	val uiState by viewModel.uiState.collectAsState()

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.units)) },
				navigationIcon = {
					IconButton(onClick = onNavigateBack) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = stringResource(R.string.back)
						)
					}
				}
			)
		}
	) { innerPadding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(innerPadding)
				.verticalScroll(rememberScrollState())
				.padding(horizontal = 16.dp)
		) {
			Spacer(modifier = Modifier.height(8.dp))

			Text(
				text = stringResource(R.string.weight_unit),
				style = MaterialTheme.typography.bodyLarge
			)
			Spacer(modifier = Modifier.height(8.dp))

			val weightUnits = WeightUnit.entries
			val weightLabels = weightUnits.map { it.name.lowercase() }
			SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
				weightUnits.forEachIndexed { index, unit ->
					SegmentedButton(
						selected = uiState.weightUnit == unit,
						onClick = { viewModel.setWeightUnit(unit) },
						shape = SegmentedButtonDefaults.itemShape(
							index = index,
							count = weightUnits.size
						)
					) {
						Text(weightLabels[index])
					}
				}
			}

			Spacer(modifier = Modifier.height(16.dp))

			Text(
				text = stringResource(R.string.preferred_weights),
				style = MaterialTheme.typography.bodyLarge
			)
			Spacer(modifier = Modifier.height(8.dp))

			var showAddWeightField by remember { mutableStateOf(false) }
			var addWeightText by remember { mutableStateOf("") }

			val sortedWeights = uiState.preferredWeightsKg.sortedBy { it }

			if (sortedWeights.isEmpty() && !showAddWeightField) {
				Text(
					text = stringResource(R.string.no_preferred_weights),
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					modifier = Modifier.padding(bottom = 8.dp)
				)
			}

			sortedWeights.forEach { weightKg ->
				Row(
					modifier = Modifier.fillMaxWidth(),
					verticalAlignment = Alignment.CenterVertically
				) {
					Text(
						text = uiState.weightUnit.formatWeight(weightKg),
						style = MaterialTheme.typography.bodyMedium,
						modifier = Modifier.weight(1f)
					)
					IconButton(onClick = {
						viewModel.setPreferredWeightsKg(uiState.preferredWeightsKg - weightKg)
					}) {
						Icon(
							imageVector = Icons.Default.Delete,
							contentDescription = stringResource(R.string.remove),
							tint = MaterialTheme.colorScheme.error
						)
					}
				}
				HorizontalDivider()
			}

			if (showAddWeightField) {
				Spacer(modifier = Modifier.height(8.dp))
				Row(
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.spacedBy(8.dp)
				) {
					val confirmAdd = {
						val value = addWeightText.toWeightDoubleOrNull()
						if (value != null && value > 0) {
							val weightKg = uiState.weightUnit.convertToKg(value)
							if (uiState.preferredWeightsKg.none { kotlin.math.abs(it - weightKg) < 0.001 }) {
								viewModel.setPreferredWeightsKg(uiState.preferredWeightsKg + weightKg)
							}
							addWeightText = ""
							showAddWeightField = false
						}
					}
					OutlinedTextField(
						value = addWeightText,
						onValueChange = { input ->
							if (isAcceptableWeightInput(input) && (input.toWeightDoubleOrNull()?.let { it > 0 } != false)) {
								addWeightText = input
							}
						},
						label = { Text(stringResource(R.string.weight)) },
						suffix = { Text(uiState.weightUnit.name.lowercase()) },
						singleLine = true,
						keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
						keyboardActions = KeyboardActions(onDone = { confirmAdd() }),
						modifier = Modifier.weight(1f)
					)
					IconButton(onClick = { confirmAdd() }) {
						Icon(
							imageVector = Icons.Default.Check,
							contentDescription = stringResource(R.string.save)
						)
					}
					IconButton(onClick = {
						addWeightText = ""
						showAddWeightField = false
					}) {
						Icon(
							imageVector = Icons.Default.Close,
							contentDescription = stringResource(R.string.cancel)
						)
					}
				}
			} else {
				Spacer(modifier = Modifier.height(8.dp))
				FilledTonalButton(
					onClick = { showAddWeightField = true },
					modifier = Modifier.fillMaxWidth()
				) {
					Icon(
						imageVector = Icons.Default.Add,
						contentDescription = null,
						modifier = Modifier.padding(end = 8.dp)
					)
					Text(stringResource(R.string.add_weight))
				}
			}

			Spacer(modifier = Modifier.height(24.dp))
		}
	}
}
