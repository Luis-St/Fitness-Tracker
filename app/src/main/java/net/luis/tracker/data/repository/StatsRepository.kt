package net.luis.tracker.data.repository

import kotlinx.coroutines.flow.Flow
import net.luis.tracker.data.local.dao.CategoryWorkoutCount
import net.luis.tracker.data.local.dao.ExerciseProgress
import net.luis.tracker.data.local.dao.ExerciseSetHistory
import net.luis.tracker.data.local.dao.PersonalRecord
import net.luis.tracker.data.local.dao.RecentWorkout
import net.luis.tracker.data.local.dao.StatsDao
import net.luis.tracker.data.local.dao.TopExercise
import net.luis.tracker.data.local.dao.WorkoutDateInfo

class StatsRepository(private val statsDao: StatsDao) {

	fun getWorkoutCount(startMillis: Long, endMillis: Long): Flow<Int> =
		statsDao.getWorkoutCount(startMillis, endMillis)

	fun getAverageDuration(startMillis: Long, endMillis: Long): Flow<Double?> =
		statsDao.getAverageDuration(startMillis, endMillis)

	fun getExerciseProgress(exerciseId: Long): Flow<List<ExerciseProgress>> =
		statsDao.getExerciseProgress(exerciseId)

	fun getAllExerciseProgress(): Flow<List<ExerciseProgress>> =
		statsDao.getAllExerciseProgress()

	fun getExerciseProgress(exerciseId: Long, startMillis: Long, endMillis: Long): Flow<List<ExerciseProgress>> =
		statsDao.getExerciseProgress(exerciseId, startMillis, endMillis)

	fun getAllExerciseProgress(startMillis: Long, endMillis: Long): Flow<List<ExerciseProgress>> =
		statsDao.getAllExerciseProgress(startMillis, endMillis)

	fun getWorkoutDatesInRange(startMillis: Long, endMillis: Long): Flow<List<Long>> =
		statsDao.getWorkoutDatesInRange(startMillis, endMillis)

	fun getTotalWorkoutCount(): Flow<Int> =
		statsDao.getTotalWorkoutCount()

	fun getTotalSetCount(): Flow<Int> =
		statsDao.getTotalSetCount()

	fun getTotalSetCount(startMillis: Long, endMillis: Long): Flow<Int> =
		statsDao.getTotalSetCount(startMillis, endMillis)

	fun getTotalReps(): Flow<Long> =
		statsDao.getTotalReps()

	fun getTotalReps(startMillis: Long, endMillis: Long): Flow<Long> =
		statsDao.getTotalReps(startMillis, endMillis)

	fun getTotalVolume(startMillis: Long, endMillis: Long): Flow<Double> =
		statsDao.getTotalVolume(startMillis, endMillis)

	fun getTotalDurationSeconds(): Flow<Long> =
		statsDao.getTotalDurationSeconds()

	fun getAllWorkoutStartTimes(): Flow<List<Long>> =
		statsDao.getAllWorkoutStartTimes()

	fun getRecentWorkouts(limit: Int): Flow<List<RecentWorkout>> =
		statsDao.getRecentWorkouts(limit)

	fun getTopExercises(limit: Int): Flow<List<TopExercise>> =
		statsDao.getTopExercises(limit)

	fun getMaxWorkoutVolume(): Flow<Double?> =
		statsDao.getMaxWorkoutVolume()

	fun getFirstWorkoutDate(): Flow<Long?> =
		statsDao.getFirstWorkoutDate()

	fun getLongestWorkoutDuration(): Flow<Long?> =
		statsDao.getLongestWorkoutDuration()

	fun getPersonalRecords(): Flow<List<PersonalRecord>> =
		statsDao.getPersonalRecords()

	fun getCategoryBreakdown(): Flow<List<CategoryWorkoutCount>> =
		statsDao.getCategoryBreakdown()

	fun getCategoryBreakdown(startMillis: Long, endMillis: Long): Flow<List<CategoryWorkoutCount>> =
		statsDao.getCategoryBreakdown(startMillis, endMillis)

	fun getWorkoutIdsInRange(startMillis: Long, endMillis: Long): Flow<List<WorkoutDateInfo>> =
		statsDao.getWorkoutIdsInRange(startMillis, endMillis)

	fun getExerciseSetHistory(exerciseId: Long): Flow<List<ExerciseSetHistory>> =
		statsDao.getExerciseSetHistory(exerciseId)
}
