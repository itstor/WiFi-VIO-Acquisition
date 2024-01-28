package com.itstor.wifivioacquisition

import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.itstor.wifivioacquisition.databinding.ActivityMainBinding
import com.itstor.wifivioacquisition.utils.FileUtils
import com.itstor.wifivioacquisition.utils.PermissionHelper
import com.itstor.wifivioacquisition.utils.Utils
import io.github.sceneview.ar.ARSceneView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    private lateinit var permissionHelper: PermissionHelper
    private lateinit var wifiScanner: WiFiScanner
    private lateinit var sensorListener: SensorListener

    private lateinit var sceneView: ARSceneView
    private lateinit var settingBottomSheet: BottomSheetDialog
    private lateinit var inputFileNameDialog: AlertDialog

    private var screenWidth by Delegates.notNull<Int>()
    private var screenHeight by Delegates.notNull<Int>()

    private var showPointCloudTemp: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Keep the screen on.
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        screenWidth = resources.displayMetrics.widthPixels
        screenHeight = resources.displayMetrics.heightPixels

        permissionHelper = PermissionHelper(this)

        // Initialize WiFiScanner. Set the callback to update the WiFi scan results.
        wifiScanner = WiFiScanner(this) { scanResult, timestamp ->
            viewModel.setWifiResult(scanResult, timestamp)
        }

        // Initialize sensor listener
        sensorListener = SensorListener(this) { _, accuracy ->
            binding.tvSensorAccuracy.text = when (accuracy) {
                android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "High"
                android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Medium"
                android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "Low"
                android.hardware.SensorManager.SENSOR_STATUS_UNRELIABLE -> "Unreliable"
                else -> "Unknown"
            }
        }

        // Initialize PointCloudView.
        val pointCloudVisualizeView = PointCloudVisualizeView(this)
        binding.flPointCloud.addView(pointCloudVisualizeView)

        // Initialize UI components.
        initializeSettingBottomSheet()
        initializeInputFileNameDialog {
            viewModel.startRecording()
        }

        binding.btnSettings.setOnClickListener {
            settingBottomSheet.show()
        }

        binding.btnStart.setOnClickListener {
            if (viewModel.isRecording.value == true) {
                viewModel.stopRecording()

                CoroutineScope(Dispatchers.IO).launch {
                    val fileName = viewModel.fileName.value

                    val timestamp = System.currentTimeMillis()
                    val vioFileName = "${fileName}_vio_${timestamp}.csv"
                    val sensorFileName = "${fileName}_rawIMU_${timestamp}.csv"
                    val wifiFileName = "${fileName}_wifi_${timestamp}.csv"

                    try {
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                "Saving to Downloads folder...",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        FileUtils.writeToCSV(
                            vioFileName,
                            viewModel.recordedVIO.toList(),
                            contentResolver
                        )
                        FileUtils.writeToCSV(
                            sensorFileName,
                            viewModel.recordedSensor.toList(),
                            contentResolver
                        )
                        FileUtils.writeToCSV(
                            wifiFileName,
                            viewModel.recordedWiFi.toList(),
                            contentResolver
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "onCreate: ", e)
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                        }
                    }

                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Saved Successfully", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

            } else {
                startRecording()
            }
        }

        viewModel.detectedApCount.observe(this) {
            binding.tvDetectedAp.text = it.toString()
        }

        viewModel.isRecording.observe(this) {
            binding.btnStart.setBackgroundColor(if (it) "#FF0000".toColorInt() else "#00000000".toColorInt())
            binding.tvRecordingTime.visibility =
                if (it) android.view.View.VISIBLE else android.view.View.GONE

            if (it) {
                showPointCloudTemp = viewModel.showPointCloud.value
                viewModel.showPointCloud(false)
            } else {
                showPointCloudTemp?.let { it1 -> viewModel.showPointCloud(it1) }
            }
        }

        viewModel.timer.observe(this) {
            binding.tvRecordingTime.text = Utils.formatSecondsToTime(it)
        }

        viewModel.showDebug.observe(this) {
            binding.llInfo.visibility =
                if (it) android.view.View.VISIBLE else android.view.View.GONE
        }

        viewModel.showPointCloud.observe(this) {
            pointCloudVisualizeView.visibility =
                if (it) android.view.View.VISIBLE else android.view.View.GONE
        }

        viewModel.fps.observe(this) {
            binding.tvFps.text = it.toString()
        }

        viewModel.trackingState.observe(this) {
            binding.tvState.text = it
        }

        viewModel.featureCount.observe(this) {
            binding.tvFeatures.text = it.toString()
        }

        viewModel.pointList.observe(this) {
            pointCloudVisualizeView.setPoints(it)
        }

        viewModel.accelerometer.observe(this) {
            binding.tvAccelerometer.text = String.format("%.2f, %.2f, %.2f", it[0], it[1], it[2])
        }

        viewModel.gyroscope.observe(this) {
            binding.tvGyroscope.text = String.format("%.2f, %.2f, %.2f", it[0], it[1], it[2])
        }

        viewModel.magnetometer.observe(this) {
            binding.tvMagnetometer.text = String.format("%.2f, %.2f, %.2f", it[0], it[1], it[2])
        }

        viewModel.rotationVector.observe(this) {
            binding.tvRotationVector.text =
                String.format("%.2f, %.2f, %.2f, %.2f", it[0], it[1], it[2], it[3])
        }

        viewModel.poseTranslation.observe(this) {
            binding.tvPoseTranslation.text = String.format("%.2f, %.2f, %.2f", it[0], it[1], it[2])
        }

        viewModel.poseRotation.observe(this) {
            binding.tvPoseQuartenion.text =
                String.format("%.2f, %.2f, %.2f, %.2f", it[0], it[1], it[2], it[3])
        }

        sceneView = binding.svCameraView.apply {
            planeRenderer.isEnabled = false
            configureSession { _, config ->
                config.focusMode = Config.FocusMode.AUTO
            }
            onSessionUpdated = { _, frame -> onSessionUpdated(frame) }
        }
    }

    private fun onSessionUpdated(frame: Frame) {
        if (viewModel.isRecording.value == true) {
            try {
                wifiScanner.startScan()
            } catch (e: Exception) {
                Log.e(TAG, "onSessionUpdated: ", e)
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()

                wifiScanner.unregisterReceiver()
                viewModel.stopRecording()
            }
        }

        val timestamp = System.currentTimeMillis()

        viewModel.processData(
            frame,
            screenWidth,
            screenHeight,
            sensorListener.getSensorData(),
            timestamp
        )
    }

    private fun startRecording() {
        inputFileNameDialog.show()
    }

    private fun initializeSettingBottomSheet() {
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val tvTitle = TextView(this).apply {
            text = "Settings"
            textSize = 24f
            setPadding(0, 0, 0, 16)
        }

        val etWiFiRegex = EditText(this).apply {
            hint = "WiFi Regex"
            inputType = InputType.TYPE_CLASS_TEXT
            setText(viewModel.wifiRegex.value)
        }

        val swInfoPanel = SwitchMaterial(this).apply {
            text = "Show Info Panel"
            isChecked = viewModel.showDebug.value == true
        }

        val swPointCloud = SwitchMaterial(this).apply {
            text = "Visualize Point Cloud"
            isChecked = viewModel.showPointCloud.value == true
        }

        val linearLayout = LinearLayout(this).apply {
            setPadding(58, 32, 58, 16)
            orientation = LinearLayout.VERTICAL
            dividerPadding = 8
            addView(tvTitle, params)
            addView(etWiFiRegex, params)
            addView(swInfoPanel, params)
            addView(swPointCloud, params)
        }

        swInfoPanel.setOnCheckedChangeListener { _, isChecked ->
            viewModel.showDebug(isChecked)
        }

        swPointCloud.setOnCheckedChangeListener { _, isChecked ->
            viewModel.showPointCloud(isChecked)
        }

        settingBottomSheet = BottomSheetDialog(this).apply {
            setContentView(linearLayout)
            setTitle("Settings")

            setCancelable(true)
            setCanceledOnTouchOutside(true)
            setOnDismissListener {
                etWiFiRegex.text.toString().let { regex ->
                    if (regex.isNotEmpty() && Utils.isRegexPatternValid(regex)) {
                        viewModel.setWifiRegex(regex)

                        Toast.makeText(this@MainActivity, "Regex saved", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Invalid Regex", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }
    }

    private fun initializeInputFileNameDialog(onClickStart: (() -> Unit)? = null) {
        val editText = EditText(this)
        val frameLayout = FrameLayout(this)

        editText.hint = "File name"
        editText.inputType = InputType.TYPE_CLASS_TEXT

        if (viewModel.fileName.value != null) {
            editText.setText(viewModel.fileName.value)
        }

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )

        params.leftMargin = 58
        params.rightMargin = 58

        frameLayout.addView(editText, params)

        inputFileNameDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Filename")
            .setMessage("Enter a filename to save the data.")
            .setView(frameLayout)
            .setNeutralButton("Cancel") { _, _ -> }
            .setPositiveButton("Start", null)
            .create()

        inputFileNameDialog.setOnShowListener {
            val button = (it as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                editText.text.toString().let { filename ->
                    if (filename.isNotEmpty()) {
                        viewModel.setFileName(filename)
                        if (onClickStart != null) onClickStart()

                        inputFileNameDialog.dismiss()
                    } else {
                        Toast.makeText(this, "Filename is required", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!permissionHelper.checkPermissionsStatus(this).values.all { it }) {
            permissionHelper.requestPermissions(this, PermissionHelper.ALL_PERMISSIONS_REQUEST_CODE)
            return
        }

        sensorListener.start()
        wifiScanner.startScan()
    }

    override fun onPause() {
        super.onPause()
        sensorListener.unregisterListener()
    }

    override fun onDestroy() {
        super.onDestroy()

        wifiScanner.unregisterReceiver()
        sensorListener.unregisterListener()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        results: IntArray
    ) {
        when (requestCode) {
            PermissionHelper.ALL_PERMISSIONS_REQUEST_CODE -> {
                if (results.isNotEmpty() && results.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Log.d(TAG, "onRequestPermissionsResult: All permissions granted.")
                } else {
                    Log.d(TAG, "onRequestPermissionsResult: Some permissions are not granted.")
                }
            }

            else -> super.onRequestPermissionsResult(requestCode, permissions, results)
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}