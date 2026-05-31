package com.example.easycamera.data.model

data class CaptureMetadata(
    val region: String,
    val date: String,
    val fieldCode: String,
    val sampleCode: String,
    val angleCode: String,
    val longitude: String = "NA",
    val latitude: String = "NA",
    val operator: String,
    val captureTime: String,
    val filename: String,
    val relativePath: String,
    val filePath: String = "",
    val retakeGroupId: String = ""
)