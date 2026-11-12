package minicla03.cashmate.data.remote.mapper

import minicla03.cashmate.data.remote.dto.CategoryDto
import minicla03.cashmate.data.remote.dto.CreateCategoryRequest
import minicla03.cashmate.data.remote.dto.UpdateCategoryRequest
import minicla03.cashmate.domain.model.Category

fun CategoryDto.toDomain(): Category = Category(
    id = id,
    name = name,
    icon = icon,
    color = color
)

fun Category.toCreateRequest(): CreateCategoryRequest = CreateCategoryRequest(
    name = name,
    icon = icon,
    color = color
)

fun Category.toUpdateRequest(): UpdateCategoryRequest = UpdateCategoryRequest(
    name = name,
    icon = icon,
    color = color
)

