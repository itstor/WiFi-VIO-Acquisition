package com.itstor.wifivioacquisition.models

import com.opencsv.bean.CsvBindByName
import com.opencsv.bean.CsvBindByPosition

data class RecordedIMU(
    @CsvBindByName(column = "timestamp", required = true)
    override var timestamp: Long,

    @CsvBindByName(column = "magnetometerX", required = true)
    var magnetometerX: Float,

    @CsvBindByName(column = "magnetometerY", required = true)
    var magnetometerY: Float,

    @CsvBindByName(column = "magnetometerZ", required = true)
    var magnetometerZ: Float,

    @CsvBindByName(column = "accelerometerX", required = true)
    var accelerometerX: Float,

    @CsvBindByName(column = "accelerometerY", required = true)
    var accelerometerY: Float,

    @CsvBindByName(column = "accelerometerZ", required = true)
    var accelerometerZ: Float,

    @CsvBindByName(column = "gyroscopeX", required = true)
    var gyroscopeX: Float,

    @CsvBindByName(column = "gyroscopeY", required = true)
    var gyroscopeY: Float,

    @CsvBindByName(column = "gyroscopeZ", required = true)
    var gyroscopeZ: Float,

    @CsvBindByName(column = "rotationVectorX", required = true)
    var rotationVectorX: Float,

    @CsvBindByName(column = "rotationVectorY", required = true)
    var rotationVectorY: Float,

    @CsvBindByName(column = "rotationVectorZ", required = true)
    var rotationVectorZ: Float,

    @CsvBindByName(column = "rotationVectorW", required = true)
    var rotationVectorW: Float
) : BaseRecordedData(timestamp) {
    override fun toListOfStrings(): List<String> {
        return listOf(
            timestamp.toString(),
            magnetometerX.toString(),
            magnetometerY.toString(),
            magnetometerZ.toString(),
            accelerometerX.toString(),
            accelerometerY.toString(),
            accelerometerZ.toString(),
            gyroscopeX.toString(),
            gyroscopeY.toString(),
            gyroscopeZ.toString(),
            rotationVectorX.toString(),
            rotationVectorY.toString(),
            rotationVectorZ.toString(),
            rotationVectorW.toString()
        )
    }

    override fun toArrayOfStrings(): Array<String> {
        return arrayOf(
            timestamp.toString(),
            magnetometerX.toString(),
            magnetometerY.toString(),
            magnetometerZ.toString(),
            accelerometerX.toString(),
            accelerometerY.toString(),
            accelerometerZ.toString(),
            gyroscopeX.toString(),
            gyroscopeY.toString(),
            gyroscopeZ.toString(),
            rotationVectorX.toString(),
            rotationVectorY.toString(),
            rotationVectorZ.toString(),
            rotationVectorW.toString()
        )
    }
}
