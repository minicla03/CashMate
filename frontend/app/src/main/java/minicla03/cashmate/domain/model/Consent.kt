package minicla03.cashmate.domain.model

/**
 * Configurable privacy consent item managed by the backend.
 * [required] consents cannot be disabled by the client.
 */
data class ConsentItem(
    val id: String,
    val title: String,
    val description: String,
    val required: Boolean,
    val granted: Boolean,
    val updatedAt: String? = null
)

/**
 * Category of stored user data disclosed by the backend privacy endpoint.
 */
data class PrivacyDataCategory(
    val id: String,
    val title: String,
    val description: String,
    val retentionSummary: String? = null
)

/**
 * Privacy disclosures and consent states returned by the backend.
 */
data class PrivacyInfo(
    val summary: String,
    val dataCategories: List<PrivacyDataCategory>,
    val consents: List<ConsentItem>,
    val policyUpdatedAt: String? = null
)

/**
 * Export payload generated and returned by the backend.
 */
data class UserDataExport(
    val fileName: String,
    val mimeType: String,
    val contentBytes: ByteArray,
    val generatedAt: String? = null
) {
    override fun toString(): String =
        "UserDataExport(fileName='$fileName', mimeType='$mimeType', sizeBytes=${contentBytes.size}, generatedAt=$generatedAt)"
}

