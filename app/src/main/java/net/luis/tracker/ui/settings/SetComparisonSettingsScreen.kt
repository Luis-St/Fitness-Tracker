package net.luis.tracker.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.luis.tracker.FitnessTrackerApp
import net.luis.tracker.R
import net.luis.tracker.data.repository.SettingsRepository
import net.luis.tracker.ui.common.components.ColorPickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetComparisonSettingsScreen(
	app: FitnessTrackerApp,
	onNavigateBack: () -> Unit
) {
	val factory = remember {
		SettingsViewModel.Factory(settingsRepository = SettingsRepository(app))
	}
	val viewModel: SettingsViewModel = viewModel(factory = factory)
	val uiState by viewModel.uiState.collectAsState()

	var colorPickerRole by remember { mutableStateOf<SetComparisonColorRole?>(null) }
	colorPickerRole?.let { role ->
		val comparison = uiState.setComparison
		val initial = when (role) {
			SetComparisonColorRole.BETTER -> comparison.betterColor
			SetComparisonColorRole.SINGLE_DROP -> comparison.singleDropColor
			SetComparisonColorRole.WORSE -> comparison.worseColor
			SetComparisonColorRole.NEUTRAL -> comparison.neutralColor
		}
		ColorPickerDialog(
			title = stringResource(role.labelRes),
			initialColor = Color(initial),
			onConfirm = { chosen ->
				val argb = chosen.toArgb()
				viewModel.setSetComparisonColors(
					betterColor = if (role == SetComparisonColorRole.BETTER) argb else comparison.betterColor,
					singleDropColor = if (role == SetComparisonColorRole.SINGLE_DROP) argb else comparison.singleDropColor,
					worseColor = if (role == SetComparisonColorRole.WORSE) argb else comparison.worseColor,
					neutralColor = if (role == SetComparisonColorRole.NEUTRAL) argb else comparison.neutralColor
				)
				colorPickerRole = null
			},
			onDismiss = { colorPickerRole = null }
		)
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.set_comparison_title)) },
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

			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically
			) {
				Column(modifier = Modifier.weight(1f)) {
					Text(
						text = stringResource(R.string.set_comparison_enable),
						style = MaterialTheme.typography.bodyLarge
					)
					Text(
						text = stringResource(R.string.set_comparison_desc),
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
				Switch(
					checked = uiState.setComparison.enabled,
					onCheckedChange = { viewModel.setSetComparisonEnabled(it) }
				)
			}

			if (uiState.setComparison.enabled) {
				Spacer(modifier = Modifier.height(8.dp))
				SetComparisonColorRow(
					label = stringResource(R.string.set_comparison_better),
					color = Color(uiState.setComparison.betterColor),
					onClick = { colorPickerRole = SetComparisonColorRole.BETTER }
				)
				SetComparisonColorRow(
					label = stringResource(R.string.set_comparison_single_drop),
					color = Color(uiState.setComparison.singleDropColor),
					onClick = { colorPickerRole = SetComparisonColorRole.SINGLE_DROP }
				)
				SetComparisonColorRow(
					label = stringResource(R.string.set_comparison_worse),
					color = Color(uiState.setComparison.worseColor),
					onClick = { colorPickerRole = SetComparisonColorRole.WORSE }
				)
				SetComparisonColorRow(
					label = stringResource(R.string.set_comparison_neutral),
					color = Color(uiState.setComparison.neutralColor),
					onClick = { colorPickerRole = SetComparisonColorRole.NEUTRAL }
				)

				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(top = 8.dp),
					horizontalArrangement = Arrangement.SpaceBetween,
					verticalAlignment = Alignment.CenterVertically
				) {
					Column(modifier = Modifier.weight(1f)) {
						Text(
							text = stringResource(R.string.set_comparison_brighten_dark),
							style = MaterialTheme.typography.bodyLarge
						)
						Text(
							text = stringResource(R.string.set_comparison_brighten_dark_desc),
							style = MaterialTheme.typography.bodySmall,
							color = MaterialTheme.colorScheme.onSurfaceVariant
						)
					}
					Switch(
						checked = uiState.setComparison.brightenInDark,
						onCheckedChange = { viewModel.setSetComparisonBrightenDark(it) }
					)
				}
			}

			Spacer(modifier = Modifier.height(24.dp))
		}
	}
}

private enum class SetComparisonColorRole(val labelRes: Int) {
	BETTER(R.string.set_comparison_better),
	SINGLE_DROP(R.string.set_comparison_single_drop),
	WORSE(R.string.set_comparison_worse),
	NEUTRAL(R.string.set_comparison_neutral)
}

@Composable
private fun SetComparisonColorRow(
	label: String,
	color: Color,
	onClick: () -> Unit
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clickable(onClick = onClick)
			.padding(vertical = 8.dp),
		horizontalArrangement = Arrangement.SpaceBetween,
		verticalAlignment = Alignment.CenterVertically
	) {
		Text(
			text = label,
			style = MaterialTheme.typography.bodyLarge,
			modifier = Modifier.weight(1f)
		)
		Spacer(
			modifier = Modifier
				.size(28.dp)
				.background(color, CircleShape)
				.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
		)
	}
}
