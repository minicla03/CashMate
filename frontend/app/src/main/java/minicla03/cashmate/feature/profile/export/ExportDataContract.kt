package minicla03.cashmate.feature.profile.export

import minicla03.cashmate.core.ui.UiError
import minicla03.cashmate.domain.model.UserDataExport

data class ExportDataUiState(
    val isLoading: Boolean = false,
    val export: UserDataExport? = null,
    val preparedUriString: String? = null,
    val error: UiError? = null
)

sealed interface ExportDataEvent {
    data object RequestExportClick : ExportDataEvent
    data object ShareExportClick : ExportDataEvent
    data object OpenExportClick : ExportDataEvent
    data object DismissError : ExportDataEvent
}

sealed interface ExportDataEffect {
    data class LaunchShareSheet(val uriString: String, val mimeType: String) : ExportDataEffect
    data class LaunchOpenViewer(val uriString: String, val mimeType: String) : ExportDataEffect
    data object ShowFilePrepareError : ExportDataEffect
}

