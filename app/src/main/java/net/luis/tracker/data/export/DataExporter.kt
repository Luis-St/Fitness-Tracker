package net.luis.tracker.data.export

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.luis.tracker.data.local.AppDatabase
import java.io.OutputStream

@Serializable
data class ExportData(
	val categories: List<ExportCategory>,
	val exercises: List<ExportExercise>,
	val workouts: List<ExportWorkout>
)

@Serializable
data class ExportCategory(
	val id: Long,
	val name: String
)

@Serializable
data class ExportExercise(
	val id: Long,
	val title: String,
	val notes: String,
	val hasWeight: Boolean,
	val categoryId: Long?,
	val isDeleted: Boolean
)

@Serializable
data class ExportWorkout(
	val id: Long,
	val startTime: Long,
	val endTime: Long?,
	val durationSeconds: Long,
	val notes: String,
	val exercises: List<ExportWorkoutExercise>
)

@Serializable
data class ExportWorkoutExercise(
	val exerciseId: Long,
	val orderIndex: Int,
	val sets: List<ExportWorkoutSet>
)

@Serializable
data class ExportWorkoutSet(
	val setNumber: Int,
	val weightKg: Double,
	val reps: Int
)

object DataExporter {

	private val json = Json {
		prettyPrint = true
		encodeDefaults = true
	}

	suspend fun export(database: AppDatabase, outputStream: OutputStream) {
		val categoryDao = database.categoryDao()
		val exerciseDao = database.exerciseDao()
		val workoutDao = database.workoutDao()
		val workoutExerciseDao = database.workoutExerciseDao()
		val workoutSetDao = database.workoutSetDao()

		// Export all categories
		val categories = categoryDao.getAllOnce().map { entity ->
			ExportCategory(
				id = entity.id,
				name = entity.name
			)
		}

		// Export all exercises (including soft-deleted)
		val exercises = exerciseDao.getAllIncludingDeleted().map { entity ->
			ExportExercise(
				id = entity.id,
				title = entity.title,
				notes = entity.notes,
				hasWeight = entity.hasWeight,
				categoryId = entity.categoryId,
				isDeleted = entity.isDeleted
			)
		}

		// Export all workouts with their exercises and sets (bulk queries)
		val workoutEntities = workoutDao.getAllOnce()
		val allWorkoutExercises = workoutExerciseDao.getAll()
		val allWorkoutSets = workoutSetDao.getAll()

		val exercisesByWorkout = allWorkoutExercises.groupBy { it.workoutId }
		val setsByExercise = allWorkoutSets.groupBy { it.workoutExerciseId }

		val workouts = workoutEntities.map { workoutEntity ->
			val workoutExercises = exercisesByWorkout[workoutEntity.id].orEmpty()
			val exportExercises = workoutExercises.map { weEntity ->
				val sets = setsByExercise[weEntity.id].orEmpty()
				ExportWorkoutExercise(
					exerciseId = weEntity.exerciseId,
					orderIndex = weEntity.orderIndex,
					sets = sets.map { setEntity ->
						ExportWorkoutSet(
							setNumber = setEntity.setNumber,
							weightKg = setEntity.weightKg,
							reps = setEntity.reps
						)
					}
				)
			}
			ExportWorkout(
				id = workoutEntity.id,
				startTime = workoutEntity.startTime,
				endTime = workoutEntity.endTime,
				durationSeconds = workoutEntity.durationSeconds,
				notes = workoutEntity.notes,
				exercises = exportExercises
			)
		}

		val exportData = ExportData(
			categories = categories,
			exercises = exercises,
			workouts = workouts
		)

		val jsonString = json.encodeToString(ExportData.serializer(), exportData)
		outputStream.bufferedWriter().use { writer ->
			writer.write(jsonString)
		}
	}
}
