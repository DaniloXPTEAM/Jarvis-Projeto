package com.gabi.assistant.ui.viewmodel

import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabi.assistant.storage.FileEntry
import com.gabi.assistant.storage.FileManager
import com.gabi.assistant.storage.JavisPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class FilesUiState(
    val hasFolder: Boolean = false,
    val breadcrumb: List<String> = emptyList(),
    val entries: List<FileEntry> = emptyList(),
    val loading: Boolean = true,
    val message: String? = null
)

@HiltViewModel
class FilesViewModel @Inject constructor(
    private val fileManager: FileManager,
    private val prefs: JavisPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(FilesUiState())
    val uiState: StateFlow<FilesUiState> = _uiState.asStateFlow()

    // Pilha de pastas do caminho atual: [raiz escolhida, ..., pasta aberta agora].
    private val folderStack = mutableListOf<DocumentFile>()

    init {
        viewModelScope.launch {
            val saved = prefs.filesTreeUri.first()
            val tree = withContext(Dispatchers.IO) { fileManager.openTree(saved) }
            if (tree != null) {
                folderStack.clear()
                folderStack.add(tree)
                refresh()
            } else {
                _uiState.update { it.copy(hasFolder = false, loading = false) }
            }
        }
    }

    /** Chamado depois que o usuário escolhe uma pasta pelo seletor do sistema. */
    fun onFolderPicked(uri: Uri) {
        fileManager.persistTreePermission(uri)
        viewModelScope.launch {
            prefs.setFilesTreeUri(uri.toString())
            val tree = withContext(Dispatchers.IO) { fileManager.openTree(uri.toString()) }
            folderStack.clear()
            if (tree != null) folderStack.add(tree)
            refresh()
        }
    }

    /** Pasta → navega pra dentro dela. Arquivo → chama [onOpenFile] com o intent de abrir. */
    fun openEntry(entry: FileEntry, onOpenFile: (FileEntry) -> Unit) {
        if (entry.isDirectory) {
            folderStack.add(entry.doc)
            viewModelScope.launch { refresh() }
        } else {
            onOpenFile(entry)
        }
    }

    /** true se subiu um nível; false se já estava na raiz (nada a fazer). */
    fun goBack(): Boolean {
        if (folderStack.size <= 1) return false
        folderStack.removeAt(folderStack.lastIndex)
        viewModelScope.launch { refresh() }
        return true
    }

    fun delete(entry: FileEntry) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { fileManager.delete(entry) }
            _uiState.update { it.copy(message = if (ok) "Apagado: ${entry.name}" else "Não consegui apagar ${entry.name}") }
            refresh()
        }
    }

    fun rename(entry: FileEntry, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { fileManager.rename(entry, newName) }
            _uiState.update { it.copy(message = if (ok) "Renomeado para $newName" else "Não consegui renomear") }
            refresh()
        }
    }

    fun intentFor(entry: FileEntry): Intent = fileManager.openFileIntent(entry)

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private suspend fun refresh() {
        val current = folderStack.lastOrNull()
        if (current == null) {
            _uiState.update { it.copy(hasFolder = false, loading = false, entries = emptyList(), breadcrumb = emptyList()) }
            return
        }
        _uiState.update { it.copy(loading = true) }
        val entries = withContext(Dispatchers.IO) { fileManager.listEntries(current) }
        _uiState.update {
            it.copy(
                hasFolder = true,
                loading = false,
                entries = entries,
                breadcrumb = folderStack.mapNotNull { doc -> doc.name }
            )
        }
    }
}
