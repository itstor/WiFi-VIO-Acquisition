package com.itstor.wifivioacquisition.utils

import android.graphics.PointF
import android.net.wifi.ScanResult
import android.opengl.Matrix
import com.google.ar.core.Camera
import com.google.ar.core.PointCloud
import java.util.regex.PatternSyntaxException


/**
 * Utility class to handle some common operations.
 */
class Utils {
    companion object {
        /**
         * Returns the number of feature points in a point cloud.
         * @param pointCloud The point cloud.
         * @return The number of feature points.
         */
        fun countNumberOfFeatures(pointCloud: PointCloud): Int {
                // Each point containing 4 floats. Which is x, y, z, and confidence. So we divide by 4 to get the number of points.
                return pointCloud.points.remaining() / 4
        }


        /**
         * Converts a 3D point in world space to 2D screen coordinates.
         *
         * @param point The 3D point in world space. This is a FloatArray of size 3 where each element represents the x, y, and z coordinates respectively.
         * @param camera The AR camera used to capture the 3D point. This is used to get the view and projection matrices.
         * @param screenWidth The width of the screen in pixels.
         * @param screenHeight The height of the screen in pixels.
         * @return A PointF object representing the 2D screen coordinates of the input 3D point.
         */
        fun convertToScreenCoordinates(
            point: FloatArray,
            camera: Camera,
            screenWidth: Int,
            screenHeight: Int
        ): PointF {
            val viewMatrix = FloatArray(16)
            val projectionMatrix = FloatArray(16)

            camera.getViewMatrix(viewMatrix, 0)
            camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f)

            // Create a temporary point for calculations
            val tempPoint = FloatArray(4)
            System.arraycopy(point, 0, tempPoint, 0, 3)
            tempPoint[3] = 1.0f  // Set the fourth element to 1 for homogeneous coordinates

            // Multiply the view matrix and the point to get the camera coordinates
            val cameraPoint = FloatArray(4)
            Matrix.multiplyMV(cameraPoint, 0, viewMatrix, 0, tempPoint, 0)

            // Multiply the projection matrix and the camera coordinates to get the screen coordinates
            val screenPoint = FloatArray(4)
            Matrix.multiplyMV(screenPoint, 0, projectionMatrix, 0, cameraPoint, 0)

            // Normalize the x and y coordinates by dividing by the w coordinate
            val normalizedX = screenPoint[0] / screenPoint[3]
            val normalizedY = screenPoint[1] / screenPoint[3]

            // Convert the normalized coordinates to pixel coordinates
            val screenX = (normalizedX + 1) * screenWidth * 0.5f
            val screenY = (1 - normalizedY) * screenHeight * 0.5f

            return PointF(screenX, screenY)
        }

        /**
         * Checks if a regex pattern is valid.
         * @param pattern The regex pattern to check.
         * @return true if the pattern is valid, false otherwise.
         */
        fun isRegexPatternValid(pattern: String): Boolean {
            return try {
                Regex(pattern)
                true
            } catch (e: PatternSyntaxException) {
                false
            }
        }

        /**
         * Filters a list of ScanResult by SSID.
         * @param wifiResult The list of ScanResult to filter.
         * @param wifiRegex The regex pattern to filter by. Defaults to ".*".
         * @return A list of ScanResult that matches the regex pattern.
         */
        fun filterWifiResult(wifiResult: List<ScanResult>, wifiRegex: String? = "TA-.*"): List<ScanResult> {
            return wifiResult.filter { scanResult ->
                scanResult.SSID.matches(Regex(wifiRegex!!))
            }
        }

        /**
         * Formats a number of seconds to a time string.
         * @param seconds The number of seconds.
         * @return A time string in the format of "mm:ss".
         */
        fun formatSecondsToTime(seconds: Long): String {
            val m = (seconds % 3600) / 60
            val s = seconds % 60

            return String.format("%02d:%02d", m, s)
        }
    }
}