package com.example.easycamera.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.example.easycamera.data.model.LocationInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class LocationProvider(private val context: Context) {

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun getLocation(): LocationInfo? {
        if (!hasPermission()) return null

        return try {
            withTimeoutOrNull(15000L) {
                suspendCancellableCoroutine { cont ->
                    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

                    // Try last known location first (from GPS or network)
                    val lastLocation = listOfNotNull(
                        runCatching { locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull(),
                        runCatching { locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }.getOrNull()
                    ).maxByOrNull { it.time }

                    if (lastLocation != null && System.currentTimeMillis() - lastLocation.time < 30_000) {
                        // Use cached location if recent (within 30 seconds)
                        cont.resume(
                            LocationInfo(
                                longitude = lastLocation.longitude,
                                latitude = lastLocation.latitude,
                                accuracy = lastLocation.accuracy.toDouble().toFloat(),
                                timestamp = lastLocation.time
                            )
                        )
                        return@suspendCancellableCoroutine
                    }

                    // Request fresh location from GPS and network providers
                    var responded = false
                    val listener = object : android.location.LocationListener {
                        override fun onLocationChanged(location: android.location.Location) {
                            if (!responded) {
                                responded = true
                                cont.resume(
                                    LocationInfo(
                                        longitude = location.longitude,
                                        latitude = location.latitude,
                                        accuracy = location.accuracy.toDouble().toFloat(),
                                        timestamp = location.time
                                    )
                                )
                                try { locationManager.removeUpdates(this) } catch (_: Exception) {}
                            }
                        }
                    }

                    // Request from both GPS and network, ignore if provider unavailable
                    try { locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, listener) } catch (_: Exception) {}
                    try { locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, listener) } catch (_: Exception) {}

                    cont.invokeOnCancellation {
                        try { locationManager.removeUpdates(listener) } catch (_: Exception) {}
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("LocationProvider", "定位异常", e)
            null
        }
    }
}