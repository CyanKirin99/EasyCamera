package com.example.easycamera.data.model

data class CaptureSessionConfig(
    val region: String = "",
    val date: String = "",
    val operator: String = "",
    val angleSequence: List<String> = listOf("A", "B", "C", "D"),
    val isNonIdealVersion: Boolean = false,
    val bbch: String = "",
    val plantHeight: String = "",
    val extraFields: Map<String, String> = emptyMap()
)