package net.luis.tracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val title: String,
	val notes: String = "",
	val hasWeight: Boolean = true,
	val allowsZeroWeight: Boolean = false,
	val isDeleted: Boolean = false
)
