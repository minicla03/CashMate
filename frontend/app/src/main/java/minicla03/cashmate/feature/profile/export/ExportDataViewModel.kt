package minicla03.cashmate.feature.profile.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.core.ui.toUiError
import minicla03.cashmate.data.platform.ExportFileHandler
import minicla03.cashmate.domain.usecase.ExportUserDataUseCase
import javax.inject.Inject

@HiltViewModel
class ExportDataViewModel @Inject constructor(
    private val exportUserDataUseCase: ExportUserDataUseCase,
    private val exportFileHandler: ExportFileHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportDataUiState())
    val uiState: StateFlow<ExportDataUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ExportDataEffect>(Channel.BUFFERED)
    val effects: Flow<ExportDataEffect> = _effects.receiveAsFlow()

    fun onEvent(event: ExportDataEvent) {
        when (event) {
            ExportDataEvent.RequestExportClick -> requestExport()
            ExportDataEvent.ShareExportClick -> emitFileEffect(openViewer = false)
            ExportDataEvent.OpenExportClick -> emitFileEffect(openViewer = true)
            ExportDataEvent.DismissError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun requestExport() {
        if (_uiState.value.isLoading) return
        _uiState.update {
            it.copy(isLoading = true, error = null, preparedUriString = null)
        }
        viewModelScope.launch {
            when (val result = exportUserDataUseCase()) {
                is AppResult.Success -> {
                    val uri = exportFileHandler.prepareShareableUri(result.data)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            export = result.data,
                            preparedUriString = uri,
                            error = null
                        )
                    }
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.error.toUiError())
                }
            }
        }
    }

    private fun emitFileEffect(openViewer: Boolean) {
        val current = _uiState.value
        val export = current.export ?: return
        val uriString = current.preparedUriString
        viewModelScope.launch {
            if (uriString == null) {
                val uri = exportFileHandler.prepareShareableUri(export)
                if (uri == null) {
                    _effects.send(ExportDataEffect.ShowFilePrepareError)
                    return@launch
                }
                val resolved = uri
                _uiState.update { it.copy(preparedUriString = resolved) }
                _effects.send(
                    if (openViewer) {
                        ExportDataEffect.LaunchOpenViewer(resolved, export.mimeType)
                    } else {
                        ExportDataEffect.LaunchShareSheet(resolved, export.mimeType)
                    }
                )
            } else {
                _effects.send(
                    if (openViewer) {
                        ExportDataEffect.LaunchOpenViewer(uriString, export.mimeType)
                    } else {
                        ExportDataEffect.LaunchShareSheet(uriString, export.mimeType)
                    }
                )
            }
        }
    }
}
