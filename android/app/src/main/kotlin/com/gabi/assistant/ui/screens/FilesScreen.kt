package com.gabi.assistant.ui.screens

import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.gabi.assistant.storage.FileEntry
import com.gabi.assistant.ui.components.JavisBottomBar
import com.gabi.assistant.ui.theme.*
import com.gabi.assistant.ui.viewmodel.FilesViewModel

@Composable
fun FilesScreen(
    navController: NavController,
    viewModel: FilesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var entryToRename by remember { mutableStateOf<FileEntry?>(null) }
    var entryToDelete by remember { mutableStateOf<FileEntry?>(null) }

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { viewModel.onFolderPicked(it) }
    }

    BackHandler(enabled = state.breadcrumb.size > 1) { viewModel.goBack() }

    Scaffold(
        bottomBar = { JavisBottomBar(navController) },
        containerColor = DarkBg,
        snackbarHost = {
            state.message?.let { msg ->
                Snackbar(
                    action = { TextButton(onClick = viewModel::dismissMessage) { Text("OK", color = CyanAccent) } },
                    containerColor = SurfaceMid
                ) { Text(msg, color = TextPrimary) }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.breadcrumb.size > 1) {
                    IconButton(onClick = { viewModel.goBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = CyanAccent)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text("Arquivos", style = MaterialTheme.typography.headlineMedium, color = CyanAccent)
                    if (state.breadcrumb.isNotEmpty()) {
                        Text(
                            state.breadcrumb.joinToString(" / "),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                }
                if (state.hasFolder) {
                    TextButton(onClick = { pickFolder.launch(null) }) {
                        Text("Trocar pasta", color = TextSecondary)
                    }
                }
            }

            when {
                state.loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CyanAccent)
                    }
                }
                !state.hasFolder -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FolderOpen, null, tint = TextSecondary, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Nenhuma pasta escolhida", color = TextSecondary)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Escolha uma pasta (ex: Downloads) pra Gabi navegar, abrir e organizar seus arquivos.",
                                color = TextSecondary.copy(0.7f),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { pickFolder.launch(null) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                            ) {
                                Icon(Icons.Default.Folder, null, tint = DarkBg)
                                Spacer(Modifier.width(8.dp))
                                Text("Escolher pasta", color = DarkBg, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                state.entries.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FolderOpen, null, tint = TextSecondary, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Pasta vazia", color = TextSecondary)
                        }
                    }
                }
                else -> {
                    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                        items(state.entries, key = { it.doc.uri.toString() }) { entry ->
                            FileRow(
                                entry = entry,
                                onClick = {
                                    viewModel.openEntry(entry) { fileEntry ->
                                        try {
                                            context.startActivity(viewModel.intentFor(fileEntry))
                                        } catch (e: ActivityNotFoundException) {
                                            // nenhum app instalado sabe abrir esse tipo de arquivo
                                        }
                                    }
                                },
                                onRename = { entryToRename = entry },
                                onDelete = { entryToDelete = entry }
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }

    entryToRename?.let { entry ->
        RenameDialog(
            currentName = entry.name,
            onConfirm = { newName -> viewModel.rename(entry, newName); entryToRename = null },
            onDismiss = { entryToRename = null }
        )
    }

    entryToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("Apagar \"${entry.name}\"?", color = TextPrimary) },
            text = { Text(if (entry.isDirectory) "Isso apaga a pasta e tudo dentro dela." else "Essa ação não pode ser desfeita.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(entry); entryToDelete = null }) {
                    Text("Apagar", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) { Text("Cancelar", color = TextSecondary) }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
private fun FileRow(
    entry: FileEntry,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(iconFor(entry), null, tint = if (entry.isDirectory) CyanAccent else BlueAccent, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.name, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1)
            if (!entry.isDirectory) {
                Text(formatSize(entry.size), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Default.MoreVert, null, tint = TextSecondary)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("Renomear") }, onClick = { menuOpen = false; onRename() })
                DropdownMenuItem(text = { Text("Apagar", color = ErrorRed) }, onClick = { menuOpen = false; onDelete() })
            }
        }
    }
}

@Composable
private fun RenameDialog(currentName: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renomear", color = TextPrimary) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanAccent.copy(0.5f),
                    unfocusedBorderColor = TextSecondary.copy(0.2f),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = CyanAccent
                )
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("Renomear", color = CyanAccent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = TextSecondary) } },
        containerColor = SurfaceDark
    )
}

private fun iconFor(entry: FileEntry): androidx.compose.ui.graphics.vector.ImageVector = when {
    entry.isDirectory -> Icons.Default.Folder
    entry.mimeType?.startsWith("image/") == true -> Icons.Default.Image
    entry.mimeType?.startsWith("video/") == true -> Icons.Default.VideoFile
    entry.mimeType?.startsWith("audio/") == true -> Icons.Default.AudioFile
    entry.mimeType == "application/pdf" -> Icons.Default.PictureAsPdf
    else -> Icons.Default.InsertDriveFile
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.0f KB".format(bytes / 1024.0)
    bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    else -> "%.1f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
}
