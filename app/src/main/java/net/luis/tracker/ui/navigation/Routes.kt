package net.luis.tracker.ui.navigation

import kotlinx.serialization.Serializable

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
object ActiveWorkoutGraphRoute

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
