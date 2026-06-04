package com.example.easycamera.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.example.easycamera.data.model.LocationInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.security.MessageDigest
import java.util.Locale
import kotlin.coroutines.resume

class LocationProvider(private val context: Context) {

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getCurrentSha1(): String? {
        return try {
            val info = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
            val cert = info.signatures[0].toByteArray()
            val md = MessageDigest.getInstance("SHA1")
            val publicKey = md.digest(cert)
            val hexString = StringBuffer()
            for (b in publicKey) {
                val appendString = Integer.toHexString(0xFF and b.toInt())
                    .uppercase(Locale.US)
                if (appendString.length == 1) hexString.append("0")
                hexString.append(appendString)
                hexString.append(":")
            }
            hexString.substring(0, hexString.length - 1)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getLocation(onStatus: (String) -> Unit = {}): LocationInfo? {
        if (!hasPermission()) {
            onStatus("定位权限未授予")
            return null
        }

        // 1. Try AMap location first
        val amapResult = try {
            getAmapLocation()
        } catch (e: Exception) {
            android.util.Log.e("LocationProvider", "AMap定位异常", e)
            null
        }

        if (amapResult != null) {
            return amapResult
        }

        // 2. If AMap failed, try Android LocationManager as fallback
        android.util.Log.w("LocationProvider", "AMap定位失败，尝试Android原生定位")
        val systemResult = getSystemLocation()
        if (systemResult != null) {
            onStatus("定位(系统)已获取")
            return systemResult
        }

        onStatus("所有定位方式均失败")
        return null
    }

    private suspend fun getAmapLocation(): LocationInfo? {
        return suspendCancellableCoroutine { cont ->
            val client = AMapLocationClient(context)
            val option = AMapLocationClientOption().apply {
                locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                isOnceLocation = true
                isNeedAddress = false
            }
            client.setLocationOption(option)
            client.setLocationListener { aMapLocation: AMapLocation? ->
                if (aMapLocation != null && aMapLocation.errorCode == 0) {
                    cont.resume(
                        LocationInfo(
                            longitude = aMapLocation.longitude,
                            latitude = aMapLocation.latitude,
                            accuracy = aMapLocation.accuracy,
                            timestamp = aMapLocation.time
                        )
                    )
                } else {
                    val errCode = aMapLocation?.errorCode ?: -1
                    val errInfo = aMapLocation?.errorInfo ?: "未知错误"
                    val sha1 = getCurrentSha1()
                    android.util.Log.w("LocationProvider",
                        "高德定位失败($errCode): $errInfo  SHA1=$sha1")
                    cont.resume(null) // resume null to trigger fallback
                }
                client.stopLocation()
                client.onDestroy()
            }
            client.startLocation()

            cont.invokeOnCancellation {
                client.stopLocation()
                client.onDestroy()
            }
        }
    }

    private suspend fun getSystemLocation(): LocationInfo? {
        return withTimeoutOrNull(10000L) {
            suspendCancellableCoroutine { cont ->
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

                // Try last known location first (within 30 seconds)
                val lastLocation = listOfNotNull(
                    runCatching { locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull(),
                    runCatching { locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }.getOrNull()
                ).maxByOrNull { it.time }

                if (lastLocation != null && System.currentTimeMillis() - lastLocation.time < 30_000) {
                    cont.resume(
                        LocationInfo(
                            longitude = lastLocation.longitude,
                            latitude = lastLocation.latitude,
                            accuracy = lastLocation.accuracy.toFloat(),
                            timestamp = lastLocation.time
                        )
                    )
                    return@suspendCancellableCoroutine
                }

                // Request fresh location
                var responded = false
                val listener = object : android.location.LocationListener {
                    override fun onLocationChanged(location: android.location.Location) {
                        if (!responded) {
                            responded = true
                            cont.resume(
                                LocationInfo(
                                    longitude = location.longitude,
                                    latitude = location.latitude,
                                    accuracy = location.accuracy.toFloat(),
                                    timestamp = location.time
                                )
                            )
                            try { locationManager.removeUpdates(this) } catch (_: Exception) {}
                        }
                    }
                }

                try { locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, listener) } catch (_: Exception) {}
                try { locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, listener) } catch (_: Exception) {}

                cont.invokeOnCancellation {
                    try { locationManager.removeUpdates(listener) } catch (_: Exception) {}
                }
            }
        }
    }
}