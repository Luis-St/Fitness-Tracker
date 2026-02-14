package net.luis.tracker.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import net.luis.tracker.data.local.entity.ExerciseEntity
import net.luis.tracker.data.local.entity.WorkoutExerciseEntity
import net.luis.tracker.data.local.entity.WorkoutSetEntity

data class WorkoutExerciseWithSets(
	@Embedded val workoutExercise: WorkoutExerciseEntity,
	@Relation(
		parentColumn = "exerciseId",
		entityColumn = "id"
	)
	val exercise: ExerciseEntity,
	@Relation(
		parentColumn = "id",
		entityColumn = "workoutExerciseId"
	)
	val sets: List<WorkoutSetEntity>
)
