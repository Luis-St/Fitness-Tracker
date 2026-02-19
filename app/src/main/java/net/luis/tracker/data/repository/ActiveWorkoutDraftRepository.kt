package net.luis.tracker.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import net.luis.tracker.data.draft.WorkoutDraft
import net.luis.tracker.domain.model.Workout
import net.luis.tracker.domain.model.WorkoutExercise
import net.luis.tracker.domain.model.WorkoutSet
import net.luis.tracker.domain.model.Category
import net.luis.tracker.domain.model.Exercise

private val Context.draftDataStore: DataStore<Preferences> by preferencesDataStore(
	name = "active_workout_draft"
)

class ActiveWorkoutDraftRepository(private val context: Context) {

	private val draftKey = stringPreferencesKey("draft_json")

	suspend fun saveDraft(draft: WorkoutDraft) {
		val json = withContext(Dispatchers.Default) { Json.encodeToString(draft) }
		context.draftDataStore.edit { it[draftKey] = json }
	}

	suspend fun loadDraft(): WorkoutDraft? = withContext(Dispatchers.IO) {
		try {
			val prefs = context.draftDataStore.data.first()
			val json = prefs[draftKey] ?: return@withContext null
			Json.decodeFromString<WorkoutDraft>(json)
		} catch (_: Exception) {
			null
		}
	}

	suspend fun hasDraft(): Boolean {
		return try {
			val prefs = context.draftDataStore.data.first()
			prefs[draftKey] != null
		} catch (_: Exception) {
			false
		}
	}

	suspend fun clearDraft() {
		context.draftDataStore.edit { it.remove(draftKey) }
	}

	suspend fun resolveDraftToWorkout(workoutRepository: WorkoutRepository): Long? {
		val draft = loadDraft() ?: return null

		val workout = withContext(Dispatchers.Default) {
			val exercises = draft.exercises
				.filter { it.sets.isNotEmpty() }
				.mapIndexed { index, entry ->
					WorkoutExercise(
						exercise = Exercise(
							id = entry.exerciseId,
							title = entry.title,
							notes = entry.notes,
							hasWeight = entry.hasWeight,
							category = if (entry.categoryId != null && entry.categoryName != null) {
								Category(id = entry.categoryId, name = entry.categoryName)
							} else {
								null
							}
						),
						orderIndex = index,
						sets = entry.sets.map { set ->
							WorkoutSet(
								setNumber = set.setNumber,
								weightKg = set.weightKg,
								reps = set.reps
							)
						}
					)
				}

			Workout(
				startTime = draft.startTimeMillis,
				endTime = System.currentTimeMillis(),
				durationSeconds = draft.elapsedMillis / 1000,
				isFinished = false,
				exercises = exercises
			)
		}

		val workoutId = workoutRepository.saveFullWorkout(workout)
		clearDraft()
		return workoutId
	}
}
