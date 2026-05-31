package com.example.easycamera.data.file

import kotlin.math.roundToLong

object FileNameGenerator {

    fun generateFileName(
        region: String,
        date: String,
        fieldCode: String,
        sampleCode: String,
        angleCode: String,
        longitude: Double?,
        latitude: Double?
    ): String {
        val safeRegion = if (region.isBlank()) "NA" else region
        val safeDate = if (date.isBlank()) "000000" else date
        val safeField = if (fieldCode.isBlank()) "00" else fieldCode
        val safeSample = if (sampleCode.isBlank()) "00" else sampleCode
        val safeAngle = if (angleCode.isBlank()) "X" else angleCode

        val lonStr = if (longitude != null) formatCoordinate(longitude) else "NA"
        val latStr = if (latitude != null) formatCoordinate(latitude) else "NA"

        return "${safeRegion}_${safeDate}_${safeField}_${safeSample}_${safeAngle}_${lonStr}_${latStr}.jpg"
    }

    private fun formatCoordinate(value: Double): String {
        return String.format("%.6f", value)
    }
}