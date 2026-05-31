package com.example.easycamera.data.repository

import android.content.Context
import com.example.easycamera.data.file.FileNameParser
import com.example.easycamera.data.model.CaptureProject
import com.example.easycamera.data.model.CapturedPhoto
import java.io.File

class PhotoGalleryRepository(private val context: Context) {

    private val easyCameraRoot: File
        get() = File(context.getExternalFilesDir(null), "EasyCamera")

    fun scanProjects(): List<CaptureProject> {
        val root = easyCameraRoot
        if (!root.exists() || !root.isDirectory) return emptyList()

        return root.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { dir ->
                val name = dir.name
                val parts = name.split("_")
                if (parts.size < 2) return@mapNotNull null
                val region = parts[0]
                val date = parts[1]
                val imagesDir = File(dir, "images")
                val photoCount = if (imagesDir.exists() && imagesDir.isDirectory) {
                    imagesDir.listFiles()?.count { f ->
                        f.name.endsWith(".jpg", ignoreCase = true) ||
                                f.name.endsWith(".jpeg", ignoreCase = true)
                    } ?: 0
                } else 0
                CaptureProject(
                    projectName = name,
                    region = region,
                    date = date,
                    imageDirPath = imagesDir.absolutePath,
                    photoCount = photoCount
                )
            }
            ?.sortedByDescending { it.projectName }
            ?: emptyList()
    }

    fun loadPhotos(project: CaptureProject): List<CapturedPhoto> {
        val imagesDir = File(project.imageDirPath)
        if (!imagesDir.exists() || !imagesDir.isDirectory) return emptyList()

        return imagesDir.listFiles()
            ?.filter { file ->
                val name = file.name.lowercase()
                name.endsWith(".jpg") || name.endsWith(".jpeg")
            }
            ?.mapNotNull { file ->
                val parsed = FileNameParser.parse(file.name)
                if (parsed == null) return@mapNotNull null
                CapturedPhoto(
                    region = parsed.region,
                    date = parsed.date,
                    fieldCode = parsed.fieldCode,
                    sampleCode = parsed.sampleCode,
                    angleCode = parsed.angleCode,
                    longitude = parsed.longitude,
                    latitude = parsed.latitude,
                    filename = file.name,
                    filePath = file.absolutePath,
                    lastModified = file.lastModified()
                )
            }
            ?.sortedWith(compareBy<CapturedPhoto> {
                it.fieldCode.toIntOrNull() ?: 0
            }.thenBy {
                it.sampleCode.toIntOrNull() ?: 0
            }.thenBy {
                ANGLE_ORDER.indexOf(it.angleCode).let { idx -> if (idx < 0) Int.MAX_VALUE else idx }
            })
            ?: emptyList()
    }

    fun deleteSampleGroup(project: CaptureProject, fieldCode: String, sampleCode: String): Boolean {
        val imagesDir = File(project.imageDirPath)
        if (!imagesDir.exists()) return true

        val prefix = "${project.region}_${project.date}_${fieldCode}_${sampleCode}_"
        var allDeleted = true

        imagesDir.listFiles()?.forEach { file ->
            if (file.name.startsWith(prefix) &&
                (file.name.endsWith(".jpg", ignoreCase = true) ||
                        file.name.endsWith(".jpeg", ignoreCase = true))
            ) {
                if (!file.delete()) allDeleted = false
            }
        }
        return allDeleted
    }

    fun deleteProject(project: CaptureProject): Boolean {
        val parts = project.projectName.split("_")
        if (parts.size < 2) return false
        val region = parts[0]
        val date = parts[1]
        val projectDir = File(easyCameraRoot, project.projectName)
        if (!projectDir.exists()) return true

        val metadataFile = File(projectDir, "metadata.csv")
        if (metadataFile.exists()) metadataFile.delete()

        val imagesDir = File(projectDir, "images")
        if (imagesDir.exists()) {
            imagesDir.listFiles()?.forEach { it.delete() }
            imagesDir.delete()
        }

        projectDir.listFiles()?.forEach { it.delete() }
        return projectDir.delete()
    }

    companion object {
        private val ANGLE_ORDER = listOf("A", "B", "C", "D")
    }
}