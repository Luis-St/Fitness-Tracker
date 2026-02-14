package net.luis.tracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
	tableName = "workout_sets",
	foreignKeys = [
		ForeignKey(
			entity = WorkoutExerciseEntity::class,
			parentColumns = ["id"],
			childColumns = ["workoutExerciseId"],
			onDelete = ForeignKey.CASCADE
		)
	],
	indices = [Index("workoutExerciseId")]
)
data class WorkoutSetEntity(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val workoutExerciseId: Long,
	val setNumber: Int,
	val weightKg: Double = 0.0,
	val reps: Int
)
