package net.luis.tracker.data.export

import kotlinx.serialization.json.Json
import net.luis.tracker.data.local.AppDatabase
import net.luis.tracker.data.local.entity.CategoryEntity
import net.luis.tracker.data.local.entity.ExerciseEntity
import net.luis.tracker.data.local.entity.WorkoutEntity
import net.luis.tracker.data.local.entity.WorkoutExerciseEntity
import net.luis.tracker.data.local.entity.WorkoutSetEntity
import java.io.InputStream

object DataImporter {

	private val json = Json {
		ignoreUnknownKeys = true
		coerceInputValues = true
	}

	suspend fun `import`(database: AppDatabase, inputStream: InputStream) {
		val jsonString = inputStream.bufferedReader().use { it.readText() }
		val exportData = json.decodeFromString(ExportData.serializer(), jsonString)

		val categoryDao = database.categoryDao()
		val exerciseDao = database.exerciseDao()
		val workoutDao = database.workoutDao()
		val workoutExerciseDao = database.workoutExerciseDao()
		val workoutSetDao = database.workoutSetDao()

		// Clear all existing data before importing
		database.clearAllTables()

		// Maps to track old ID -> new ID relationships
		val categoryIdMap = mutableMapOf<Long, Long>()
		val exerciseIdMap = mutableMapOf<Long, Long>()

		// Insert categories and build ID map
		for (category in exportData.categories) {
			val newId = categoryDao.insert(
				CategoryEntity(
					name = category.name
				)
			)
			categoryIdMap[category.id] = newId
		}

		// Insert exercises and build ID map
		for (exercise in exportData.exercises) {
			val mappedCategoryId = exercise.categoryId?.let { categoryIdMap[it] }
			val newId = exerciseDao.insert(
				ExerciseEntity(
					title = exercise.title,
					notes = exercise.notes,
					hasWeight = exercise.hasWeight,
					categoryId = mappedCategoryId,
					isDeleted = exercise.isDeleted
				)
			)
			exerciseIdMap[exercise.id] = newId
		}

		// Insert workouts with their exercises and sets
		for (workout in exportData.workouts) {
			val newWorkoutId = workoutDao.insert(
				WorkoutEntity(
					startTime = workout.startTime,
					endTime = workout.endTime,
					durationSeconds = workout.durationSeconds,
					notes = workout.notes
				)
			)

			for (workoutExercise in workout.exercises) {
				val mappedExerciseId = exerciseIdMap[workoutExercise.exerciseId]
					?: continue // Skip if exercise ID not found

				val newWeId = workoutExerciseDao.insert(
					WorkoutExerciseEntity(
						workoutId = newWorkoutId,
						exerciseId = mappedExerciseId,
						orderIndex = workoutExercise.orderIndex
					)
				)

				if (workoutExercise.sets.isNotEmpty()) {
					val setEntities = workoutExercise.sets.map { set ->
						WorkoutSetEntity(
							workoutExerciseId = newWeId,
							setNumber = set.setNumber,
							weightKg = set.weightKg,
							reps = set.reps
						)
					}
					workoutSetDao.insertAll(setEntities)
				}
			}
		}
	}
}
