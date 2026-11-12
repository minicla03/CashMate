package minicla03.cashmate.feature

import android.Manifest
import android.os.Build
import minicla03.cashmate.feature.receipt.presentation.MediaPermissionPolicy
import minicla03.cashmate.feature.receipt.presentation.ReceiptPermissionTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPermissionPolicyTest {

    @Test
    fun `camera always requires CAMERA permission across API levels`() {
        listOf(
            Build.VERSION_CODES.N_MR1,
            Build.VERSION_CODES.S_V2,
            Build.VERSION_CODES.TIRAMISU,
            Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        ).forEach { sdk ->
            assertEquals(
                listOf(Manifest.permission.CAMERA),
                MediaPermissionPolicy.requiredPermissions(ReceiptPermissionTarget.CAMERA, sdk)
            )
            assertFalse(
                MediaPermissionPolicy.isGranted(ReceiptPermissionTarget.CAMERA, sdk) { false }
            )
            assertTrue(
                MediaPermissionPolicy.isGranted(ReceiptPermissionTarget.CAMERA, sdk) {
                    it == Manifest.permission.CAMERA
                }
            )
        }
    }

    @Test
    fun `gallery uses READ_EXTERNAL_STORAGE only below Android 13`() {
        val sdk = Build.VERSION_CODES.S_V2
        assertEquals(
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            MediaPermissionPolicy.requiredPermissions(ReceiptPermissionTarget.GALLERY, sdk)
        )
        assertFalse(
            MediaPermissionPolicy.isGranted(ReceiptPermissionTarget.GALLERY, sdk) { false }
        )
        assertTrue(
            MediaPermissionPolicy.isGranted(ReceiptPermissionTarget.GALLERY, sdk) {
                it == Manifest.permission.READ_EXTERNAL_STORAGE
            }
        )
    }

    @Test
    fun `gallery uses READ_MEDIA_IMAGES on Android 13`() {
        val sdk = Build.VERSION_CODES.TIRAMISU
        assertEquals(
            listOf(Manifest.permission.READ_MEDIA_IMAGES),
            MediaPermissionPolicy.requiredPermissions(ReceiptPermissionTarget.GALLERY, sdk)
        )
        assertFalse(
            MediaPermissionPolicy.isGranted(ReceiptPermissionTarget.GALLERY, sdk) {
                it == Manifest.permission.READ_EXTERNAL_STORAGE
            }
        )
        assertTrue(
            MediaPermissionPolicy.isGranted(ReceiptPermissionTarget.GALLERY, sdk) {
                it == Manifest.permission.READ_MEDIA_IMAGES
            }
        )
    }

    @Test
    fun `gallery supports full and limited photo access on Android 14 and newer`() {
        val sdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        assertEquals(
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            ),
            MediaPermissionPolicy.requiredPermissions(ReceiptPermissionTarget.GALLERY, sdk)
        )
        assertFalse(
            MediaPermissionPolicy.isGranted(ReceiptPermissionTarget.GALLERY, sdk) { false }
        )
        assertTrue(
            MediaPermissionPolicy.isGranted(ReceiptPermissionTarget.GALLERY, sdk) {
                it == Manifest.permission.READ_MEDIA_IMAGES
            }
        )
        assertTrue(
            MediaPermissionPolicy.isGranted(ReceiptPermissionTarget.GALLERY, sdk) {
                it == Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            }
        )
    }
}

