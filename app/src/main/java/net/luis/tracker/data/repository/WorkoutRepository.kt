package net.luis.tracker.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.luis.tracker.data.local.dao.WorkoutDao
import net.luis.tracker.data.local.dao.WorkoutExerciseDao
import net.luis.tracker.data.local.dao.WorkoutSetDao
import net.luis.tracker.domain.mapper.toDomain
import net.luis.tracker.domain.mapper.toEntity
import net.luis.tracker.domain.model.Workout
import net.luis.tracker.domain.model.WorkoutExercise
import net.luis.tracker.domain.model.WorkoutSet

class WorkoutRepository(
	private val workoutDao: WorkoutDao,
	private val workoutExerciseDao: WorkoutExerciseDao,
	private val workoutSetDao: WorkoutSetDao
) {

	fun getAllWithExercises(): Flow<List<Workout>> =
		workoutDao.getAllWithExercises().map { list -> list.map { it.toDomain() } }

	suspend fun getByIdWithExercises(id: Long): Workout? =
		workoutDao.getByIdWithExercises(id)?.toDomain()

	suspend fun insert(workout: Workout): Long =
		workoutDao.insert(workout.toEntity())

	suspend fun update(workout: Workout) =
		workoutDao.update(workout.toEntity())

	suspend fun delete(workout: Workout) =
		workoutDao.delete(workout.toEntity())

	suspend fun saveFullWorkout(workout: Workout): Long {
		val workoutId = workoutDao.insert(workout.toEntity())
		for (exercise in workout.exercises) {
			val weEntity = exercise.toEntity().copy(workoutId = workoutId)
			val workoutExerciseId = workoutExerciseDao.insert(weEntity)
			val setEntities = exercise.sets.map { set ->
				set.toEntity().copy(workoutExerciseId = workoutExerciseId)
			}
			workoutSetDao.insertAll(setEntities)
		}
		return workoutId
	}

	suspend fun updateFullWorkout(workout: Workout) {
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

	suspend fun addExerciseToWorkout(workoutExercise: WorkoutExercise): Long =
		workoutExerciseDao.insert(workoutExercise.toEntity())

	suspend fun addSetToExercise(set: WorkoutSet): Long =
		workoutSetDao.insert(set.toEntity())

	suspend fun deleteSet(set: WorkoutSet) =
		workoutSetDao.delete(set.toEntity())
}
