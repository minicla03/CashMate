package minicla03.cashmate.data.remote.datasource

import minicla03.cashmate.data.remote.dto.ConfirmReceiptRequest
import minicla03.cashmate.data.remote.dto.ReceiptDraftDto
import minicla03.cashmate.data.remote.dto.TransactionDto
import minicla03.cashmate.domain.model.ReceiptImage
import javax.inject.Inject

class FakeReceiptRemoteDataSource @Inject constructor(
    private val store: FakeRemoteStore
) : ReceiptRemoteDataSource {

    override suspend fun processReceipt(image: ReceiptImage): ReceiptDraftDto =
        store.processReceipt(image)

    override suspend fun confirmReceipt(
        id: String,
        request: ConfirmReceiptRequest
    ): TransactionDto = store.confirmReceipt(id, request)

    override suspend fun getReceiptForTransaction(transactionId: String): ReceiptDraftDto? =
        store.getReceiptForTransaction(transactionId)
}

