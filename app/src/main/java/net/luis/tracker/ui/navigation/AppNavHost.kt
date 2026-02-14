package net.luis.tracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import net.luis.tracker.FitnessTrackerApp
import net.luis.tracker.domain.model.WeightUnit
import net.luis.tracker.ui.activeworkout.ActiveWorkoutScreen
import net.luis.tracker.ui.exercises.AddExerciseScreen
import net.luis.tracker.ui.exercises.ExerciseDetailScreen
import net.luis.tracker.ui.exercises.ExercisesScreen
import net.luis.tracker.ui.overview.OverviewScreen
import net.luis.tracker.ui.settings.SettingsScreen
import net.luis.tracker.ui.workouts.EditWorkoutScreen
import net.luis.tracker.ui.workouts.WorkoutDetailScreen
import net.luis.tracker.ui.workouts.WorkoutsScreen

@Composable
fun AppNavHost(
	navController: NavHostController,
	app: FitnessTrackerApp,
	weightUnit: WeightUnit,
	modifier: Modifier = Modifier
) {
	NavHost(
		navController = navController,
		startDestination = OverviewRoute,
		modifier = modifier
	) {
		composable<OverviewRoute> {
			OverviewScreen(
				app = app,
				weightUnit = weightUnit,
				onOpenSettings = { navController.navigate(SettingsRoute) }
			)
		}
		composable<ExercisesRoute> {
			ExercisesScreen(
				app = app,
				onAddExercise = { navController.navigate(AddExerciseRoute) },
				onExerciseClick = { id -> navController.navigate(ExerciseDetailRoute(id)) },
				onOpenSettings = { navController.navigate(SettingsRoute) }
			)
		}
		composable<WorkoutsRoute> {
			WorkoutsScreen(
				app = app,
				weightUnit = weightUnit,
				onStartWorkout = { navController.navigate(ActiveWorkoutRoute) },
				onWorkoutClick = { id -> navController.navigate(WorkoutDetailRoute(id)) },
				onOpenSettings = { navController.navigate(SettingsRoute) }
			)
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
		composable<ActiveWorkoutRoute> {
			ActiveWorkoutScreen(
				app = app,
				weightUnit = weightUnit,
				onFinished = { navController.popBackStack() }
			)
		}
		composable<WorkoutDetailRoute> { backStackEntry ->
			val route = backStackEntry.toRoute<WorkoutDetailRoute>()
			WorkoutDetailScreen(
				app = app,
				workoutId = route.workoutId,
				weightUnit = weightUnit,
				onNavigateBack = { navController.popBackStack() },
				onEdit = { navController.navigate(EditWorkoutRoute(route.workoutId)) }
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
