package minicla03.cashmate.feature.receipt.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import minicla03.cashmate.R
import minicla03.cashmate.core.designsystem.CashMateCard
import minicla03.cashmate.core.designsystem.CashMateOutlinedButton
import minicla03.cashmate.core.designsystem.CashMatePrimaryButton
import minicla03.cashmate.core.designsystem.CashMateTheme
import minicla03.cashmate.data.platform.AndroidReceiptImageReader
import java.io.File

@Composable
fun ReceiptScanRoute(
    viewModel: ReceiptScanViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToReview: (String) -> Unit,
    onNavigateToTransactionDetail: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }
    var permissionDenial by remember { mutableStateOf<PermissionDenialState?>(null) }

    fun hasPermission(target: ReceiptPermissionTarget): Boolean =
        MediaPermissionPolicy.isGranted(target) { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

    fun recordPermissionDenied(target: ReceiptPermissionTarget) {
        val primaryPermission = MediaPermissionPolicy.primaryPermissionForRationale(target)
        val activity = context.findActivity()
        val canAskAgain = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, primaryPermission)
        } ?: false
        permissionDenial = PermissionDenialState(
            target = target,
            permanentlyDenied = !canAskAgain
        )
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val currentDenial = permissionDenial
        if (currentDenial != null && hasPermission(currentDenial.target)) {
            permissionDenial = null
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCaptureUri
        pendingCaptureUri = null
        if (success && uri != null) {
            viewModel.onEvent(ReceiptScanEvent.ImageSelected(uri.toString()))
        } else {
            viewModel.onEvent(ReceiptScanEvent.ImageSelectionCancelled)
        }
    }

    val pickGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onEvent(ReceiptScanEvent.ImageSelected(uri.toString()))
        } else {
            viewModel.onEvent(ReceiptScanEvent.ImageSelectionCancelled)
        }
    }

    fun launchCameraCapture() {
        val tempUri = createTempReceiptImageUri(context)
        if (tempUri == null) {
            viewModel.onEvent(ReceiptScanEvent.ImageSelectionCancelled)
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.error_unknown))
            }
            return
        }
        pendingCaptureUri = tempUri
        takePictureLauncher.launch(tempUri)
    }

    fun launchGalleryPicker() {
        pickGalleryLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDenial = null
            launchCameraCapture()
        } else {
            viewModel.onEvent(ReceiptScanEvent.ImageSelectionCancelled)
            recordPermissionDenied(ReceiptPermissionTarget.CAMERA)
        }
    }

    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = MediaPermissionPolicy.isGranted(ReceiptPermissionTarget.GALLERY) { permission ->
            results[permission] == true ||
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
        if (granted) {
            permissionDenial = null
            launchGalleryPicker()
        } else {
            viewModel.onEvent(ReceiptScanEvent.ImageSelectionCancelled)
            recordPermissionDenied(ReceiptPermissionTarget.GALLERY)
        }
    }

    fun requestCameraFlow() {
        viewModel.onEvent(ReceiptScanEvent.BeginImageSelection)
        if (hasPermission(ReceiptPermissionTarget.CAMERA)) {
            permissionDenial = null
            launchCameraCapture()
        } else {
            val permission = MediaPermissionPolicy.requiredPermissions(ReceiptPermissionTarget.CAMERA).first()
            cameraPermissionLauncher.launch(permission)
        }
    }

    fun requestGalleryFlow() {
        viewModel.onEvent(ReceiptScanEvent.BeginImageSelection)
        if (hasPermission(ReceiptPermissionTarget.GALLERY)) {
            permissionDenial = null
            launchGalleryPicker()
        } else {
            val permissions = MediaPermissionPolicy.requiredPermissions(ReceiptPermissionTarget.GALLERY)
                .toTypedArray()
            galleryPermissionLauncher.launch(permissions)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ReceiptScanEffect.NavigateToReview -> onNavigateToReview(effect.receiptId)
                is ReceiptScanEffect.NavigateToTransactionDetail ->
                    onNavigateToTransactionDetail(effect.transactionId)
                ReceiptScanEffect.ExitFlow -> onNavigateBack()
            }
        }
    }

    ReceiptScanScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        permissionDenial = permissionDenial,
        onNavigateBack = { viewModel.onEvent(ReceiptScanEvent.CancelClick) },
        onTakePhotoClick = ::requestCameraFlow,
        onPickGalleryClick = ::requestGalleryFlow,
        onRetryPermission = { target ->
            when (target) {
                ReceiptPermissionTarget.CAMERA -> requestCameraFlow()
                ReceiptPermissionTarget.GALLERY -> requestGalleryFlow()
            }
        },
        onOpenAppSettings = { context.openAppSettings() },
        onDismissPermissionDenial = { permissionDenial = null },
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScanScreen(
    state: ReceiptScanState,
    snackbarHostState: SnackbarHostState,
    permissionDenial: PermissionDenialState? = null,
    onNavigateBack: () -> Unit,
    onTakePhotoClick: () -> Unit,
    onPickGalleryClick: () -> Unit,
    onRetryPermission: (ReceiptPermissionTarget) -> Unit = {},
    onOpenAppSettings: () -> Unit = {},
    onDismissPermissionDenial: () -> Unit = {},
    onEvent: (ReceiptScanEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val isBusy = state is ReceiptScanState.Uploading || state is ReceiptScanState.Processing

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.receipt_scan_title)) },
                navigationIcon = {
                    if (!isBusy) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.cd_navigate_back)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)

        when (state) {
            ReceiptScanState.Idle,
            ReceiptScanState.SelectingImage -> ReceiptAcquireContent(
                enabled = state is ReceiptScanState.Idle,
                permissionDenial = permissionDenial,
                onTakePhotoClick = onTakePhotoClick,
                onPickGalleryClick = onPickGalleryClick,
                onRetryPermission = onRetryPermission,
                onOpenAppSettings = onOpenAppSettings,
                onDismissPermissionDenial = onDismissPermissionDenial,
                modifier = contentModifier
            )

            ReceiptScanState.Uploading -> ReceiptProcessingContent(
                title = stringResource(R.string.receipt_uploading_title),
                subtitle = stringResource(R.string.receipt_uploading_subtitle),
                modifier = contentModifier
            )

            ReceiptScanState.Processing -> ReceiptProcessingContent(
                title = stringResource(R.string.receipt_processing_title),
                subtitle = stringResource(R.string.receipt_processing_subtitle),
                modifier = contentModifier
            )

            is ReceiptScanState.Error -> ReceiptErrorContent(
                message = stringResource(state.error.messageResId),
                canRetry = state.canRetryLastImage,
                onRetry = { onEvent(ReceiptScanEvent.RetryProcessing) },
                onChooseAnother = { onEvent(ReceiptScanEvent.ChooseAnotherImage) },
                modifier = contentModifier
            )

            is ReceiptScanState.Review,
            is ReceiptScanState.Confirming,
            is ReceiptScanState.Success -> ReceiptProcessingContent(
                title = stringResource(R.string.receipt_processing_title),
                subtitle = stringResource(R.string.receipt_processing_subtitle),
                modifier = contentModifier
            )
        }
    }
}

