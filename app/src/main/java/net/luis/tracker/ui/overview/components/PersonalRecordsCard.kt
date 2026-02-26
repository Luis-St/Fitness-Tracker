package net.luis.tracker.ui.overview.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import net.luis.tracker.R
import net.luis.tracker.data.local.dao.PersonalRecord
import net.luis.tracker.domain.model.WeightUnit

@Composable
fun PersonalRecordsCard(
	personalRecords: List<PersonalRecord>,
	weightUnit: WeightUnit,
	onClick: () -> Unit = {},
	modifier: Modifier = Modifier
) {
	if (personalRecords.isEmpty()) return

	Card(
		onClick = onClick,
		modifier = modifier.fillMaxWidth(),
		elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
	) {
		Column(
			modifier = Modifier.padding(16.dp)
		) {
			Text(
				text = stringResource(R.string.personal_records),
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.Bold
			)
			Spacer(modifier = Modifier.height(12.dp))

			val displayRecords = personalRecords.take(5)
			displayRecords.forEachIndexed { index, record ->
				PersonalRecordRow(record = record, weightUnit = weightUnit)
				if (index < displayRecords.lastIndex) {
					HorizontalDivider(
						modifier = Modifier.padding(vertical = 8.dp),
						color = MaterialTheme.colorScheme.outlineVariant
					)
				}
			}
		}
	}
}

@Composable
fun PersonalRecordRow(
	record: PersonalRecord,
	weightUnit: WeightUnit,
	modifier: Modifier = Modifier
) {
	Column(modifier = modifier.fillMaxWidth()) {
		Text(
			text = record.exerciseTitle,
			style = MaterialTheme.typography.bodyLarge,
			fontWeight = FontWeight.Medium
		)
		Spacer(modifier = Modifier.height(4.dp))
		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically
		) {
			if (record.maxWeight > 0) {
				StatChip(
					label = stringResource(R.string.heaviest_weight),
					value = weightUnit.formatWeight(record.maxWeight),
					modifier = Modifier.weight(1f)
				)
			}
			if (record.maxReps > 0) {
				StatChip(
					label = stringResource(R.string.most_reps),
					value = record.maxReps.toString(),
					modifier = Modifier.weight(1f)
				)
			}
			if (record.maxVolume > 0) {
				StatChip(
					label = stringResource(R.string.highest_volume),
					value = weightUnit.formatWeight(record.maxVolume),
					modifier = Modifier.weight(1f)
				)
			}
		}
	}
}

@Composable
internal fun StatChip(
	label: String,
	value: String,
	modifier: Modifier = Modifier
) {
	Column(
		modifier = modifier,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Text(
			text = value,
			style = MaterialTheme.typography.bodyMedium,
			fontWeight = FontWeight.Bold,
			color = MaterialTheme.colorScheme.primary
		)
		Text(
			text = label,
			style = MaterialTheme.typography.labelSmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			textAlign = TextAlign.Center
		)
	}
}
