package com.itstor.wifivioacquisition.models

import com.opencsv.bean.CsvBindByName
import com.opencsv.bean.CsvBindByPosition

data class RecordedVIO(
    @CsvBindByName(column = "timestamp", required = true)
    override var timestamp: Long,

    @CsvBindByName(column = "positionX", required = true)
    var positionX: Float,

    @CsvBindByName(column = "positionY", required = true)
    var positionY: Float,

    @CsvBindByName(column = "positionZ", required = true)
    var positionZ: Float,

    @CsvBindByName(column = "quaternionX", required = true)
    var quaternionX: Float,

    @CsvBindByName(column = "quaternionY", required = true)
    var quaternionY: Float,

    @CsvBindByName(column = "quaternionZ", required = true)
    var quaternionZ: Float,

    @CsvBindByName(column = "quaternionW", required = true)
    var quaternionW: Float,

    @CsvBindByName(column = "state", required = true)
    var state: String
) : BaseRecordedData(timestamp) {
    override fun toListOfStrings(): List<String> {
        return listOf(
            timestamp.toString(),
            positionX.toString(),
            positionY.toString(),
            positionZ.toString(),
            quaternionX.toString(),
            quaternionY.toString(),
            quaternionZ.toString(),
            quaternionW.toString(),
            state
        )
    }

    override fun toArrayOfStrings(): Array<String> {
        return arrayOf(
            timestamp.toString(),
            positionX.toString(),
            positionY.toString(),
            positionZ.toString(),
            quaternionX.toString(),
            quaternionY.toString(),
            quaternionZ.toString(),
            quaternionW.toString(),
            state
        )
    }
}