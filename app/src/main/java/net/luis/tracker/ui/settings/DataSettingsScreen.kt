package net.luis.tracker.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.luis.tracker.FitnessTrackerApp
import net.luis.tracker.R
import net.luis.tracker.data.export.DataExporter
import net.luis.tracker.data.export.DataImporter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataSettingsScreen(
	app: FitnessTrackerApp,
	importUri: String = "",
	onNavigateBack: () -> Unit
) {
	val context = LocalContext.current
	val scope = rememberCoroutineScope()
	val snackbarHostState = remember { SnackbarHostState() }

	val exportSuccessMsg = stringResource(R.string.export_success)
	val exportErrorMsg = stringResource(R.string.export_error)
	val importSuccessMsg = stringResource(R.string.import_success)
	val importErrorMsg = stringResource(R.string.import_error)
	val restoreSuccessMsg = stringResource(R.string.restore_success)
	val restoreErrorMsg = stringResource(R.string.restore_error)

	var pendingImportUriState by remember { mutableStateOf<Uri?>(null) }
	val hasBackup by app.importBackup.collectAsState()
	var showRestoreDialog by remember { mutableStateOf(false) }

	LaunchedEffect(importUri) {
		if (importUri.isNotEmpty()) {
			pendingImportUriState = Uri.parse(importUri)
		}
	}

	val exportLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.CreateDocument("application/json")
	) { uri: Uri? ->
		uri?.let {
			scope.launch {
				try {
					withContext(Dispatchers.IO) {
						context.contentResolver.openOutputStream(it)?.use { outputStream ->
							DataExporter.export(app.database, outputStream)
						}
					}
					snackbarHostState.showSnackbar(exportSuccessMsg)
				} catch (e: Exception) {
					snackbarHostState.showSnackbar(exportErrorMsg)
				}
			}
		}
	}

	val importLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.OpenDocument()
	) { uri: Uri? ->
		if (uri != null) pendingImportUriState = uri
	}

	pendingImportUriState?.let { uriToImport ->
		AlertDialog(
			onDismissRequest = { pendingImportUriState = null },
			title = { Text(stringResource(R.string.import_confirm_title)) },
			text = { Text(stringResource(R.string.import_confirm_message)) },
			confirmButton = {
				Button(
					onClick = {
						pendingImportUriState = null
						scope.launch {
							try {
								withContext(Dispatchers.IO) {
									app.importBackup.value = DataExporter.snapshot(app.database)
									context.contentResolver.openInputStream(uriToImport)?.use { inputStream ->
										DataImporter.`import`(app.database, inputStream)
									}
								}
								snackbarHostState.showSnackbar(importSuccessMsg)
							} catch (e: Exception) {
								app.importBackup.value = null
								snackbarHostState.showSnackbar(importErrorMsg)
							}
						}
					}
				) {
					Text(stringResource(R.string.import_data))
				}
			},
			dismissButton = {
				TextButton(onClick = { pendingImportUriState = null }) {
					Text(stringResource(R.string.cancel))
				}
			}
		)
	}

	if (showRestoreDialog) {
		AlertDialog(
			onDismissRequest = { showRestoreDialog = false },
			title = { Text(stringResource(R.string.restore_confirm_title)) },
			text = { Text(stringResource(R.string.restore_confirm_message)) },
			confirmButton = {
				Button(
					onClick = {
						showRestoreDialog = false
						scope.launch {
							try {
								withContext(Dispatchers.IO) {
									val backup = app.importBackup.value ?: return@withContext
									DataImporter.importFromData(app.database, backup)
								}
								app.importBackup.value = null
								snackbarHostState.showSnackbar(restoreSuccessMsg)
							} catch (e: Exception) {
								snackbarHostState.showSnackbar(restoreErrorMsg)
							}
						}
					}
				) {
					Text(stringResource(R.string.restore_data))
				}
			},
			dismissButton = {
				TextButton(onClick = { showRestoreDialog = false }) {
					Text(stringResource(R.string.cancel))
				}
			}
		)
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.data)) },
				navigationIcon = {
					IconButton(onClick = onNavigateBack) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = stringResource(R.string.back)
						)
					}
				}
			)
		},
		snackbarHost = { SnackbarHost(snackbarHostState) }
	) { innerPadding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(innerPadding)
				.verticalScroll(rememberScrollState())
				.padding(horizontal = 16.dp)
		) {
			Spacer(modifier = Modifier.height(8.dp))

			FilledTonalButton(
				onClick = { exportLauncher.launch("fitness_tracker_backup.json") },
				modifier = Modifier.fillMaxWidth()
			) {
				Icon(
					imageVector = Icons.Default.FileDownload,
					contentDescription = null,
					modifier = Modifier.padding(end = 8.dp)
				)
				Text(stringResource(R.string.export_data))
			}

			Spacer(modifier = Modifier.height(8.dp))

			FilledTonalButton(
				onClick = { importLauncher.launch(arrayOf("application/json")) },
				modifier = Modifier.fillMaxWidth()
			) {
				Icon(
					imageVector = Icons.Default.FileUpload,
					contentDescription = null,
					modifier = Modifier.padding(end = 8.dp)
				)
				Text(stringResource(R.string.import_data))
			}

			if (hasBackup != null) {
				Spacer(modifier = Modifier.height(8.dp))

				FilledTonalButton(
					onClick = { showRestoreDialog = true },
					modifier = Modifier.fillMaxWidth()
				) {
					Icon(
						imageVector = Icons.Default.Restore,
						contentDescription = null,
						modifier = Modifier.padding(end = 8.dp)
					)
					Text(stringResource(R.string.restore_data))
				}
			}

			Spacer(modifier = Modifier.height(24.dp))
		}
	}
}
