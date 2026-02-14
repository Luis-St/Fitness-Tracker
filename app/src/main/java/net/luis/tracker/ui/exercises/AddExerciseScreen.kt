package net.luis.tracker.ui.exercises

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.luis.tracker.FitnessTrackerApp
import net.luis.tracker.R
import net.luis.tracker.data.repository.CategoryRepository
import net.luis.tracker.data.repository.ExerciseRepository
import net.luis.tracker.domain.model.Category
import net.luis.tracker.domain.model.Exercise

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseScreen(
	app: FitnessTrackerApp,
	onNavigateBack: () -> Unit
) {
	val exerciseRepository = remember { ExerciseRepository(app.database.exerciseDao()) }
	val categoryRepository = remember { CategoryRepository(app.database.categoryDao()) }
	val scope = rememberCoroutineScope()

	val categories by categoryRepository.getAll().collectAsState(initial = emptyList())

	var title by remember { mutableStateOf("") }
	var notes by remember { mutableStateOf("") }
	var hasWeight by remember { mutableStateOf(true) }
	var selectedCategory by remember { mutableStateOf<Category?>(null) }
	var categoryExpanded by remember { mutableStateOf(false) }
	var titleError by remember { mutableStateOf(false) }

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.add_exercise)) },
				navigationIcon = {
					IconButton(onClick = onNavigateBack) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = stringResource(R.string.back)
						)
					}
				},
				actions = {
					TextButton(
						onClick = {
							val trimmedTitle = title.trim()
							if (trimmedTitle.isEmpty()) {
								titleError = true
								return@TextButton
							}
							scope.launch {
								exerciseRepository.insert(
									Exercise(
										title = trimmedTitle,
										notes = notes.trim(),
										hasWeight = hasWeight,
										category = selectedCategory
									)
								)
								onNavigateBack()
							}
						}
					) {
						Text(stringResource(R.string.save))
					}
				}
			)
		}
	) { innerPadding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(innerPadding)
				.padding(horizontal = 16.dp)
				.verticalScroll(rememberScrollState())
		) {
			Spacer(modifier = Modifier.height(8.dp))

			// Title field
			OutlinedTextField(
				value = title,
				onValueChange = {
					title = it
					if (it.isNotBlank()) titleError = false
				},
				label = { Text(stringResource(R.string.exercise_title)) },
				isError = titleError,
				supportingText = if (titleError) {
					{ Text(stringResource(R.string.field_required)) }
				} else null,
				singleLine = true,
				modifier = Modifier.fillMaxWidth()
			)

			Spacer(modifier = Modifier.height(16.dp))

			// Notes field
			OutlinedTextField(
				value = notes,
				onValueChange = { notes = it },
				label = { Text(stringResource(R.string.exercise_notes)) },
				minLines = 3,
				maxLines = 5,
				modifier = Modifier.fillMaxWidth()
			)

			Spacer(modifier = Modifier.height(16.dp))

			// Has weight switch
			androidx.compose.foundation.layout.Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
			) {
				Text(
					text = stringResource(R.string.has_weight),
					style = MaterialTheme.typography.bodyLarge,
					modifier = Modifier.weight(1f)
				)
				Switch(
					checked = hasWeight,
					onCheckedChange = { hasWeight = it }
				)
			}

			Spacer(modifier = Modifier.height(16.dp))

			// Category dropdown
			ExposedDropdownMenuBox(
				expanded = categoryExpanded,
				onExpandedChange = { categoryExpanded = it }
			) {
				OutlinedTextField(
					value = selectedCategory?.name ?: stringResource(R.string.no_category),
					onValueChange = {},
					readOnly = true,
					label = { Text(stringResource(R.string.category)) },
					trailingIcon = {
						ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
					},
					modifier = Modifier
						.fillMaxWidth()
						.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
				)
				ExposedDropdownMenu(
					expanded = categoryExpanded,
					onDismissRequest = { categoryExpanded = false }
				) {
					DropdownMenuItem(
						text = { Text(stringResource(R.string.no_category)) },
						onClick = {
							selectedCategory = null
							categoryExpanded = false
						}
					)
					categories.forEach { category ->
						DropdownMenuItem(
							text = { Text(category.name) },
							onClick = {
								selectedCategory = category
								categoryExpanded = false
							}
						)
					}
				}
			}

			Spacer(modifier = Modifier.height(24.dp))
		}
	}
}
