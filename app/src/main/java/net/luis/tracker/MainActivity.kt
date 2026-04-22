package net.luis.tracker

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import net.luis.tracker.data.repository.SettingsRepository
import net.luis.tracker.domain.model.AppLanguage
import net.luis.tracker.domain.model.ThemeMode
import net.luis.tracker.domain.model.TimerResumeMode
import net.luis.tracker.domain.model.WeightUnit
import java.util.Locale
import net.luis.tracker.ui.navigation.ActiveWorkoutExerciseRoute
import net.luis.tracker.ui.navigation.ActiveWorkoutRoute
import net.luis.tracker.ui.navigation.RestTimerRoute
import net.luis.tracker.ui.navigation.SelectExerciseForWorkoutRoute
import net.luis.tracker.ui.navigation.TabsRoute
import net.luis.tracker.ui.navigation.AppNavHost
import net.luis.tracker.ui.navigation.BottomNavBar
import net.luis.tracker.ui.theme.FitnessTrackerTheme

private class LocalizedContextWrapper(base: Context, locale: Locale) : ContextWrapper(base) {
	private val localizedResources: Resources = run {
		val config = Configuration(base.resources.configuration)
		config.setLocale(locale)
		base.createConfigurationContext(config).resources
	}
	override fun getResources(): Resources = localizedResources
}

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()

		val app = application as FitnessTrackerApp
		val settingsRepo = SettingsRepository(applicationContext)

		val pendingImportUri: String? = when (intent.action) {
			Intent.ACTION_VIEW -> intent.data?.toString()
			Intent.ACTION_SEND -> intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)?.toString()
			else -> null
		}

		setContent {
			data class AppSettings(
				val themeMode: ThemeMode = ThemeMode.SYSTEM,
				val dynamicColors: Boolean = true,
				val weightUnit: WeightUnit = WeightUnit.KG,
				val restTimerSeconds: Int = 90,
				val weeklyWorkoutGoal: Int = 2,
				val timerResumeMode: TimerResumeMode = TimerResumeMode.RESUME,
				val appLanguage: AppLanguage = AppLanguage.SYSTEM
			)

			val settingsFlow = remember {
				combine(
					settingsRepo.themeMode,
					settingsRepo.dynamicColors,
					settingsRepo.weightUnit,
					combine(
						settingsRepo.restTimerSeconds,
						settingsRepo.weeklyWorkoutGoal,
						settingsRepo.timerResumeMode
					) { rest, goal, timerMode -> Triple(rest, goal, timerMode) },
					settingsRepo.appLanguage
				) { theme, dynamic, unit, (restTimer, goal, timerMode), lang ->
					AppSettings(theme, dynamic, unit, restTimer, goal, timerMode, lang)
				}
			}
			val settings by settingsFlow.collectAsState(initial = AppSettings())

			val baseContext = LocalContext.current
			val localizedContext = remember(settings.appLanguage) {
				when (settings.appLanguage) {
					AppLanguage.SYSTEM -> baseContext
					AppLanguage.ENGLISH -> LocalizedContextWrapper(baseContext, Locale.ENGLISH)
					AppLanguage.DEUTSCH -> LocalizedContextWrapper(baseContext, Locale.GERMAN)
				}
			}

			val darkTheme = when (settings.themeMode) {
				ThemeMode.LIGHT -> false
				ThemeMode.DARK -> true
				ThemeMode.SYSTEM -> null
			}

			CompositionLocalProvider(LocalContext provides localizedContext) {
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
						timerResumeMode = settings.timerResumeMode,
						pendingImportUri = pendingImportUri,
						modifier = when {
							isActiveWorkout -> Modifier
							isBottomNavRoute -> Modifier.padding(bottom = innerPadding.calculateBottomPadding())
							else -> Modifier.padding(innerPadding)
						}
					)
				}
			}
			} // CompositionLocalProvider
		}
	}
}
