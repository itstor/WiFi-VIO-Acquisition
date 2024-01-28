package com.itstor.wifivioacquisition.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.itstor.wifivioacquisition.models.BaseRecordedData
import com.opencsv.CSVWriter
import com.opencsv.bean.StatefulBeanToCsvBuilder
import java.io.FileWriter
import java.io.IOException
import java.io.OutputStreamWriter


class FileUtils {
    companion object {
        private const val TAG = "FileUtils"

        /**
         * Appends a list of data to a CSV file
         * @param filePath The path to the CSV file
         * @param dataToAppend The data to append to the CSV file
         * @throws IOException
         */
        fun<T: BaseRecordedData> appendToCSV(filePath: String, dataToAppend: List<T>) {
            try {
                FileWriter(filePath, true).use { writer ->
                    CSVWriter(writer).use { csvWriter ->
                        dataToAppend.forEach {
                            csvWriter.writeNext(it.toArrayOfStrings(), true)
                        }
                    }
                }
            } catch (e: IOException) {
                throw e
            }
        }

        /**
         * Writes a list of data to a CSV file
         * @param fileName The name of the CSV file
         * @param dataToWrite The data to write to the CSV file
         * @param resolver The content resolver
         * @throws IOException
         */
        fun <T> writeToCSV(fileName: String, dataToWrite: List<T>, resolver: ContentResolver): String {
            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        OutputStreamWriter(outputStream).use { writer ->
                            StatefulBeanToCsvBuilder<T>(writer)
                                .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
                                .build()
                                .write(dataToWrite)
                        }
                    }
                }

                return uri.toString()
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
                throw IOException("Failed to write to CSV file.")
            }
        }
    }
}