package net.luis.tracker.domain.model

data class Workout(
	val id: Long = 0,
	val startTime: Long,
	val endTime: Long? = null,
	val durationSeconds: Long = 0,
	val notes: String = "",
	val exercises: List<WorkoutExercise> = emptyList()
) {
	val totalVolume: Double
		get() = exercises.sumOf { exercise ->
			exercise.sets.sumOf { it.weightKg * it.reps }
		}

	val exerciseCount: Int
		get() = exercises.size

	val totalSets: Int
		get() = exercises.sumOf { it.sets.size }
}
