package net.luis.tracker.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import net.luis.tracker.FitnessTrackerApp
import net.luis.tracker.data.repository.ActiveWorkoutDraftRepository
import net.luis.tracker.data.repository.ExerciseRepository
import net.luis.tracker.data.repository.WorkoutRepository
import net.luis.tracker.domain.model.WeightUnit
import net.luis.tracker.ui.activeworkout.ActiveWorkoutExerciseScreen
import net.luis.tracker.ui.activeworkout.ActiveWorkoutScreen
import net.luis.tracker.ui.activeworkout.ActiveWorkoutViewModel
import net.luis.tracker.ui.activeworkout.RestTimerScreen
import net.luis.tracker.ui.activeworkout.SelectExerciseScreen
import net.luis.tracker.ui.exercises.AddExerciseScreen
import net.luis.tracker.ui.exercises.ExerciseDetailScreen
import net.luis.tracker.ui.exercises.ExercisesScreen
import net.luis.tracker.ui.overview.AllPersonalRecordsScreen
import net.luis.tracker.ui.overview.OverviewScreen
import net.luis.tracker.ui.settings.SettingsScreen
import net.luis.tracker.ui.workouts.EditWorkoutScreen
import net.luis.tracker.ui.workouts.WorkoutDetailScreen
import net.luis.tracker.ui.workouts.WorkoutsScreen

@Composable
private fun activeWorkoutViewModel(
	navController: NavHostController,
	app: FitnessTrackerApp,
	draftRepository: ActiveWorkoutDraftRepository,
	currentEntry: NavBackStackEntry
): ActiveWorkoutViewModel {
	val parentEntry = remember(currentEntry) {
		navController.getBackStackEntry<ActiveWorkoutGraphRoute>()
	}
	val graphRoute = parentEntry.toRoute<ActiveWorkoutGraphRoute>()
	val factory = remember(graphRoute.resumeWorkoutId) {
		ActiveWorkoutViewModel.Factory(
			ExerciseRepository(app.database.exerciseDao()),
			WorkoutRepository(
				app.database,
				app.database.workoutDao(),
				app.database.workoutExerciseDao(),
				app.database.workoutSetDao()
			),
			draftRepository,
			resumeWorkoutId = graphRoute.resumeWorkoutId
		)
	}
	return viewModel(
		viewModelStoreOwner = parentEntry,
		factory = factory
	)
}

