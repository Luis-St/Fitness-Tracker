package net.luis.tracker.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import net.luis.tracker.data.local.entity.WorkoutEntity
import net.luis.tracker.data.local.entity.WorkoutExerciseEntity

data class WorkoutWithExercises(
	@Embedded val workout: WorkoutEntity,
	@Relation(
		parentColumn = "id",
		entityColumn = "workoutId",
		entity = WorkoutExerciseEntity::class
	)
	val exercises: List<WorkoutExerciseWithSets>
)
