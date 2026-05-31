package com.example.easycamera.data.model

data class CapturedPhoto(
    val region: String,
    val date: String,
    val fieldCode: String,
    val sampleCode: String,
    val angleCode: String,
    val longitude: String,
    val latitude: String,
    val filename: String,
    val filePath: String,
    val lastModified: Long
)