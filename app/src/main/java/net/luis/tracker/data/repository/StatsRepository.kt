package net.luis.tracker.data.repository

import kotlinx.coroutines.flow.Flow
import net.luis.tracker.data.local.dao.CategoryWorkoutCount
import net.luis.tracker.data.local.dao.ExerciseProgress
import net.luis.tracker.data.local.dao.PersonalRecord
import net.luis.tracker.data.local.dao.StatsDao
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

	fun getWorkoutDatesInRange(startMillis: Long, endMillis: Long): Flow<List<Long>> =
		statsDao.getWorkoutDatesInRange(startMillis, endMillis)

	fun getTotalWorkoutCount(): Flow<Int> =
		statsDao.getTotalWorkoutCount()

	fun getTotalVolume(): Flow<Double?> =
		statsDao.getTotalVolume()

	fun getFirstWorkoutDate(): Flow<Long?> =
		statsDao.getFirstWorkoutDate()

	fun getLongestWorkoutDuration(): Flow<Long?> =
		statsDao.getLongestWorkoutDuration()

	fun getPersonalRecords(): Flow<List<PersonalRecord>> =
		statsDao.getPersonalRecords()

	fun getCategoryBreakdown(): Flow<List<CategoryWorkoutCount>> =
		statsDao.getCategoryBreakdown()

	fun getWorkoutIdsInRange(startMillis: Long, endMillis: Long): Flow<List<WorkoutDateInfo>> =
		statsDao.getWorkoutIdsInRange(startMillis, endMillis)
}
