package minicla03.cashmate.data.platform

import android.content.Context
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import minicla03.cashmate.domain.model.UserDataExport
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Abstraction for preparing a downloaded [UserDataExport] so Android can share or open it.
 */
interface ExportFileHandler {
    suspend fun prepareShareableUri(export: UserDataExport): String?
}

@Singleton
class AndroidExportFileHandler @Inject constructor(
    @ApplicationContext private val context: Context
) : ExportFileHandler {

    override suspend fun prepareShareableUri(export: UserDataExport): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val exportDir = File(context.cacheDir, EXPORTS_DIR_NAME).apply {
                    mkdirs()
                }
                val safeName = export.fileName
                    .replace(Regex("[^A-Za-z0-9._-]"), "_")
                    .ifBlank { "cashmate-export.json" }
                val targetFile = File(exportDir, safeName)
                targetFile.writeBytes(export.contentBytes)
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", targetFile)
                    .toString()
            }.getOrNull()
        }

    companion object {
        const val EXPORTS_DIR_NAME = "exports"
    }
}
