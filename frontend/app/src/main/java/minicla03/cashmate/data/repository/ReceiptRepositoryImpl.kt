package minicla03.cashmate.data.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.core.error.AppError
import minicla03.cashmate.data.remote.datasource.ReceiptRemoteDataSource
import minicla03.cashmate.data.remote.mapper.toConfirmRequest
import minicla03.cashmate.data.remote.mapper.toDomain
import minicla03.cashmate.domain.model.Receipt
import minicla03.cashmate.domain.model.ReceiptImage
import minicla03.cashmate.domain.model.Transaction
import minicla03.cashmate.domain.repository.ReceiptRepository
import javax.inject.Inject

/**
 * Coordinates receipt processing and confirmation with the remote backend.
 * Keeps active review drafts strictly in-memory for the current flow without local persistence.
 */
class ReceiptRepositoryImpl @Inject constructor(
    private val remoteDataSource: ReceiptRemoteDataSource,
    private val safeRemoteCall: SafeRemoteCall
) : ReceiptRepository {

    private val draftMutex = Mutex()
    private val activeDrafts = mutableMapOf<String, Receipt>()

    override suspend fun processReceipt(image: ReceiptImage): AppResult<Receipt> =
        safeRemoteCall {
            val draft = remoteDataSource.processReceipt(image).toDomain()
            draftMutex.withLock {
                activeDrafts[draft.id] = draft
            }
            draft
        }

    override suspend fun getReceiptDraft(receiptId: String): AppResult<Receipt> {
        val draft = draftMutex.withLock { activeDrafts[receiptId] }
        return if (draft != null) {
            AppResult.Success(draft)
        } else {
            AppResult.Error(AppError.Network.NotFound)
        }
    }

    override suspend fun getReceiptForTransaction(transactionId: String): AppResult<Receipt?> =
        safeRemoteCall {
            remoteDataSource.getReceiptForTransaction(transactionId)?.toDomain()
        }

    override suspend fun confirmReceipt(receipt: Receipt): AppResult<Transaction> =
        safeRemoteCall {
            val transaction = remoteDataSource.confirmReceipt(
                id = receipt.id,
                request = receipt.toConfirmRequest()
            ).toDomain()
            draftMutex.withLock {
                activeDrafts.remove(receipt.id)
            }
            transaction
        }

    override suspend fun discardReceiptDraft(receiptId: String) {
        draftMutex.withLock {
            activeDrafts.remove(receiptId)
        }
    }
}

