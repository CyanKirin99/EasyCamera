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
     * Swaps all photos between fieldCodeA and fieldCodeB across the entire project.
     * Uses copy-to-backup strategy for safety: both field's files are copied to a
     * temp directory, then originals are deleted, then backup copies are restored
     * with swapped field code names. The entire operation rolls back on any failure.
     */
    fun swapFieldCode(
        project: CaptureProject,
        fieldCodeA: String,
        fieldCodeB: String
    ): Boolean {
        val imagesDir = File(project.imageDirPath)
        if (!imagesDir.exists()) return false

        val prefixA = "${project.region}_${project.date}_${fieldCodeA}_"
        val prefixB = "${project.region}_${project.date}_${fieldCodeB}_"

        // Collect all files for both field codes
        val filesA = imagesDir.listFiles()?.filter { file ->
            file.name.startsWith(prefixA) &&
                (file.name.endsWith(".jpg", ignoreCase = true) ||
                    file.name.endsWith(".jpeg", ignoreCase = true))
        } ?: emptyList()

        val filesB = imagesDir.listFiles()?.filter { file ->
            file.name.startsWith(prefixB) &&
                (file.name.endsWith(".jpg", ignoreCase = true) ||
                    file.name.endsWith(".jpeg", ignoreCase = true))
        } ?: emptyList()

        if (filesA.isEmpty() && filesB.isEmpty()) return false

        // Create a temp backup directory outside imagesDir
        val tempDir = File(imagesDir.parentFile, "._swap_backup_${System.currentTimeMillis()}")
        if (!tempDir.mkdirs()) return false

        try {
            // 1. Copy all fieldCodeA files to backup
            for (file in filesA) {
                file.copyTo(File(tempDir, file.name), overwrite = true)
            }
            // 2. Copy all fieldCodeB files to backup
            for (file in filesB) {
                file.copyTo(File(tempDir, file.name), overwrite = true)
            }

            // 3. Delete original files for both field codes
            for (file in filesA) file.delete()
            for (file in filesB) file.delete()

            // 4. Restore fieldCodeA's backup with fieldCodeB naming
            for (file in filesA) {
                val newName = file.name.replace("_${fieldCodeA}_", "_${fieldCodeB}_")
                File(tempDir, file.name).renameTo(File(imagesDir, newName))
            }

            // 5. Restore fieldCodeB's backup with fieldCodeA naming
            for (file in filesB) {
                val newName = file.name.replace("_${fieldCodeB}_", "_${fieldCodeA}_")
                File(tempDir, file.name).renameTo(File(imagesDir, newName))
            }

            // 6. Cleanup temp dir
            tempDir.deleteRecursively()
            return true
        } catch (e: Exception) {
            // Rollback: restore everything from backup
            tempDir.listFiles()?.forEach { backup ->
                backup.renameTo(File(imagesDir, backup.name))
            }
            tempDir.deleteRecursively()
            return false
        }
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