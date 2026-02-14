package net.luis.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import net.luis.tracker.data.local.entity.CategoryEntity

@Dao
interface CategoryDao {

	@Query("SELECT * FROM categories ORDER BY name ASC")
	fun getAll(): Flow<List<CategoryEntity>>

	@Query("SELECT * FROM categories WHERE id = :id")
	suspend fun getById(id: Long): CategoryEntity?

	@Query("SELECT * FROM categories ORDER BY name ASC")
	suspend fun getAllOnce(): List<CategoryEntity>

	@Insert
	suspend fun insert(category: CategoryEntity): Long

	@Update
	suspend fun update(category: CategoryEntity)

	@Delete
	suspend fun delete(category: CategoryEntity)
}
