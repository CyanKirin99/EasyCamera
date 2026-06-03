package com.example.easycamera.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.example.easycamera.data.model.LocationInfo
import kotlinx.coroutines.suspendCancellableCoroutine
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

        return try {
            suspendCancellableCoroutine { cont ->
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
                        val errorMsg = "高德定位错误($errCode): $errInfo"
                        android.util.Log.w("LocationProvider", errorMsg)
                        android.util.Log.w("LocationProvider", "SHA1=$sha1 包名=${context.packageName}")
                        onStatus(errorMsg)
                        cont.resume(null)
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
        } catch (e: Exception) {
            val sha1 = getCurrentSha1()
            android.util.Log.e("LocationProvider", "定位异常: SHA1=$sha1, 包名=${context.packageName}", e)
            onStatus("定位抛异常: ${e.message}")
            null
        }
    }
}