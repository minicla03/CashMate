package minicla03.cashmate.domain.repository

import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.domain.model.Receipt
import minicla03.cashmate.domain.model.ReceiptImage
import minicla03.cashmate.domain.model.Transaction

interface ReceiptRepository {

    suspend fun processReceipt(image: ReceiptImage): AppResult<Receipt>

    suspend fun getReceiptDraft(receiptId: String): AppResult<Receipt>

    suspend fun getReceiptForTransaction(transactionId: String): AppResult<Receipt?>

    suspend fun confirmReceipt(receipt: Receipt): AppResult<Transaction>

    suspend fun discardReceiptDraft(receiptId: String)
}

