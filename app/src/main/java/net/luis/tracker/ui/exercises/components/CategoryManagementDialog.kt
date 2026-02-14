package net.luis.tracker.ui.exercises.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.luis.tracker.R
import net.luis.tracker.domain.model.Category

@Composable
fun CategoryManagementDialog(
	categories: List<Category>,
	onAdd: (String) -> Unit,
	onUpdate: (Category) -> Unit,
	onDelete: (Category) -> Unit,
	onDismiss: () -> Unit
) {
	var newCategoryName by remember { mutableStateOf("") }
	var editingCategoryId by remember { mutableStateOf<Long?>(null) }
	var editingCategoryName by remember { mutableStateOf("") }
	var categoryToDelete by remember { mutableStateOf<Category?>(null) }

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(stringResource(R.string.manage_categories)) },
		text = {
			Column(modifier = Modifier.fillMaxWidth()) {
				// Add new category row
				Row(
					modifier = Modifier.fillMaxWidth(),
					verticalAlignment = Alignment.CenterVertically
				) {
					OutlinedTextField(
						value = newCategoryName,
						onValueChange = { newCategoryName = it },
						label = { Text(stringResource(R.string.new_category)) },
						singleLine = true,
						modifier = Modifier.weight(1f)
					)
					Spacer(modifier = Modifier.width(8.dp))
					IconButton(
						onClick = {
							val trimmed = newCategoryName.trim()
							if (trimmed.isNotEmpty()) {
								onAdd(trimmed)
								newCategoryName = ""
							}
						},
						enabled = newCategoryName.trim().isNotEmpty()
					) {
						Icon(
							imageVector = Icons.Default.Add,
							contentDescription = stringResource(R.string.add_category)
						)
					}
				}

				Spacer(modifier = Modifier.height(16.dp))

				if (categories.isEmpty()) {
					Text(
						text = stringResource(R.string.no_categories),
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				} else {
					LazyColumn {
						items(categories, key = { it.id }) { category ->
							if (editingCategoryId == category.id) {
								// Edit mode
								Row(
									modifier = Modifier
										.fillMaxWidth()
										.padding(vertical = 4.dp),
									verticalAlignment = Alignment.CenterVertically
								) {
									OutlinedTextField(
										value = editingCategoryName,
										onValueChange = { editingCategoryName = it },
										singleLine = true,
										modifier = Modifier.weight(1f)
									)
									IconButton(
										onClick = {
											val trimmed = editingCategoryName.trim()
											if (trimmed.isNotEmpty()) {
												onUpdate(category.copy(name = trimmed))
												editingCategoryId = null
												editingCategoryName = ""
											}
										},
										enabled = editingCategoryName.trim().isNotEmpty()
									) {
										Icon(
											imageVector = Icons.Default.Check,
											contentDescription = stringResource(R.string.save)
										)
									}
									IconButton(
										onClick = {
											editingCategoryId = null
											editingCategoryName = ""
										}
									) {
										Icon(
											imageVector = Icons.Default.Close,
											contentDescription = stringResource(R.string.cancel)
										)
									}
								}
							} else {
								// Display mode
								Row(
									modifier = Modifier
										.fillMaxWidth()
										.padding(vertical = 4.dp),
									verticalAlignment = Alignment.CenterVertically,
									horizontalArrangement = Arrangement.SpaceBetween
								) {
									Text(
										text = category.name,
										style = MaterialTheme.typography.bodyLarge,
										modifier = Modifier.weight(1f)
									)
									IconButton(
										onClick = {
											editingCategoryId = category.id
											editingCategoryName = category.name
										}
									) {
										Icon(
											imageVector = Icons.Default.Edit,
											contentDescription = stringResource(R.string.edit)
										)
									}
									IconButton(
										onClick = { categoryToDelete = category }
									) {
										Icon(
											imageVector = Icons.Default.Delete,
											contentDescription = stringResource(R.string.delete)
										)
									}
								}
							}
							HorizontalDivider()
						}
					}
				}
			}
		},
		confirmButton = {
			TextButton(onClick = onDismiss) {
				Text(stringResource(R.string.close))
			}
		}
	)

	// Confirm delete category dialog
	categoryToDelete?.let { category ->
		AlertDialog(
			onDismissRequest = { categoryToDelete = null },
			title = { Text(stringResource(R.string.delete_category)) },
			text = { Text(stringResource(R.string.delete_category_confirmation, category.name)) },
			confirmButton = {
				TextButton(
					onClick = {
						onDelete(category)
						categoryToDelete = null
					}
				) {
					Text(stringResource(R.string.delete))
				}
			},
			dismissButton = {
				TextButton(onClick = { categoryToDelete = null }) {
					Text(stringResource(R.string.cancel))
				}
			}
		)
	}
}
