package minicla03.cashmate.data.remote.mapper

import minicla03.cashmate.data.remote.dto.CreateTransactionRequest
import minicla03.cashmate.data.remote.dto.TransactionDto
import minicla03.cashmate.data.remote.dto.TransactionFilterDto
import minicla03.cashmate.data.remote.dto.UpdateTransactionRequest
import minicla03.cashmate.domain.model.Money
import minicla03.cashmate.domain.model.Receipt
import minicla03.cashmate.domain.model.Transaction
import minicla03.cashmate.domain.model.TransactionFilter
import minicla03.cashmate.domain.model.TransactionSource
import minicla03.cashmate.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

fun TransactionDto.toDomain(): Transaction = Transaction(
    id = id,
    amount = Money(amount),
    type = TransactionType.entries.firstOrNull { it.name.equals(type, ignoreCase = true) }
        ?: throw IllegalArgumentException("Unsupported transaction type: $type"),
    description = description,
    categoryId = categoryId,
    accountId = accountId,
    date = parseIsoDate(date),
    source = TransactionSource.entries.firstOrNull { it.name.equals(source, ignoreCase = true) }
        ?: TransactionSource.MANUAL,
    receiptId = receiptId ?: receipt?.id
)

fun Transaction.toCreateRequest(): CreateTransactionRequest = CreateTransactionRequest(
    amount = amount.minorUnits,
    type = type.name,
    description = description,
    categoryId = categoryId,
    accountId = accountId,
    date = date.toString()
)

fun Transaction.toUpdateRequest(receipt: Receipt? = null): UpdateTransactionRequest = UpdateTransactionRequest(
    amount = amount.minorUnits,
    type = type.name,
    description = description,
    categoryId = categoryId,
    accountId = accountId,
    date = date.toString(),
    receipt = receipt?.toUpdateRequest()
)

fun TransactionFilter.toDto(): TransactionFilterDto = TransactionFilterDto(
    from = fromDate?.toString(),
    to = toDate?.toString(),
    categoryId = categoryId,
    type = type?.name
)

private fun parseIsoDate(raw: String): LocalDate = runCatching {
    LocalDate.parse(raw)
}.recoverCatching {
    OffsetDateTime.parse(raw).toLocalDate()
}.recoverCatching {
    Instant.parse(raw).atZone(ZoneOffset.UTC).toLocalDate()
}.getOrElse {
    throw IllegalArgumentException("Invalid ISO date format: $raw", it)
}

