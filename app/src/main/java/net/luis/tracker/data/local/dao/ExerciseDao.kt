package net.luis.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import net.luis.tracker.data.local.entity.ExerciseEntity
import net.luis.tracker.data.local.relation.ExerciseWithCategory

@Dao
interface ExerciseDao {

	@Transaction
	@Query("SELECT * FROM exercises WHERE isDeleted = 0 ORDER BY title ASC")
	fun getAllActive(): Flow<List<ExerciseWithCategory>>

	@Transaction
	@Query("SELECT * FROM exercises WHERE isDeleted = 0 AND categoryId = :categoryId ORDER BY title ASC")
	fun getByCategory(categoryId: Long): Flow<List<ExerciseWithCategory>>

	@Query("SELECT * FROM exercises ORDER BY title ASC")
	suspend fun getAllIncludingDeleted(): List<ExerciseEntity>

	@Transaction
	@Query("SELECT * FROM exercises WHERE id = :id")
	suspend fun getById(id: Long): ExerciseWithCategory?

	@Insert
	suspend fun insert(exercise: ExerciseEntity): Long

	@Update
	suspend fun update(exercise: ExerciseEntity)

	@Query("UPDATE exercises SET isDeleted = 1 WHERE id = :id")
	suspend fun softDelete(id: Long)
}
