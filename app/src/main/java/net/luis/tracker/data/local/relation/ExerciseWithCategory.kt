package net.luis.tracker.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import net.luis.tracker.data.local.entity.CategoryEntity
import net.luis.tracker.data.local.entity.ExerciseEntity

data class ExerciseWithCategory(
	@Embedded val exercise: ExerciseEntity,
	@Relation(
		parentColumn = "categoryId",
		entityColumn = "id"
	)
	val category: CategoryEntity?
)
