package net.luis.tracker.domain.model

data class Exercise(
	val id: Long = 0,
	val title: String,
	val notes: String = "",
	val hasWeight: Boolean = true,
	val category: Category? = null,
	val isDeleted: Boolean = false
)
