package com.itstor.wifivioacquisition.models

import com.opencsv.bean.CsvBindByName
import com.opencsv.bean.CsvBindByPosition

abstract class BaseRecordedData(
    @CsvBindByName(column = "timestamp", required = true)
    open var timestamp: Long
) {
    abstract fun toListOfStrings(): List<String>

    abstract fun toArrayOfStrings(): Array<String>
}
