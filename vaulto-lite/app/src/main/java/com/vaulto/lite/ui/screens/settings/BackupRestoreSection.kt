package com.vaulto.lite.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.vaulto.lite.ui.MainViewModel
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Backup/Restore rows for Settings > Data, using Storage Access Framework
 * document pickers so no extra storage permission is required (works on
 * scoped storage, fully offline).
 */
@Composable
fun BackupRestoreSection(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showBackupConfirm by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = viewModel.exportBackupJson()
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        OutputStreamWriter(stream).use { it.write(json) }
                    }
                    statusMessage = "Backup saved"
                } catch (e: Exception) {
                    statusMessage = "Backup failed: ${e.message}"
                }
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
            showRestoreConfirm = true
        }
    }

    ListItem(
        headlineContent = { Text("Backup to JSON") },
        supportingContent = { Text("Save all categories, expenses, and budgets to a file") },
        modifier = Modifier.clickable { showBackupConfirm = true }
    )
    ListItem(
        headlineContent = { Text("Restore from JSON") },
        supportingContent = { Text("Add categories and expenses from a previously saved backup") },
        modifier = Modifier.clickable {
            openDocumentLauncher.launch(arrayOf("application/json"))
        }
    )

    statusMessage?.let { message ->
        ListItem(
            headlineContent = { Text(message) },
            colors = ListItemDefaults.colors(headlineColor = MaterialTheme.colorScheme.primary)
        )
    }

    if (showBackupConfirm) {
        AlertDialog(
            onDismissRequest = { showBackupConfirm = false },
            title = { Text("Back up your data?") },
            text = { Text("This will save a JSON file containing all your categories, expenses, and budgets.") },
            confirmButton = {
                TextButton(onClick = {
                    showBackupConfirm = false
                    val filename = "vaulto_backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.json"
                    createDocumentLauncher.launch(filename)
                }) { Text("Back up") }
            },
            dismissButton = {
                TextButton(onClick = { showBackupConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restore from backup?") },
            text = { Text("This will add categories and expenses from the backup file. Existing data with the same names won't be duplicated.") },
            confirmButton = {
                TextButton(onClick = {
                    val uri = pendingRestoreUri
                    showRestoreConfirm = false
                    if (uri != null) {
                        scope.launch {
                            try {
                                val json = context.contentResolver.openInputStream(uri)
                                    ?.bufferedReader()
                                    ?.use { it.readText() }
                                    ?: throw IllegalStateException("Could not read file")
                                viewModel.restoreFromBackupJson(json)
                                statusMessage = "Restore complete"
                            } catch (e: Exception) {
                                statusMessage = "Restore failed: ${e.message}"
                            }
                        }
                    }
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
