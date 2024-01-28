package com.itstor.wifivioacquisition

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class SensorListener(context: Context, private val sensorDelay: Int = SensorManager.SENSOR_DELAY_GAME, private val onAccuracyChangedCallback: (Sensor?, Int) -> Unit = { _, _ -> }) : SensorEventListener {
    private var sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private var accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private var rotationVector: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private var magnetometerValues: FloatArray = FloatArray(3)
    private var accelerometerValues: FloatArray = FloatArray(3)
    private var gyroscopeValues: FloatArray = FloatArray(3)
    private var rotationVectorValues: FloatArray = FloatArray(4)

    /**
     * Starts logging sensor data.
     */
    fun start() {
        sensorManager.registerListener(this, magnetometer, sensorDelay)
        sensorManager.registerListener(this, accelerometer, sensorDelay)
        sensorManager.registerListener(this, gyroscope, sensorDelay)
        sensorManager.registerListener(this, rotationVector, sensorDelay)
    }

    /**
     * Unregisters the sensor listener.
     */
    fun unregisterListener() {
        sensorManager.unregisterListener(this)
    }

    /**
     * Gets the latest sensor data.
     * @return A float array of sensor data. Contains 13 elements in the following order: magnetometer, accelerometer, gyroscope, rotation vector.
     */
    fun getSensorData(): FloatArray {
        return magnetometerValues + accelerometerValues + gyroscopeValues + rotationVectorValues
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) {
            return
        }

        when (event.sensor.type) {
            Sensor.TYPE_MAGNETIC_FIELD -> {
                magnetometerValues = event.values.clone()
            }
            Sensor.TYPE_ACCELEROMETER -> {
                accelerometerValues = event.values.clone()
            }
            Sensor.TYPE_GYROSCOPE -> {
                gyroscopeValues = event.values.clone()
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                rotationVectorValues = event.values.clone()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        onAccuracyChangedCallback(sensor, accuracy)
    }
}