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
        // Scan actual photo files on disk instead of CSV, to avoid issues with
        // CSV file ballooning or corruption.
        val imagesDir = File(context.getExternalFilesDir(null), "EasyCamera/${region}_${date}/images")
        if (!imagesDir.exists() || !imagesDir.isDirectory) return emptyList()

        val locations = mutableListOf<PhotoLocation>()
        try {
            imagesDir.listFiles()
                ?.filter { f ->
                    val name = f.name.lowercase()
                    name.endsWith(".jpg") || name.endsWith(".jpeg")
                }
                ?.forEach { file ->
                    val parsed = com.example.easycamera.data.file.FileNameParser.parse(file.name)
                    if (parsed != null) {
                        val lon = parsed.longitude.toDoubleOrNull()
                        val lat = parsed.latitude.toDoubleOrNull()
                        if (lon != null && lat != null && lon != 0.0 && lat != 0.0) {
                            locations.add(
                                PhotoLocation(
                                    fieldCode = parsed.fieldCode,
                                    sampleCode = parsed.sampleCode,
                                    angleCode = parsed.angleCode,
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

        // Scan actual photo files on disk for accurate counts
        val projectDir = File(context.getExternalFilesDir(null), "EasyCamera/${region}_${date}/images")
        val photoFiles = if (projectDir.exists() && projectDir.isDirectory) {
            projectDir.listFiles()?.filter { f ->
                val name = f.name.lowercase()
                name.endsWith(".jpg") || name.endsWith(".jpeg")
            }?.mapNotNull { f ->
                com.example.easycamera.data.file.FileNameParser.parse(f.name)
            } ?: emptyList()
        } else emptyList()

        // Group files by field+sample for accurate counts
        val diskFieldSampleKeys = mutableMapOf<String, MutableSet<String>>()
        val diskFieldPhotoCount = mutableMapOf<String, Int>()
        var diskTotalPhotos = 0
        val diskSampleKeys = mutableSetOf<String>()

        for (parsed in photoFiles) {
            diskTotalPhotos++
            val key = "${parsed.fieldCode}_${parsed.sampleCode}"
            diskSampleKeys.add(key)
            diskFieldSampleKeys.getOrPut(parsed.fieldCode) { mutableSetOf() }.add(parsed.sampleCode)
            diskFieldPhotoCount[parsed.fieldCode] = (diskFieldPhotoCount[parsed.fieldCode] ?: 0) + 1
        }

        // Read CSV for BBCH/plantHeight metadata (safely handle potentially bloated files)
        val allBbch = mutableListOf<Double>()
        val allPlantHeight = mutableListOf<Double>()
        val csvFieldBbchPh = mutableMapOf<String, MutableList<Pair<Double?, Double?>>>()

        if (file.exists()) {
            try {
                // Only read CSV if it's reasonably sized (< 1MB), otherwise skip to avoid OOM
                if (file.length() > 1_048_576) {
                    android.util.Log.w("AnalysisRepo", "CSV file too large (${file.length()}), skipping metadata read")
                } else {
                    val allLines = CsvUtils.readAllLines(file)
                    if (allLines.size >= 2) {
                        for (row in allLines.drop(1)) {
                            if (row.size >= 12) {
                                val fieldCode = row[2]

                                val bbch = row.getOrElse(12) { "" }.toDoubleOrNull()
                                val ph = row.getOrElse(13) { "" }.toDoubleOrNull()

                                if (bbch != null) allBbch.add(bbch)
                                if (ph != null) allPlantHeight.add(ph)

                                csvFieldBbchPh.getOrPut(fieldCode) { mutableListOf() }
                                    .add(bbch to ph)
                            }
                        }
                    }
                }
            } catch (_: Exception) { }
        }

        // Use actual disk counts for field stats, CSV data for BBCH/plantHeight
        val fieldStats = diskFieldSampleKeys.keys.map { fieldCode ->
            val fieldBbchPh = csvFieldBbchPh[fieldCode]
            val fieldBbch = fieldBbchPh?.mapNotNull { it.first } ?: emptyList()
            val fieldPh = fieldBbchPh?.mapNotNull { it.second } ?: emptyList()
            FieldStats(
                fieldCode = fieldCode,
                sampleCount = diskFieldSampleKeys[fieldCode]?.size ?: 0,
                photoCount = diskFieldPhotoCount[fieldCode] ?: 0,
                avgBbch = if (fieldBbch.isNotEmpty()) fieldBbch.average() else null,
                avgPlantHeight = if (fieldPh.isNotEmpty()) fieldPh.average() else null
            )
        }.sortedBy { it.fieldCode.toIntOrNull() ?: 0 }

        return ProjectStats(
            region = region,
            date = date,
            totalFields = diskFieldSampleKeys.size,
            totalSamples = diskSampleKeys.size,
            totalPhotos = diskTotalPhotos,
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