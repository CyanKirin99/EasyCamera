package com.example.easycamera.data.imports

import android.content.Context
import com.example.easycamera.data.file.CsvUtils
import com.example.easycamera.data.repository.MetadataRepository
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class ImportEntry(
    val fileName: String,
    val relativePath: String,
    val exists: Boolean,
    val metadataValues: List<String>? = null
)

data class ImportPlan(
    val zipFile: File,
    val region: String,
    val date: String,
    val entries: List<ImportEntry>,
    val hasConflicts: Boolean
)

sealed class ImportResult {
    data class Ready(val zipFile: File, val plan: ImportPlan) : ImportResult()
    data class Success(val importedCount: Int, val skippedCount: Int) : ImportResult()
    data class Cancelled(val message: String) : ImportResult()
    data class Error(val message: String) : ImportResult()
}

object ProjectImportManager {

    private const val PROJECT_ROOT_NAME = "EasyCamera"

    fun analyze(context: Context, zipFile: File): ImportResult {
        val entries = mutableListOf<ImportEntry>()
        val pendingNames = mutableListOf<String>()
        var metadataLines = mutableListOf<List<String>>()
        var detectedRegion = ""
        var detectedDate = ""

        try {
            ZipInputStream(zipFile.inputStream()).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val name = entry.name
                        val fileName = name.substringAfterLast("/")

                        if (name.endsWith(".csv", ignoreCase = true)) {
                            val csvContent = zis.readBytes().toString(Charsets.UTF_8)
                            val lines = csvContent
                                .trimStart('\uFEFF')
                                .lineSequence()
                                .filter { it.isNotBlank() }
                                .toList()
                            for (line in lines.drop(1)) {
                                metadataLines.add(parseLine(line))
                            }
                        } else if (name.endsWith(".jpg", ignoreCase = true) || name.endsWith(".jpeg", ignoreCase = true)) {
                            val pathParts = name.split("/")
                            if (pathParts.size >= 2) {
                                val projectPart = pathParts[0]
                                val projParts = projectPart.split("_")
                                if (projParts.size >= 2) {
                                    detectedRegion = projParts[0]
                                    detectedDate = projParts[1]
                                }
                            }

                            if (metadataLines.isNotEmpty()) {
                                val appDir = File(
                                    context.getExternalFilesDir(null),
                                    "${PROJECT_ROOT_NAME}/${name}"
                                )
                                val exists = appDir.exists()
                                val metadataRow = metadataLines.find { row ->
                                    row.getOrElse(9) { "" } == fileName ||
                                            row.getOrElse(10) { "" }.endsWith(fileName)
                                }
                                entries.add(
                                    ImportEntry(
                                        fileName = fileName,
                                        relativePath = name,
                                        exists = exists,
                                        metadataValues = metadataRow
                                    )
                                )
                            } else {
                                pendingNames.add(name)
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            // Match pending entries (CSV was processed after JPGs in the ZIP)
            for (name in pendingNames) {
                val fileName = name.substringAfterLast("/")
                val appDir = File(
                    context.getExternalFilesDir(null),
                    "${PROJECT_ROOT_NAME}/${name}"
                )
                val exists = appDir.exists()
                val metadataRow = if (metadataLines.isNotEmpty()) {
                    metadataLines.find { row ->
                        row.getOrElse(9) { "" } == fileName ||
                                row.getOrElse(10) { "" }.endsWith(fileName)
                    }
                } else null
                entries.add(
                    ImportEntry(
                        fileName = fileName,
                        relativePath = name,
                        exists = exists,
                        metadataValues = metadataRow
                    )
                )
            }
        } catch (e: Exception) {
            return ImportResult.Error("无法读取压缩包：${e.message ?: "未知错误"}")
        }

        if (entries.isEmpty()) {
            return ImportResult.Error("压缩包中未找到有效的照片文件")
        }

        if (detectedRegion.isEmpty()) {
            return ImportResult.Error("无法从压缩包路径中识别地区信息")
        }

        val hasConflicts = entries.any { it.exists }
        return ImportResult.Ready(
            zipFile = zipFile,
            plan = ImportPlan(
                zipFile = zipFile,
                region = detectedRegion,
                date = detectedDate,
                entries = entries,
                hasConflicts = hasConflicts
            )
        )
    }

    fun executeImport(context: Context, plan: ImportPlan, overwriteExisting: Boolean): ImportResult {
        var importedCount = 0
        var skippedCount = 0

        try {
            ZipInputStream(plan.zipFile.inputStream()).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val name = entry.name
                        if (name.endsWith(".jpg", ignoreCase = true) || name.endsWith(".jpeg", ignoreCase = true)) {
                            val targetFile = File(
                                context.getExternalFilesDir(null),
                                "${PROJECT_ROOT_NAME}/${name}"
                            )

                            if (targetFile.exists() && !overwriteExisting) {
                                skippedCount++
                            } else {
                                targetFile.parentFile?.mkdirs()
                                FileOutputStream(targetFile).use { fos ->
                                    zis.copyTo(fos)
                                }
                                importedCount++
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            val metadataFile = File(
                context.getExternalFilesDir(null),
                "${PROJECT_ROOT_NAME}/${plan.region}_${plan.date}/metadata.csv"
            )

            val newDataLines = mutableListOf<List<String>>()
            for (importEntry in plan.entries) {
                val metadataValues = importEntry.metadataValues
                if (metadataValues != null && metadataValues.size >= 12) {
                    if (metadataValues[0].isNotEmpty()) {
                        val updatedValues = metadataValues.toMutableList()
                        val newRelativePath = "${PROJECT_ROOT_NAME}/${importEntry.relativePath}"
                        val newFilePath = File(
                            context.getExternalFilesDir(null),
                            newRelativePath
                        ).absolutePath
                        if (updatedValues.size > 11) {
                            updatedValues[11] = newFilePath
                        }
                        if (updatedValues.size > 10) {
                            updatedValues[10] = newRelativePath
                        }
                        // Pad to current HEADERS size for backward compatibility
                        while (updatedValues.size < MetadataRepository.HEADERS.size) {
                            updatedValues.add("")
                        }
                        newDataLines.add(updatedValues)
                    }
                }
            }

            // Rewrite CSV: remove duplicate old rows, add new rows
            if (newDataLines.isNotEmpty()) {
                val existingDataLines = if (metadataFile.exists()) {
                    CsvUtils.readAllLines(metadataFile)
                } else {
                    emptyList()
                }
                val existingBody = if (existingDataLines.size > 1) existingDataLines.drop(1) else emptyList()

                // Retain old rows that do NOT conflict with any new row (matched by region+date+field+sample+angle)
                val retainedLines = existingBody.filter { oldRow ->
                    oldRow.size < 5 || !newDataLines.any { newRow ->
                        newRow.size >= 5 &&
                                oldRow[0] == newRow[0] &&
                                oldRow[1] == newRow[1] &&
                                oldRow[2] == newRow[2] &&
                                oldRow[3] == newRow[3] &&
                                oldRow[4] == newRow[4]
                    }
                }

                // Only add new rows that are either forced (overwrite) or have no existing duplicate
                val linesToAdd = newDataLines.filter { newRow ->
                    newRow.size >= 5 && (overwriteExisting || !existingBody.any { oldRow ->
                        oldRow.size >= 5 &&
                                oldRow[0] == newRow[0] &&
                                oldRow[1] == newRow[1] &&
                                oldRow[2] == newRow[2] &&
                                oldRow[3] == newRow[3] &&
                                oldRow[4] == newRow[4]
                    })
                }

                if (retainedLines.isNotEmpty() || linesToAdd.isNotEmpty()) {
                    CsvUtils.writeHeader(metadataFile, MetadataRepository.HEADERS)
                    for (line in retainedLines) CsvUtils.appendLine(metadataFile, line)
                    for (line in linesToAdd) CsvUtils.appendLine(metadataFile, line)
                } else if (!metadataFile.exists() && !existingBody.isEmpty()) {
                    CsvUtils.writeHeader(metadataFile, MetadataRepository.HEADERS)
                    for (line in existingBody) CsvUtils.appendLine(metadataFile, line)
                }
            }

        } catch (e: Exception) {
            return ImportResult.Error("导入失败：${e.message ?: "未知错误"}")
        }

        return ImportResult.Success(importedCount = importedCount, skippedCount = skippedCount)
    }

    private fun parseLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQuotes -> {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i += 2
                    } else {
                        inQuotes = false
                        i++
                    }
                }
                c == '"' && !inQuotes -> {
                    inQuotes = true
                    i++
                }
                c == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current.clear()
                    i++
                }
                else -> {
                    current.append(c)
                    i++
                }
            }
        }
        result.add(current.toString().trim())
        return result
    }
}