package net.luis.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.combine
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import net.luis.tracker.data.repository.SettingsRepository
import net.luis.tracker.domain.model.ThemeMode
import net.luis.tracker.domain.model.WeightUnit
import net.luis.tracker.ui.navigation.ActiveWorkoutExerciseRoute
import net.luis.tracker.ui.navigation.ActiveWorkoutRoute
import net.luis.tracker.ui.navigation.RestTimerRoute
import net.luis.tracker.ui.navigation.SelectExerciseForWorkoutRoute
import net.luis.tracker.ui.navigation.AppNavHost
import net.luis.tracker.ui.navigation.BottomNavBar
import net.luis.tracker.ui.navigation.bottomNavItems
import net.luis.tracker.ui.theme.FitnessTrackerTheme

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()

		val app = application as FitnessTrackerApp
		val settingsRepo = SettingsRepository(applicationContext)

		setContent {
			data class AppSettings(
				val themeMode: ThemeMode = ThemeMode.SYSTEM,
				val dynamicColors: Boolean = true,
				val weightUnit: WeightUnit = WeightUnit.KG,
				val restTimerSeconds: Int = 90
			)

			val settingsFlow = remember {
				combine(
					settingsRepo.themeMode,
					settingsRepo.dynamicColors,
					settingsRepo.weightUnit,
					settingsRepo.restTimerSeconds
				) { theme, dynamic, unit, restTimer ->
					AppSettings(theme, dynamic, unit, restTimer)
				}
			}
			val settings by settingsFlow.collectAsState(initial = AppSettings())

			val darkTheme = when (settings.themeMode) {
				ThemeMode.LIGHT -> false
				ThemeMode.DARK -> true
				ThemeMode.SYSTEM -> null
			}

			FitnessTrackerTheme(
				darkTheme = darkTheme ?: isSystemInDarkTheme(),
				dynamicColor = settings.dynamicColors
			) {
				val navController = rememberNavController()
				val navBackStackEntry by navController.currentBackStackEntryAsState()
				val currentRoute = navBackStackEntry?.destination?.route

				val isBottomNavRoute = bottomNavItems.any { it.route::class.qualifiedName == currentRoute }
				val isActiveWorkout = currentRoute == ActiveWorkoutRoute::class.qualifiedName ||
				currentRoute == ActiveWorkoutExerciseRoute::class.qualifiedName ||
				currentRoute == SelectExerciseForWorkoutRoute::class.qualifiedName ||
				currentRoute == RestTimerRoute::class.qualifiedName

				Scaffold(
					bottomBar = {
						if (isBottomNavRoute) {
							BottomNavBar(
								currentRoute = currentRoute,
								onNavigate = { route ->
									navController.navigate(route) {
										popUpTo(navController.graph.startDestinationId) {
											saveState = true
										}
										launchSingleTop = true
										restoreState = true
									}
								}
							)
						}
					}
				) { innerPadding ->
					AppNavHost(
						navController = navController,
						app = app,
						weightUnit = settings.weightUnit,
						restTimerSeconds = settings.restTimerSeconds,
						modifier = if (!isActiveWorkout) Modifier.padding(innerPadding) else Modifier
					)
				}
			}
		}
	}
}
