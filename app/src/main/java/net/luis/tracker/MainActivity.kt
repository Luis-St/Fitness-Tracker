package net.luis.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
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
import net.luis.tracker.ui.navigation.TabsRoute
import net.luis.tracker.ui.navigation.AppNavHost
import net.luis.tracker.ui.navigation.BottomNavBar
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
				val restTimerSeconds: Int = 90,
				val weeklyWorkoutGoal: Int = 2
			)

			val settingsFlow = remember {
				combine(
					settingsRepo.themeMode,
					settingsRepo.dynamicColors,
					settingsRepo.weightUnit,
					combine(
						settingsRepo.restTimerSeconds,
						settingsRepo.weeklyWorkoutGoal
					) { rest, goal -> rest to goal }
				) { theme, dynamic, unit, (restTimer, goal) ->
					AppSettings(theme, dynamic, unit, restTimer, goal)
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

				val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
				val coroutineScope = rememberCoroutineScope()

				val isBottomNavRoute = currentRoute == TabsRoute::class.qualifiedName
				val isActiveWorkout = currentRoute == ActiveWorkoutRoute::class.qualifiedName ||
				currentRoute == ActiveWorkoutExerciseRoute::class.qualifiedName ||
				currentRoute == SelectExerciseForWorkoutRoute::class.qualifiedName ||
				currentRoute == RestTimerRoute::class.qualifiedName

				Scaffold(
					bottomBar = {
						if (isBottomNavRoute) {
							BottomNavBar(
								selectedIndex = pagerState.currentPage,
								onTabSelected = { index ->
									coroutineScope.launch {
										pagerState.animateScrollToPage(index)
									}
								}
							)
						}
					}
				) { innerPadding ->
					AppNavHost(
						navController = navController,
						pagerState = pagerState,
						app = app,
						weightUnit = settings.weightUnit,
						restTimerSeconds = settings.restTimerSeconds,
						weeklyWorkoutGoal = settings.weeklyWorkoutGoal,
						modifier = when {
							isActiveWorkout -> Modifier
							isBottomNavRoute -> Modifier.padding(bottom = innerPadding.calculateBottomPadding())
							else -> Modifier.padding(innerPadding)
						}
					)
				}
			}
		}
	}
}
