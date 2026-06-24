package net.luis.tracker.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import net.luis.tracker.data.local.AppDatabase
import net.luis.tracker.data.local.dao.SetHistoryEntry
import net.luis.tracker.data.local.dao.WorkoutDao
import net.luis.tracker.data.local.dao.WorkoutExerciseDao
import net.luis.tracker.data.local.dao.WorkoutSetDao
import net.luis.tracker.domain.mapper.toDomain
import net.luis.tracker.domain.mapper.toEntity
import net.luis.tracker.domain.model.Workout
import net.luis.tracker.domain.model.WorkoutExercise
import net.luis.tracker.domain.model.WorkoutSet

class WorkoutRepository(
	private val database: AppDatabase,
	private val workoutDao: WorkoutDao,
	private val workoutExerciseDao: WorkoutExerciseDao,
	private val workoutSetDao: WorkoutSetDao
) {

	fun getAllWithExercises(): Flow<List<Workout>> =
		workoutDao.getAllWithExercises()
			.map { list -> list.map { it.toDomain() } }
			.flowOn(Dispatchers.Default)

	suspend fun getByIdWithExercises(id: Long): Workout? =
		workoutDao.getByIdWithExercises(id)?.toDomain()

	suspend fun insert(workout: Workout): Long =
		workoutDao.insert(workout.toEntity())

	suspend fun update(workout: Workout) =
		workoutDao.update(workout.toEntity())

	suspend fun delete(workout: Workout) =
		workoutDao.delete(workout.toEntity())

	suspend fun deleteById(id: Long) =
		workoutDao.deleteById(id)

	suspend fun saveFullWorkout(workout: Workout): Long = withContext(Dispatchers.IO) {
		database.withTransaction {
			val workoutId = workoutDao.insert(workout.toEntity())
			for (exercise in workout.exercises) {
				val weEntity = exercise.toEntity().copy(workoutId = workoutId)
				val workoutExerciseId = workoutExerciseDao.insert(weEntity)
				val setEntities = exercise.sets.map { set ->
					set.toEntity().copy(workoutExerciseId = workoutExerciseId)
				}
				workoutSetDao.insertAll(setEntities)
			}
			workoutId
		}
	}

	suspend fun updateFullWorkout(workout: Workout) = withContext(Dispatchers.IO) {
		database.withTransaction {
			workoutDao.update(workout.toEntity())
			workoutExerciseDao.deleteByWorkoutId(workout.id)
			for (exercise in workout.exercises) {
				val weEntity = exercise.toEntity().copy(workoutId = workout.id, id = 0)
				val workoutExerciseId = workoutExerciseDao.insert(weEntity)
				val setEntities = exercise.sets.map { set ->
					set.toEntity().copy(id = 0, workoutExerciseId = workoutExerciseId)
				}
				workoutSetDao.insertAll(setEntities)
			}
		}
	}

	suspend fun addExerciseToWorkout(workoutExercise: WorkoutExercise): Long =
		workoutExerciseDao.insert(workoutExercise.toEntity())

	/**
	 * Sets from the most recent previously-logged workout containing [exerciseId], used as the
	 * baseline for the active-workout set comparison. [excludeWorkoutId] skips the workout being
	 * resumed/edited. Empty when the exercise has never been logged before.
	 */
	suspend fun getLastPerformanceSets(exerciseId: Long, excludeWorkoutId: Long = 0L): List<WorkoutSet> =
		withContext(Dispatchers.IO) {
			workoutSetDao.getLastPerformanceSets(exerciseId, excludeWorkoutId).map { it.toDomain() }
		}

	/**
	 * Sets of the second most recent logged workout containing the exercise, used to distinguish a
	 * one-off drop from an ongoing regression. [excludeWorkoutId] skips the workout being
	 * resumed/edited. Empty when the exercise has fewer than two prior workouts.
	 */
	suspend fun getPreviousPerformanceSets(exerciseId: Long, excludeWorkoutId: Long = 0L): List<WorkoutSet> =
		withContext(Dispatchers.IO) {
			workoutSetDao.getPreviousPerformanceSets(exerciseId, excludeWorkoutId).map { it.toDomain() }
		}

	/** Full history of a specific set number for an exercise, newest workout first. */
	suspend fun getSetHistory(exerciseId: Long, setNumber: Int): List<SetHistoryEntry> =
		withContext(Dispatchers.IO) {
			workoutSetDao.getSetHistory(exerciseId, setNumber)
		}

	suspend fun addSetToExercise(set: WorkoutSet): Long =
		workoutSetDao.insert(set.toEntity())

	suspend fun deleteSet(set: WorkoutSet) =
		workoutSetDao.delete(set.toEntity())
}
