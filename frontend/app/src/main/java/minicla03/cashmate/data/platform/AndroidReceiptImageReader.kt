package minicla03.cashmate.data.platform

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.core.error.AppError
import minicla03.cashmate.domain.model.ReceiptImage
import minicla03.cashmate.feature.receipt.presentation.ReceiptImageReader
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

/**
 * Reads temporary receipt image bytes from a content/file URI and immediately cleans up
 * temporary capture files so receipt images are never persisted locally.
 */
class AndroidReceiptImageReader @Inject constructor(
    @ApplicationContext private val context: Context
) : ReceiptImageReader {

    override suspend fun read(uriString: String): AppResult<ReceiptImage> = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(uriString)
            val mimeType = resolveMimeType(uri)
            if (mimeType == null || !mimeType.startsWith("image/", ignoreCase = true)) {
                return@withContext AppResult.Error(AppError.Receipt.UnsupportedImage)
            }

            val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
                val buffer = ByteArrayOutputStream()
                val chunk = ByteArray(DEFAULT_BUFFER_SIZE)
                var totalRead = 0
                while (true) {
                    val read = stream.read(chunk)
                    if (read <= 0) break
                    totalRead += read
                    if (totalRead > MAX_IMAGE_BYTES) {
                        return@withContext AppResult.Error(AppError.Receipt.UnsupportedImage)
                    }
                    buffer.write(chunk, 0, read)
                }
                buffer.toByteArray()
            } ?: return@withContext AppResult.Error(AppError.Receipt.UnsupportedImage)

            if (bytes.isEmpty()) {
                return@withContext AppResult.Error(AppError.Receipt.UnsupportedImage)
            }

            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
            AppResult.Success(
                ReceiptImage(
                    bytes = bytes,
                    mimeType = mimeType,
                    fileName = "receipt.$extension"
                )
            )
        } catch (_: SecurityException) {
            AppResult.Error(AppError.Receipt.UnsupportedImage)
        } catch (_: IllegalArgumentException) {
            AppResult.Error(AppError.Receipt.UnsupportedImage)
        } catch (_: Throwable) {
            AppResult.Error(AppError.Unknown)
        } finally {
            cleanupTemporaryFiles()
        }
    }

    override fun cleanupTemporaryFiles() {
        val tempDir = File(context.cacheDir, TEMP_DIR_NAME)
        if (tempDir.exists()) {
            tempDir.listFiles()?.forEach { file ->
                file.delete()
            }
        }
    }

    private fun resolveMimeType(uri: Uri): String? {
        val resolverType = context.contentResolver.getType(uri)
        if (!resolverType.isNullOrBlank()) return resolverType

        val ext = MimeTypeMap.getFileExtensionFromUrl(uri.toString()).lowercase()
        if (ext.isNotBlank()) {
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        }
        return null
    }

    companion object {
        const val TEMP_DIR_NAME = "receipt_temp"
        private const val MAX_IMAGE_BYTES = 15 * 1024 * 1024
    }
}

