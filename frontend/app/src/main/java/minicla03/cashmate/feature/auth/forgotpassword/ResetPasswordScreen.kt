package minicla03.cashmate.feature.auth.forgotpassword

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import minicla03.cashmate.R
import minicla03.cashmate.core.designsystem.CashMateCard
import minicla03.cashmate.core.designsystem.CashMatePrimaryButton
import minicla03.cashmate.core.designsystem.CashMateTextField
import minicla03.cashmate.core.designsystem.CashMateTheme
import minicla03.cashmate.feature.auth.AuthMessageBanner
import minicla03.cashmate.feature.auth.CashMatePasswordField

@Composable
fun ResetPasswordRoute(
    onNavigateBack: () -> Unit,
    onResetCompleted: () -> Unit,
    viewModel: ResetPasswordViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ResetPasswordEffect.NavigateToLogin -> onResetCompleted()
            }
        }
    }

    ResetPasswordScreen(
        state = state,
        onNavigateBack = onNavigateBack,
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    state: ResetPasswordUiState,
    onNavigateBack: () -> Unit,
    onEvent: (ResetPasswordEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = CashMateTheme.spacing
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.auth_reset_title)) },
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
            if (state.isCompleted) {
                CashMateCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(spacing.md)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = CashMateTheme.semanticColors.income,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = stringResource(R.string.auth_reset_success_message),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        CashMatePrimaryButton(
                            text = stringResource(R.string.auth_continue_to_login),
                            onClick = { onEvent(ResetPasswordEvent.ContinueToLoginClick) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.auth_reset_subtitle),
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

                        CashMateTextField(
                            value = state.email,
                            onValueChange = { onEvent(ResetPasswordEvent.EmailChanged(it)) },
                            label = stringResource(R.string.auth_email_label),
                            placeholder = stringResource(R.string.auth_email_hint),
                            errorMessage = state.emailErrorResId?.let { stringResource(it) },
                            enabled = !state.isSubmitting,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Email,
                                    contentDescription = null
                                )
                            }
                        )

                        CashMateTextField(
                            value = state.token,
                            onValueChange = { onEvent(ResetPasswordEvent.TokenChanged(it)) },
                            label = stringResource(R.string.auth_reset_token_label),
                            placeholder = stringResource(R.string.auth_reset_token_hint),
                            errorMessage = state.tokenErrorResId?.let { stringResource(it) },
                            enabled = !state.isSubmitting,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Key,
                                    contentDescription = null
                                )
                            }
                        )

                        CashMatePasswordField(
                            value = state.newPassword,
                            onValueChange = { onEvent(ResetPasswordEvent.NewPasswordChanged(it)) },
                            label = stringResource(R.string.auth_new_password_label),
                            errorMessage = state.newPasswordErrorResId?.let { stringResource(it) },
                            enabled = !state.isSubmitting,
                            imeAction = ImeAction.Next
                        )

                        CashMatePasswordField(
                            value = state.confirmPassword,
                            onValueChange = { onEvent(ResetPasswordEvent.ConfirmPasswordChanged(it)) },
                            label = stringResource(R.string.auth_confirm_password_label),
                            errorMessage = state.confirmPasswordErrorResId?.let { stringResource(it) },
                            enabled = !state.isSubmitting,
                            imeAction = ImeAction.Done,
                            keyboardActions = KeyboardActions(
                                onDone = { onEvent(ResetPasswordEvent.SubmitClick) }
                            )
                        )

                        CashMatePrimaryButton(
                            text = stringResource(R.string.auth_reset_button),
                            onClick = { onEvent(ResetPasswordEvent.SubmitClick) },
                            isLoading = state.isSubmitting,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

