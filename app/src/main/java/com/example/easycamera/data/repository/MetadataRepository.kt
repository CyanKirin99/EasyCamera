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

    fun swapFieldCode(
        region: String,
        date: String,
        fieldCodeA: String,
        fieldCodeB: String
    ): Boolean {
        return try {
            val file = getMetadataFile(region, date)
            if (!file.exists()) return true

            val allLines = CsvUtils.readAllLines(file)
            if (allLines.isEmpty()) return true

            val updatedData = allLines.drop(1).map { row ->
                if (row.size >= 12 && row[0] == region && row[1] == date) {
                    if (row[2] == fieldCodeA) {
                        val newFilename = row[9].replace("_${fieldCodeA}_", "_${fieldCodeB}_")
                        val newRelPath = row[10].replace("_${fieldCodeA}_", "_${fieldCodeB}_")
                        val newFilePath = row[11].replace("_${fieldCodeA}_", "_${fieldCodeB}_")
                        row.toMutableList().apply {
                            this[2] = fieldCodeB
                            this[9] = newFilename
                            this[10] = newRelPath
                            this[11] = newFilePath
                        }
                    } else if (row[2] == fieldCodeB) {
                        val newFilename = row[9].replace("_${fieldCodeB}_", "_${fieldCodeA}_")
                        val newRelPath = row[10].replace("_${fieldCodeB}_", "_${fieldCodeA}_")
                        val newFilePath = row[11].replace("_${fieldCodeB}_", "_${fieldCodeA}_")
                        row.toMutableList().apply {
                            this[2] = fieldCodeA
                            this[9] = newFilename
                            this[10] = newRelPath
                            this[11] = newFilePath
                        }
                    } else {
                        row
                    }
                } else {
                    row
                }
            }

            return rewriteCsv(file, updatedData)
        } catch (e: Exception) {
            false
        }
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

            val dataLines = allLines.drop(1)

            val filteredData = dataLines.filter { row ->
                val rowRegion = row.getOrElse(0) { "" }
                val rowDate = row.getOrElse(1) { "" }
                val rowField = row.getOrElse(2) { "" }
                val rowSample = row.getOrElse(3) { "" }
                !(rowRegion == region && rowDate == date && rowField == fieldCode && rowSample == sampleCode)
            }

            return rewriteCsv(file, filteredData)
        } catch (e: Exception) {
            false
        }
    }

    /** Deletes a single metadata record matching by filename (unique within a project). */
    fun deleteRecord(region: String, date: String, filename: String): Boolean {
        return try {
            val file = getMetadataFile(region, date)
            if (!file.exists()) return true

            val allLines = CsvUtils.readAllLines(file)
            if (allLines.isEmpty()) return true

            val filteredData = allLines.drop(1).filter { row ->
                row.getOrElse(9) { "" } != filename
            }

            return rewriteCsv(file, filteredData)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Updates the field_code column and file path info for all records matching
     * (region, date, oldFieldCode, sampleCode) to use newFieldCode instead.
     */
    fun updateFieldCode(
        region: String,
        date: String,
        oldFieldCode: String,
        sampleCode: String,
        newFieldCode: String
    ): Boolean {
        return try {
            val file = getMetadataFile(region, date)
            if (!file.exists()) return true

            val allLines = CsvUtils.readAllLines(file)
            if (allLines.isEmpty()) return true

            val updatedData = allLines.drop(1).map { row ->
                if (row.size >= 12 &&
                    row[0] == region &&
                    row[1] == date &&
                    row[2] == oldFieldCode &&
                    row[3] == sampleCode
                ) {
                    val oldFilename = row[9]
                    val newFilename = oldFilename.replace("_${oldFieldCode}_", "_${newFieldCode}_")
                    val oldRelPath = row[10]
                    val newRelPath = oldRelPath.replace("_${oldFieldCode}_", "_${newFieldCode}_")
                    val oldFilePath = row[11]
                    val newFilePath = oldFilePath.replace("_${oldFieldCode}_", "_${newFieldCode}_")
                    row.toMutableList().apply {
                        this[2] = newFieldCode
                        this[9] = newFilename
                        this[10] = newRelPath
                        this[11] = newFilePath
                    }
                } else {
                    row
                }
            }

            return rewriteCsv(file, updatedData)
        } catch (e: Exception) {
            false
        }
    }

    private fun rewriteCsv(file: File, dataLines: List<List<String>>): Boolean {
        val headerOk = CsvUtils.writeHeader(file, HEADERS)
        if (!headerOk) return false
        for (row in dataLines) {
            if (!CsvUtils.appendLine(file, row)) return false
        }
        return true
    }
}