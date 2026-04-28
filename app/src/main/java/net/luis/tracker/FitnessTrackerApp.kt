package net.luis.tracker

import android.app.Application
import androidx.room.Room
import kotlinx.coroutines.flow.MutableStateFlow
import net.luis.tracker.data.export.ExportData
import net.luis.tracker.data.local.AppDatabase

class FitnessTrackerApp : Application() {

	val importBackup: MutableStateFlow<ExportData?> = MutableStateFlow(null)

	val database: AppDatabase by lazy {
		Room.databaseBuilder(
			applicationContext,
			AppDatabase::class.java,
			"fitness_tracker_db"
		)
			.addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
			.build()
	}
}
