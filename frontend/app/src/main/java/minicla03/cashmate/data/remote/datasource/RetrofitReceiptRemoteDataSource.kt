package minicla03.cashmate.data.remote.datasource

import minicla03.cashmate.data.remote.api.CashMateApi
import minicla03.cashmate.data.remote.dto.ConfirmReceiptRequest
import minicla03.cashmate.data.remote.dto.ReceiptDraftDto
import minicla03.cashmate.data.remote.dto.TransactionDto
import minicla03.cashmate.domain.model.ReceiptImage
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class RetrofitReceiptRemoteDataSource @Inject constructor(
    private val api: CashMateApi
) : ReceiptRemoteDataSource {

    override suspend fun processReceipt(image: ReceiptImage): ReceiptDraftDto {
        val mediaType = image.mimeType.toMediaTypeOrNull()
        val requestBody = image.bytes.toRequestBody(mediaType)
        val part = MultipartBody.Part.createFormData(
            name = PART_IMAGE,
            filename = image.fileName,
            body = requestBody
        )
        return api.processReceipt(part)
    }

    override suspend fun confirmReceipt(
        id: String,
        request: ConfirmReceiptRequest
    ): TransactionDto = api.confirmReceipt(id, request)

    override suspend fun getReceiptForTransaction(transactionId: String): ReceiptDraftDto? =
        api.getTransaction(transactionId).receipt

    companion object {
        private const val PART_IMAGE = "image"
    }
}

