package net.luis.tracker.domain.model

data class WorkoutSet(
	val id: Long = 0,
	val workoutExerciseId: Long = 0,
	val setNumber: Int,
	val weightKg: Double = 0.0,
	val reps: Int
)
