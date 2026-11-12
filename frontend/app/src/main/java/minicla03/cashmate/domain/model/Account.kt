package minicla03.cashmate.domain.model

import java.util.Currency

data class Account(
    val id: String,
    val name: String,
    val currency: Currency
)

