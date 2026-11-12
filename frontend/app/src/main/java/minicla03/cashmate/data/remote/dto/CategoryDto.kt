package minicla03.cashmate.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val icon: String,
    val color: String
)

@Serializable
data class CreateCategoryRequest(
    val name: String,
    val icon: String,
    val color: String
)

@Serializable
data class UpdateCategoryRequest(
    val name: String,
    val icon: String,
    val color: String
)

