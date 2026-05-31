package com.example.easycamera.data.export

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

sealed class ExportResult {
    data class Ready(val tempFile: File, val suggestedName: String) : ExportResult()
    data class Success(val outputPath: String) : ExportResult()
    data class Warning(val message: String, val tempFile: File, val suggestedName: String) : ExportResult()
    data class Error(val message: String) : ExportResult()
}

object ProjectExportManager {

    private const val PROJECT_ROOT_NAME = "EasyCamera"

    fun prepareExport(context: Context, region: String, date: String): ExportResult {
        val projectDir = File(
            context.getExternalFilesDir(null),
            "$PROJECT_ROOT_NAME/${region}_$date"
        )

        if (!projectDir.exists() || !projectDir.isDirectory) {
            return ExportResult.Error("项目目录不存在")
        }

        val imagesDir = File(projectDir, "images")
        if (!imagesDir.exists() || !imagesDir.isDirectory) {
            return ExportResult.Error("当前项目暂无照片，无法导出")
        }

        val imageFiles = imagesDir.listFiles()?.filter { f ->
            f.name.endsWith(".jpg", ignoreCase = true) ||
                    f.name.endsWith(".jpeg", ignoreCase = true)
        } ?: emptyList()

        if (imageFiles.isEmpty()) {
            return ExportResult.Error("当前项目暂无照片，无法导出")
        }

        val metadataFile = File(projectDir, "metadata.csv")
        val hasMetadata = metadataFile.exists() && metadataFile.isFile

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val suggestedName = "${region}_${date}_$timestamp.zip"
        val tempZip = File(context.cacheDir, suggestedName)

        try {
            ZipOutputStream(FileOutputStream(tempZip)).use { zos ->
                for (imageFile in imageFiles) {
                    addToZip(zos, imageFile, "${region}_$date/images/${imageFile.name}")
                }

                if (hasMetadata) {
                    addToZip(zos, metadataFile, "${region}_$date/metadata.csv")
                }
            }
        } catch (e: Exception) {
            tempZip.delete()
            return ExportResult.Error("导出失败：${e.message ?: "未知错误"}")
        }

        val warning = if (!hasMetadata) "未找到 metadata.csv，仅导出图片" else null
        val ready = ExportResult.Ready(tempFile = tempZip, suggestedName = suggestedName)
        return if (warning != null) ExportResult.Warning(warning, tempZip, suggestedName) else ready
    }

    private fun addToZip(zos: ZipOutputStream, file: File, entryName: String) {
        val entry = ZipEntry(entryName)
        entry.size = file.length()
        entry.time = file.lastModified()
        zos.putNextEntry(entry)
        file.inputStream().use { input ->
            input.copyTo(zos)
        }
        zos.closeEntry()
    }
}