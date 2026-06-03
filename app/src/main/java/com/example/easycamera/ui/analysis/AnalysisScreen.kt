package com.example.easycamera.ui.analysis

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.TextureMapView
import com.amap.api.maps.model.BitmapDescriptor
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.MarkerOptions
import java.util.Locale

@Composable
fun AnalysisScreen(
    region: String,
    date: String,
    onBack: () -> Unit,
    mapView: com.amap.api.maps.TextureMapView? = null,
    sharedAMap: AMap? = null,
    viewModel: AnalysisViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Back press is handled at the top level in MainActivity via BackHandler(enabled = showAnalysis)
    // to prevent the handler from being removed during callback and causing app exit.

    LaunchedEffect(region, date) {
        viewModel.loadProject(region, date)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Header row with back arrow and title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "数据分析",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$region / $date",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            MapSection(uiState.photoLocations, mapView, sharedAMap)
            Spacer(modifier = Modifier.height(12.dp))
            StatsSection(uiState.projectStats)
            Spacer(modifier = Modifier.height(12.dp))
            TimeSeriesSection(uiState.regionTimeSeries)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun MapSection(
    locations: List<com.example.easycamera.data.model.PhotoLocation>,
    mapView: TextureMapView?,
    sharedAMap: AMap?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "拍照定位分布",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            if (locations.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "该项目的照片暂无有效的GPS定位数据",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "共 ${locations.size} 个有效定位点",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    // Limit markers to prevent OOM on map rendering
                    AMapMapView(locations.take(500), mapView, sharedAMap)
                }
            }
        }
    }
}

@Composable
private fun AMapMapView(
    locations: List<com.example.easycamera.data.model.PhotoLocation>,
    mapView: TextureMapView?,
    sharedAMap: AMap?
) {
    val aMap = sharedAMap
    val view = mapView

    if (aMap == null || view == null) return

    // 复用 MainActivity 创建的 TextureMapView，不再创建新的
    AndroidView(
        factory = { view },
        modifier = Modifier.fillMaxSize(),
        onRelease = {
            // 不销毁 — 生命周期由 MainActivity 管理
        }
    )

    LaunchedEffect(locations) {
        if (locations.isEmpty()) return@LaunchedEffect

        // 卫星地图更适合农业场景
        aMap.mapType = AMap.MAP_TYPE_SATELLITE
        // 限制最大缩放级别，避免放大到无卫星影像的区域
        aMap.setMaxZoomLevel(18f)

        aMap.clear()

        val markerOptionsList = locations.map { loc ->
            MarkerOptions()
                .position(LatLng(loc.latitude, loc.longitude))
                .title("田块${loc.fieldCode}_样本${loc.sampleCode}_${loc.angleCode}")
                .snippet("(${loc.latitude}, ${loc.longitude})")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                .draggable(false)
        }
        aMap.addMarkers(ArrayList(markerOptionsList), false)

        val fieldCodePositions = locations.groupBy { it.fieldCode }
            .mapValues { (_, locs) ->
                val lat = locs.map { it.latitude }.average()
                val lon = locs.map { it.longitude }.average()
                LatLng(lat, lon)
            }
        fieldCodePositions.forEach { (fieldCode, latLng) ->
            val bitmap = createFieldCodeBitmap(fieldCode)
            aMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .icon(bitmap)
                    .anchor(0.5f, 0.5f)
                    .draggable(false)
            )
        }

        val latNorth = locations.maxOf { it.latitude }
        val latSouth = locations.minOf { it.latitude }
        val lonEast = locations.maxOf { it.longitude }
        val lonWest = locations.minOf { it.longitude }

        val latPad = ((latNorth - latSouth) * 0.2).coerceAtLeast(0.001)
        val lonPad = ((lonEast - lonWest) * 0.2).coerceAtLeast(0.001)

        val bounds = LatLngBounds.builder()
            .include(LatLng(latNorth + latPad, lonEast + lonPad))
            .include(LatLng(latSouth - latPad, lonWest - lonPad))
            .build()

        aMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 60))
    }
}

