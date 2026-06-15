package net.luis.tracker.data.local.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import net.luis.tracker.data.local.entity.CategoryEntity
import net.luis.tracker.data.local.entity.ExerciseCategoryCrossRef
import net.luis.tracker.data.local.entity.ExerciseEntity

data class ExerciseWithCategories(
	@Embedded val exercise: ExerciseEntity,
	@Relation(
		parentColumn = "id",
		entityColumn = "id",
		associateBy = Junction(
			value = ExerciseCategoryCrossRef::class,
			parentColumn = "exerciseId",
			entityColumn = "categoryId"
		)
	)
	val categories: List<CategoryEntity>
)
