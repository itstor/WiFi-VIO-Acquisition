package com.itstor.wifivioacquisition.models

import com.opencsv.bean.CsvBindByName
import com.opencsv.bean.CsvBindByPosition

data class RecordedWiFi(
    @CsvBindByName(column = "timestamp", required = true)
    override var timestamp: Long,

    @CsvBindByName(column = "successfulTimestamp", required = true)
    var successfulTimestamp: Long,

    @CsvBindByName(column = "rssi", required = true)
    var rssi: Int,

    @CsvBindByName(column = "ssid", required = true)
    var ssid: String,

    @CsvBindByName(column = "bssid", required = true)
    var bssid: String,

    @CsvBindByName(column = "frequency", required = true)
    var frequency: Int,

    @CsvBindByName(column = "lastSeen", required = true)
    var lastSeen: Long
) : BaseRecordedData(timestamp) {
    override fun toListOfStrings(): List<String> {
        return listOf(
            timestamp.toString(),
            successfulTimestamp.toString(),
            rssi.toString(),
            ssid,
            bssid,
            frequency.toString()
        )
    }

    override fun toArrayOfStrings(): Array<String> {
        return arrayOf(
            timestamp.toString(),
            successfulTimestamp.toString(),
            rssi.toString(),
            ssid,
            bssid,
            frequency.toString()
        )
    }
}