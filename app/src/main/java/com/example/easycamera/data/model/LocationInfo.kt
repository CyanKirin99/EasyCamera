package com.example.easycamera.data.model

data class LocationInfo(
    val longitude: Double,
    val latitude: Double,
    val accuracy: Float? = null,
    val timestamp: Long = System.currentTimeMillis()
)