package minicla03.cashmate.data.remote.mapper

import minicla03.cashmate.data.remote.dto.AccountDto
import minicla03.cashmate.domain.model.Account
import java.util.Currency

fun AccountDto.toDomain(): Account = Account(
    id = id,
    name = name,
    currency = Currency.getInstance(currency)
)

