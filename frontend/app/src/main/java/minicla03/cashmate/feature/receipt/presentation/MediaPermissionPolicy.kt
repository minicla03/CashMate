package minicla03.cashmate.feature.receipt.presentation

import android.Manifest
import android.os.Build

enum class ReceiptPermissionTarget {
    CAMERA,
    GALLERY
}

data class PermissionDenialState(
    val target: ReceiptPermissionTarget,
    val permanentlyDenied: Boolean
)

/**
 * Resolves the exact Android runtime permissions required for camera and gallery image access
 * according to the device's SDK level, avoiding deprecated storage permissions on Android 13+.
 */
object MediaPermissionPolicy {

    fun requiredPermissions(
        target: ReceiptPermissionTarget,
        sdkInt: Int = Build.VERSION.SDK_INT
    ): List<String> = when (target) {
        ReceiptPermissionTarget.CAMERA -> listOf(Manifest.permission.CAMERA)
        ReceiptPermissionTarget.GALLERY -> when {
            sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
            sdkInt >= Build.VERSION_CODES.TIRAMISU -> listOf(
                Manifest.permission.READ_MEDIA_IMAGES
            )
            else -> listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    fun isGranted(
        target: ReceiptPermissionTarget,
        sdkInt: Int = Build.VERSION.SDK_INT,
        isPermissionGranted: (String) -> Boolean
    ): Boolean {
        val permissions = requiredPermissions(target, sdkInt)
        return when (target) {
            ReceiptPermissionTarget.CAMERA -> permissions.all(isPermissionGranted)
            ReceiptPermissionTarget.GALLERY -> permissions.any(isPermissionGranted)
        }
    }

    fun primaryPermissionForRationale(
        target: ReceiptPermissionTarget,
        sdkInt: Int = Build.VERSION.SDK_INT
    ): String = requiredPermissions(target, sdkInt).first()
}

