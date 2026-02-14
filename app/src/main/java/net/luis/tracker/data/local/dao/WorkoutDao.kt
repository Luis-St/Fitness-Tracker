package net.luis.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import net.luis.tracker.data.local.entity.WorkoutEntity
import net.luis.tracker.data.local.relation.WorkoutWithExercises

@Dao
interface WorkoutDao {

	@Transaction
	@Query("SELECT * FROM workouts ORDER BY startTime DESC")
	fun getAllWithExercises(): Flow<List<WorkoutWithExercises>>

	@Transaction
	@Query("SELECT * FROM workouts WHERE id = :id")
	suspend fun getByIdWithExercises(id: Long): WorkoutWithExercises?

	@Query("SELECT * FROM workouts ORDER BY startTime DESC")
	suspend fun getAllOnce(): List<WorkoutEntity>

	@Query("SELECT * FROM workouts WHERE id = :id")
	suspend fun getById(id: Long): WorkoutEntity?

	@Insert
	suspend fun insert(workout: WorkoutEntity): Long

	@Update
	suspend fun update(workout: WorkoutEntity)

	@Delete
	suspend fun delete(workout: WorkoutEntity)

	@Query("SELECT * FROM workouts WHERE startTime >= :startMillis AND startTime < :endMillis ORDER BY startTime DESC")
	fun getWorkoutsInRange(startMillis: Long, endMillis: Long): Flow<List<WorkoutEntity>>
}
