package net.luis.tracker.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object TabsRoute

@Serializable
object OverviewRoute

@Serializable
object ExercisesRoute

@Serializable
object WorkoutsRoute

@Serializable
object SettingsRoute

@Serializable
object AddExerciseRoute

@Serializable
data class ExerciseDetailRoute(val exerciseId: Long)

@Serializable
data class ActiveWorkoutGraphRoute(val resumeWorkoutId: Long = 0L, val planWorkoutId: Long = 0L)

@Serializable
object ActiveWorkoutRoute

@Serializable
object SelectExerciseForWorkoutRoute

@Serializable
data class ActiveWorkoutExerciseRoute(val entryId: Long)

@Serializable
data class WorkoutDetailRoute(val workoutId: Long)

@Serializable
data class EditWorkoutRoute(val workoutId: Long)

@Serializable
data class RestTimerRoute(val durationSeconds: Int)

@Serializable
object AllPersonalRecordsRoute

@Serializable
data class ExerciseHistoryRoute(val exerciseId: Long, val exerciseTitle: String)
