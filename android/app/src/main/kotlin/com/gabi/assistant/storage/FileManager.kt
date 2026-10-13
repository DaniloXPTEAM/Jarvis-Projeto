package com.gabi.assistant.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class FileEntry(
    val name: String,
    val doc: DocumentFile,
    val isDirectory: Boolean,
    val size: Long,
    val mimeType: String?
)

/**
 * Gerenciador de arquivos real via Storage Access Framework — o usuário
 * escolhe uma pasta uma vez (ACTION_OPEN_DOCUMENT_TREE) e a Gabi navega,
 * abre, renomeia e apaga arquivos dentro dela sem precisar da permissão
 * MANAGE_EXTERNAL_STORAGE (restrita pela Play Store e nem necessária aqui).
 *
 * Substitui o antigo `FileHelper` (pacote com.javis.assistant, nunca migrado
 * pro rebranding — ficou em src/main/java, fora do sourceSet compilado).
 */
@Singleton
class FileManager @Inject constructor(@ApplicationContext private val context: Context) {

    /** Persiste a permissão de acesso à árvore escolhida entre reinícios do app. */
    fun persistTreePermission(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            // Alguns provedores (raro) não suportam permissão persistente — a pasta ainda
            // funciona nesta sessão, só não sobrevive a um reinício do app.
        }
    }

    fun openTree(treeUriString: String): DocumentFile? {
        if (treeUriString.isBlank()) return null
        return try {
            DocumentFile.fromTreeUri(context, Uri.parse(treeUriString))?.takeIf { it.canRead() }
        } catch (e: Exception) {
            null
        }
    }

    fun listEntries(folder: DocumentFile): List<FileEntry> =
        folder.listFiles()
            .mapNotNull { doc -> doc.name?.let { FileEntry(it, doc, doc.isDirectory, doc.length(), doc.type) } }
            .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))

    fun openFileIntent(entry: FileEntry): Intent =
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(entry.doc.uri, entry.mimeType?.takeIf { it.isNotBlank() } ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    fun delete(entry: FileEntry): Boolean = entry.doc.delete()

    fun rename(entry: FileEntry, newName: String): Boolean = entry.doc.renameTo(newName)
}
