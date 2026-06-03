package com.example.easycamera

import android.app.Application
import com.amap.api.location.AMapLocationClient
import com.amap.api.maps.MapsInitializer

class EasyCameraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 高德地图SDK隐私合规必须放在Application.onCreate中优先调用
        AMapLocationClient.updatePrivacyShow(this, true, true)
        AMapLocationClient.updatePrivacyAgree(this, true)
        MapsInitializer.updatePrivacyShow(this, true, true)
        MapsInitializer.updatePrivacyAgree(this, true)
    }
}