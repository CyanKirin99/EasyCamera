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
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
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

            val existingLines = if (metadataFile.exists()) {
                CsvUtils.readAllLines(metadataFile)
            } else {
                emptyList()
            }

            val hasAnyMetadata = plan.entries.any { it.metadataValues != null }
            if (hasAnyMetadata && !metadataFile.exists()) {
                CsvUtils.writeHeader(metadataFile, MetadataRepository.HEADERS)
            }

            for (importEntry in plan.entries) {
                val metadataValues = importEntry.metadataValues
                if (metadataValues != null && metadataValues.size == MetadataRepository.HEADERS.size) {
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

                        val isDuplicate = existingLines.any { existingRow ->
                            existingRow.size >= 5 &&
                                    existingRow[0] == updatedValues[0] &&
                                    existingRow[1] == updatedValues[1] &&
                                    existingRow[2] == updatedValues[2] &&
                                    existingRow[3] == updatedValues[3] &&
                                    existingRow[4] == updatedValues[4]
                        }

                        if (!isDuplicate || overwriteExisting) {
                            if (updatedValues[9].isNotEmpty()) {
                                CsvUtils.appendLine(metadataFile, updatedValues)
                            }
                        }
                    }
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