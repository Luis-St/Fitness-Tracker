package net.luis.tracker.ui.overview.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import net.luis.tracker.R

@Composable
fun StreakCard(
	currentStreak: Int,
	modifier: Modifier = Modifier
) {
	Card(
		modifier = modifier.fillMaxWidth(),
		elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Text(
				text = stringResource(R.string.current_streak),
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.Bold
			)
			Spacer(modifier = Modifier.height(12.dp))
			val weeksLabel = stringResource(R.string.weeks)
			Text(
				text = buildAnnotatedString {
					withStyle(
						SpanStyle(
							fontWeight = FontWeight.Bold,
							color = MaterialTheme.colorScheme.primary
						)
					) {
						append(currentStreak.toString())
					}
					withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
						append(" $weeksLabel")
					}
				},
				style = MaterialTheme.typography.headlineSmall
			)
		}
	}
}
