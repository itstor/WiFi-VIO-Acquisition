package com.itstor.wifivioacquisition.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * A helper class to manage permissions.
 */
class PermissionHelper(private val context: Context) {

    companion object {
        const val CAMERA_PERMISSION_REQUEST_CODE = 100
        const val ALL_PERMISSIONS_REQUEST_CODE = 101
    }

    /**
     * Checks if the camera permission is granted.
     * @return true if permission is granted, false otherwise.
     */
    fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Requests camera permission.
     * Should be called from an Activity.
     * @param activity The activity on which to request the permission.
     */
    fun requestCameraPermission(activity: AppCompatActivity) {
        activity.requestPermissions(arrayOf(android.Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
    }

    /**
     * Gets all the permissions declared in the manifest.
     * @return A list of all the permissions declared in the manifest.
     */
    private fun getAllManifestPermissions(context: Context): List<String> {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        return packageInfo.requestedPermissions?.toList() ?: emptyList()
    }

    /**
     * Checks the status of all the permissions declared in the manifest.
     * @return A map of all the permissions declared in the manifest and their status.
     */
    fun checkPermissionsStatus(context: Context): Map<String, Boolean> {
        val allPermissions = getAllManifestPermissions(context)
        val permissionsStatus = mutableMapOf<String, Boolean>()

        for (permission in allPermissions) {
            permissionsStatus[permission] = ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }

        return permissionsStatus
    }

    /**
     * Requests all the permissions that are not granted.
     * Should be called from an Activity.
     * @param activity The activity on which to request the permissions.
     * @param requestCode The request code to use when requesting the permissions.
     */
    fun requestPermissions(activity: AppCompatActivity, requestCode: Int) {
        val allPermissions = getAllManifestPermissions(context)
        val permissionsToRequest = mutableListOf<String>()

        for (permission in allPermissions) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(permission)
            }
        }

        activity.requestPermissions(permissionsToRequest.toTypedArray(), requestCode)
    }

}