package com.example.easycamera.data.model

data class PhotoLocation(
    val fieldCode: String,
    val sampleCode: String,
    val angleCode: String,
    val longitude: Double,
    val latitude: Double
)

data class FieldStats(
    val fieldCode: String,
    val sampleCount: Int,
    val photoCount: Int,
    val avgBbch: Double?,
    val avgPlantHeight: Double?
)

data class ProjectStats(
    val region: String,
    val date: String,
    val totalFields: Int,
    val totalSamples: Int,
    val totalPhotos: Int,
    val avgBbch: Double?,
    val avgPlantHeight: Double?,
    val fieldStats: List<FieldStats>
)

data class RegionTimeSeries(
    val region: String,
    val datePoints: List<DateDataPoint>
)

data class DateDataPoint(
    val date: String,
    val avgBbch: Double?,
    val avgPlantHeight: Double?,
    val fieldCount: Int,
    val sampleCount: Int
)