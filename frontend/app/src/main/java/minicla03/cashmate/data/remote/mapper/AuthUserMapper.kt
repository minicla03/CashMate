package minicla03.cashmate.data.remote.mapper

import minicla03.cashmate.data.remote.dto.AuthTokensDto
import minicla03.cashmate.data.remote.dto.ConsentItemDto
import minicla03.cashmate.data.remote.dto.EmailVerificationResponseDto
import minicla03.cashmate.data.remote.dto.PrivacyDataCategoryDto
import minicla03.cashmate.data.remote.dto.PrivacyInfoDto
import minicla03.cashmate.data.remote.dto.RegisterResponseDto
import minicla03.cashmate.data.remote.dto.UserDataExportDto
import minicla03.cashmate.data.remote.dto.UserDto
import minicla03.cashmate.data.remote.dto.UserPreferencesDto
import minicla03.cashmate.domain.model.AppPreferences
import minicla03.cashmate.domain.model.AppThemeMode
import minicla03.cashmate.domain.model.AuthTokens
import minicla03.cashmate.domain.model.ConsentItem
import minicla03.cashmate.domain.model.EmailVerificationResult
import minicla03.cashmate.domain.model.PrivacyDataCategory
import minicla03.cashmate.domain.model.PrivacyInfo
import minicla03.cashmate.domain.model.RegistrationResult
import minicla03.cashmate.domain.model.User
import minicla03.cashmate.domain.model.UserDataExport
import java.util.Currency

fun UserDto.toDomain(): User = User(
    id = id,
    name = name,
    email = email,
    emailVerified = emailVerified,
    createdAt = createdAt
)

fun AuthTokensDto.toDomain(): AuthTokens = AuthTokens(
    accessToken = accessToken,
    refreshToken = refreshToken,
    expiresInSeconds = expiresInSeconds
)

fun RegisterResponseDto.toDomain(): RegistrationResult = RegistrationResult(
    user = user.toDomain(),
    requiresEmailVerification = requiresEmailVerification
)

fun EmailVerificationResponseDto.toDomain(): EmailVerificationResult = EmailVerificationResult(
    email = email,
    verified = verified
)

fun UserPreferencesDto.toDomain(): AppPreferences {
    val parsedTheme = runCatching { AppThemeMode.valueOf(themeMode.uppercase()) }
        .getOrDefault(AppThemeMode.SYSTEM)
    val parsedCurrency = runCatching { Currency.getInstance(currency.uppercase()) }
        .getOrDefault(Currency.getInstance("EUR"))
    return AppPreferences(
        themeMode = parsedTheme,
        currency = parsedCurrency
    )
}

fun AppPreferences.toDto(): UserPreferencesDto = UserPreferencesDto(
    themeMode = themeMode.name,
    currency = currency.currencyCode
)

fun ConsentItemDto.toDomain(): ConsentItem = ConsentItem(
    id = id,
    title = title,
    description = description,
    required = required,
    granted = granted,
    updatedAt = updatedAt
)

fun PrivacyDataCategoryDto.toDomain(): PrivacyDataCategory = PrivacyDataCategory(
    id = id,
    title = title,
    description = description,
    retentionSummary = retentionSummary
)

fun PrivacyInfoDto.toDomain(): PrivacyInfo = PrivacyInfo(
    summary = summary,
    dataCategories = dataCategories.map { it.toDomain() },
    consents = consents.map { it.toDomain() },
    policyUpdatedAt = policyUpdatedAt
)

fun UserDataExportDto.toDomain(): UserDataExport = UserDataExport(
    fileName = fileName,
    mimeType = mimeType,
    contentBytes = contentBytes,
    generatedAt = generatedAt
)

