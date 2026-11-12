package minicla03.cashmate.feature.profile.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import minicla03.cashmate.R
import minicla03.cashmate.core.designsystem.CashMateCard
import minicla03.cashmate.core.designsystem.CashMatePrimaryButton
import minicla03.cashmate.core.designsystem.CashMateTheme
import minicla03.cashmate.feature.auth.AuthMessageBanner
import minicla03.cashmate.feature.auth.CashMatePasswordField

@Composable
fun ChangePasswordRoute(
    onNavigateBack: () -> Unit,
    onPasswordChanged: () -> Unit,
    viewModel: ChangePasswordViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ChangePasswordEffect.PasswordChanged -> onPasswordChanged()
            }
        }
    }

    ChangePasswordScreen(
        state = state,
        onNavigateBack = onNavigateBack,
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    state: ChangePasswordUiState,
    onNavigateBack: () -> Unit,
    onEvent: (ChangePasswordEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = CashMateTheme.spacing
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.change_password_title)) },
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
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            CashMateCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    state.error?.let { err ->
                        AuthMessageBanner(
                            message = stringResource(err.messageResId),
                            isError = true
                        )
                    }

                    CashMatePasswordField(
                        value = state.currentPassword,
                        onValueChange = { onEvent(ChangePasswordEvent.CurrentPasswordChanged(it)) },
                        label = stringResource(R.string.change_password_current_label),
                        errorMessage = state.currentPasswordErrorResId?.let { stringResource(it) },
                        enabled = !state.isSubmitting,
                        imeAction = ImeAction.Next
                    )

                    CashMatePasswordField(
                        value = state.newPassword,
                        onValueChange = { onEvent(ChangePasswordEvent.NewPasswordChanged(it)) },
                        label = stringResource(R.string.change_password_new_label),
                        errorMessage = state.newPasswordErrorResId?.let { stringResource(it) },
                        enabled = !state.isSubmitting,
                        imeAction = ImeAction.Next
                    )

                    CashMatePasswordField(
                        value = state.confirmPassword,
                        onValueChange = { onEvent(ChangePasswordEvent.ConfirmPasswordChanged(it)) },
                        label = stringResource(R.string.change_password_confirm_label),
                        errorMessage = state.confirmPasswordErrorResId?.let { stringResource(it) },
                        enabled = !state.isSubmitting,
                        imeAction = ImeAction.Done,
                        keyboardActions = KeyboardActions(
                            onDone = { onEvent(ChangePasswordEvent.SubmitClick) }
                        )
                    )

                    CashMatePrimaryButton(
                        text = stringResource(R.string.change_password_submit),
                        onClick = { onEvent(ChangePasswordEvent.SubmitClick) },
                        isLoading = state.isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

