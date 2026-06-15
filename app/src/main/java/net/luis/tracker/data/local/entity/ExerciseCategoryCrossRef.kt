package net.luis.tracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
	tableName = "exercise_categories",
	primaryKeys = ["exerciseId", "categoryId"],
	foreignKeys = [
		ForeignKey(
			entity = ExerciseEntity::class,
			parentColumns = ["id"],
			childColumns = ["exerciseId"],
			onDelete = ForeignKey.CASCADE
		),
		ForeignKey(
			entity = CategoryEntity::class,
			parentColumns = ["id"],
			childColumns = ["categoryId"],
			onDelete = ForeignKey.CASCADE
		)
	],
	indices = [Index("categoryId")]
)
data class ExerciseCategoryCrossRef(
	val exerciseId: Long,
	val categoryId: Long
)
