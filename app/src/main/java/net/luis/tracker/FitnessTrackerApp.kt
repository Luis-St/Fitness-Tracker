package net.luis.tracker

import android.app.Application
import androidx.room.Room
import net.luis.tracker.data.local.AppDatabase

class FitnessTrackerApp : Application() {

	lateinit var database: AppDatabase
		private set

	override fun onCreate() {
		super.onCreate()
		database = Room.databaseBuilder(
			applicationContext,
			AppDatabase::class.java,
			"fitness_tracker_db"
		).build()
	}
}
