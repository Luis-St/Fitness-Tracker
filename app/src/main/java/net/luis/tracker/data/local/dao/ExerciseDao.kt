package net.luis.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import net.luis.tracker.data.local.entity.ExerciseCategoryCrossRef
import net.luis.tracker.data.local.entity.ExerciseEntity
import net.luis.tracker.data.local.relation.ExerciseWithCategories

@Dao
interface ExerciseDao {

	@Transaction
	@Query("SELECT * FROM exercises WHERE isDeleted = 0 ORDER BY title ASC")
	fun getAllActive(): Flow<List<ExerciseWithCategories>>

	@Transaction
	@Query(
		"SELECT e.* FROM exercises e " +
			"INNER JOIN exercise_categories ec ON ec.exerciseId = e.id " +
			"WHERE e.isDeleted = 0 AND ec.categoryId = :categoryId ORDER BY title ASC"
	)
	fun getByCategory(categoryId: Long): Flow<List<ExerciseWithCategories>>

	@Query("SELECT * FROM exercises ORDER BY title ASC")
	suspend fun getAllIncludingDeleted(): List<ExerciseEntity>

	@Transaction
	@Query("SELECT * FROM exercises WHERE id = :id")
	suspend fun getById(id: Long): ExerciseWithCategories?

	@Insert
	suspend fun insert(exercise: ExerciseEntity): Long

	@Update
	suspend fun update(exercise: ExerciseEntity)

	@Query("UPDATE exercises SET isDeleted = 1 WHERE id = :id")
	suspend fun softDelete(id: Long)

	@Insert
	suspend fun insertCrossRefs(refs: List<ExerciseCategoryCrossRef>)

	@Query("DELETE FROM exercise_categories WHERE exerciseId = :id")
	suspend fun deleteCrossRefs(id: Long)

	@Query("SELECT * FROM exercise_categories")
	suspend fun getAllCrossRefs(): List<ExerciseCategoryCrossRef>

	@Transaction
	suspend fun insertWithCategories(exercise: ExerciseEntity, categoryIds: List<Long>): Long {
		val newId = insert(exercise)
		if (categoryIds.isNotEmpty()) {
			insertCrossRefs(categoryIds.map { ExerciseCategoryCrossRef(exerciseId = newId, categoryId = it) })
		}
		return newId
	}

	@Transaction
	suspend fun updateWithCategories(exercise: ExerciseEntity, categoryIds: List<Long>) {
		update(exercise)
		deleteCrossRefs(exercise.id)
		if (categoryIds.isNotEmpty()) {
			insertCrossRefs(categoryIds.map { ExerciseCategoryCrossRef(exerciseId = exercise.id, categoryId = it) })
		}
	}
}