@Composable
private fun ReceiptAcquireContent(
    enabled: Boolean,
    permissionDenial: PermissionDenialState?,
    onTakePhotoClick: () -> Unit,
    onPickGalleryClick: () -> Unit,
    onRetryPermission: (ReceiptPermissionTarget) -> Unit,
    onOpenAppSettings: () -> Unit,
    onDismissPermissionDenial: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = CashMateTheme.spacing
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(spacing.md),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (permissionDenial != null) {
            PermissionDenialCard(
                denial = permissionDenial,
                onRetry = { onRetryPermission(permissionDenial.target) },
                onOpenSettings = onOpenAppSettings,
                onDismiss = onDismissPermissionDenial,
                modifier = Modifier.padding(bottom = spacing.md)
            )
        }

        CashMateCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DocumentScanner,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.receipt_scan_heading),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(R.string.receipt_scan_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(spacing.xs))

                CashMatePrimaryButton(
                    text = stringResource(R.string.receipt_scan_take_photo),
                    onClick = onTakePhotoClick,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth()
                )

                CashMateOutlinedButton(
                    text = stringResource(R.string.receipt_scan_pick_gallery),
                    onClick = onPickGalleryClick,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.md))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.CameraAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(end = spacing.xs)
                    .size(16.dp)
            )
            Icon(
                imageVector = Icons.Rounded.PhotoLibrary,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ReceiptProcessingContent(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val spacing = CashMateTheme.spacing
    Box(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "$title. $subtitle"
        },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            CircularProgressIndicator()
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ReceiptErrorContent(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onChooseAnother: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = CashMateTheme.spacing
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Icon(
                imageVector = Icons.Rounded.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = stringResource(R.string.error_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(spacing.xs))
            if (canRetry) {
                CashMatePrimaryButton(
                    text = stringResource(R.string.action_retry),
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth()
                )
                CashMateOutlinedButton(
                    text = stringResource(R.string.receipt_scan_choose_another),
                    onClick = onChooseAnother,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                CashMatePrimaryButton(
                    text = stringResource(R.string.receipt_scan_choose_another),
                    onClick = onChooseAnother,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun createTempReceiptImageUri(context: Context): Uri? = runCatching {
    val tempDir = File(context.cacheDir, AndroidReceiptImageReader.TEMP_DIR_NAME).apply {
        mkdirs()
    }
    val tempFile = File.createTempFile("receipt_capture_", ".jpg", tempDir)
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
}.getOrNull()

@Composable
private fun PermissionDenialCard(
    denial: PermissionDenialState,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = CashMateTheme.spacing
    val titleRes = when (denial.target) {
        ReceiptPermissionTarget.CAMERA -> R.string.permission_camera_rationale_title
        ReceiptPermissionTarget.GALLERY -> R.string.permission_gallery_rationale_title
    }
    val messageRes = when (denial.target) {
        ReceiptPermissionTarget.CAMERA -> if (denial.permanentlyDenied) {
            R.string.permission_camera_permanently_denied_message
        } else {
            R.string.permission_camera_rationale_message
        }
        ReceiptPermissionTarget.GALLERY -> if (denial.permanentlyDenied) {
            R.string.permission_gallery_permanently_denied_message
        } else {
            R.string.permission_gallery_rationale_message
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
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
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = stringResource(messageRes),
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
                if (denial.permanentlyDenied) {
                    TextButton(onClick = onOpenSettings) {
                        Text(stringResource(R.string.permission_action_open_settings))
                    }
                } else {
                    TextButton(onClick = onRetry) {
                        Text(stringResource(R.string.permission_action_grant))
                    }
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

