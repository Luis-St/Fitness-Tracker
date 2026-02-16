package net.luis.tracker.ui.overview.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarView(
	yearMonth: YearMonth,
	workoutDays: Set<Int>,
	onMonthChange: (YearMonth) -> Unit,
	onDayClick: (Int) -> Unit = {},
	modifier: Modifier = Modifier
) {
	Column(modifier = modifier.fillMaxWidth()) {
		// Header with month navigation
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 8.dp),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically
		) {
			IconButton(onClick = { onMonthChange(yearMonth.minusMonths(1)) }) {
				Icon(
					imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
					contentDescription = "Previous month"
				)
			}
			Text(
				text = "${yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${yearMonth.year}",
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.Bold
			)
			IconButton(onClick = { onMonthChange(yearMonth.plusMonths(1)) }) {
				Icon(
					imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
					contentDescription = "Next month"
				)
			}
		}

		// Day of week headers (Monday to Sunday)
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 4.dp)
		) {
			val daysOfWeek = listOf(
				DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
				DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
				DayOfWeek.SUNDAY
			)
			for (day in daysOfWeek) {
				Text(
					text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
					modifier = Modifier.weight(1f),
					textAlign = TextAlign.Center,
					style = MaterialTheme.typography.bodySmall,
					fontWeight = FontWeight.Medium,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
		}

		// Calendar grid
		val firstDayOfMonth = yearMonth.atDay(1)
		// Offset so Monday = 0, Sunday = 6
		val startOffset = (firstDayOfMonth.dayOfWeek.value - 1)
		val daysInMonth = yearMonth.lengthOfMonth()
		val totalCells = startOffset + daysInMonth
		val rows = (totalCells + 6) / 7

		val today = LocalDate.now()
		val todayDay = if (yearMonth.year == today.year && yearMonth.monthValue == today.monthValue) {
			today.dayOfMonth
		} else {
			-1
		}

		for (row in 0 until rows) {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 4.dp)
			) {
				for (col in 0 until 7) {
					val cellIndex = row * 7 + col
					val dayNumber = cellIndex - startOffset + 1

					Box(
						modifier = Modifier
							.weight(1f)
							.aspectRatio(1f)
							.padding(2.dp),
						contentAlignment = Alignment.Center
					) {
						if (dayNumber in 1..daysInMonth) {
							val isWorkoutDay = workoutDays.contains(dayNumber)
							val isToday = dayNumber == todayDay
							Box(
								modifier = Modifier
									.matchParentSize()
									.clip(CircleShape)
									.then(
										if (isWorkoutDay) {
											Modifier
												.background(MaterialTheme.colorScheme.primary)
												.clickable { onDayClick(dayNumber) }
										} else {
											Modifier
										}
									)
									.then(
										if (isToday) {
											Modifier.border(
												width = 2.dp,
												color = if (isWorkoutDay) {
													MaterialTheme.colorScheme.onPrimary
												} else {
													MaterialTheme.colorScheme.primary
												},
												shape = CircleShape
											)
										} else {
											Modifier
										}
									),
								contentAlignment = Alignment.Center
							) {
								Text(
									text = dayNumber.toString(),
									style = MaterialTheme.typography.bodySmall,
									color = if (isWorkoutDay) {
										MaterialTheme.colorScheme.onPrimary
									} else {
										MaterialTheme.colorScheme.onSurface
									},
									textAlign = TextAlign.Center
								)
							}
						}
					}
				}
			}
		}
	}
}
