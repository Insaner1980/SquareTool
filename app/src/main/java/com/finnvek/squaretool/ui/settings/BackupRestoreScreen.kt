package com.finnvek.squaretool.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.finnvek.squaretool.R
import com.finnvek.squaretool.backup.BackupService
import com.finnvek.squaretool.backup.SquareToolBackupDto
import com.finnvek.squaretool.ui.theme.SquareToolSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    service: BackupService,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var pendingRestore by remember { mutableStateOf<SquareToolBackupDto?>(null) }
    val backupCreated = stringResource(R.string.backup_created)
    val backupFailed = stringResource(R.string.backup_failed, "%s")
    val restoreFailed = stringResource(R.string.restore_failed, "%s")
    val restoreComplete = stringResource(R.string.restore_complete)

    val createBackup =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) {
                scope.launch {
                    runCatching {
                        val json = withContext(Dispatchers.IO) { service.createJson() }
                        withContext(Dispatchers.IO) {
                            requireNotNull(context.contentResolver.openOutputStream(uri)).bufferedWriter().use { it.write(json) }
                        }
                    }.onSuccess { snackbar.showSnackbar(backupCreated) }
                        .onFailure { snackbar.showSnackbar(backupFailed.format(it.message ?: it::class.simpleName.orEmpty())) }
                }
            }
        }
    val openBackup =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                scope.launch {
                    runCatching {
                        val json =
                            withContext(Dispatchers.IO) {
                                requireNotNull(context.contentResolver.openInputStream(uri)).bufferedReader().use { it.readText() }
                            }
                        withContext(Dispatchers.Default) { service.decodeAndValidate(json) }
                    }.onSuccess { pendingRestore = it }
                        .onFailure { snackbar.showSnackbar(restoreFailed.format(it.message ?: it::class.simpleName.orEmpty())) }
                }
            }
        }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_and_restore)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                    ) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back)) }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(SquareToolSpacing.Standard),
            verticalArrangement = Arrangement.spacedBy(SquareToolSpacing.Standard),
        ) {
            Text(stringResource(R.string.backup_explanation))
            Button(
                onClick = { createBackup.launch("squaretool_backup_${LocalDate.now()}.json") },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.export_backup_file)) }
            OutlinedButton(
                onClick = { openBackup.launch(arrayOf("application/json", "text/json", "text/plain")) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.choose_backup_file)) }
        }
    }

    pendingRestore?.let { backup ->
        val summary = service.summary(backup)
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text(stringResource(R.string.restore_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.restore_confirm_message,
                        summary.projectCount,
                        summary.squareDesignCount,
                        summary.colorCount,
                        summary.paletteCount,
                        summary.cellCount,
                    ),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingRestore = null
                        scope.launch {
                            runCatching { service.restore(backup) }
                                .onSuccess { snackbar.showSnackbar(restoreComplete) }
                                .onFailure { snackbar.showSnackbar(restoreFailed.format(it.message ?: it::class.simpleName.orEmpty())) }
                        }
                    },
                ) { Text(stringResource(R.string.restore_now)) }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
