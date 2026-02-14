package net.luis.tracker.domain.model

data class WorkoutExercise(
	val id: Long = 0,
	val workoutId: Long = 0,
	val exercise: Exercise,
	val orderIndex: Int,
	val sets: List<WorkoutSet> = emptyList()
)
