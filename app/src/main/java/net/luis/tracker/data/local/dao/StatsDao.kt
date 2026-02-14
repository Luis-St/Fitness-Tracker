package net.luis.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class ExerciseProgress(
	val startTime: Long,
	val maxWeight: Double,
	val totalVolume: Double,
	val maxReps: Int,
	val setCount: Int
)

@Dao
interface StatsDao {

	@Query("SELECT COUNT(*) FROM workouts WHERE startTime >= :startMillis AND startTime < :endMillis")
	fun getWorkoutCount(startMillis: Long, endMillis: Long): Flow<Int>

	@Query("SELECT AVG(durationSeconds) FROM workouts WHERE startTime >= :startMillis AND startTime < :endMillis")
	fun getAverageDuration(startMillis: Long, endMillis: Long): Flow<Double?>

	@Query(
		"""
		SELECT w.startTime,
			MAX(ws.weightKg) as maxWeight,
			SUM(ws.weightKg * ws.reps) as totalVolume,
			MAX(ws.reps) as maxReps,
			COUNT(ws.id) as setCount
		FROM workouts w
		INNER JOIN workout_exercises we ON we.workoutId = w.id
		INNER JOIN workout_sets ws ON ws.workoutExerciseId = we.id
		WHERE we.exerciseId = :exerciseId
		GROUP BY w.id
		ORDER BY w.startTime ASC
		"""
	)
	fun getExerciseProgress(exerciseId: Long): Flow<List<ExerciseProgress>>

	@Query(
		"""
		SELECT w.startTime,
			MAX(ws.weightKg) as maxWeight,
			SUM(ws.weightKg * ws.reps) as totalVolume,
			MAX(ws.reps) as maxReps,
			COUNT(ws.id) as setCount
		FROM workouts w
		INNER JOIN workout_exercises we ON we.workoutId = w.id
		INNER JOIN workout_sets ws ON ws.workoutExerciseId = we.id
		GROUP BY w.id
		ORDER BY w.startTime ASC
		"""
	)
	fun getAllExerciseProgress(): Flow<List<ExerciseProgress>>

	@Query("SELECT DISTINCT startTime FROM workouts WHERE startTime >= :startMillis AND startTime < :endMillis")
	fun getWorkoutDatesInRange(startMillis: Long, endMillis: Long): Flow<List<Long>>
}
