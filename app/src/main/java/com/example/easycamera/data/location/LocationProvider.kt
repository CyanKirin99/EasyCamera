package com.example.easycamera.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.easycamera.data.model.LocationInfo
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationProvider(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun getLocation(): LocationInfo? {
        if (!hasPermission()) return null

        return try {
            suspendCancellableCoroutine { cont ->
                val task = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                )
                task.addOnSuccessListener { location ->
                    if (location != null) {
                        cont.resume(
                            LocationInfo(
                                longitude = location.longitude,
                                latitude = location.latitude,
                                accuracy = location.accuracy,
                                timestamp = location.time
                            )
                        )
                    } else {
                        cont.resume(null)
                    }
                }
                task.addOnFailureListener {
                    cont.resume(null)
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}