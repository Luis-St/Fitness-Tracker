package net.luis.tracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
	tableName = "exercises",
	foreignKeys = [
		ForeignKey(
			entity = CategoryEntity::class,
			parentColumns = ["id"],
			childColumns = ["categoryId"],
			onDelete = ForeignKey.SET_NULL
		)
	],
	indices = [Index("categoryId")]
)
data class ExerciseEntity(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val title: String,
	val notes: String = "",
	val hasWeight: Boolean = true,
	val categoryId: Long? = null,
	val isDeleted: Boolean = false
)
