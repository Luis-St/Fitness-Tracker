package net.luis.tracker.ui.overview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.luis.tracker.FitnessTrackerApp
import net.luis.tracker.R
import net.luis.tracker.data.repository.ExerciseRepository
import net.luis.tracker.data.repository.SettingsRepository
import net.luis.tracker.data.repository.StatsRepository
import net.luis.tracker.domain.model.ChartMetric
import net.luis.tracker.domain.model.OverviewSection
import net.luis.tracker.domain.model.WeightUnit
import net.luis.tracker.ui.overview.components.CalendarView
import net.luis.tracker.ui.overview.components.CategoryBreakdownChart
import net.luis.tracker.ui.overview.components.LifetimeStatsSummaryCard
import net.luis.tracker.ui.overview.components.MonthStatsSummaryCard
import net.luis.tracker.ui.overview.components.PersonalRecordsCard
import net.luis.tracker.ui.overview.components.ProgressChart
import net.luis.tracker.ui.overview.components.StreakCard
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
	app: FitnessTrackerApp,
	weightUnit: WeightUnit,
	weeklyWorkoutGoal: Int = 2,
	onOpenSettings: () -> Unit = {},
	onNavigateToWorkout: (Long) -> Unit = {},
	onViewAllRecords: () -> Unit = {}
) {
	val factory = remember(weeklyWorkoutGoal) {
		OverviewViewModel.Factory(
			statsRepository = StatsRepository(app.database.statsDao()),
			exerciseRepository = ExerciseRepository(app.database.exerciseDao()),
			settingsRepository = SettingsRepository(app),
			weeklyWorkoutGoal = weeklyWorkoutGoal
		)
	}
	val viewModel: OverviewViewModel = viewModel(factory = factory)

	val uiState by viewModel.uiState.collectAsState()

	// Handle navigation events
	LaunchedEffect(uiState.navigateToWorkoutId) {
		uiState.navigateToWorkoutId?.let { workoutId ->
			onNavigateToWorkout(workoutId)
			viewModel.onNavigationHandled()
		}
	}

	// Bottom sheet for multiple workouts on same day
	if (uiState.showWorkoutPicker) {
		val sheetState = rememberModalBottomSheetState()
		val dateFormatter = remember { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT) }
		val zone = remember { ZoneId.systemDefault() }

		ModalBottomSheet(
			onDismissRequest = { viewModel.dismissWorkoutPicker() },
			sheetState = sheetState
		) {
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 16.dp)
					.padding(bottom = 32.dp)
			) {
				Text(
					text = if (uiState.selectedDayWorkouts.isNotEmpty()) {
						val firstTime = uiState.selectedDayWorkouts.first().startTime
						val date = Instant.ofEpochMilli(firstTime).atZone(zone).toLocalDate()
						stringResource(R.string.workouts_on_day, date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
					} else {
						stringResource(R.string.nav_workouts)
					},
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.Bold
				)
				Spacer(modifier = Modifier.height(12.dp))

				uiState.selectedDayWorkouts.forEach { workoutInfo ->
					val time = Instant.ofEpochMilli(workoutInfo.startTime)
						.atZone(zone)
						.toLocalDateTime()
						.format(dateFormatter)

					Row(
						modifier = Modifier
							.fillMaxWidth()
							.clickable {
								viewModel.dismissWorkoutPicker()
								onNavigateToWorkout(workoutInfo.workoutId)
							}
							.padding(vertical = 12.dp),
						horizontalArrangement = Arrangement.SpaceBetween,
						verticalAlignment = Alignment.CenterVertically
					) {
						Text(
							text = time,
							style = MaterialTheme.typography.bodyLarge
						)
					}
				}
			}
		}
	}

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
		},
		contentWindowInsets = WindowInsets(0)
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
			) {
				// Tab selector (above the calendar)
				val tabs = OverviewTab.entries
				PrimaryTabRow(selectedTabIndex = tabs.indexOf(uiState.selectedTab)) {
					tabs.forEach { tab ->
						Tab(
							selected = uiState.selectedTab == tab,
							onClick = { viewModel.selectTab(tab) },
							text = {
								Text(
									text = when (tab) {
										OverviewTab.MONTH -> stringResource(R.string.overview_tab_month)
										OverviewTab.LIFETIME -> stringResource(R.string.overview_tab_lifetime)
									}
								)
							}
						)
					}
				}

				Column(modifier = Modifier.padding(horizontal = 16.dp)) {
					Spacer(modifier = Modifier.height(8.dp))

					// Calendar with clickable days (always visible, below the tabs)
					CalendarView(
						yearMonth = uiState.currentMonth,
						workoutDays = uiState.workoutDays,
						onMonthChange = { viewModel.changeMonth(it) },
						onDayClick = { viewModel.onDayClick(it) }
					)

					Spacer(modifier = Modifier.height(16.dp))

					// Configurable, tab-scoped sections (order & visibility set in Settings)
					val sections = when (uiState.selectedTab) {
						OverviewTab.MONTH -> uiState.overviewLayout.month
						OverviewTab.LIFETIME -> uiState.overviewLayout.lifetime
					}
					sections.filter { it.visible }.forEach { sectionState ->
						OverviewSectionContent(
							section = sectionState.section,
							tab = uiState.selectedTab,
							uiState = uiState,
							weightUnit = weightUnit,
							onSelectMetric = { viewModel.selectMetric(it) },
							onSelectExercise = { viewModel.selectExercise(it) },
							onViewAllRecords = onViewAllRecords
						)

						Spacer(modifier = Modifier.height(24.dp))
					}
				}
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverviewSectionContent(
	section: OverviewSection,
	tab: OverviewTab,
	uiState: OverviewUiState,
	weightUnit: WeightUnit,
	onSelectMetric: (ChartMetric) -> Unit,
	onSelectExercise: (Long?) -> Unit,
	onViewAllRecords: () -> Unit
) {
	when (section) {
		OverviewSection.STREAK -> StreakCard(currentStreak = uiState.currentStreak)

		OverviewSection.SUMMARY -> when (tab) {
			OverviewTab.MONTH -> MonthStatsSummaryCard(
				workoutsThisWeek = uiState.workoutsThisWeek,
				workoutsThisMonth = uiState.workoutsThisMonth,
				averageDuration = uiState.averageDuration
			)

			OverviewTab.LIFETIME -> LifetimeStatsSummaryCard(
				totalWorkoutsAllTime = uiState.totalWorkoutsAllTime,
				maxWorkoutVolume = uiState.maxWorkoutVolume,
				avgWorkoutsPerWeek = uiState.avgWorkoutsPerWeek,
				longestWorkoutMinutes = uiState.longestWorkoutMinutes,
				weightUnit = weightUnit
			)
		}

		OverviewSection.PROGRESS -> ProgressSection(
			progressData = if (tab == OverviewTab.MONTH) uiState.monthProgressData else uiState.progressData,
			exercises = uiState.exercises,
			selectedExerciseId = uiState.selectedExerciseId,
			selectedMetric = uiState.selectedMetric,
			weightUnit = weightUnit,
			onSelectMetric = onSelectMetric,
			onSelectExercise = onSelectExercise
		)

		OverviewSection.RECORDS -> PersonalRecordsCard(
			personalRecords = uiState.personalRecords,
			weightUnit = weightUnit,
			onClick = onViewAllRecords
		)

		OverviewSection.CATEGORY -> CategoryBreakdownChart(
			categoryBreakdown = if (tab == OverviewTab.MONTH) uiState.monthCategoryBreakdown else uiState.categoryBreakdown
		)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressSection(
	progressData: List<net.luis.tracker.data.local.dao.ExerciseProgress>,
	exercises: List<net.luis.tracker.domain.model.Exercise>,
	selectedExerciseId: Long?,
	selectedMetric: ChartMetric,
	weightUnit: WeightUnit,
	onSelectMetric: (ChartMetric) -> Unit,
	onSelectExercise: (Long?) -> Unit
) {
	Text(
		text = stringResource(R.string.progress),
		style = MaterialTheme.typography.titleMedium
	)

	Spacer(modifier = Modifier.height(8.dp))

	// Metric selector chips (2-column grid)
	ChartMetric.entries.chunked(2).forEach { row ->
		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.spacedBy(8.dp)
		) {
			row.forEach { metric ->
				FilterChip(
					selected = selectedMetric == metric,
					onClick = { onSelectMetric(metric) },
					modifier = Modifier.weight(1f),
					label = {
						Text(
							text = when (metric) {
								ChartMetric.MAX_WEIGHT -> stringResource(R.string.metric_max_weight)
								ChartMetric.TOTAL_VOLUME -> stringResource(R.string.metric_total_volume)
								ChartMetric.MAX_REPS -> stringResource(R.string.metric_max_reps)
								ChartMetric.SET_COUNT -> stringResource(R.string.metric_set_count)
							},
							maxLines = 1
						)
					}
				)
			}
			// Fill remaining space if odd number of items
			if (row.size < 2) {
				Spacer(modifier = Modifier.weight(1f))
			}
		}
	}

	Spacer(modifier = Modifier.height(8.dp))

	// Exercise selector dropdown
	var exerciseDropdownExpanded by remember { mutableStateOf(false) }
	val selectedExerciseName = if (selectedExerciseId == null) {
		stringResource(R.string.all_exercises)
	} else {
		exercises.find { it.id == selectedExerciseId }?.title
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
					onSelectExercise(null)
					exerciseDropdownExpanded = false
				}
			)
			exercises.forEach { exercise ->
				DropdownMenuItem(
					text = { Text(exercise.title) },
					onClick = {
						onSelectExercise(exercise.id)
						exerciseDropdownExpanded = false
					}
				)
			}
		}
	}

	Spacer(modifier = Modifier.height(16.dp))

	// Progress chart (ComposeCharts LineChart)
	ProgressChart(
		progressData = progressData,
		metric = selectedMetric,
		weightUnit = weightUnit
	)
}
