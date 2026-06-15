package net.luis.tracker.domain.model

data class Exercise(
	val id: Long = 0,
	val title: String,
	val notes: String = "",
	val hasWeight: Boolean = true,
	val allowsZeroWeight: Boolean = false,
	val categories: List<Category> = emptyList(),
	val isDeleted: Boolean = false
)
