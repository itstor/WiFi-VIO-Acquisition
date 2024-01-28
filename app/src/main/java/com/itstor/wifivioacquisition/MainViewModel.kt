package com.itstor.wifivioacquisition

import android.graphics.Point
import android.net.wifi.ScanResult
import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.ar.core.Frame
import com.itstor.wifivioacquisition.models.RecordedIMU
import com.itstor.wifivioacquisition.models.RecordedVIO
import com.itstor.wifivioacquisition.models.RecordedWiFi
import com.itstor.wifivioacquisition.utils.FileUtils
import com.itstor.wifivioacquisition.utils.Utils
import com.itstor.wifivioacquisition.utils.Utils.Companion.countNumberOfFeatures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 20_000 is about 11 minute of recording at 30 FPS.
// If the recording is longer than this, it will overwrite from the beginning.
const val BUFFER_SIZE = 20_000

class MainViewModel: ViewModel() {
    private val _trackingState = MutableLiveData<String>()
    val trackingState: LiveData<String> = _trackingState

    private val _featureCount = MutableLiveData<Int>()
    val featureCount: LiveData<Int> = _featureCount

    private val _fps = MutableLiveData<Int>()
    val fps: LiveData<Int> = _fps

    private val _pointList = MutableLiveData<List<Point>>()
    val pointList: LiveData<List<Point>> = _pointList

    private val _showDebug = MutableLiveData(true)
    val showDebug: LiveData<Boolean> = _showDebug

    private val _showPointCloud = MutableLiveData(true)
    val showPointCloud: LiveData<Boolean> = _showPointCloud

    private val _wifiRegex = MutableLiveData("TA.*")
    val wifiRegex: LiveData<String> = _wifiRegex

    private val _fileName = MutableLiveData<String>()
    val fileName: LiveData<String> = _fileName

    private val _isRecording = MutableLiveData(false)
    val isRecording: LiveData<Boolean> = _isRecording

    private val _detectedApCount = MutableLiveData<Int>()
    val detectedApCount: LiveData<Int> = _detectedApCount

    private val _poseTranslation = MutableLiveData<FloatArray>()
    val poseTranslation: LiveData<FloatArray> = _poseTranslation

    private val _poseRotation = MutableLiveData<FloatArray>()
    val poseRotation: LiveData<FloatArray> = _poseRotation

    private val _accelerometer = MutableLiveData<FloatArray>()
    val accelerometer: LiveData<FloatArray> = _accelerometer

    private val _gyroscope = MutableLiveData<FloatArray>()
    val gyroscope: LiveData<FloatArray> = _gyroscope

    private val _magnetometer = MutableLiveData<FloatArray>()
    val magnetometer: LiveData<FloatArray> = _magnetometer

    private val _rotationVector = MutableLiveData<FloatArray>()
    val rotationVector: LiveData<FloatArray> = _rotationVector

    private val _timer = MutableLiveData<Long>()
    val timer: LiveData<Long> = _timer

    private var wifiScanResult: List<ScanResult> = emptyList()
    private var lastFrameTimestamp = 0L
    private var lastSuccessfulWifiScanTimestamp = 0L
    private var elapsedRecordingTime = 0L
    private var recordingTimer: CountDownTimer? = null
    private var recordCounter = 0
    private var wifiRecordCounter = 0

    var recordedVIO: Array<RecordedVIO?> = arrayOfNulls(BUFFER_SIZE)
        private set
    var recordedWiFi: Array<RecordedWiFi?> = arrayOfNulls(BUFFER_SIZE)
        private set
    var recordedSensor: Array<RecordedIMU?> = arrayOfNulls(BUFFER_SIZE)
        private set

    fun setWifiResult(result: List<ScanResult>, timestamp: Long) {
        // Only update the latest successful WiFi scan if the new timestamp is greater.
        // This is to prevent UI from updating too frequently.
        if (timestamp > lastSuccessfulWifiScanTimestamp) {
            lastSuccessfulWifiScanTimestamp = timestamp
        } else {
            return
        }

        // Update the number of detected APs. Only include the APs that are in the regex.
        wifiScanResult = Utils.filterWifiResult(result, _wifiRegex.value)
        _detectedApCount.postValue(wifiScanResult.size)
    }

    fun setFileName(fileName: String) {
        val fileNameWithoutSpaces = fileName.replace(" ", "")
        _fileName.postValue(fileNameWithoutSpaces)
    }

    fun setWifiRegex(regex: String) {
        _wifiRegex.postValue(regex)
    }

    fun showDebug(show: Boolean) {
        _showDebug.postValue(show)
    }

    fun showPointCloud(show: Boolean) {
        _showPointCloud.postValue(show)
    }

    fun startRecording() {
        recordingTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                elapsedRecordingTime += 1000
                _timer.postValue(elapsedRecordingTime / 1000)
            }

