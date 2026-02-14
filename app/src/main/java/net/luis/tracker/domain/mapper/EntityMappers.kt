package net.luis.tracker.domain.mapper

import net.luis.tracker.data.local.entity.CategoryEntity
import net.luis.tracker.data.local.entity.ExerciseEntity
import net.luis.tracker.data.local.entity.WorkoutEntity
import net.luis.tracker.data.local.entity.WorkoutExerciseEntity
import net.luis.tracker.data.local.entity.WorkoutSetEntity
import net.luis.tracker.data.local.relation.ExerciseWithCategory
import net.luis.tracker.data.local.relation.WorkoutExerciseWithSets
import net.luis.tracker.data.local.relation.WorkoutWithExercises
import net.luis.tracker.domain.model.Category
import net.luis.tracker.domain.model.Exercise
import net.luis.tracker.domain.model.Workout
import net.luis.tracker.domain.model.WorkoutExercise
import net.luis.tracker.domain.model.WorkoutSet

// Category
fun CategoryEntity.toDomain() = Category(id = id, name = name)
fun Category.toEntity() = CategoryEntity(id = id, name = name)

// Exercise
fun ExerciseWithCategory.toDomain() = Exercise(
	id = exercise.id,
	title = exercise.title,
	notes = exercise.notes,
	hasWeight = exercise.hasWeight,
	category = category?.toDomain(),
	isDeleted = exercise.isDeleted
)

fun ExerciseEntity.toDomain(category: Category? = null) = Exercise(
	id = id,
	title = title,
	notes = notes,
	hasWeight = hasWeight,
	category = category,
	isDeleted = isDeleted
)

fun Exercise.toEntity() = ExerciseEntity(
	id = id,
	title = title,
	notes = notes,
	hasWeight = hasWeight,
	categoryId = category?.id,
	isDeleted = isDeleted
)

// WorkoutSet
fun WorkoutSetEntity.toDomain() = WorkoutSet(
	id = id,
	workoutExerciseId = workoutExerciseId,
	setNumber = setNumber,
	weightKg = weightKg,
	reps = reps
)

fun WorkoutSet.toEntity() = WorkoutSetEntity(
	id = id,
	workoutExerciseId = workoutExerciseId,
	setNumber = setNumber,
	weightKg = weightKg,
	reps = reps
)

// WorkoutExercise
fun WorkoutExerciseWithSets.toDomain() = WorkoutExercise(
	id = workoutExercise.id,
	workoutId = workoutExercise.workoutId,
	exercise = exercise.toDomain(),
	orderIndex = workoutExercise.orderIndex,
	sets = sets.map { it.toDomain() }.sortedBy { it.setNumber }
)

fun WorkoutExercise.toEntity() = WorkoutExerciseEntity(
	id = id,
	workoutId = workoutId,
	exerciseId = exercise.id,
	orderIndex = orderIndex
)

// Workout
fun WorkoutWithExercises.toDomain() = Workout(
	id = workout.id,
	startTime = workout.startTime,
	endTime = workout.endTime,
	durationSeconds = workout.durationSeconds,
	notes = workout.notes,
	exercises = exercises.map { it.toDomain() }.sortedBy { it.orderIndex }
)

fun Workout.toEntity() = WorkoutEntity(
	id = id,
	startTime = startTime,
	endTime = endTime,
	durationSeconds = durationSeconds,
	notes = notes
)
