package net.luis.tracker.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun SectionHeader(text: String) {
	Text(
		text = text,
		style = MaterialTheme.typography.titleSmall,
		color = MaterialTheme.colorScheme.primary,
		modifier = Modifier.padding(bottom = 12.dp)
	)
}

/** A clickable settings row that navigates to a sub-page: title + description + chevron. */
@Composable
internal fun SettingsNavigationRow(
	title: String,
	description: String,
	onClick: () -> Unit
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clickable(onClick = onClick)
			.padding(vertical = 16.dp),
		horizontalArrangement = Arrangement.SpaceBetween,
		verticalAlignment = Alignment.CenterVertically
	) {
		Column(modifier = Modifier.weight(1f)) {
			Text(
				text = title,
				style = MaterialTheme.typography.bodyLarge
			)
			Text(
				text = description,
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
		Icon(
			imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
			contentDescription = null,
			tint = MaterialTheme.colorScheme.onSurfaceVariant
		)
	}
}
