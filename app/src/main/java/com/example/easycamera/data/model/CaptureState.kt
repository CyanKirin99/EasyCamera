package com.example.easycamera.data.model

data class CaptureState(
    val fieldCode: Int = 1,
    val sampleCode: Int = 1,
    val currentAngleIndex: Int = 0,
    val capturedAngles: List<String> = emptyList(),
    val isGroupComplete: Boolean = false
)