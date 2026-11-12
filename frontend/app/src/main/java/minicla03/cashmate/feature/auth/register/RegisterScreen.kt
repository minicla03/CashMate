package minicla03.cashmate.feature.auth.register

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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Person
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
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
fun RegisterRoute(
    onNavigateBack: () -> Unit,
    onNavigateToEmailVerification: (String) -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is RegisterEffect.NavigateToEmailVerification ->
                    onNavigateToEmailVerification(effect.email)
                RegisterEffect.NavigateBackToLogin -> onNavigateBack()
            }
        }
    }

    RegisterScreen(
        state = state,
        onNavigateBack = onNavigateBack,
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    state: RegisterUiState,
    onNavigateBack: () -> Unit,
    onEvent: (RegisterEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = CashMateTheme.spacing
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.auth_register_title)) },
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
                text = stringResource(R.string.auth_register_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { heading() }
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
                        value = state.name,
                        onValueChange = { onEvent(RegisterEvent.NameChanged(it)) },
                        label = stringResource(R.string.auth_name_label),
                        placeholder = stringResource(R.string.auth_name_hint),
                        errorMessage = state.nameErrorResId?.let { stringResource(it) },
                        enabled = !state.isSubmitting,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null
                            )
                        }
                    )

                    CashMateTextField(
                        value = state.email,
                        onValueChange = { onEvent(RegisterEvent.EmailChanged(it)) },
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
                        onValueChange = { onEvent(RegisterEvent.PasswordChanged(it)) },
                        label = stringResource(R.string.auth_password_label),
                        errorMessage = state.passwordErrorResId?.let { stringResource(it) },
                        enabled = !state.isSubmitting,
                        imeAction = ImeAction.Next
                    )

                    CashMatePasswordField(
                        value = state.confirmPassword,
                        onValueChange = { onEvent(RegisterEvent.ConfirmPasswordChanged(it)) },
                        label = stringResource(R.string.auth_confirm_password_label),
                        errorMessage = state.confirmPasswordErrorResId?.let { stringResource(it) },
                        enabled = !state.isSubmitting,
                        imeAction = ImeAction.Done,
                        keyboardActions = KeyboardActions(
                            onDone = { onEvent(RegisterEvent.SubmitClick) }
                        )
                    )

                    Spacer(modifier = Modifier.height(spacing.xxs))

                    CashMatePrimaryButton(
                        text = stringResource(R.string.auth_register_button),
                        onClick = { onEvent(RegisterEvent.SubmitClick) },
                        isLoading = state.isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.auth_have_account_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = { onEvent(RegisterEvent.SignInClick) },
                    enabled = !state.isSubmitting
                ) {
                    Text(text = stringResource(R.string.auth_sign_in_instead))
                }
            }
        }
    }
}

