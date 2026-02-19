package net.luis.tracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workouts")
data class WorkoutEntity(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val startTime: Long, // epoch millis
	val endTime: Long? = null, // epoch millis
	val durationSeconds: Long = 0,
	val notes: String = "",
	val isFinished: Boolean = true
)
