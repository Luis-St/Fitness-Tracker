package net.luis.tracker.ui.activeworkout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.luis.tracker.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectExerciseScreen(
	viewModel: ActiveWorkoutViewModel,
	onExerciseSelected: (entryId: Long) -> Unit,
	onNavigateBack: () -> Unit
) {
	val availableExercises by viewModel.availableExercises.collectAsStateWithLifecycle()
	var searchQuery by remember { mutableStateOf("") }

	val filtered = remember(availableExercises, searchQuery) {
		val exercises = availableExercises ?: return@remember emptyList()
		if (searchQuery.isBlank()) exercises
		else exercises.filter { it.title.contains(searchQuery, ignoreCase = true) }
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.select_exercise)) },
				navigationIcon = {
					IconButton(onClick = onNavigateBack) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = stringResource(R.string.back)
						)
					}
				}
			)
		}
	) { innerPadding ->
		if (availableExercises == null) {
			Box(
				modifier = Modifier
					.fillMaxSize()
					.padding(innerPadding),
				contentAlignment = Alignment.Center
			) {
				CircularProgressIndicator()
			}
		} else {
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.padding(innerPadding)
					.padding(horizontal = 16.dp)
			) {
				OutlinedTextField(
					value = searchQuery,
					onValueChange = { searchQuery = it },
					placeholder = { Text(stringResource(R.string.search_exercises)) },
					leadingIcon = {
						Icon(Icons.Default.Search, contentDescription = null)
					},
					modifier = Modifier.fillMaxWidth(),
					singleLine = true
				)
				Spacer(modifier = Modifier.height(8.dp))
				LazyColumn {
					items(filtered, key = { it.id }) { exercise ->
						ListItem(
							headlineContent = { Text(exercise.title) },
							supportingContent = exercise.category?.let {
								{ Text(it.name) }
							},
							modifier = Modifier.fillMaxWidth(),
							trailingContent = {
								IconButton(onClick = {
									val entryId = viewModel.addExercise(exercise)
									onExerciseSelected(entryId)
								}) {
									Icon(Icons.Default.Add, contentDescription = null)
								}
							}
						)
						HorizontalDivider()
					}
				}
			}
		}
	}
}
