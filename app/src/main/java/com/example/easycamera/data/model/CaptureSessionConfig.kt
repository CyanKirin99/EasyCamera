package com.example.easycamera.data.model

data class CaptureSessionConfig(
    val region: String = "JL",
    val date: String = "",
    val operator: String = "黄添",
    val angleSequence: List<String> = listOf("A", "B", "C", "D")
)