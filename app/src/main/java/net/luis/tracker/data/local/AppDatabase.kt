package net.luis.tracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import net.luis.tracker.data.local.dao.CategoryDao
import net.luis.tracker.data.local.dao.ExerciseDao
import net.luis.tracker.data.local.dao.StatsDao
import net.luis.tracker.data.local.dao.WorkoutDao
import net.luis.tracker.data.local.dao.WorkoutExerciseDao
import net.luis.tracker.data.local.dao.WorkoutSetDao
import net.luis.tracker.data.local.entity.CategoryEntity
import net.luis.tracker.data.local.entity.ExerciseEntity
import net.luis.tracker.data.local.entity.WorkoutEntity
import net.luis.tracker.data.local.entity.WorkoutExerciseEntity
import net.luis.tracker.data.local.entity.WorkoutSetEntity

@Database(
	entities = [
		CategoryEntity::class,
		ExerciseEntity::class,
		WorkoutEntity::class,
		WorkoutExerciseEntity::class,
		WorkoutSetEntity::class
	],
	version = 1,
	exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
	abstract fun categoryDao(): CategoryDao
	abstract fun exerciseDao(): ExerciseDao
	abstract fun workoutDao(): WorkoutDao
	abstract fun workoutExerciseDao(): WorkoutExerciseDao
	abstract fun workoutSetDao(): WorkoutSetDao
	abstract fun statsDao(): StatsDao
}
