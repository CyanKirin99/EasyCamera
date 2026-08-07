package com.example.easycamera.data.repository

import android.content.Context
import com.example.easycamera.data.file.CsvUtils
import com.example.easycamera.data.file.FileNameParser
import com.example.easycamera.data.model.CaptureMetadata
import java.io.File

class MetadataRepository(private val context: Context) {

    /**
     * Safely replaces a component in a filename by index, avoiding the replace-all bug.
     * Filename format: {region}_{date}_{fieldCode}_{sampleCode}_{angleCode}_{longitude}_{latitude}.jpg
     * @param filename the original filename
     * @param index the 0-based index of the component to replace (2=fieldCode, 3=sampleCode)
     * @param newValue the new value for that component
     */
    private fun replaceFilenameComponent(filename: String, index: Int, newValue: String): String {
        val parts = filename.split("_")
        if (index < 0 || index >= parts.size) return filename
        return parts.toMutableList().apply { this[index] = newValue }.joinToString("_")
    }

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
            "file_path",
            "BBCH",
            "plant_height",
            "extra_fields"
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
                        val newFilename = replaceFilenameComponent(row[9], 2, fieldCodeB)
                        val newRelPath = replaceFilenameComponent(row[10], 2, fieldCodeB)
                        val newFilePath = replaceFilenameComponent(row[11], 2, fieldCodeB)
                        row.toMutableList().apply {
                            this[2] = fieldCodeB
                            this[9] = newFilename
                            this[10] = newRelPath
                            this[11] = newFilePath
                        }
                    } else if (row[2] == fieldCodeB) {
                        val newFilename = replaceFilenameComponent(row[9], 2, fieldCodeA)
                        val newRelPath = replaceFilenameComponent(row[10], 2, fieldCodeA)
                        val newFilePath = replaceFilenameComponent(row[11], 2, fieldCodeA)
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
                metadata.filePath,
                metadata.bbch,
                metadata.plantHeight,
                if (metadata.extraFields.isEmpty()) "" else
                    metadata.extraFields.entries.joinToString(";") { "${it.key}=${it.value}" }
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
     * Finds a single metadata record from CSV matching (region, date, fieldCode, sampleCode, angleCode).
     * Returns null if no match is found.
     */
    fun findMatchingRecord(
        region: String,
        date: String,
        fieldCode: String,
        sampleCode: String,
        angleCode: String
    ): CaptureMetadata? {
        return try {
            val file = getMetadataFile(region, date)
            if (!file.exists()) return null

            val allLines = CsvUtils.readAllLines(file)
            if (allLines.size < 2) return null

            for (row in allLines.drop(1)) {
                if (row.size >= 12 &&
                    row[0] == region &&
                    row[1] == date &&
                    row[2] == fieldCode &&
                    row[3] == sampleCode &&
                    row[4] == angleCode
                ) {
                    return CaptureMetadata(
                        region = row[0],
                        date = row[1],
                        fieldCode = row[2],
                        sampleCode = row[3],
                        angleCode = row[4],
                        longitude = row.getOrElse(5) { "" },
                        latitude = row.getOrElse(6) { "" },
                        operator = row.getOrElse(7) { "" },
                        captureTime = row.getOrElse(8) { "" },
                        filename = row.getOrElse(9) { "" },
                        relativePath = row.getOrElse(10) { "" },
                        filePath = row.getOrElse(11) { "" },
                        bbch = row.getOrElse(12) { "" },
                        plantHeight = row.getOrElse(13) { "" },
                        extraFields = parseExtraFields(row.getOrElse(14) { "" })
                    )
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Finds ALL metadata records for a given (region, date, fieldCode, sampleCode) across all angles.
     * Returns a list of matching CaptureMetadata. Empty list if no matches.
     */
    fun findSampleGroupRecords(
        region: String,
        date: String,
        fieldCode: String,
        sampleCode: String
    ): List<CaptureMetadata> {
        val result = mutableListOf<CaptureMetadata>()
        try {
            val file = getMetadataFile(region, date)
            if (file.exists()) {
                val allLines = CsvUtils.readAllLines(file)
                if (allLines.size >= 2) {
                    for (row in allLines.drop(1)) {
                        if (row.size >= 12 &&
                            row[0] == region &&
                            row[1] == date &&
                            row[2] == fieldCode &&
                            row[3] == sampleCode
                        ) {
                            result.add(
                                CaptureMetadata(
                                    region = row[0],
                                    date = row[1],
                                    fieldCode = row[2],
                                    sampleCode = row[3],
                                    angleCode = row[4],
                                    longitude = row.getOrElse(5) { "" },
                                    latitude = row.getOrElse(6) { "" },
                                    operator = row.getOrElse(7) { "" },
                                    captureTime = row.getOrElse(8) { "" },
                                    filename = row.getOrElse(9) { "" },
                                    relativePath = row.getOrElse(10) { "" },
                                    filePath = row.getOrElse(11) { "" },
                                    bbch = row.getOrElse(12) { "" },
                                    plantHeight = row.getOrElse(13) { "" },
                                    extraFields = parseExtraFields(row.getOrElse(14) { "" })
                                )
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) { }

        if (result.isNotEmpty()) return result

        // Fallback: scan filesystem for matching files when CSV is missing or incomplete.
        // This handles cases where files exist on disk but CSV metadata wasn't written
        // (e.g., imported projects where CSV may not be present).
        try {
            val imagesDir = File(
                context.getExternalFilesDir(null),
                "EasyCamera/${region}_${date}/images"
            )
            if (imagesDir.exists()) {
                val prefix = "${region}_${date}_${fieldCode}_${sampleCode}_"
                imagesDir.listFiles()?.forEach { file ->
                    if ((file.name.endsWith(".jpg", ignoreCase = true) || file.name.endsWith(".jpeg", ignoreCase = true)) &&
                        file.name.startsWith(prefix)
                    ) {
                        val parsed = FileNameParser.parse(file.name)
                        if (parsed != null) {
                            result.add(
                                CaptureMetadata(
                                    region = parsed.region,
                                    date = parsed.date,
                                    fieldCode = parsed.fieldCode,
                                    sampleCode = parsed.sampleCode,
                                    angleCode = parsed.angleCode,
                                    longitude = parsed.longitude,
                                    latitude = parsed.latitude,
                                    operator = "",
                                    captureTime = "",
                                    filename = file.name,
                                    relativePath = "EasyCamera/${region}_${date}/images/${file.name}",
                                    filePath = file.absolutePath
                                )
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) { }

        return result
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
                    val newFilename = replaceFilenameComponent(oldFilename, 2, newFieldCode)
                    val oldRelPath = row[10]
                    val newRelPath = replaceFilenameComponent(oldRelPath, 2, newFieldCode)
                    val oldFilePath = row[11]
                    val newFilePath = replaceFilenameComponent(oldFilePath, 2, newFieldCode)
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

    /**
     * Updates the sample_code column and file path info for all records matching
     * (region, date, fieldCode, oldSampleCode) to use newSampleCode instead.
     */
    fun updateSampleCode(
        region: String,
        date: String,
        fieldCode: String,
        oldSampleCode: String,
        newSampleCode: String
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
                    row[2] == fieldCode &&
                    row[3] == oldSampleCode
                ) {
                    val oldFilename = row[9]
                    val newFilename = replaceFilenameComponent(oldFilename, 3, newSampleCode)
                    val oldRelPath = row[10]
                    val newRelPath = replaceFilenameComponent(oldRelPath, 3, newSampleCode)
                    val oldFilePath = row[11]
                    val newFilePath = replaceFilenameComponent(oldFilePath, 3, newSampleCode)
                    row.toMutableList().apply {
                        this[3] = newSampleCode
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

    /**
     * Swaps sample_code values between two sample codes within the same field,
     * updating all CSV rows and file path references.
     */
    fun swapSampleCode(
        region: String,
        date: String,
        fieldCode: String,
        sampleCodeA: String,
        sampleCodeB: String
    ): Boolean {
        return try {
            val file = getMetadataFile(region, date)
            if (!file.exists()) return true

            val allLines = CsvUtils.readAllLines(file)
            if (allLines.isEmpty()) return true

            val updatedData = allLines.drop(1).map { row ->
                if (row.size >= 12 && row[0] == region && row[1] == date && row[2] == fieldCode) {
                    if (row[3] == sampleCodeA) {
                        val newFilename = replaceFilenameComponent(row[9], 3, sampleCodeB)
                        val newRelPath = replaceFilenameComponent(row[10], 3, sampleCodeB)
                        val newFilePath = replaceFilenameComponent(row[11], 3, sampleCodeB)
                        row.toMutableList().apply {
                            this[3] = sampleCodeB
                            this[9] = newFilename
                            this[10] = newRelPath
                            this[11] = newFilePath
                        }
                    } else if (row[3] == sampleCodeB) {
                        val newFilename = replaceFilenameComponent(row[9], 3, sampleCodeA)
                        val newRelPath = replaceFilenameComponent(row[10], 3, sampleCodeA)
                        val newFilePath = replaceFilenameComponent(row[11], 3, sampleCodeA)
                        row.toMutableList().apply {
                            this[3] = sampleCodeA
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

    /**
     * Swaps sample_code AND field_code values between two (field, sample) combinations,
     * updating all CSV rows and file path references.
     */
    fun swapFieldSampleCode(
        region: String,
        date: String,
        oldFieldCode: String,
        oldSampleCode: String,
        newFieldCode: String,
        newSampleCode: String
    ): Boolean {
        return try {
            val file = getMetadataFile(region, date)
            if (!file.exists()) return true

            val allLines = CsvUtils.readAllLines(file)
            if (allLines.isEmpty()) return true

            val updatedData = allLines.drop(1).map { row ->
                if (row.size >= 12 && row[0] == region && row[1] == date) {
                    if (row[2] == oldFieldCode && row[3] == oldSampleCode) {
                        val newFilename = replaceFilenameComponent(
                            replaceFilenameComponent(row[9], 2, newFieldCode), 3, newSampleCode)
                        val newRelPath = replaceFilenameComponent(
                            replaceFilenameComponent(row[10], 2, newFieldCode), 3, newSampleCode)
                        val newFilePath = replaceFilenameComponent(
                            replaceFilenameComponent(row[11], 2, newFieldCode), 3, newSampleCode)
                        row.toMutableList().apply {
                            this[2] = newFieldCode
                            this[3] = newSampleCode
                            this[9] = newFilename
                            this[10] = newRelPath
                            this[11] = newFilePath
                        }
                    } else if (row[2] == newFieldCode && row[3] == newSampleCode) {
                        val newFilename = replaceFilenameComponent(
                            replaceFilenameComponent(row[9], 2, oldFieldCode), 3, oldSampleCode)
                        val newRelPath = replaceFilenameComponent(
                            replaceFilenameComponent(row[10], 2, oldFieldCode), 3, oldSampleCode)
                        val newFilePath = replaceFilenameComponent(
                            replaceFilenameComponent(row[11], 2, oldFieldCode), 3, oldSampleCode)
                        row.toMutableList().apply {
                            this[2] = oldFieldCode
                            this[3] = oldSampleCode
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

    /**
     * Deletes all metadata records for a specific sample group within a field.
     */
    fun deleteSampleCodeGroup(
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

            val filteredData = allLines.drop(1).filter { row ->
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

    /**
     * Reads ALL metadata records from a project's CSV file.
     * Returns empty list if the file does not exist or cannot be read.
     */
    fun readAllMetadata(region: String, date: String): List<CaptureMetadata> {
        val result = mutableListOf<CaptureMetadata>()
        try {
            val file = getMetadataFile(region, date)
            if (file.exists()) {
                val allLines = CsvUtils.readAllLines(file)
                if (allLines.size >= 2) {
                    for (row in allLines.drop(1)) {
                        if (row.size >= 12) {
                            result.add(
                                CaptureMetadata(
                                    region = row[0],
                                    date = row[1],
                                    fieldCode = row[2],
                                    sampleCode = row[3],
                                    angleCode = row[4],
                                    longitude = row.getOrElse(5) { "" },
                                    latitude = row.getOrElse(6) { "" },
                                    operator = row.getOrElse(7) { "" },
                                    captureTime = row.getOrElse(8) { "" },
                                    filename = row.getOrElse(9) { "" },
                                    relativePath = row.getOrElse(10) { "" },
                                    filePath = row.getOrElse(11) { "" },
                                    bbch = row.getOrElse(12) { "" },
                                    plantHeight = row.getOrElse(13) { "" },
                                    extraFields = parseExtraFields(row.getOrElse(14) { "" })
                                )
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) { }
        return result
    }

    /**
     * Finds all metadata records matching (region, date, operator).
     * Returns empty list if no matches found.
     */
    fun findOperatorRecords(region: String, date: String, operator: String): List<CaptureMetadata> {
        return readAllMetadata(region, date).filter { it.operator == operator }
    }

    /**
     * Updates BBCH and plantHeight for ALL rows matching (region, date, fieldCode, sampleCode)
     * in a single CSV read/write cycle. This avoids race conditions when updating multiple angles.
     * Normalizes fieldCode and sampleCode to handle "1" vs "01" format mismatches.
     */
    fun updateSampleBbchAndPlantHeight(
        region: String,
        date: String,
        fieldCode: String,
        sampleCode: String,
        bbch: String,
        plantHeight: String
    ): Boolean {
        return try {
            val file = getMetadataFile(region, date)
            if (!file.exists()) return false

            // Normalize codes for robust matching
            val fcNorm = fieldCode.toIntOrNull()?.let { it.toString().padStart(2, '0') } ?: fieldCode
            val scNorm = sampleCode.toIntOrNull()?.let { it.toString().padStart(2, '0') } ?: sampleCode

            val allLines = CsvUtils.readAllLines(file)
            if (allLines.isEmpty()) return false

            var matchCount = 0
            val updatedData = allLines.drop(1).map { row ->
                // Normalize row codes too for comparison
                val rowFc = (row.getOrElse(2) { "" }).toIntOrNull()?.let { it.toString().padStart(2, '0') } ?: row.getOrElse(2) { "" }
                val rowSc = (row.getOrElse(3) { "" }).toIntOrNull()?.let { it.toString().padStart(2, '0') } ?: row.getOrElse(3) { "" }

                if (row.size >= 5 &&
                    row[0] == region &&
                    row[1] == date &&
                    rowFc == fcNorm &&
                    rowSc == scNorm
                ) {
                    matchCount++
                    row.toMutableList().apply {
                        while (this.size < 15) this.add("")
                        this[12] = bbch
                        this[13] = plantHeight
                    }
                } else {
                    row
                }
            }

            if (matchCount == 0) {
                android.util.Log.w("MetadataRepo", "updateSampleBbchAndPlantHeight: no rows matched for region=$region date=$date field=$fcNorm sample=$scNorm")
                return false
            }

            return rewriteCsv(file, updatedData)
        } catch (e: Exception) {
            android.util.Log.e("MetadataRepo", "updateSampleBbchAndPlantHeight error", e)
            false
        }
    }

    /**
     * Updates the BBCH and plantHeight fields for a specific metadata record
     * matching (region, date, fieldCode, sampleCode, angleCode).
     * Normalizes fieldCode and sampleCode to handle "1" vs "01" format mismatches.
     */
    fun updateBbchAndPlantHeight(
        region: String,
        date: String,
        fieldCode: String,
        sampleCode: String,
        angleCode: String,
        bbch: String,
        plantHeight: String
    ): Boolean {
        return try {
            val file = getMetadataFile(region, date)
            if (!file.exists()) return false

            // Normalize codes for robust matching
            val fcNorm = fieldCode.toIntOrNull()?.let { it.toString().padStart(2, '0') } ?: fieldCode
            val scNorm = sampleCode.toIntOrNull()?.let { it.toString().padStart(2, '0') } ?: sampleCode

            val allLines = CsvUtils.readAllLines(file)
            if (allLines.isEmpty()) return false

            var matchCount = 0
            val updatedData = allLines.drop(1).map { row ->
                // Normalize row codes too for comparison
                val rowFc = (row.getOrElse(2) { "" }).toIntOrNull()?.let { it.toString().padStart(2, '0') } ?: row.getOrElse(2) { "" }
                val rowSc = (row.getOrElse(3) { "" }).toIntOrNull()?.let { it.toString().padStart(2, '0') } ?: row.getOrElse(3) { "" }

                // Match by key fields regardless of total column count (>= 5 is enough)
                if (row.size >= 5 &&
                    row[0] == region &&
                    row[1] == date &&
                    rowFc == fcNorm &&
                    rowSc == scNorm &&
                    row[4] == angleCode
                ) {
                    matchCount++
                    row.toMutableList().apply {
                        while (this.size < 15) this.add("")
                        this[12] = bbch
                        this[13] = plantHeight
                    }
                } else {
                    row
                }
            }

            if (matchCount == 0) {
                android.util.Log.w("MetadataRepo", "updateBbchAndPlantHeight: no rows matched for region=$region date=$date field=$fcNorm sample=$scNorm angle=$angleCode")
                return false
            }

            return rewriteCsv(file, updatedData)
        } catch (e: Exception) {
            android.util.Log.e("MetadataRepo", "updateBbchAndPlantHeight error", e)
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

    private fun parseExtraFields(value: String): Map<String, String> {
        if (value.isBlank()) return emptyMap()
        return try {
            value.split(";").mapNotNull { pair ->
                val eqIdx = pair.indexOf('=')
                if (eqIdx > 0 && eqIdx < pair.length - 1) {
                    pair.substring(0, eqIdx) to pair.substring(eqIdx + 1)
                } else null
            }.toMap()
        } catch (_: Exception) { emptyMap() }
    }
}