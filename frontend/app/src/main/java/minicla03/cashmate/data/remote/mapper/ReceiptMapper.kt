package minicla03.cashmate.data.remote.mapper

import minicla03.cashmate.data.remote.dto.ConfirmReceiptRequest
import minicla03.cashmate.data.remote.dto.ReceiptAdjustmentDto
import minicla03.cashmate.data.remote.dto.ReceiptDraftDto
import minicla03.cashmate.data.remote.dto.ReceiptItemDto
import minicla03.cashmate.data.remote.dto.UpdateReceiptRequest
import minicla03.cashmate.domain.model.Money
import minicla03.cashmate.domain.model.Receipt
import minicla03.cashmate.domain.model.ReceiptAdjustment
import minicla03.cashmate.domain.model.ReceiptAdjustmentType
import minicla03.cashmate.domain.model.ReceiptItem
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

fun ReceiptDraftDto.toDomain(): Receipt {
    if (id.isBlank()) {
        throw InvalidReceiptException("Receipt draft id is blank")
    }
    if (subtotal < 0L || totalDiscount < 0L || total <= 0L) {
        throw InvalidReceiptException("Receipt totals are invalid")
    }
    return Receipt(
        id = id,
        merchant = merchant.trim(),
        date = parseReceiptIsoDate(date),
        subtotal = Money(subtotal),
        totalDiscount = Money(totalDiscount),
        total = Money(total),
        categoryId = categoryId?.takeIf { it.isNotBlank() },
        accountId = accountId?.takeIf { it.isNotBlank() },
        items = items.map { it.toDomain() },
        adjustments = adjustments.map { it.toDomain() }
    )
}

fun ReceiptItemDto.toDomain(): ReceiptItem {
    if (quantity <= 0 || unitPrice < 0L || grossAmount < 0L || discountAmount < 0L || netAmount < 0L) {
        throw InvalidReceiptException("Receipt item values are invalid")
    }
    return ReceiptItem(
        id = id,
        description = description.trim(),
        quantity = quantity,
        unitPrice = Money(unitPrice),
        grossAmount = Money(grossAmount),
        discountAmount = Money(discountAmount),
        netAmount = Money(netAmount),
        category = category.trim()
    )
}

fun ReceiptAdjustmentDto.toDomain(): ReceiptAdjustment {
    if (amount < 0L) {
        throw InvalidReceiptException("Receipt adjustment amount cannot be negative")
    }
    val mappedType = ReceiptAdjustmentType.entries.firstOrNull {
        it.name.equals(type.trim(), ignoreCase = true)
    } ?: ReceiptAdjustmentType.OTHER

    return ReceiptAdjustment(
        id = id,
        description = description.trim(),
        amount = Money(amount),
        type = mappedType
    )
}

fun ReceiptItem.toDto(): ReceiptItemDto = ReceiptItemDto(
    id = id,
    description = description,
    quantity = quantity,
    unitPrice = unitPrice.minorUnits,
    grossAmount = grossAmount.minorUnits,
    discountAmount = discountAmount.minorUnits,
    netAmount = netAmount.minorUnits,
    category = category
)

fun ReceiptAdjustment.toDto(): ReceiptAdjustmentDto = ReceiptAdjustmentDto(
    id = id,
    description = description,
    amount = amount.minorUnits,
    type = type.name
)

fun Receipt.toConfirmRequest(): ConfirmReceiptRequest = ConfirmReceiptRequest(
    merchant = merchant,
    date = date.toString(),
    subtotal = subtotal.minorUnits,
    totalDiscount = totalDiscount.minorUnits,
    total = total.minorUnits,
    categoryId = categoryId.orEmpty(),
    accountId = accountId.orEmpty(),
    items = items.map { it.toDto() },
    adjustments = adjustments.map { it.toDto() }
)

fun Receipt.toUpdateRequest(): UpdateReceiptRequest = UpdateReceiptRequest(
    id = id,
    merchant = merchant,
    date = date.toString(),
    subtotal = subtotal.minorUnits,
    totalDiscount = totalDiscount.minorUnits,
    total = total.minorUnits,
    categoryId = categoryId,
    accountId = accountId,
    items = items.map { it.toDto() },
    adjustments = adjustments.map { it.toDto() }
)

private fun parseReceiptIsoDate(raw: String): LocalDate = runCatching {
    LocalDate.parse(raw)
}.recoverCatching {
    OffsetDateTime.parse(raw).toLocalDate()
}.recoverCatching {
    Instant.parse(raw).atZone(ZoneOffset.UTC).toLocalDate()
}.getOrElse {
    throw InvalidReceiptException("Invalid receipt ISO date format")
}