private fun createFieldCodeBitmap(fieldCode: String): BitmapDescriptor {
    val text = "田${fieldCode}"
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        textSize = 42f
        isFakeBoldText = true
    }
    val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(200, 255, 255, 255)
    }
    val bounds = android.graphics.Rect()
    paint.getTextBounds(text, 0, text.length, bounds)
    val padding = 12
    val width = bounds.width() + padding * 2
    val height = bounds.height() + padding * 2
    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawRoundRect(
        0f, 0f, width.toFloat(), height.toFloat(),
        10f, 10f, bgPaint
    )
    canvas.drawText(text, padding.toFloat(), (height - padding + bounds.height() / 2).toFloat(), paint)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

@Composable
private fun StatsSection(stats: com.example.easycamera.data.model.ProjectStats?) {
    if (stats == null) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "数据统计",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("田块", "${stats.totalFields}")
                StatItem("样本", "${stats.totalSamples}")
                StatItem("照片", "${stats.totalPhotos}")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    "BBCH均值(项目)",
                    stats.avgBbch?.let { String.format(Locale.CHINA, "%.1f", it) } ?: "无数据"
                )
                StatItem(
                    "株高均值(项目)",
                    stats.avgPlantHeight?.let { String.format(Locale.CHINA, "%.1f cm", it) } ?: "无数据"
                )
            }

            if (stats.fieldStats.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "田块级统计",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    Column {
                        HeaderRow()
                        stats.fieldStats.forEach { field ->
                            FieldStatsRow(field)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val COL_WIDTHS = listOf(60.dp, 60.dp, 60.dp, 80.dp, 96.dp)

@Composable
private fun HeaderRow() {
    Row {
        listOf("田块", "样本数", "照片数", "BBCH均", "株高均").forEachIndexed { i, header ->
            Box(modifier = Modifier.width(COL_WIDTHS[i]).padding(horizontal = 4.dp)) {
                Text(
                    text = header,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun FieldStatsRow(field: com.example.easycamera.data.model.FieldStats) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(COL_WIDTHS[0]).padding(horizontal = 4.dp)) {
            Text(field.fieldCode, fontSize = 12.sp, maxLines = 1)
        }
        Box(modifier = Modifier.width(COL_WIDTHS[1]).padding(horizontal = 4.dp)) {
            Text("${field.sampleCount}", fontSize = 12.sp, maxLines = 1)
        }
        Box(modifier = Modifier.width(COL_WIDTHS[2]).padding(horizontal = 4.dp)) {
            Text("${field.photoCount}", fontSize = 12.sp, maxLines = 1)
        }
        Box(modifier = Modifier.width(COL_WIDTHS[3]).padding(horizontal = 4.dp)) {
            Text(
                field.avgBbch?.let { String.format(Locale.CHINA, "%.1f", it) } ?: "-",
                fontSize = 12.sp,
                maxLines = 1
            )
        }
        Box(modifier = Modifier.width(COL_WIDTHS[4]).padding(horizontal = 4.dp)) {
            Text(
                field.avgPlantHeight?.let { String.format(Locale.CHINA, "%.1f", it) } ?: "-",
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TimeSeriesSection(timeSeries: com.example.easycamera.data.model.RegionTimeSeries?) {
    if (timeSeries == null || timeSeries.datePoints.size <= 1) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "跨日期趋势（数据接口预留）",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "地区「${timeSeries.region}」共 ${timeSeries.datePoints.size} 次采集记录",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            timeSeries.datePoints.forEach { point ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(point.date, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text(
                        "BBCH: ${point.avgBbch?.let { String.format(Locale.CHINA, "%.1f", it) } ?: "-"}",
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "株高: ${point.avgPlantHeight?.let { String.format(Locale.CHINA, "%.1f", it) } ?: "-"}",
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "田块: ${point.fieldCount}",
                        fontSize = 12.sp,
                        modifier = Modifier.weight(0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "注：后续版本将在此处集成图表，展示该地区BBCH、株高随时间的变化趋势。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}