@Composable
fun AppNavHost(
	navController: NavHostController,
	pagerState: PagerState,
	app: FitnessTrackerApp,
	weightUnit: WeightUnit,
	restTimerSeconds: Int,
	weeklyWorkoutGoal: Int,
	modifier: Modifier = Modifier
) {
	val context = LocalContext.current
	val draftRepository = remember { ActiveWorkoutDraftRepository(context.applicationContext) }

	NavHost(
		navController = navController,
		startDestination = TabsRoute,
		modifier = modifier
	) {
		composable<TabsRoute> {
			HorizontalPager(
				state = pagerState,
				beyondViewportPageCount = 1
			) { page ->
				when (page) {
					0 -> ExercisesScreen(
						app = app,
						onAddExercise = { navController.navigate(AddExerciseRoute) },
						onExerciseClick = { id -> navController.navigate(ExerciseDetailRoute(id)) },
						onOpenSettings = { navController.navigate(SettingsRoute) }
					)
					1 -> OverviewScreen(
						app = app,
						weightUnit = weightUnit,
						weeklyWorkoutGoal = weeklyWorkoutGoal,
						onOpenSettings = { navController.navigate(SettingsRoute) },
						onNavigateToWorkout = { workoutId ->
							navController.navigate(WorkoutDetailRoute(workoutId))
						},
						onViewAllRecords = { navController.navigate(AllPersonalRecordsRoute) }
					)
					2 -> WorkoutsScreen(
						app = app,
						weightUnit = weightUnit,
						draftRepository = draftRepository,
						onStartWorkout = { navController.navigate(ActiveWorkoutGraphRoute()) },
						onWorkoutClick = { id -> navController.navigate(WorkoutDetailRoute(id)) },
						onOpenSettings = { navController.navigate(SettingsRoute) }
					)
				}
			}
		}
		composable<SettingsRoute> {
			SettingsScreen(
				app = app,
				onNavigateBack = { navController.popBackStack() }
			)
		}
		composable<AddExerciseRoute> {
			AddExerciseScreen(
				app = app,
				onNavigateBack = { navController.popBackStack() }
			)
		}
		composable<ExerciseDetailRoute> { backStackEntry ->
			val route = backStackEntry.toRoute<ExerciseDetailRoute>()
			ExerciseDetailScreen(
				app = app,
				exerciseId = route.exerciseId,
				onNavigateBack = { navController.popBackStack() }
			)
		}
		navigation<ActiveWorkoutGraphRoute>(startDestination = ActiveWorkoutRoute) {
			composable<ActiveWorkoutRoute> {
				val sharedViewModel = activeWorkoutViewModel(navController, app, draftRepository, it)
				ActiveWorkoutScreen(
					viewModel = sharedViewModel,
					onFinished = { navController.popBackStack(ActiveWorkoutGraphRoute::class, inclusive = true) },
					onEditExercise = { entryId ->
						navController.navigate(ActiveWorkoutExerciseRoute(entryId))
					},
					onAddExercise = {
						navController.navigate(SelectExerciseForWorkoutRoute)
					}
				)
			}
			composable<SelectExerciseForWorkoutRoute>(
				enterTransition = { EnterTransition.None },
				exitTransition = { ExitTransition.None }
			) {
				val sharedViewModel = activeWorkoutViewModel(navController, app, draftRepository, it)
				SelectExerciseScreen(
					viewModel = sharedViewModel,
					onExerciseSelected = { entryId ->
						navController.navigate(ActiveWorkoutExerciseRoute(entryId)) {
							popUpTo<ActiveWorkoutRoute>()
						}
					},
					onNavigateBack = { navController.popBackStack() }
				)
			}
			composable<ActiveWorkoutExerciseRoute> { backStackEntry ->
				val route = backStackEntry.toRoute<ActiveWorkoutExerciseRoute>()
				val sharedViewModel = activeWorkoutViewModel(navController, app, draftRepository, backStackEntry)
				ActiveWorkoutExerciseScreen(
					viewModel = sharedViewModel,
					entryId = route.entryId,
					weightUnit = weightUnit,
					onNavigateBack = {
						sharedViewModel.removeExerciseIfEmpty(route.entryId)
						navController.popBackStack()
					},
					onRest = {
						navController.navigate(RestTimerRoute(restTimerSeconds))
					},
					onFinishWithTimer = {
						sharedViewModel.removeExerciseIfEmpty(route.entryId)
						navController.navigate(RestTimerRoute(restTimerSeconds)) {
							popUpTo<ActiveWorkoutRoute>()
						}
					}
				)
			}
			composable<RestTimerRoute> { backStackEntry ->
				val route = backStackEntry.toRoute<RestTimerRoute>()
				RestTimerScreen(
					durationSeconds = route.durationSeconds,
					onFinished = { navController.popBackStack() },
					onSkip = { navController.popBackStack() }
				)
			}
		}
		composable<AllPersonalRecordsRoute> {
			AllPersonalRecordsScreen(
				app = app,
				weightUnit = weightUnit,
				onNavigateBack = { navController.popBackStack() }
			)
		}
		composable<WorkoutDetailRoute> { backStackEntry ->
			val route = backStackEntry.toRoute<WorkoutDetailRoute>()
			WorkoutDetailScreen(
				app = app,
				workoutId = route.workoutId,
				weightUnit = weightUnit,
				onNavigateBack = { navController.popBackStack() },
				onEdit = { navController.navigate(EditWorkoutRoute(route.workoutId)) },
				onResume = {
					navController.navigate(ActiveWorkoutGraphRoute(resumeWorkoutId = route.workoutId)) {
						popUpTo<WorkoutDetailRoute> { inclusive = true }
					}
				}
			)
		}
		composable<EditWorkoutRoute> { backStackEntry ->
			val route = backStackEntry.toRoute<EditWorkoutRoute>()
			EditWorkoutScreen(
				app = app,
				workoutId = route.workoutId,
				weightUnit = weightUnit,
				onNavigateBack = { navController.popBackStack() }
			)
		}
	}
}
