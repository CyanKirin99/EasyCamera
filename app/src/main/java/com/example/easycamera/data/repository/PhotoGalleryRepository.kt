package com.example.easycamera.data.repository

import android.content.Context
import com.example.easycamera.data.file.FileNameParser
import com.example.easycamera.data.model.CaptureProject
import com.example.easycamera.data.model.CapturedPhoto
import java.io.File

class PhotoGalleryRepository(private val context: Context) {

    private val easyCameraRoot: File
        get() = File(context.getExternalFilesDir(null), "EasyCamera")

    fun updateFieldCode(photo: CapturedPhoto, newFieldCode: String): Boolean {
        val file = File(photo.filePath)
        if (!file.exists()) return false
        val parentDir = file.parentFile ?: return false
        val newFileName = "${photo.region}_${photo.date}_${newFieldCode}_${photo.sampleCode}_${photo.angleCode}_${photo.longitude}_${photo.latitude}.jpg"
        val newFile = File(parentDir, newFileName)
        return file.renameTo(newFile)
    }

    /**
     * Swaps field codes A and B for all photos matching the given sample code in a project.
     * Handles potential filename collisions by using temporary names.
     */
    fun swapFieldCode(
        project: CaptureProject,
        fieldCodeA: String,
        sampleCode: String,
        fieldCodeB: String
    ): Boolean {
        val imagesDir = File(project.imageDirPath)
        if (!imagesDir.exists()) return false

        val prefixA = "${project.region}_${project.date}_${fieldCodeA}_${sampleCode}_"
        val prefixB = "${project.region}_${project.date}_${fieldCodeB}_${sampleCode}_"

        // 1. Rename A files to temp
        val tempARenames = mutableListOf<Pair<File, File>>()
        imagesDir.listFiles()?.forEach { file ->
            if (file.name.startsWith(prefixA) &&
                (file.name.endsWith(".jpg", ignoreCase = true) ||
                    file.name.endsWith(".jpeg", ignoreCase = true))
            ) {
                val tempFile = File(imagesDir, "._swap_temp_A_${file.name}")
                if (file.renameTo(tempFile)) {
                    tempARenames.add(file to tempFile)
                } else {
                    // Rollback
                    tempARenames.forEach { (orig, temp) -> temp.renameTo(orig) }
                    return false
                }
            }
        }

        // 2. Rename B files to temp
        val tempBRenames = mutableListOf<Pair<File, File>>()
        imagesDir.listFiles()?.forEach { file ->
            if (file.name.startsWith(prefixB) &&
                (file.name.endsWith(".jpg", ignoreCase = true) ||
                    file.name.endsWith(".jpeg", ignoreCase = true))
            ) {
                val tempFile = File(imagesDir, "._swap_temp_B_${file.name}")
                if (file.renameTo(tempFile)) {
                    tempBRenames.add(file to tempFile)
                } else {
                    tempBRenames.forEach { (orig, temp) -> temp.renameTo(orig) }
                    tempARenames.forEach { (orig, temp) -> temp.renameTo(orig) }
                    return false
                }
            }
        }

        // 3. Rename temp A files to fieldCodeB
        for ((origFile, tempFile) in tempARenames) {
            val newName = origFile.name.replace("_${fieldCodeA}_", "_${fieldCodeB}_")
            val newFile = File(imagesDir, newName)
            if (!tempFile.renameTo(newFile)) {
                tempFile.renameTo(origFile)
                tempBRenames.forEach { (orig, temp) -> temp.renameTo(orig) }
                return false
            }
        }

        // 4. Rename temp B files to fieldCodeA
        for ((origFile, tempFile) in tempBRenames) {
            val newName = origFile.name.replace("_${fieldCodeB}_", "_${fieldCodeA}_")
            val newFile = File(imagesDir, newName)
            if (!tempFile.renameTo(newFile)) {
                tempFile.renameTo(origFile)
                tempARenames.forEach { (orig, temp) -> temp.renameTo(orig) }
                return false
            }
        }

        return true
    }


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