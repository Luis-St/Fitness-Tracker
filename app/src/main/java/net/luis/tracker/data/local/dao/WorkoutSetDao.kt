package net.luis.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import net.luis.tracker.data.local.entity.WorkoutSetEntity

data class SetHistoryEntry(
	val workoutDate: Long,
	val weightKg: Double,
	val reps: Int,
	val dropWeightKg: Double?,
	val dropReps: Int?
)

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

	/**
	 * Returns the sets of the most recent previously-logged workout that contains the given
	 * exercise, ordered by set number. [excludeWorkoutId] skips the workout currently being
	 * resumed/edited (pass 0 to consider all workouts). Empty when the exercise has no history.
	 */
	@Query(
		"""
		SELECT ws.* FROM workout_sets ws
		INNER JOIN workout_exercises we ON ws.workoutExerciseId = we.id
		WHERE we.exerciseId = :exerciseId
			AND we.workoutId = (
				SELECT w.id FROM workouts w
				INNER JOIN workout_exercises we2 ON we2.workoutId = w.id
				WHERE we2.exerciseId = :exerciseId AND w.id != :excludeWorkoutId
				ORDER BY w.startTime DESC
				LIMIT 1
			)
		ORDER BY ws.setNumber ASC
		"""
	)
	suspend fun getLastPerformanceSets(exerciseId: Long, excludeWorkoutId: Long): List<WorkoutSetEntity>

	/** Every logged occurrence of a given set number for an exercise, newest workout first. */
	@Query(
		"""
		SELECT w.startTime AS workoutDate, ws.weightKg, ws.reps, ws.dropWeightKg, ws.dropReps
		FROM workout_sets ws
		INNER JOIN workout_exercises we ON ws.workoutExerciseId = we.id
		INNER JOIN workouts w ON we.workoutId = w.id
		WHERE we.exerciseId = :exerciseId AND ws.setNumber = :setNumber
		ORDER BY w.startTime DESC
		"""
	)
	suspend fun getSetHistory(exerciseId: Long, setNumber: Int): List<SetHistoryEntry>

	@Query("SELECT * FROM workout_sets ORDER BY workoutExerciseId, setNumber ASC")
	suspend fun getAll(): List<WorkoutSetEntity>

	@Query("DELETE FROM workout_sets WHERE workoutExerciseId = :workoutExerciseId")
	suspend fun deleteByWorkoutExerciseId(workoutExerciseId: Long)
}
