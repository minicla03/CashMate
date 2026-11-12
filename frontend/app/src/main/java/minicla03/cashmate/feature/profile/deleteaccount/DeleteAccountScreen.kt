package minicla03.cashmate.feature.profile.deleteaccount

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import minicla03.cashmate.R
import minicla03.cashmate.core.designsystem.CashMateCard
import minicla03.cashmate.core.designsystem.CashMateTextField
import minicla03.cashmate.core.designsystem.CashMateTheme
import minicla03.cashmate.feature.auth.AuthMessageBanner
import minicla03.cashmate.feature.auth.CashMatePasswordField

@Composable
fun DeleteAccountRoute(
    onNavigateBack: () -> Unit,
    viewModel: DeleteAccountViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DeleteAccountScreen(
        state = state,
        onNavigateBack = onNavigateBack,
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAccountScreen(
    state: DeleteAccountUiState,
    onNavigateBack: () -> Unit,
    onEvent: (DeleteAccountEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = CashMateTheme.spacing
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.delete_account_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !state.isDeleting) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null
                        )
                        Text(
                            text = stringResource(R.string.delete_account_warning_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = stringResource(R.string.delete_account_warning_message),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            CashMateCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    Text(
                        text = stringResource(R.string.delete_account_consequences_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "• ${stringResource(R.string.delete_account_consequence_1)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• ${stringResource(R.string.delete_account_consequence_2)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• ${stringResource(R.string.delete_account_consequence_3)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            CashMateCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    state.error?.let { err ->
                        AuthMessageBanner(
                            message = stringResource(err.messageResId),
                            isError = true
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !state.isDeleting) {
                                onEvent(DeleteAccountEvent.AcknowledgedChanged(!state.acknowledgedConsequences))
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        Checkbox(
                            checked = state.acknowledgedConsequences,
                            onCheckedChange = { onEvent(DeleteAccountEvent.AcknowledgedChanged(it)) },
                            enabled = !state.isDeleting
                        )
                        Text(
                            text = stringResource(R.string.delete_account_acknowledge),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = stringResource(R.string.delete_account_confirm_instruction),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    CashMateTextField(
                        value = state.confirmationText,
                        onValueChange = { onEvent(DeleteAccountEvent.ConfirmationTextChanged(it)) },
                        label = stringResource(R.string.delete_account_confirm_label),
                        placeholder = stringResource(R.string.delete_account_confirm_hint),
                        errorMessage = state.confirmationErrorResId?.let { stringResource(it) },
                        enabled = !state.isDeleting,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Next
                        )
                    )

                    CashMatePasswordField(
                        value = state.password,
                        onValueChange = { onEvent(DeleteAccountEvent.PasswordChanged(it)) },
                        label = stringResource(R.string.delete_account_password_label),
                        enabled = !state.isDeleting,
                        imeAction = ImeAction.Done
                    )

                    Button(
                        onClick = { onEvent(DeleteAccountEvent.RequestDeleteClick) },
                        enabled = state.acknowledgedConsequences && !state.isDeleting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = spacing.minTouchTarget)
                    ) {
                        if (state.isDeleting) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(end = spacing.xs),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onError
                            )
                        }
                        Text(
                            text = stringResource(R.string.delete_account_button),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }

    if (state.showFinalConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { onEvent(DeleteAccountEvent.DismissDialog) },
            title = { Text(stringResource(R.string.delete_account_dialog_title)) },
            text = { Text(stringResource(R.string.delete_account_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = { onEvent(DeleteAccountEvent.ConfirmDeleteClick) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(DeleteAccountEvent.DismissDialog) }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

