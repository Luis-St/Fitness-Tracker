package net.luis.tracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
	version = 4,
	exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

	companion object {
		val MIGRATION_1_2 = object : Migration(1, 2) {
			override fun migrate(db: SupportSQLiteDatabase) {
				db.execSQL("ALTER TABLE workouts ADD COLUMN isFinished INTEGER NOT NULL DEFAULT 1")
			}
		}
		val MIGRATION_2_3 = object : Migration(2, 3) {
			override fun migrate(db: SupportSQLiteDatabase) {
				db.execSQL("ALTER TABLE workout_sets ADD COLUMN dropWeightKg REAL")
				db.execSQL("ALTER TABLE workout_sets ADD COLUMN dropReps INTEGER")
			}
		}
		val MIGRATION_3_4 = object : Migration(3, 4) {
			override fun migrate(db: SupportSQLiteDatabase) {
				db.execSQL("ALTER TABLE workouts ADD COLUMN planWorkoutId INTEGER")
			}
		}
	}

	abstract fun categoryDao(): CategoryDao
	abstract fun exerciseDao(): ExerciseDao
	abstract fun workoutDao(): WorkoutDao
	abstract fun workoutExerciseDao(): WorkoutExerciseDao
	abstract fun workoutSetDao(): WorkoutSetDao
	abstract fun statsDao(): StatsDao
}
