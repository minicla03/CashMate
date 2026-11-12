package minicla03.cashmate.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AccountDto(
    val id: String,
    val name: String,
    val currency: String
)

