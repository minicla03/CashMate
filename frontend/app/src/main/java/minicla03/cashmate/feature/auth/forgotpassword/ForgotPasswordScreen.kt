package minicla03.cashmate.feature.auth.forgotpassword

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import minicla03.cashmate.R
import minicla03.cashmate.core.designsystem.CashMateCard
import minicla03.cashmate.core.designsystem.CashMatePrimaryButton
import minicla03.cashmate.core.designsystem.CashMateTextField
import minicla03.cashmate.core.designsystem.CashMateTheme
import minicla03.cashmate.feature.auth.AuthMessageBanner

@Composable
fun ForgotPasswordRoute(
    onNavigateBack: () -> Unit,
    onNavigateToResetPassword: (String) -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ForgotPasswordEffect.NavigateToResetPassword ->
                    onNavigateToResetPassword(effect.email)
            }
        }
    }

    ForgotPasswordScreen(
        state = state,
        onNavigateBack = onNavigateBack,
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    state: ForgotPasswordUiState,
    onNavigateBack: () -> Unit,
    onEvent: (ForgotPasswordEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = CashMateTheme.spacing
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.auth_forgot_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !state.isSubmitting) {
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
                .padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Text(
                text = stringResource(R.string.auth_forgot_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            CashMateCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    state.error?.let { err ->
                        AuthMessageBanner(
                            message = stringResource(err.messageResId),
                            isError = true
                        )
                    }

                    if (state.requestSent) {
                        AuthMessageBanner(
                            message = stringResource(R.string.auth_reset_sent_message),
                            isError = false
                        )
                    }

                    CashMateTextField(
                        value = state.email,
                        onValueChange = { onEvent(ForgotPasswordEvent.EmailChanged(it)) },
                        label = stringResource(R.string.auth_email_label),
                        placeholder = stringResource(R.string.auth_email_hint),
                        errorMessage = state.emailErrorResId?.let { stringResource(it) },
                        enabled = !state.isSubmitting,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { onEvent(ForgotPasswordEvent.SubmitClick) }
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Email,
                                contentDescription = null
                            )
                        }
                    )

                    CashMatePrimaryButton(
                        text = stringResource(R.string.auth_request_reset_button),
                        onClick = { onEvent(ForgotPasswordEvent.SubmitClick) },
                        isLoading = state.isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    )

                    TextButton(
                        onClick = { onEvent(ForgotPasswordEvent.ProceedToResetClick) },
                        enabled = !state.isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.auth_have_reset_code))
                    }
                }
            }
        }
    }
}

