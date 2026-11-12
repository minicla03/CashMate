package minicla03.cashmate.feature.auth.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
import minicla03.cashmate.feature.auth.CashMatePasswordField

@Composable
fun LoginRoute(
    onNavigateToForgotPassword: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToEmailVerification: (String) -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                LoginEffect.NavigateToForgotPassword -> onNavigateToForgotPassword()
                LoginEffect.NavigateToRegister -> onNavigateToRegister()
                is LoginEffect.NavigateToEmailVerification -> onNavigateToEmailVerification(effect.email)
            }
        }
    }

    LoginScreen(
        state = state,
        onEvent = viewModel::onEvent
    )
}

@Composable
fun LoginScreen(
    state: LoginUiState,
    onEvent: (LoginEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = CashMateTheme.spacing
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(spacing.lg),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.auth_login_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = stringResource(R.string.auth_login_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(spacing.lg))

            CashMateCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    state.error?.let { err ->
                        AuthMessageBanner(
                            message = stringResource(err.messageResId),
                            isError = true,
                            actionLabel = if (state.requiresEmailVerification) {
                                stringResource(R.string.auth_verify_now)
                            } else {
                                null
                            },
                            onAction = if (state.requiresEmailVerification) {
                                { onEvent(LoginEvent.VerifyEmailClick) }
                            } else {
                                null
                            }
                        )
                    }

                    CashMateTextField(
                        value = state.email,
                        onValueChange = { onEvent(LoginEvent.EmailChanged(it)) },
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

                    CashMatePasswordField(
                        value = state.password,
                        onValueChange = { onEvent(LoginEvent.PasswordChanged(it)) },
                        label = stringResource(R.string.auth_password_label),
                        errorMessage = state.passwordErrorResId?.let { stringResource(it) },
                        enabled = !state.isSubmitting,
                        imeAction = ImeAction.Done,
                        keyboardActions = KeyboardActions(
                            onDone = { onEvent(LoginEvent.SubmitClick) }
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { onEvent(LoginEvent.ForgotPasswordClick) },
                            enabled = !state.isSubmitting
                        ) {
                            Text(text = stringResource(R.string.auth_forgot_password))
                        }
                    }

                    CashMatePrimaryButton(
                        text = stringResource(R.string.auth_sign_in),
                        onClick = { onEvent(LoginEvent.SubmitClick) },
                        isLoading = state.isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.md))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.auth_no_account_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = { onEvent(LoginEvent.CreateAccountClick) },
                    enabled = !state.isSubmitting
                ) {
                    Text(text = stringResource(R.string.auth_create_account))
                }
            }
        }
    }
}

