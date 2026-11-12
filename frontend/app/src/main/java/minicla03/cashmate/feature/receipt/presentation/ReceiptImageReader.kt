package minicla03.cashmate.feature.receipt.presentation

import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.domain.model.ReceiptImage

/**
 * Abstraction for reading a user-selected temporary receipt image into memory without coupling
 * ViewModels to Android framework APIs.
 */
interface ReceiptImageReader {
    suspend fun read(uriString: String): AppResult<ReceiptImage>
    fun cleanupTemporaryFiles()
}

