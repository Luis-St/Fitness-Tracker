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

data class PersonalRecord(
	val exerciseId: Long,
	val exerciseTitle: String,
	val maxWeight: Double,
	val maxReps: Int,
	val maxVolume: Double
)

data class CategoryWorkoutCount(
	val categoryName: String,
	val count: Int
)

data class ExerciseSetHistory(
	val workoutId: Long,
	val workoutDate: Long,
	val setNumber: Int,
	val weightKg: Double,
	val reps: Int,
	val dropWeightKg: Double?,
	val dropReps: Int?
)

data class WorkoutDateInfo(
	val workoutId: Long,
	val startTime: Long
)

data class RecentWorkout(
	val workoutId: Long,
	val startTime: Long,
	val durationSeconds: Long
)

data class TopExercise(
	val exerciseId: Long,
	val exerciseTitle: String,
	val workoutCount: Int,
	val totalVolume: Double
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
			SUM(ws.weightKg * ws.reps + COALESCE(ws.dropWeightKg * ws.dropReps, 0)) as totalVolume,
			MAX(ws.reps + COALESCE(ws.dropReps, 0)) as maxReps,
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
			SUM(ws.weightKg * ws.reps + COALESCE(ws.dropWeightKg * ws.dropReps, 0)) as totalVolume,
			MAX(ws.reps + COALESCE(ws.dropReps, 0)) as maxReps,
			COUNT(ws.id) as setCount
		FROM workouts w
		INNER JOIN workout_exercises we ON we.workoutId = w.id
		INNER JOIN workout_sets ws ON ws.workoutExerciseId = we.id
		GROUP BY w.id
		ORDER BY w.startTime ASC
		"""
	)
	fun getAllExerciseProgress(): Flow<List<ExerciseProgress>>

	@Query(
		"""
		SELECT w.startTime,
			MAX(ws.weightKg) as maxWeight,
			SUM(ws.weightKg * ws.reps + COALESCE(ws.dropWeightKg * ws.dropReps, 0)) as totalVolume,
			MAX(ws.reps + COALESCE(ws.dropReps, 0)) as maxReps,
			COUNT(ws.id) as setCount
		FROM workouts w
		INNER JOIN workout_exercises we ON we.workoutId = w.id
		INNER JOIN workout_sets ws ON ws.workoutExerciseId = we.id
		WHERE we.exerciseId = :exerciseId AND w.startTime >= :startMillis AND w.startTime < :endMillis
		GROUP BY w.id
		ORDER BY w.startTime ASC
		"""
	)
	fun getExerciseProgress(exerciseId: Long, startMillis: Long, endMillis: Long): Flow<List<ExerciseProgress>>

	@Query(
		"""
		SELECT w.startTime,
			MAX(ws.weightKg) as maxWeight,
			SUM(ws.weightKg * ws.reps + COALESCE(ws.dropWeightKg * ws.dropReps, 0)) as totalVolume,
			MAX(ws.reps + COALESCE(ws.dropReps, 0)) as maxReps,
			COUNT(ws.id) as setCount
		FROM workouts w
		INNER JOIN workout_exercises we ON we.workoutId = w.id
		INNER JOIN workout_sets ws ON ws.workoutExerciseId = we.id
		WHERE w.startTime >= :startMillis AND w.startTime < :endMillis
		GROUP BY w.id
		ORDER BY w.startTime ASC
		"""
	)
	fun getAllExerciseProgress(startMillis: Long, endMillis: Long): Flow<List<ExerciseProgress>>

	@Query("SELECT DISTINCT startTime FROM workouts WHERE startTime >= :startMillis AND startTime < :endMillis")
	fun getWorkoutDatesInRange(startMillis: Long, endMillis: Long): Flow<List<Long>>

	@Query("SELECT COUNT(*) FROM workouts")
	fun getTotalWorkoutCount(): Flow<Int>

	@Query("SELECT COUNT(*) FROM workout_sets")
	fun getTotalSetCount(): Flow<Int>

	@Query("SELECT COALESCE(SUM(reps + COALESCE(dropReps, 0)), 0) FROM workout_sets")
	fun getTotalReps(): Flow<Long>

	@Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM workouts")
	fun getTotalDurationSeconds(): Flow<Long>

	@Query("SELECT startTime FROM workouts ORDER BY startTime ASC")
	fun getAllWorkoutStartTimes(): Flow<List<Long>>

	@Query(
		"""
		SELECT id as workoutId, startTime, durationSeconds
		FROM workouts
		ORDER BY startTime DESC
		LIMIT :limit
		"""
	)
	fun getRecentWorkouts(limit: Int): Flow<List<RecentWorkout>>

	@Query(
		"""
		SELECT e.id as exerciseId,
			e.title as exerciseTitle,
			COUNT(DISTINCT we.workoutId) as workoutCount,
			SUM(ws.weightKg * ws.reps + COALESCE(ws.dropWeightKg * ws.dropReps, 0)) as totalVolume
		FROM exercises e
		INNER JOIN workout_exercises we ON we.exerciseId = e.id
		INNER JOIN workout_sets ws ON ws.workoutExerciseId = we.id
		WHERE e.isDeleted = 0
		GROUP BY e.id
		ORDER BY workoutCount DESC, totalVolume DESC
		LIMIT :limit
		"""
	)
	fun getTopExercises(limit: Int): Flow<List<TopExercise>>

	@Query(
		"""
		SELECT MAX(workout_volume) FROM (
			SELECT SUM(ws.weightKg * ws.reps + COALESCE(ws.dropWeightKg * ws.dropReps, 0)) as workout_volume
			FROM workout_sets ws
			INNER JOIN workout_exercises we ON ws.workoutExerciseId = we.id
			GROUP BY we.workoutId
		)
		"""
	)
	fun getMaxWorkoutVolume(): Flow<Double?>

	@Query("SELECT MIN(startTime) FROM workouts")
	fun getFirstWorkoutDate(): Flow<Long?>

	@Query("SELECT MAX(durationSeconds) FROM workouts")
	fun getLongestWorkoutDuration(): Flow<Long?>

	@Query(
		"""
		SELECT e.id as exerciseId,
			e.title as exerciseTitle,
			MAX(ws.weightKg) as maxWeight,
			MAX(ws.reps + COALESCE(ws.dropReps, 0)) as maxReps,
			MAX(ws.weightKg * ws.reps + COALESCE(ws.dropWeightKg * ws.dropReps, 0)) as maxVolume
		FROM exercises e
		INNER JOIN workout_exercises we ON we.exerciseId = e.id
		INNER JOIN workout_sets ws ON ws.workoutExerciseId = we.id
		WHERE e.isDeleted = 0
		GROUP BY e.id
		ORDER BY maxWeight DESC
		"""
	)
	fun getPersonalRecords(): Flow<List<PersonalRecord>>

	@Query(
		"""
		SELECT COALESCE(c.name, 'Uncategorized') as categoryName,
			COUNT(we.id) as count
		FROM workout_exercises we
		INNER JOIN exercises e ON we.exerciseId = e.id
		LEFT JOIN exercise_categories ec ON ec.exerciseId = e.id
		LEFT JOIN categories c ON ec.categoryId = c.id
		GROUP BY COALESCE(c.name, 'Uncategorized')
		ORDER BY count DESC
		"""
	)
	fun getCategoryBreakdown(): Flow<List<CategoryWorkoutCount>>

	@Query(
		"""
		SELECT COALESCE(c.name, 'Uncategorized') as categoryName,
			COUNT(we.id) as count
		FROM workout_exercises we
		INNER JOIN workouts w ON we.workoutId = w.id
		INNER JOIN exercises e ON we.exerciseId = e.id
		LEFT JOIN exercise_categories ec ON ec.exerciseId = e.id
		LEFT JOIN categories c ON ec.categoryId = c.id
		WHERE w.startTime >= :startMillis AND w.startTime < :endMillis
		GROUP BY COALESCE(c.name, 'Uncategorized')
		ORDER BY count DESC
		"""
	)
	fun getCategoryBreakdown(startMillis: Long, endMillis: Long): Flow<List<CategoryWorkoutCount>>

	@Query(
		"""
		SELECT id as workoutId, startTime
		FROM workouts
		WHERE startTime >= :startMillis AND startTime < :endMillis
		ORDER BY startTime ASC
		"""
	)
	fun getWorkoutIdsInRange(startMillis: Long, endMillis: Long): Flow<List<WorkoutDateInfo>>

	@Query(
		"""
		SELECT w.id as workoutId,
			w.startTime as workoutDate,
			ws.setNumber,
			ws.weightKg,
			ws.reps,
			ws.dropWeightKg,
			ws.dropReps
		FROM workouts w
		INNER JOIN workout_exercises we ON we.workoutId = w.id
		INNER JOIN workout_sets ws ON ws.workoutExerciseId = we.id
		WHERE we.exerciseId = :exerciseId
		ORDER BY w.startTime DESC, ws.setNumber ASC
		"""
	)
	fun getExerciseSetHistory(exerciseId: Long): Flow<List<ExerciseSetHistory>>
}