            override fun onFinish() {
                // Do nothing.
            }
        }.start()

        _isRecording.value = true
    }

    fun stopRecording() {
        recordingTimer?.cancel()
        elapsedRecordingTime = 0
        _isRecording.value = false
        recordCounter = 0
        wifiRecordCounter = 0
    }

    fun processData(frame: Frame, screenWidth: Int, screenHeight: Int, sensorResult: FloatArray, timestamp: Long) {
        processARFrame(frame, screenWidth, screenHeight)

        val pose = frame.camera.pose
        val translation = pose.translation
        val rotation = pose.rotationQuaternion

        val magnetometer = sensorResult.sliceArray(0..2)
        val accelerometer = sensorResult.sliceArray(3..5)
        val gyroscope = sensorResult.sliceArray(6..8)
        val rotationVector = sensorResult.sliceArray(9..12)

        _poseTranslation.postValue(translation)
        _poseRotation.postValue(rotation)
        _rotationVector.postValue(rotationVector)

        if (_isRecording.value == false) {
            _magnetometer.postValue(magnetometer)
            _accelerometer.postValue(accelerometer)
            _gyroscope.postValue(gyroscope)
        }

        if (_isRecording.value == true) {
//            CoroutineScope(Dispatchers.Default).launch {
                recordSensorData(timestamp, translation, rotation, magnetometer, accelerometer, gyroscope, rotationVector, frame.camera.trackingState.toString())
//            }
        }
    }

    private fun recordSensorData(timestamp: Long, translation: FloatArray, rotation: FloatArray, magnetometer: FloatArray, accelerometer: FloatArray, gyroscope: FloatArray, rotationVector: FloatArray, trackingState: String) {
//        withContext(Dispatchers.IO) {
            val vioData = RecordedVIO(
                timestamp = timestamp,
                positionX = translation[0],
                positionY = translation[1],
                positionZ = translation[2],
                quaternionX = rotation[0],
                quaternionY = rotation[1],
                quaternionZ = rotation[2],
                quaternionW = rotation[3],
                state = trackingState
            )
            recordedVIO[recordCounter % BUFFER_SIZE] = vioData

            for (scanResult in wifiScanResult) {
                val wifiData = RecordedWiFi(
                    timestamp = timestamp,
                    successfulTimestamp = lastSuccessfulWifiScanTimestamp,
                    rssi = scanResult.level,
                    ssid = scanResult.SSID,
                    bssid = scanResult.BSSID,
                    frequency = scanResult.frequency,
                    lastSeen = scanResult.timestamp
                )

                recordedWiFi[wifiRecordCounter % BUFFER_SIZE] = wifiData
                wifiRecordCounter++
            }

            val imuData = RecordedIMU(
                timestamp = timestamp,
                magnetometerX = magnetometer[0],
                magnetometerY = magnetometer[1],
                magnetometerZ = magnetometer[2],
                accelerometerX = accelerometer[0],
                accelerometerY = accelerometer[1],
                accelerometerZ = accelerometer[2],
                gyroscopeX = gyroscope[0],
                gyroscopeY = gyroscope[1],
                gyroscopeZ = gyroscope[2],
                rotationVectorX = rotationVector[0],
                rotationVectorY = rotationVector[1],
                rotationVectorZ = rotationVector[2],
                rotationVectorW = rotationVector[3]
            )

            recordedSensor[recordCounter % BUFFER_SIZE] = imuData
//        }

        recordCounter++
    }

    private fun processARFrame(frame: Frame, screenWidth: Int, screenHeight: Int) {
        val pointCloud = frame.acquirePointCloud()

        // Calculate the FPS.
        val currentFrameTimestamp = frame.timestamp
        val frameTimestampDelta = currentFrameTimestamp - lastFrameTimestamp
        if (frameTimestampDelta > 0) {
            _fps.value = (1e9 / frameTimestampDelta.toDouble()).toInt()
        }
        lastFrameTimestamp = currentFrameTimestamp

        _trackingState.postValue(frame.camera.trackingState.toString())
        _featureCount.postValue(countNumberOfFeatures(pointCloud))

        if (_showPointCloud.value == true) {
            try {
                val camera = frame.camera
                val pointList = ArrayList<Point>()
                val points = pointCloud.points

                while (points.hasRemaining()) {
                    val x = points.get()
                    val y = points.get()
                    val z = points.get()
                    points.get() // confidence, not used.

                    val point = Utils.convertToScreenCoordinates(
                        floatArrayOf(x, y, z),
                        camera,
                        screenWidth,
                        screenHeight
                    )
                    pointList.add(Point(point.x.toInt(), point.y.toInt()))
                }

                _pointList.postValue(pointList)
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
            }
        }

        pointCloud.release()
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}