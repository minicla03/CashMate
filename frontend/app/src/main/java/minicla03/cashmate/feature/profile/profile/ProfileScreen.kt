package minicla03.cashmate.feature.profile.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import minicla03.cashmate.R
import minicla03.cashmate.core.designsystem.CashMateCard
import minicla03.cashmate.core.designsystem.CashMateTheme
import minicla03.cashmate.core.designsystem.ErrorStateView
import minicla03.cashmate.core.designsystem.LoadingStateView

@Composable
fun ProfileRoute(
    onNavigateToEditProfile: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    onNavigateToPreferences: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToExportData: () -> Unit,
    onNavigateToDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ProfileEffect.NavigateToEditProfile -> onNavigateToEditProfile()
                ProfileEffect.NavigateToChangePassword -> onNavigateToChangePassword()
                ProfileEffect.NavigateToPreferences -> onNavigateToPreferences()
                ProfileEffect.NavigateToCategories -> onNavigateToCategories()
                ProfileEffect.NavigateToPrivacy -> onNavigateToPrivacy()
                ProfileEffect.NavigateToExportData -> onNavigateToExportData()
                ProfileEffect.NavigateToDeleteAccount -> onNavigateToDeleteAccount()
            }
        }
    }

    ProfileScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onEvent: (ProfileEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = CashMateTheme.spacing

    when {
        state.isLoading -> LoadingStateView(modifier = modifier)
        state.error != null && state.user == null -> ErrorStateView(
            message = stringResource(state.error.messageResId),
            onRetry = if (state.error.canRetry) {
                { onEvent(ProfileEvent.Refresh) }
            } else {
                null
            },
            modifier = modifier
        )
        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                state.user?.let { user ->
                    item(key = "user_summary") {
                        CashMateCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = stringResource(R.string.profile_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.semantics { heading() }
                                        )
                                    }
                                    Surface(
                                        color = if (user.emailVerified) {
                                            CashMateTheme.semanticColors.incomeContainer
                                        } else {
                                            MaterialTheme.colorScheme.errorContainer
                                        },
                                        contentColor = if (user.emailVerified) {
                                            CashMateTheme.semanticColors.onIncomeContainer
                                        } else {
                                            MaterialTheme.colorScheme.onErrorContainer
                                        },
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = stringResource(
                                                if (user.emailVerified) {
                                                    R.string.profile_verified_badge
                                                } else {
                                                    R.string.profile_unverified_badge
                                                }
                                            ),
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = stringResource(R.string.profile_name_label),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = user.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                HorizontalDivider()

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = stringResource(R.string.profile_email_label),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = user.email,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "account_section") {
                    ProfileSectionCard(title = stringResource(R.string.profile_section_account)) {
                        ProfileMenuRow(
                            icon = Icons.Rounded.Edit,
                            title = stringResource(R.string.profile_edit_profile),
                            onClick = { onEvent(ProfileEvent.EditProfileClick) }
                        )
                        HorizontalDivider()
                        ProfileMenuRow(
                            icon = Icons.Rounded.Lock,
                            title = stringResource(R.string.profile_change_password),
                            onClick = { onEvent(ProfileEvent.ChangePasswordClick) }
                        )
                    }
                }

                item(key = "preferences_section") {
                    ProfileSectionCard(title = stringResource(R.string.profile_section_preferences)) {
                        ProfileMenuRow(
                            icon = Icons.Rounded.Tune,
                            title = stringResource(R.string.profile_preferences),
                            subtitle = stringResource(R.string.profile_preferences_subtitle),
                            onClick = { onEvent(ProfileEvent.PreferencesClick) }
                        )
                        HorizontalDivider()
                        ProfileMenuRow(
                            icon = Icons.Rounded.Category,
                            title = stringResource(R.string.settings_manage_categories),
                            subtitle = stringResource(R.string.settings_manage_categories_subtitle),
                            onClick = { onEvent(ProfileEvent.ManageCategoriesClick) }
                        )
                    }
                }

                item(key = "privacy_section") {
                    ProfileSectionCard(title = stringResource(R.string.profile_section_privacy)) {
                        ProfileMenuRow(
                            icon = Icons.Rounded.PrivacyTip,
                            title = stringResource(R.string.profile_privacy),
                            subtitle = stringResource(R.string.profile_privacy_subtitle),
                            onClick = { onEvent(ProfileEvent.PrivacyClick) }
                        )
                        HorizontalDivider()
                        ProfileMenuRow(
                            icon = Icons.Rounded.Download,
                            title = stringResource(R.string.profile_export_data),
                            subtitle = stringResource(R.string.profile_export_data_subtitle),
                            onClick = { onEvent(ProfileEvent.ExportDataClick) }
                        )
                        HorizontalDivider()
                        ProfileMenuRow(
                            icon = Icons.Rounded.DeleteForever,
                            title = stringResource(R.string.profile_delete_account),
                            subtitle = stringResource(R.string.profile_delete_account_subtitle),
                            tint = MaterialTheme.colorScheme.error,
                            onClick = { onEvent(ProfileEvent.DeleteAccountClick) }
                        )
                    }
                }

                item(key = "logout_button") {
                    OutlinedButton(
                        onClick = { onEvent(ProfileEvent.LogoutClick) },
                        enabled = !state.isLoggingOut,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(spacing.minTouchTarget),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = null,
                            modifier = Modifier.padding(end = spacing.xs)
                        )
                        Text(text = stringResource(R.string.profile_logout))
                    }
                    Spacer(modifier = Modifier.height(spacing.md))
                }
            }
        }
    }

    if (state.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { onEvent(ProfileEvent.DismissLogoutDialog) },
            title = { Text(stringResource(R.string.profile_logout_confirm_title)) },
            text = { Text(stringResource(R.string.profile_logout_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = { onEvent(ProfileEvent.ConfirmLogout) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.profile_logout))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(ProfileEvent.DismissLogoutDialog) }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun ProfileSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(CashMateTheme.spacing.xs)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() }
        )
        CashMateCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    val spacing = CashMateTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (tint == MaterialTheme.colorScheme.error) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

