package com.example.easycamera.data.repository

import android.content.Context
import com.example.easycamera.data.file.CsvUtils
import com.example.easycamera.data.model.CaptureMetadata
import java.io.File

class MetadataRepository(private val context: Context) {

    companion object {
        val HEADERS = listOf(
            "region",
            "date",
            "field_code",
            "sample_code",
            "angle_code",
            "longitude",
            "latitude",
            "operator",
            "capture_time",
            "filename",
            "relative_path",
            "file_path"
        )
    }

    fun getMetadataFile(region: String, date: String): File {
        val dir = File(
            context.getExternalFilesDir(null),
            "EasyCamera/${region}_${date}"
        )
        return File(dir, "metadata.csv")
    }

    fun appendRecord(metadata: CaptureMetadata): Boolean {
        return try {
            val file = getMetadataFile(metadata.region, metadata.date)
            if (!file.exists()) {
                val headerOk = CsvUtils.writeHeader(file, HEADERS)
                if (!headerOk) return false
            }
            val values = listOf(
                metadata.region,
                metadata.date,
                metadata.fieldCode,
                metadata.sampleCode,
                metadata.angleCode,
                metadata.longitude,
                metadata.latitude,
                metadata.operator,
                metadata.captureTime,
                metadata.filename,
                metadata.relativePath,
                metadata.filePath
            )
            CsvUtils.appendLine(file, values)
        } catch (e: Exception) {
            false
        }
    }

    fun deleteSampleGroup(
        region: String,
        date: String,
        fieldCode: String,
        sampleCode: String
    ): Boolean {
        return try {
            val file = getMetadataFile(region, date)
            if (!file.exists()) return true

            val allLines = CsvUtils.readAllLines(file)
            if (allLines.isEmpty()) return true

            val headerLine = allLines.first()
            val dataLines = allLines.drop(1)

            val filteredData = dataLines.filter { row ->
                val rowRegion = row.getOrElse(0) { "" }
                val rowDate = row.getOrElse(1) { "" }
                val rowField = row.getOrElse(2) { "" }
                val rowSample = row.getOrElse(3) { "" }
                !(rowRegion == region && rowDate == date && rowField == fieldCode && rowSample == sampleCode)
            }

            val headerOk = CsvUtils.writeHeader(file, HEADERS)
            if (!headerOk) return false

            for (row in filteredData) {
                val ok = CsvUtils.appendLine(file, row)
                if (!ok) return false
            }

            true
        } catch (e: Exception) {
            false
        }
    }
}