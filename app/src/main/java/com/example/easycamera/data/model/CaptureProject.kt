package com.example.easycamera.data.model

data class CaptureProject(
    val projectName: String,
    val region: String,
    val date: String,
    val imageDirPath: String,
    val photoCount: Int
)