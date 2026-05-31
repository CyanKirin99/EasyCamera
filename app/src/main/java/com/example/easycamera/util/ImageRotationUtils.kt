package com.example.easycamera.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

object ImageRotationUtils {

    /**
     * Ensures the JPEG file is in landscape orientation (width >= height).
     * Reads the raw pixel dimensions; if portrait, rotates 90 degrees
     * counter-clockwise in-place. Also resets the EXIF orientation tag
     * to normal to avoid double-rotation when viewing.
     * Returns true if the image is already or was made landscape.
     * On failure the original file is preserved.
     */
    fun rotateJpegIfNeeded(file: File): Boolean {
        return try {
            // Check raw pixel dimensions
            val opts = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, opts)

            if (opts.outWidth < 0 || opts.outHeight < 0) return false

            // If already landscape (width >= height), just reset EXIF and return
            if (opts.outWidth >= opts.outHeight) {
                resetExifOrientation(file)
                return true
            }

            // Decode full bitmap and rotate to landscape
            opts.inJustDecodeBounds = false
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, opts)
                ?: return false

            val matrix = Matrix().apply { postRotate(-90f) }
            val rotated = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )

            FileOutputStream(file).use { out ->
                rotated.compress(Bitmap.CompressFormat.JPEG, 95, out)
                out.flush()
            }

            resetExifOrientation(file)

            bitmap.recycle()
            rotated.recycle()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun resetExifOrientation(file: File) {
        try {
            val exif = ExifInterface(file.absolutePath)
            exif.setAttribute(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL.toString()
            )
            exif.saveAttributes()
        } catch (_: Exception) {
            // EXIF reset is best-effort; non-critical
        }
    }
}