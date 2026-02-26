package net.luis.tracker.data.draft

import kotlinx.serialization.Serializable

@Serializable
data class WorkoutDraft(
	val startTimeMillis: Long,
	val elapsedMillis: Long,
	val entryIdCounter: Long,
	val exercises: List<DraftExerciseEntry>
)

@Serializable
data class DraftExerciseEntry(
	val entryId: Long,
	val exerciseId: Long,
	val title: String,
	val notes: String,
	val hasWeight: Boolean,
	val categoryId: Long? = null,
	val categoryName: String? = null,
	val sets: List<DraftWorkoutSet>,
	val isGhost: Boolean = false,
	val planWeightKg: Double = 0.0,
	val planSets: Int = 0,
	val planSetsData: List<DraftWorkoutSet> = emptyList()
)

@Serializable
data class DraftWorkoutSet(
	val setNumber: Int,
	val weightKg: Double,
	val reps: Int
)
