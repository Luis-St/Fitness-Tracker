package net.luis.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import net.luis.tracker.data.local.entity.WorkoutSetEntity

@Dao
interface WorkoutSetDao {

	@Insert
	suspend fun insert(set: WorkoutSetEntity): Long

	@Insert
	suspend fun insertAll(sets: List<WorkoutSetEntity>)

	@Update
	suspend fun update(set: WorkoutSetEntity)

	@Delete
	suspend fun delete(set: WorkoutSetEntity)

	@Query("SELECT * FROM workout_sets WHERE workoutExerciseId = :workoutExerciseId ORDER BY setNumber ASC")
	suspend fun getByWorkoutExerciseId(workoutExerciseId: Long): List<WorkoutSetEntity>

	@Query("SELECT * FROM workout_sets ORDER BY workoutExerciseId, setNumber ASC")
	suspend fun getAll(): List<WorkoutSetEntity>

	@Query("DELETE FROM workout_sets WHERE workoutExerciseId = :workoutExerciseId")
	suspend fun deleteByWorkoutExerciseId(workoutExerciseId: Long)
}
