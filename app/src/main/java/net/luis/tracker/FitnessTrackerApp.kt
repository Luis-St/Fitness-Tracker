package net.luis.tracker

import android.app.Application
import androidx.room.Room
import net.luis.tracker.data.local.AppDatabase

class FitnessTrackerApp : Application() {

	val database: AppDatabase by lazy {
		Room.databaseBuilder(
			applicationContext,
			AppDatabase::class.java,
			"fitness_tracker_db"
		).build()
	}
}
