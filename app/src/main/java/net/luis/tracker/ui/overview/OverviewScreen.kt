package net.luis.tracker.ui.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.luis.tracker.FitnessTrackerApp
import net.luis.tracker.R
import net.luis.tracker.data.repository.ExerciseRepository
import net.luis.tracker.data.repository.StatsRepository
import net.luis.tracker.domain.model.ChartMetric
import net.luis.tracker.domain.model.WeightUnit
import net.luis.tracker.ui.overview.components.CalendarView
import net.luis.tracker.ui.overview.components.ProgressChart
import net.luis.tracker.ui.overview.components.StatsSummaryCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
	app: FitnessTrackerApp,
	weightUnit: WeightUnit,
	onOpenSettings: () -> Unit = {}
) {
	val factory = remember {
		OverviewViewModel.Factory(
			statsRepository = StatsRepository(app.database.statsDao()),
			exerciseRepository = ExerciseRepository(app.database.exerciseDao())
		)
	}
	val viewModel: OverviewViewModel = viewModel(factory = factory)

	val uiState by viewModel.uiState.collectAsState()

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.nav_overview)) },
				actions = {
					IconButton(onClick = onOpenSettings) {
						Icon(
							imageVector = Icons.Default.Settings,
							contentDescription = stringResource(R.string.settings)
						)
					}
				}
			)
		}
	) { innerPadding ->
		if (uiState.isLoading) {
			Box(
				modifier = Modifier
					.fillMaxSize()
					.padding(innerPadding),
				contentAlignment = Alignment.Center
			) {
				CircularProgressIndicator()
			}
		} else {
			Column(
				modifier = Modifier
					.fillMaxSize()
					.padding(innerPadding)
					.verticalScroll(rememberScrollState())
					.padding(horizontal = 16.dp)
			) {
				Spacer(modifier = Modifier.height(8.dp))

				// Calendar
				CalendarView(
					yearMonth = uiState.currentMonth,
					workoutDays = uiState.workoutDays,
					onMonthChange = { viewModel.changeMonth(it) }
				)

				Spacer(modifier = Modifier.height(16.dp))

				// Stats summary
				StatsSummaryCard(
					workoutsThisWeek = uiState.workoutsThisWeek,
					workoutsThisMonth = uiState.workoutsThisMonth,
					currentStreak = uiState.currentStreak,
					averageDuration = uiState.averageDuration,
					weightUnit = weightUnit
				)

				Spacer(modifier = Modifier.height(24.dp))

				// Progress section title
				Text(
					text = stringResource(R.string.progress),
					style = MaterialTheme.typography.titleMedium
				)

				Spacer(modifier = Modifier.height(8.dp))

				// Metric selector chips
				LazyRow(
					contentPadding = PaddingValues(vertical = 4.dp),
					horizontalArrangement = Arrangement.spacedBy(8.dp)
				) {
					val metrics = ChartMetric.entries
					items(metrics.size) { index ->
						val metric = metrics[index]
						FilterChip(
							selected = uiState.selectedMetric == metric,
							onClick = { viewModel.selectMetric(metric) },
							label = {
								Text(
									text = when (metric) {
										ChartMetric.MAX_WEIGHT -> stringResource(R.string.metric_max_weight)
										ChartMetric.TOTAL_VOLUME -> stringResource(R.string.metric_total_volume)
										ChartMetric.MAX_REPS -> stringResource(R.string.metric_max_reps)
										ChartMetric.SET_COUNT -> stringResource(R.string.metric_set_count)
									}
								)
							}
						)
					}
				}

				Spacer(modifier = Modifier.height(8.dp))

				// Exercise selector dropdown
				var exerciseDropdownExpanded by remember { mutableStateOf(false) }
				val selectedExerciseName = if (uiState.selectedExerciseId == null) {
					stringResource(R.string.all_exercises)
				} else {
					uiState.exercises.find { it.id == uiState.selectedExerciseId }?.title
						?: stringResource(R.string.all_exercises)
				}

				ExposedDropdownMenuBox(
					expanded = exerciseDropdownExpanded,
					onExpandedChange = { exerciseDropdownExpanded = it },
					modifier = Modifier.fillMaxWidth()
				) {
					OutlinedTextField(
						value = selectedExerciseName,
						onValueChange = {},
						readOnly = true,
						label = { Text(stringResource(R.string.select_exercise)) },
						trailingIcon = {
							ExposedDropdownMenuDefaults.TrailingIcon(expanded = exerciseDropdownExpanded)
						},
						colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
						modifier = Modifier
							.fillMaxWidth()
							.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
					)
					ExposedDropdownMenu(
						expanded = exerciseDropdownExpanded,
						onDismissRequest = { exerciseDropdownExpanded = false }
					) {
						DropdownMenuItem(
							text = { Text(stringResource(R.string.all_exercises)) },
							onClick = {
								viewModel.selectExercise(null)
								exerciseDropdownExpanded = false
							}
						)
						uiState.exercises.forEach { exercise ->
							DropdownMenuItem(
								text = { Text(exercise.title) },
								onClick = {
									viewModel.selectExercise(exercise.id)
									exerciseDropdownExpanded = false
								}
							)
						}
					}
				}

				Spacer(modifier = Modifier.height(16.dp))

				// Progress chart
				ProgressChart(
					progressData = uiState.progressData,
					metric = uiState.selectedMetric,
					weightUnit = weightUnit
				)

				Spacer(modifier = Modifier.height(16.dp))
			}
		}
	}
}
