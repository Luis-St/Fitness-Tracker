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
import net.luis.tracker.data.local.entity.ExerciseCategoryCrossRef
import net.luis.tracker.data.local.entity.ExerciseEntity
import net.luis.tracker.data.local.entity.WorkoutEntity
import net.luis.tracker.data.local.entity.WorkoutExerciseEntity
import net.luis.tracker.data.local.entity.WorkoutSetEntity

@Database(
	entities = [
		CategoryEntity::class,
		ExerciseEntity::class,
		ExerciseCategoryCrossRef::class,
		WorkoutEntity::class,
		WorkoutExerciseEntity::class,
		WorkoutSetEntity::class
	],
	version = 5,
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
		val MIGRATION_4_5 = object : Migration(4, 5) {
			override fun migrate(db: SupportSQLiteDatabase) {
				// 1. New allowsZeroWeight flag on exercises
				db.execSQL("ALTER TABLE exercises ADD COLUMN allowsZeroWeight INTEGER NOT NULL DEFAULT 0")

				// 2. Many-to-many join table for exercise categories
				db.execSQL(
					"CREATE TABLE IF NOT EXISTS `exercise_categories` (" +
						"`exerciseId` INTEGER NOT NULL, `categoryId` INTEGER NOT NULL, " +
						"PRIMARY KEY(`exerciseId`, `categoryId`), " +
						"FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
						"FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
				)
				db.execSQL(
					"CREATE INDEX IF NOT EXISTS `index_exercise_categories_categoryId` " +
						"ON `exercise_categories` (`categoryId`)"
				)

				// 3. Migrate existing single-category assignments into the join table
				db.execSQL(
					"INSERT INTO exercise_categories(exerciseId, categoryId) " +
						"SELECT id, categoryId FROM exercises WHERE categoryId IS NOT NULL"
				)

				// 4. Recreate exercises without categoryId (SQLite can't DROP an indexed/FK column)
				db.execSQL(
					"CREATE TABLE IF NOT EXISTS `exercises_new` (" +
						"`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
						"`title` TEXT NOT NULL, `notes` TEXT NOT NULL, " +
						"`hasWeight` INTEGER NOT NULL, `allowsZeroWeight` INTEGER NOT NULL, " +
						"`isDeleted` INTEGER NOT NULL)"
				)
				db.execSQL(
					"INSERT INTO exercises_new (id, title, notes, hasWeight, allowsZeroWeight, isDeleted) " +
						"SELECT id, title, notes, hasWeight, allowsZeroWeight, isDeleted FROM exercises"
				)
				db.execSQL("DROP TABLE exercises")
				db.execSQL("ALTER TABLE exercises_new RENAME TO exercises")
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
