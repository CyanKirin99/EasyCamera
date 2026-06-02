package com.example.easycamera.data.repository

import android.content.Context
import com.example.easycamera.data.file.CsvUtils
import com.example.easycamera.data.model.DateDataPoint
import com.example.easycamera.data.model.FieldStats
import com.example.easycamera.data.model.PhotoLocation
import com.example.easycamera.data.model.ProjectStats
import com.example.easycamera.data.model.RegionTimeSeries
import java.io.File

class AnalysisRepository(private val context: Context) {

    fun loadPhotoLocations(region: String, date: String): List<PhotoLocation> {
        val metadataRepo = MetadataRepository(context)
        val file = metadataRepo.getMetadataFile(region, date)
        if (!file.exists()) return emptyList()

        val locations = mutableListOf<PhotoLocation>()
        try {
            val allLines = CsvUtils.readAllLines(file)
            if (allLines.size < 2) return emptyList()

            for (row in allLines.drop(1)) {
                if (row.size >= 12) {
                    val lon = row.getOrElse(5) { "" }.toDoubleOrNull()
                    val lat = row.getOrElse(6) { "" }.toDoubleOrNull()
                    if (lon != null && lat != null && lon != 0.0 && lat != 0.0) {
                        locations.add(
                            PhotoLocation(
                                fieldCode = row[2],
                                sampleCode = row[3],
                                angleCode = row[4],
                                longitude = lon,
                                latitude = lat
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) { }
        return locations
    }

    fun computeProjectStats(region: String, date: String): ProjectStats {
        val metadataRepo = MetadataRepository(context)
        val file = metadataRepo.getMetadataFile(region, date)

        val allBbch = mutableListOf<Double>()
        val allPlantHeight = mutableListOf<Double>()
        val fieldData = mutableMapOf<String, MutableList<Pair<Double?, Double?>>>()
        val fieldSampleKeys = mutableMapOf<String, MutableSet<String>>()
        val sampleKeys = mutableSetOf<String>()
        var photoCount = 0

        if (file.exists()) {
            try {
                val allLines = CsvUtils.readAllLines(file)
                if (allLines.size >= 2) {
                    for (row in allLines.drop(1)) {
                        if (row.size >= 12) {
                            photoCount++
                            val fieldCode = row[2]
                            val sampleCode = row[3]
                            sampleKeys.add("${fieldCode}_${sampleCode}")
                            fieldSampleKeys.getOrPut(fieldCode) { mutableSetOf() }
                                .add(sampleCode)

                            val bbch = row.getOrElse(12) { "" }.toDoubleOrNull()
                            val ph = row.getOrElse(13) { "" }.toDoubleOrNull()

                            if (bbch != null) allBbch.add(bbch)
                            if (ph != null) allPlantHeight.add(ph)

                            fieldData.getOrPut(fieldCode) { mutableListOf() }
                                .add(bbch to ph)
                        }
                    }
                }
            } catch (_: Exception) { }
        }

        val fieldStats = fieldData.map { (fieldCode, values) ->
            val fieldBbch = values.mapNotNull { it.first }
            val fieldPh = values.mapNotNull { it.second }
            FieldStats(
                fieldCode = fieldCode,
                sampleCount = fieldSampleKeys[fieldCode]?.size ?: 0,
                photoCount = values.size,
                avgBbch = if (fieldBbch.isNotEmpty()) fieldBbch.average() else null,
                avgPlantHeight = if (fieldPh.isNotEmpty()) fieldPh.average() else null
            )
        }.sortedBy { it.fieldCode.toIntOrNull() ?: 0 }

        return ProjectStats(
            region = region,
            date = date,
            totalFields = fieldData.size,
            totalSamples = sampleKeys.size,
            totalPhotos = photoCount,
            avgBbch = if (allBbch.isNotEmpty()) allBbch.average() else null,
            avgPlantHeight = if (allPlantHeight.isNotEmpty()) allPlantHeight.average() else null,
            fieldStats = fieldStats
        )
    }

    fun loadRegionTimeSeries(region: String): RegionTimeSeries {
        val root = File(context.getExternalFilesDir(null), "EasyCamera")
        if (!root.exists() || !root.isDirectory) {
            return RegionTimeSeries(region, emptyList())
        }

        val datePoints = mutableListOf<DateDataPoint>()
        root.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("${region}_") }
            ?.forEach { dir ->
                val parts = dir.name.split("_")
                if (parts.size >= 2) {
                    val date = parts[1]
                    val stats = computeProjectStats(region, date)
                    datePoints.add(
                        DateDataPoint(
                            date = date,
                            avgBbch = stats.avgBbch,
                            avgPlantHeight = stats.avgPlantHeight,
                            fieldCount = stats.totalFields,
                            sampleCount = stats.totalSamples
                        )
                    )
                }
            }

        return RegionTimeSeries(
            region = region,
            datePoints = datePoints.sortedBy { it.date }
        )
    }
}