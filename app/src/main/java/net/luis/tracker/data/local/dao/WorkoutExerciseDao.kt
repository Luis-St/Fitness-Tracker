package net.luis.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import net.luis.tracker.data.local.entity.WorkoutExerciseEntity

@Dao
interface WorkoutExerciseDao {

	@Insert
	suspend fun insert(workoutExercise: WorkoutExerciseEntity): Long

	@Update
	suspend fun update(workoutExercise: WorkoutExerciseEntity)

	@Delete
	suspend fun delete(workoutExercise: WorkoutExerciseEntity)

	@Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY orderIndex ASC")
	suspend fun getByWorkoutId(workoutId: Long): List<WorkoutExerciseEntity>

	@Query("SELECT * FROM workout_exercises ORDER BY workoutId, orderIndex ASC")
	suspend fun getAll(): List<WorkoutExerciseEntity>

	@Query("DELETE FROM workout_exercises WHERE workoutId = :workoutId")
	suspend fun deleteByWorkoutId(workoutId: Long)
}
