package com.example.easycamera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import android.view.KeyEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraControl
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Slider
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.easycamera.BuildConfig
import com.example.easycamera.camera.CameraPreview
import com.example.easycamera.camera.rememberCameraControlState
import com.example.easycamera.camera.rememberCameraState
import com.example.easycamera.camera.rememberImageCaptureState
import com.example.easycamera.camera.takePhoto
import com.example.easycamera.data.location.LocationProvider
import com.example.easycamera.data.model.CaptureMetadata
import com.example.easycamera.data.model.CaptureSessionConfig
import com.example.easycamera.data.model.NonIdealPromptType
import com.example.easycamera.data.model.CaptureState
import com.example.easycamera.data.repository.MetadataRepository
import com.example.easycamera.domain.CaptureCodeManager
import com.example.easycamera.ui.CaptureViewModel
import com.example.easycamera.ui.analysis.AnalysisScreen
import com.example.easycamera.ui.gallery.PhotoGalleryScreen
import com.example.easycamera.ui.gallery.PhotoGalleryViewModel
import com.example.easycamera.ui.theme.EasyCameraTheme
import com.example.easycamera.util.ImageRotationUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    var onVolumeKeyCapture: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        com.amap.api.location.AMapLocationClient.updatePrivacyShow(this, true, true)
        com.amap.api.location.AMapLocationClient.updatePrivacyAgree(this, true)
        com.amap.api.maps.MapsInitializer.updatePrivacyShow(this, true, true)
        com.amap.api.maps.MapsInitializer.updatePrivacyAgree(this, true)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EasyCameraTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    EasyCameraApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0 &&
            (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) &&
            onVolumeKeyCapture != null
        ) {
            onVolumeKeyCapture?.invoke()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}

@Composable
fun EasyCameraApp(modifier: Modifier = Modifier) {
    val viewModel: CaptureViewModel = viewModel()
    val sessionConfig by viewModel.sessionConfig.collectAsState()
    val captureState by viewModel.captureState.collectAsState()
    val capturedMetadataList by viewModel.capturedMetadataList.collectAsState()
    val showFieldEditDialog by viewModel.showFieldEditDialog.collectAsState()
    val showSampleEditDialog by viewModel.showSampleEditDialog.collectAsState()
    val captureMessage by viewModel.captureMessage.collectAsState()
    val isCapturing by viewModel.isCapturing.collectAsState()
    val capturedFilePaths by viewModel.capturedFilePaths.collectAsState()
    val codeLockMessage by viewModel.codeLockMessage.collectAsState()
    val whiteBalanceValue by viewModel.whiteBalanceValue.collectAsState()

    val imageCaptureState = rememberImageCaptureState()
    val cameraControlState = rememberCameraControlState()
    val cameraState = rememberCameraState()

    val context = LocalContext.current
    var metadataWriteFailed by remember { mutableStateOf(false) }
    var capturedPreviewPath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(capturedPreviewPath) {
        if (capturedPreviewPath != null) {
            kotlinx.coroutines.delay(500)
            val oldPreviewPath = capturedPreviewPath
            capturedPreviewPath = null
            if (oldPreviewPath != null && oldPreviewPath.startsWith(context.cacheDir.absolutePath)) {
                try { File(oldPreviewPath).delete() } catch (_: Exception) { }
            }
        }
    }

    var showCamera by remember { mutableStateOf(true) }

    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted = granted
    }

    var locationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        locationPermissionGranted = granted
        if (granted) {
            viewModel.setLocationStatus("定位中...")
        } else {
            viewModel.updateLocation(null)
            viewModel.setLocationStatus("定位权限未授予，将使用 NA_NA")
        }
    }

    val locationProvider = remember { LocationProvider(context) }

    LaunchedEffect(Unit) {
        if (!locationPermissionGranted) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(locationPermissionGranted) {
        if (locationPermissionGranted) {
            viewModel.setLocationStatus("定位中...")
            var errMsg: String? = null
            val loc = locationProvider.getLocation { errMsg = it }
            viewModel.updateLocation(loc, errorStatus = errMsg)
        }
    }

    var showPhotoGallery by remember { mutableStateOf(false) }

    var showAnalysis by remember { mutableStateOf(false) }
    var analysisRegion by remember { mutableStateOf("") }
    var analysisDate by remember { mutableStateOf("") }

    // 复用同一个 TextureMapView，避免频繁进出分析页面时反复创建 GL 上下文导致黑屏
    val analysisContext = LocalContext.current
    val analysisMapView = remember { com.amap.api.maps.TextureMapView(analysisContext) }
    val analysisAMap = remember { mutableStateOf<com.amap.api.maps.AMap?>(null) }
    val analysisMapBundle = remember { Bundle() }

    val analysisMapInitialized = remember { mutableStateOf(false) }

    // 进入分析页面时才初始化地图，避免在定位权限授予前提前触发AMap SDK内部缓存
    LaunchedEffect(showAnalysis) {
        if (showAnalysis) {
            if (!analysisMapInitialized.value) {
                analysisMapView.onCreate(analysisMapBundle)
                analysisAMap.value = analysisMapView.map
                analysisMapInitialized.value = true
            }
            analysisMapView.onResume()
        } else {
            try { analysisMapView.onPause() } catch (_: Throwable) {}
        }
    }

    val coroutineScope = rememberCoroutineScope()

    var dontShowNAWarning by remember { mutableStateOf(false) }
    var showNAWarningDialog by remember { mutableStateOf(false) }
    var pendingNAConfirm by remember { mutableStateOf<(() -> Unit)?>(null) }

    var showRetakeConfirmDialog by remember { mutableStateOf(false) }

    var showOverwriteConfirmDialog by remember { mutableStateOf(false) }
    var pendingOverwriteDoCapture by remember { mutableStateOf<(() -> Unit)?>(null) }
    var overwriteExistingMatch by remember { mutableStateOf<CaptureMetadata?>(null) }
    var showSampleOverwriteDialog by remember { mutableStateOf(false) }
    var pendingSampleOverwriteGroup by remember { mutableStateOf<Pair<List<CaptureMetadata>, () -> Unit>?>(null) }

    val locationText by viewModel.locationStatus.collectAsState()
    val locationDetermined by viewModel.locationDetermined.collectAsState()
    val isLocationNA = remember(locationText) {
        locationText.contains("NA") || locationText.contains("不可用")
    }

    val metadataRepository = remember { MetadataRepository(context) }

    var showExitConfirm by remember { mutableStateOf(false) }

    var showMissingFieldsPrompt by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = showPhotoGallery,
        transitionSpec = {
            if (targetState) {
                (slideInHorizontally { width -> width } + fadeIn()) togetherWith
                        (slideOutHorizontally { width -> -width / 3 } + fadeOut())
            } else {
                (slideInHorizontally { width -> -width } + fadeIn()) togetherWith
                        (slideOutHorizontally { width -> width / 3 } + fadeOut())
            }
        },
        label = "pageTransition"
    ) { showGallery ->
        if (showGallery) {
            BackHandler { showPhotoGallery = false }

            val galleryViewModel: PhotoGalleryViewModel = viewModel()
            val currentProjectName = "${sessionConfig.region}_${sessionConfig.date}"
            LaunchedEffect(showPhotoGallery) {
                galleryViewModel.setCurrentProjectName(currentProjectName)
            }
            PhotoGalleryScreen(
                viewModel = galleryViewModel,
                onNavigateBack = { showPhotoGallery = false },
                onNavigateToAnalysis = { region, date ->
                    analysisRegion = region
                    analysisDate = date
                    showAnalysis = true
                }
            )
        } else {
        BackHandler { showExitConfirm = true }

        val performCapture = {
            val ic = imageCaptureState.value
            if (ic != null && !captureState.isGroupComplete && !isCapturing) {
                if (sessionConfig.region.isBlank() || sessionConfig.operator.isBlank()) {
                    showMissingFieldsPrompt = true
                } else {
                    val doCapture: () -> Unit = {
                    if (viewModel.tryStartCapture()) {
                        val dir = File(
                            context.getExternalFilesDir(null),
                            "EasyCamera/${sessionConfig.region}_${sessionConfig.date}/images"
                        )
                        val file = File(dir, viewModel.previewFileName)
                        takePhoto(
                            imageCapture = ic,
                            context = context,
                            outputFile = file,
                            onSuccess = {
                                vibrate(context)
                                val previewFile = File(context.cacheDir, "preview_${file.name}")
                                try {
                                    file.copyTo(previewFile, overwrite = true)
                                } catch (_: Exception) { }
                                capturedPreviewPath = previewFile.absolutePath
                                coroutineScope.launch(Dispatchers.IO) {
                                    val rotationOk = ImageRotationUtils.rotateJpegIfNeeded(file)
                                    withContext(Dispatchers.Main) {
                                        viewModel.onPhotoCaptured(file.absolutePath) { metadata ->
                                            val ok = metadataRepository.appendRecord(metadata)
                                            if (!ok) metadataWriteFailed = true
                                        }
                                        if (!rotationOk) {
                                            viewModel.setCaptureMessage("照片已保存，但方向处理失败。")
                                        }
                                    }
                                }
                            },
                            onError = { msg ->
                                viewModel.onPhotoCaptureError(msg)
                            }
                        )
                    }
                }

                val config = sessionConfig
                val curFieldCode = CaptureCodeManager.formatCode(captureState.fieldCode)
                val curSampleCode = CaptureCodeManager.formatCode(captureState.sampleCode)
                val curAngleCode = config.angleSequence.getOrElse(captureState.currentAngleIndex) { "?" }

                val sampleGroupRecords = metadataRepository.findSampleGroupRecords(
                    region = config.region,
                    date = config.date,
                    fieldCode = curFieldCode,
                    sampleCode = curSampleCode
                )
                val currentFilenames = capturedMetadataList.map { it.filename }.toSet()
                val existingGroupRecords = sampleGroupRecords.filter { it.filename !in currentFilenames }
                if (existingGroupRecords.isNotEmpty()) {
                    pendingSampleOverwriteGroup = existingGroupRecords to doCapture
                    showSampleOverwriteDialog = true
                } else {
                    val existingMatch = capturedMetadataList.find { meta ->
                        meta.fieldCode == curFieldCode &&
                                meta.sampleCode == curSampleCode &&
                                meta.angleCode == curAngleCode
                    }
                    val dir = File(
                        context.getExternalFilesDir(null),
                        "EasyCamera/${config.region}_${config.date}/images"
                    )
                    val outputFile = File(dir, viewModel.previewFileName)
                    if (existingMatch != null || outputFile.exists()) {
                        overwriteExistingMatch = existingMatch
                        pendingOverwriteDoCapture = doCapture
                        showOverwriteConfirmDialog = true
                    } else if ((!locationDetermined || isLocationNA) && !dontShowNAWarning) {
                        pendingNAConfirm = doCapture
                        showNAWarningDialog = true
                    } else {
                        doCapture()
                    }
                }
            }
        }
    }

        // 全局音量键监听：通过 Activity.onKeyDown 拦截，按音量+/-触发拍照
        val mainActivity = LocalContext.current as? MainActivity
        DisposableEffect(mainActivity) {
            mainActivity?.onVolumeKeyCapture = performCapture
            onDispose {
                mainActivity?.onVolumeKeyCapture = null
            }
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
                .imePadding()
        ) {
            CompactInfoBar(
                sessionConfig = sessionConfig,
                locationStatus = viewModel.locationStatus,
                onRegionSelected = { viewModel.updateRegion(it) },
                onDateSelected = { viewModel.updateDate(it) },
                onOperatorSelected = { viewModel.updateOperator(it) },
                onRefreshLocation = {
                    val permGranted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!permGranted) {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    } else {
                        viewModel.setLocationStatus("定位中...")
                        coroutineScope.launch {
                            var errMsg: String? = null
                            val loc = locationProvider.getLocation { errMsg = it }
                            viewModel.updateLocation(loc, errorStatus = errMsg)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (captureState.isGroupComplete) {
                    CompactGroupConfirmContent(
                        captureState = captureState,
                        angleSequence = sessionConfig.angleSequence,
                        capturedMetadataList = capturedMetadataList,
                        onRetake = {
                            capturedFilePaths.forEach { path ->
                                try { File(path).delete() } catch (_: Exception) { }
                            }
                            viewModel.retakeGroup()
                            metadataRepository.deleteSampleGroup(
                                region = sessionConfig.region,
                                date = sessionConfig.date,
                                fieldCode = CaptureCodeManager.formatCode(captureState.fieldCode),
                                sampleCode = CaptureCodeManager.formatCode(captureState.sampleCode)
                            )
                            metadataWriteFailed = false
                        },
                        onConfirm = { viewModel.confirmGroup() }
                    )
                } else {
                    val wbFilterColor = remember(whiteBalanceValue) {
                        if (whiteBalanceValue == 0) {
                            Color.Transparent
                        } else {
                            val alpha = (kotlin.math.abs(whiteBalanceValue) / 100f) * 0.18f
                            if (whiteBalanceValue > 0) {
                                // 暖色（红黄色）
                                Color(1f, 0.7f, 0.2f, alpha)
                            } else {
                                // 冷色（蓝色）
                                Color(0.3f, 0.5f, 1f, alpha)
                            }
                        }
                    }
                    val wbModifier = if (whiteBalanceValue != 0) {
                        Modifier.drawWithContent {
                            drawContent()
                            drawRect(color = wbFilterColor, size = size)
                        }
                    } else {
                        Modifier
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.6f)
                            .clipToBounds()
                            .then(wbModifier),
                        contentAlignment = Alignment.Center
                    ) {
                        if (cameraPermissionGranted && showCamera) {
                            CameraPreview(
                                modifier = Modifier.fillMaxSize(),
                                imageCaptureState = imageCaptureState,
                                cameraControlState = cameraControlState,
                                cameraState = cameraState
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!cameraPermissionGranted) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "需要相机权限才能拍照",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("授予相机权限")
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "相机已关闭",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            IconButton(
                                onClick = { showCamera = !showCamera },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        Color.Black.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = if (showCamera) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                    contentDescription = if (showCamera) "关闭相机预览" else "打开相机预览",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.4f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(modifier = Modifier.height(2.dp))

                        CompactCodeAngleBar(
                            captureState = captureState,
                            angleSequence = sessionConfig.angleSequence,
                            onSelectAngle = { viewModel.selectAngleIndex(it) },
                            onDecrementField = { viewModel.decrementFieldCode() },
                            onIncrementField = { viewModel.incrementFieldCode() },
                            onDecrementSample = { viewModel.decrementSampleCode() },
                            onIncrementSample = { viewModel.incrementSampleCode() },
                            progressLabel = viewModel.progressLabel
                        )

                        if (BuildConfig.IS_NON_IDEAL) {
                            Spacer(modifier = Modifier.height(6.dp))
                            // 非理想提示文字
                            Text(
                                text = viewModel.currentPromptInstruction,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 18.sp,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // 仅「拍摄参数错误」时才开放曝光和白平衡调节
                            if (viewModel.currentPromptType == NonIdealPromptType.BAD_EXPOSURE) {
                                // 曝光补偿滑块（范围固定为±12，相机硬件会自动限制实际范围）
                                val ev by viewModel.exposureCompensation.collectAsState()
                                Text("曝光补偿: ${ev}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                Slider(
                                    value = ev.toFloat(),
                                    onValueChange = { viewModel.updateExposureCompensation(it.roundToInt()) },
                                    valueRange = -12f..12f,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                LaunchedEffect(ev, cameraControlState.value) {
                                    val cc = cameraControlState.value ?: return@LaunchedEffect
                                    try {
                                        cc.setExposureCompensationIndex(ev)
                                    } catch (_: Exception) { }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // 白平衡滑块（仅拉取数值供滤镜使用）
                                val wb by viewModel.whiteBalanceValue.collectAsState()
                                Text("白平衡: ${wb} (冷← ${wb} →暖)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                Slider(
                                    value = wb.toFloat(),
                                    onValueChange = { viewModel.updateWhiteBalanceValue(it.roundToInt()) },
                                    valueRange = -100f..100f,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // 非理想版不显示BBCH和株高
                        if (!BuildConfig.IS_NON_IDEAL) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = sessionConfig.bbch,
                                    onValueChange = { viewModel.updateBbch(it) },
                                    label = { Text("BBCH", fontSize = 11.sp) },
                                    placeholder = { Text("生育期", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    textStyle = MaterialTheme.typography.bodySmall,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                OutlinedTextField(
                                    value = sessionConfig.plantHeight,
                                    onValueChange = { viewModel.updatePlantHeight(it) },
                                    label = { Text("株高(cm)", fontSize = 11.sp) },
                                    placeholder = { Text("可选", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    textStyle = MaterialTheme.typography.bodySmall,
                                    shape = RoundedCornerShape(6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val retakeInteractionSource = remember { MutableInteractionSource() }
                            val isRetakePressed by retakeInteractionSource.collectIsPressedAsState()
                            val retakeScale by animateFloatAsState(
                                if (isRetakePressed) 0.95f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "retakeScale"
                            )

                            OutlinedButton(
                                onClick = {
                                    val oldPreviewPath = capturedPreviewPath
                                    capturedPreviewPath = null
                                    if (oldPreviewPath != null && oldPreviewPath.startsWith(context.cacheDir.absolutePath)) {
                                        try { File(oldPreviewPath).delete() } catch (_: Exception) { }
                                    }
                                    capturedFilePaths.forEach { path ->
                                        try { File(path).delete() } catch (_: Exception) { }
                                    }
                                    viewModel.retakeGroup()
                                    metadataRepository.deleteSampleGroup(
                                        region = sessionConfig.region,
                                        date = sessionConfig.date,
                                        fieldCode = CaptureCodeManager.formatCode(captureState.fieldCode),
                                        sampleCode = CaptureCodeManager.formatCode(captureState.sampleCode)
                                    )
                                    metadataWriteFailed = false
                                },
                                modifier = Modifier
                                    .weight(0.35f)
                                    .graphicsLayer(scaleX = retakeScale, scaleY = retakeScale),
                                shape = RoundedCornerShape(8.dp),
                                interactionSource = retakeInteractionSource
                            ) {
                                Text("重拍本组", fontSize = 14.sp, maxLines = 1)
                            }

                            val captureInteractionSource = remember { MutableInteractionSource() }
                            val isCapturePressed by captureInteractionSource.collectIsPressedAsState()
                            val captureScale by animateFloatAsState(
                                if (isCapturePressed) 0.95f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "captureScale"
                            )

                            Button(
                                onClick = { performCapture() },
                                modifier = Modifier
                                    .weight(0.65f)
                                    .graphicsLayer(scaleX = captureScale, scaleY = captureScale),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isCapturing,
                                interactionSource = captureInteractionSource
                            ) {
                                Text(
                                    text = if (isCapturing) "拍照中..." else "拍照",
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            CompactBottomBar(
                captureMessage = captureMessage,
                metadataWriteFailed = metadataWriteFailed,
                previewFileName = viewModel.previewFileName,
                onOpenGallery = { showPhotoGallery = true }
            )

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
    // BackHandler for analysis page - must be OUTSIDE AnimatedContent so it
    // registers LAST and takes priority over the gallery BackHandler.
    // Only enabled when analysis is showing.
    BackHandler(enabled = showAnalysis) {
        showAnalysis = false
        // showPhotoGallery stays true - we stay on the gallery page
    }

    // Analysis page displayed as a full-screen overlay.
    if (showAnalysis) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Scrim overlay (visual only - back arrow closes the analysis page)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            )

            // Analysis content panel
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(top = 48.dp),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 8.dp
            ) {
                AnalysisScreen(
                    region = analysisRegion,
                    date = analysisDate,
                    mapView = analysisMapView,
                    sharedAMap = analysisAMap.value,
                    onBack = {
                        showAnalysis = false
                        showPhotoGallery = true
                    }
                )
            }
        }
    }
    if (showFieldEditDialog) {
        FieldEditDialog(
            viewModel = viewModel,
            onFieldChanged = {
                coroutineScope.launch {
                    viewModel.setLocationStatus("定位中...")
                    var errMsg: String? = null
                    val loc = locationProvider.getLocation { errMsg = it }
                    viewModel.updateLocation(loc, errorStatus = errMsg)
                }
            }
        )
    }

    if (showSampleEditDialog) {
        SampleEditDialog(viewModel)
    }

    if (codeLockMessage != null) {
        CodeLockDialog(
            message = codeLockMessage!!,
            onDismiss = { viewModel.clearCaptureMessage() }
        )
    }

    if (showMissingFieldsPrompt) {
        MissingFieldsDialog(
            sessionConfig = sessionConfig,
            onUpdateRegion = { viewModel.updateRegion(it) },
            onUpdateOperator = { viewModel.updateOperator(it) },
            onDismiss = { showMissingFieldsPrompt = false }
        )
    }

    if (showNAWarningDialog) {
        var dontShowAgain by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = {
                showNAWarningDialog = false
                pendingNAConfirm = null
            },
            shape = RoundedCornerShape(12.dp),
            title = { Text("定位不可用") },
            text = {
                Column {
                    Text(
                        text = "当前无法获取有效定位信息，照片元数据中将记录为 NA_NA。\n是否继续拍照？",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            dontShowAgain = !dontShowAgain
                        }
                    ) {
                        Checkbox(
                            checked = dontShowAgain,
                            onCheckedChange = { dontShowAgain = !dontShowAgain }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("注意：APP重启前不再提醒")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (dontShowAgain) dontShowNAWarning = true
                    showNAWarningDialog = false
                    pendingNAConfirm?.invoke()
                    pendingNAConfirm = null
                }) {
                    Text("继续拍照")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNAWarningDialog = false
                    pendingNAConfirm = null
                }) {
                    Text("取消")
                }
            }
        )
    }

    if (showRetakeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRetakeConfirmDialog = false },
            shape = RoundedCornerShape(12.dp),
            title = { Text("确认重拍") },
            text = {
                Text(
                    "确定要重新拍摄当前角度吗？\n\n" +
                            "当前照片将被删除，相机将回到该角度的拍摄状态。"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRetakeConfirmDialog = false
                        val undoInfo = viewModel.undoLastCapture()
                        if (undoInfo != null) {
                            val oldPreviewPath = capturedPreviewPath
                            capturedPreviewPath = null
                            if (oldPreviewPath != null && oldPreviewPath.startsWith(context.cacheDir.absolutePath)) {
                                try { File(oldPreviewPath).delete() } catch (_: Exception) { }
                            }
                            try { File(undoInfo.filePath).delete() } catch (_: Exception) { }
                            metadataRepository.deleteRecord(
                                region = sessionConfig.region,
                                date = sessionConfig.date,
                                filename = undoInfo.metadata.filename
                            )
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("重拍")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRetakeConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showOverwriteConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showOverwriteConfirmDialog = false
                pendingOverwriteDoCapture = null
                overwriteExistingMatch = null
            },
            shape = RoundedCornerShape(12.dp),
            title = { Text("确认覆盖") },
            text = {
                Text(
                    "该田块 + 样本 + 角度的照片已存在，确定要覆盖吗？\n\n" +
                            "原有照片将被删除并重新拍摄。"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showOverwriteConfirmDialog = false
                        val match = overwriteExistingMatch
                        overwriteExistingMatch = null
                        pendingOverwriteDoCapture = null
                        if (match != null) {
                            try { File(match.filePath).delete() } catch (_: Exception) { }
                            metadataRepository.deleteRecord(match.region, match.date, match.filename)
                        }
                        viewModel.forceAllowCaptureForCurrentAngle()
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("覆盖")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showOverwriteConfirmDialog = false
                    pendingOverwriteDoCapture = null
                    overwriteExistingMatch = null
                }) {
                    Text("取消")
                }
            }
        )
    }

    if (showSampleOverwriteDialog) {
        AlertDialog(
            onDismissRequest = {
                showSampleOverwriteDialog = false
                pendingSampleOverwriteGroup = null
            },
            shape = RoundedCornerShape(12.dp),
            title = { Text("警告：该样本组已有数据") },
            text = {
                val group = pendingSampleOverwriteGroup?.first
                if (group != null && group.isNotEmpty()) {
                    val sample = group.first()
                    Text(
                        "田块 ${sample.fieldCode} 样本 ${sample.sampleCode} 在下辖项目 ${sample.region}_${sample.date} 中已有 ${group.size} 张照片。\n\n" +
                                "覆盖将永久删除该样本组的所有已有照片和记录，此操作不可恢复。\n\n" +
                                "是否继续？"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSampleOverwriteDialog = false
                        val pair = pendingSampleOverwriteGroup
                        pendingSampleOverwriteGroup = null
                        if (pair != null) {
                            val (records, captureAction) = pair
                            val sample = records.first()
                            val imagesDir = File(
                                context.getExternalFilesDir(null),
                                "EasyCamera/${sample.region}_${sample.date}/images"
                            )
                            val prefix = "${sample.region}_${sample.date}_${sample.fieldCode}_${sample.sampleCode}_"
                            imagesDir.listFiles()?.forEach { file ->
                                if (file.name.startsWith(prefix)) {
                                    try { file.delete() } catch (_: Exception) { }
                                }
                            }
                            metadataRepository.deleteSampleGroup(
                                region = sample.region,
                                date = sample.date,
                                fieldCode = sample.fieldCode,
                                sampleCode = sample.sampleCode
                            )
                            viewModel.forceAllowCaptureForCurrentAngle()
                            captureAction()
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("覆盖")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSampleOverwriteDialog = false
                    pendingSampleOverwriteGroup = null
                }) {
                    Text("取消")
                }
            }
        )
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            shape = RoundedCornerShape(12.dp),
            title = { Text("确认退出") },
            text = { Text("当前有未完成的拍摄任务，确定要退出应用吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirm = false
                        (context as? android.app.Activity)?.finish()
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("确认退出")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) {
                    Text("继续拍摄")
                }
            }
        )
    }
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactInfoBar(
    sessionConfig: CaptureSessionConfig,
    locationStatus: kotlinx.coroutines.flow.StateFlow<String>,
    onRegionSelected: (String) -> Unit,
    onDateSelected: (String) -> Unit,
    onOperatorSelected: (String) -> Unit,
    onRefreshLocation: () -> Unit
) {
    val locationText by locationStatus.collectAsState()
    val context = LocalContext.current

    // 自定义选项：从 SharedPreferences 读取
    val prefs = remember { context.getSharedPreferences("custom_options", Context.MODE_PRIVATE) }
    var customRegions by remember { mutableStateOf(prefs.getStringSet("regions", emptySet())?.toSet() ?: emptySet()) }
    var customOperators by remember { mutableStateOf(prefs.getStringSet("operators", emptySet())?.toSet() ?: emptySet()) }

    val defaultRegions = listOf("JL", "XT", "JS")
    val defaultOperators = listOf("黄添", "史俊尧", "苏辰晔", "王宇杰", "张浩然")
    val allRegions = remember(customRegions) { defaultRegions + customRegions.sorted() }
    val allOperators = remember(customOperators) { defaultOperators + customOperators.sorted() }

    var regionExpanded by remember { mutableStateOf(false) }
    var operatorExpanded by remember { mutableStateOf(false) }
    var showAddRegionDialog by remember { mutableStateOf(false) }
    var showAddOperatorDialog by remember { mutableStateOf(false) }
    var addCustomText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                AssistChip(
                    onClick = { regionExpanded = true },
                    label = {
                        Text(
                            if (sessionConfig.region.isNotEmpty()) sessionConfig.region else "请选择",
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                )
                DropdownMenu(
                    expanded = regionExpanded,
                    onDismissRequest = { regionExpanded = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    allRegions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                regionExpanded = false
                                if (option != sessionConfig.region) onRegionSelected(option)
                            }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("+ 添加自定义...", color = MaterialTheme.colorScheme.primary) },
                        onClick = {
                            regionExpanded = false
                            addCustomText = ""
                            showAddRegionDialog = true
                        }
                    )
                }
            }

            var showDatePicker by remember { mutableStateOf(false) }
            val dateSdf = remember { SimpleDateFormat("yyMMdd", Locale.getDefault()) }
            val displaySdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

            Box(modifier = Modifier.weight(1.3f)) {
                val parsedDate = remember(sessionConfig.date) {
                    try { dateSdf.parse(sessionConfig.date) } catch (_: Exception) { null }
                }

                AssistChip(
                    onClick = { showDatePicker = true },
                    label = {
                        Text(
                            if (parsedDate != null) displaySdf.format(parsedDate) else sessionConfig.date,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                )
            }

            if (showDatePicker) {
                val cal = Calendar.getInstance()
                val parsed = try { dateSdf.parse(sessionConfig.date) } catch (_: Exception) { null }
                if (parsed != null) cal.time = parsed
                cal.set(Calendar.HOUR_OF_DAY, 12)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = cal.timeInMillis
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val cal2 = Calendar.getInstance().apply { timeInMillis = millis }
                                val newDate = dateSdf.format(cal2.time)
                                if (newDate != sessionConfig.date) onDateSelected(newDate)
                            }
                            showDatePicker = false
                        }) { Text("确定") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("取消") }
                    },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    DatePicker(
                        state = datePickerState,
                        colors = DatePickerDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            headlineContentColor = MaterialTheme.colorScheme.onSurface,
                            weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            subheadContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                            selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                            todayContentColor = MaterialTheme.colorScheme.primary,
                            todayDateBorderColor = MaterialTheme.colorScheme.primary,
                            dayContentColor = MaterialTheme.colorScheme.onSurface,
                            yearContentColor = MaterialTheme.colorScheme.onSurface,
                            selectedYearContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Box(modifier = Modifier.weight(0.7f)) {
                AssistChip(
                    onClick = { operatorExpanded = true },
                    label = {
                        Text(
                            if (sessionConfig.operator.isNotEmpty()) sessionConfig.operator else "请选择",
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                )
                DropdownMenu(
                    expanded = operatorExpanded,
                    onDismissRequest = { operatorExpanded = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    allOperators.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                operatorExpanded = false
                                if (option != sessionConfig.operator) onOperatorSelected(option)
                            }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("+ 添加自定义...", color = MaterialTheme.colorScheme.primary) },
                        onClick = {
                            operatorExpanded = false
                            addCustomText = ""
                            showAddOperatorDialog = true
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = locationText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            IconButton(
                onClick = onRefreshLocation,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "获取定位",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // 添加自定义地区对话框
    if (showAddRegionDialog) {
        AlertDialog(
            onDismissRequest = { showAddRegionDialog = false; addCustomText = "" },
            shape = RoundedCornerShape(12.dp),
            title = { Text("添加自定义地区") },
            text = {
                OutlinedTextField(
                    value = addCustomText,
                    onValueChange = { addCustomText = it },
                    label = { Text("输入地区名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val text = addCustomText.trim()
                        if (text.isNotBlank()) {
                            val newSet = customRegions + text
                            prefs.edit().putStringSet("regions", newSet).apply()
                            customRegions = newSet
                            onRegionSelected(text)
                            addCustomText = ""
                            showAddRegionDialog = false
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { showAddRegionDialog = false; addCustomText = "" }) { Text("取消") }
            }
        )
    }

    // 添加自定义人员对话框
    if (showAddOperatorDialog) {
        AlertDialog(
            onDismissRequest = { showAddOperatorDialog = false; addCustomText = "" },
            shape = RoundedCornerShape(12.dp),
            title = { Text("添加自定义人员") },
            text = {
                OutlinedTextField(
                    value = addCustomText,
                    onValueChange = { addCustomText = it },
                    label = { Text("输入人员名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val text = addCustomText.trim()
                        if (text.isNotBlank()) {
                            val newSet = customOperators + text
                            prefs.edit().putStringSet("operators", newSet).apply()
                            customOperators = newSet
                            onOperatorSelected(text)
                            addCustomText = ""
                            showAddOperatorDialog = false
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { showAddOperatorDialog = false; addCustomText = "" }) { Text("取消") }
            }
        )
    }
}

@Composable
fun CompactCodeAngleBar(
    captureState: CaptureState,
    angleSequence: List<String>,
    onSelectAngle: (Int) -> Unit,
    onDecrementField: () -> Unit,
    onIncrementField: () -> Unit,
    onDecrementSample: () -> Unit,
    onIncrementSample: () -> Unit,
    progressLabel: String
) {
    val view = LocalView.current
    val hapticClick: () -> Unit = {
        view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text("田块", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(
                    onClick = { onDecrementField(); hapticClick() },
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                    modifier = Modifier.size(24.dp)
                ) { Text("−", fontSize = 14.sp) }
                Text(
                    text = CaptureCodeManager.formatCode(captureState.fieldCode),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
                TextButton(
                    onClick = { onIncrementField(); hapticClick() },
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                    modifier = Modifier.size(24.dp)
                ) { Text("+", fontSize = 14.sp) }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text("样本", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(
                    onClick = { onDecrementSample(); hapticClick() },
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                    modifier = Modifier.size(24.dp)
                ) { Text("−", fontSize = 14.sp) }
                Text(
                    text = CaptureCodeManager.formatCode(captureState.sampleCode),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
                TextButton(
                    onClick = { onIncrementSample(); hapticClick() },
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                    modifier = Modifier.size(24.dp)
                ) { Text("+", fontSize = 14.sp) }
            }

            Text(
                text = progressLabel,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            angleSequence.forEachIndexed { index, angle ->
                val isCaptured = angle in captureState.capturedAngles
                val isSelected = index == captureState.currentAngleIndex

                val bgColor = when {
                    isCaptured -> MaterialTheme.colorScheme.primaryContainer
                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val textColor = when {
                    isCaptured -> MaterialTheme.colorScheme.onPrimaryContainer
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                val angleInteractionSource = remember { MutableInteractionSource() }
                val isAnglePressed by angleInteractionSource.collectIsPressedAsState()
                val angleScale by animateFloatAsState(
                    if (isAnglePressed) 0.92f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "angleScale"
                )

                Button(
                    onClick = { onSelectAngle(index); hapticClick() },
                    modifier = Modifier
                        .widthIn(min = 60.dp)
                        .graphicsLayer(scaleX = angleScale, scaleY = angleScale),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = bgColor,
                        contentColor = textColor
                    ),
                    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = if (isSelected) 6.dp else 0.dp
                    ),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    interactionSource = angleInteractionSource
                ) {
                    Text(
                        text = if (isCaptured) "$angle ✓" else angle,
                        fontSize = 12.sp,
                        fontWeight = if (isCaptured || isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun CompactGroupConfirmContent(
    captureState: CaptureState,
    angleSequence: List<String>,
    capturedMetadataList: List<CaptureMetadata>,
    onRetake: () -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    var previewFilePath by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "田块 ${CaptureCodeManager.formatCode(captureState.fieldCode)} / " +
                    "样本 ${CaptureCodeManager.formatCode(captureState.sampleCode)} — 拍摄完成",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 动态计算2列布局的行数
            val cols = 2
            val rows = (angleSequence.size + cols - 1) / cols
            for (row in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (col in 0 until cols) {
                        val index = row * cols + col
                        if (index >= angleSequence.size) break
                        val angle = angleSequence[index]
                        val metadata = capturedMetadataList.find { it.angleCode == angle }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (metadata != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { previewFilePath = metadata.filePath }
                                ) {
                                    val painter = rememberAsyncImagePainter(
                                        model = ImageRequest.Builder(context)
                                            .data(File(metadata.filePath))
                                            .crossfade(true)
                                            .build()
                                    )
                                    Image(
                                        painter = painter,
                                        contentDescription = "角度 $angle",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .background(
                                                Color.Black.copy(alpha = 0.55f),
                                                RoundedCornerShape(topStart = 6.dp, bottomEnd = 6.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(angle, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(angle, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val retakeInteractionSource = remember { MutableInteractionSource() }
            val isRetakePressed by retakeInteractionSource.collectIsPressedAsState()
            val retakeScale by animateFloatAsState(
                if (isRetakePressed) 0.95f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "retakeScale"
            )
            val confirmInteractionSource = remember { MutableInteractionSource() }
            val isConfirmPressed by confirmInteractionSource.collectIsPressedAsState()
            val confirmScale by animateFloatAsState(
                if (isConfirmPressed) 0.95f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "confirmScale"
            )

            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer(scaleX = retakeScale, scaleY = retakeScale),
                shape = RoundedCornerShape(8.dp),
                interactionSource = retakeInteractionSource
            ) {
                Text("重拍本组")
            }
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer(scaleX = confirmScale, scaleY = confirmScale),
                shape = RoundedCornerShape(8.dp),
                interactionSource = confirmInteractionSource
            ) {
                Text("确认进入下一组")
            }
        }
    }

    if (previewFilePath != null) {
        val filePath = previewFilePath!!
        Dialog(
            onDismissRequest = { previewFilePath = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable { previewFilePath = null },
                contentAlignment = Alignment.Center
            ) {
                val painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(context)
                        .data(File(filePath))
                        .crossfade(true)
                        .build()
                )
                Image(
                    painter = painter,
                    contentDescription = "预览",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentScale = ContentScale.Fit
                )
                Text(
                    text = "点击任意位置关闭",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                )
            }
        }
    }
}

@Composable
fun CompactBottomBar(
    captureMessage: String?,
    metadataWriteFailed: Boolean,
    previewFileName: String,
    onOpenGallery: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val statusMsg = when {
            metadataWriteFailed -> "照片已保存，但元数据记录失败"
            captureMessage != null -> captureMessage
            else -> null
        }
        if (statusMsg != null) {
            Text(
                text = statusMsg,
                fontSize = 13.sp,
                color = if (metadataWriteFailed) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = previewFileName,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onOpenGallery,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Storage, contentDescription = "数据库", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun FieldEditDialog(viewModel: CaptureViewModel, onFieldChanged: () -> Unit = {}) {
    val editText by viewModel.fieldEditText.collectAsState()
    val editError by viewModel.fieldEditError.collectAsState()
    val captureState by viewModel.captureState.collectAsState()

    AlertDialog(
        onDismissRequest = { viewModel.closeFieldEditDialog() },
        shape = RoundedCornerShape(12.dp),
        title = { Text("手动修改田块编号") },
        text = {
            Column {
                Text(
                    text = "当前田块编号：${CaptureCodeManager.formatCode(captureState.fieldCode)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "目标田块编号：",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            val current = editText.toIntOrNull() ?: 1
                            if (current > 1) {
                                viewModel.updateFieldEditText((current - 1).toString())
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("−", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = editText.padStart(2, '0'),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(48.dp),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    OutlinedButton(
                        onClick = {
                            val current = editText.toIntOrNull() ?: 1
                            if (current < 99) {
                                viewModel.updateFieldEditText((current + 1).toString())
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("+", fontSize = 20.sp)
                    }
                }
                if (editError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = editError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.confirmFieldEdit()
                    onFieldChanged()
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.closeFieldEditDialog() }) {
                Text("取消")
            }
        }
    )
}

@Composable
fun SampleEditDialog(viewModel: CaptureViewModel) {
    val editText by viewModel.sampleEditText.collectAsState()
    val editError by viewModel.sampleEditError.collectAsState()

    AlertDialog(
        onDismissRequest = { viewModel.closeSampleEditDialog() },
        shape = RoundedCornerShape(12.dp),
        title = { Text("手动修改样本编号") },
        text = {
            Column {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { viewModel.updateSampleEditText(it) },
                    label = { Text("请输入样本编号 (1-99)") },
                    singleLine = true,
                    isError = editError != null,
                    supportingText = if (editError != null) {
                        { Text(editError!!, color = MaterialTheme.colorScheme.error) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { viewModel.confirmSampleEdit() }, shape = RoundedCornerShape(8.dp)) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.closeSampleEditDialog() }) {
                Text("取消")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MissingFieldsDialog(
    sessionConfig: CaptureSessionConfig,
    onUpdateRegion: (String) -> Unit,
    onUpdateOperator: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("custom_options", Context.MODE_PRIVATE) }
    var customRegions by remember { mutableStateOf(prefs.getStringSet("regions", emptySet())?.toSet() ?: emptySet()) }
    var customOperators by remember { mutableStateOf(prefs.getStringSet("operators", emptySet())?.toSet() ?: emptySet()) }

    val defaultRegions = listOf("JL", "XT", "JS")
    val defaultOperators = listOf("黄添", "史俊尧", "苏辰晔", "王宇杰", "张浩然")
    val allRegions = remember(customRegions) { defaultRegions + customRegions.sorted() }
    val allOperators = remember(customOperators) { defaultOperators + customOperators.sorted() }

    val missingRegion = sessionConfig.region.isBlank()
    val missingOperator = sessionConfig.operator.isBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(12.dp),
        title = { Text("请补充以下信息") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "拍摄前需要填写以下内容：",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (missingRegion) {
                    Column {
                        Text(
                            text = "选择地区",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            allRegions.take(6).forEach { option ->
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        onUpdateRegion(option)
                                        if (!missingOperator || sessionConfig.operator.isNotEmpty()) {
                                            onDismiss()
                                        }
                                    },
                                    label = { Text(option, fontSize = 13.sp) },
                                    shape = RoundedCornerShape(6.dp)
                                )
                            }
                            FilterChip(
                                selected = false,
                                onClick = {
                                    onDismiss()
                                },
                                label = { Text("+ 添加", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary) },
                                shape = RoundedCornerShape(6.dp)
                            )
                        }
                    }
                }
                if (missingOperator) {
                    Column {
                        Text(
                            text = "选择拍摄人",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            allOperators.take(8).forEach { option ->
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        onUpdateOperator(option)
                                        if (!missingRegion || sessionConfig.region.isNotEmpty()) {
                                            onDismiss()
                                        }
                                    },
                                    label = { Text(option, fontSize = 13.sp) },
                                    shape = RoundedCornerShape(6.dp)
                                )
                            }
                            FilterChip(
                                selected = false,
                                onClick = {
                                    onDismiss()
                                },
                                label = { Text("+ 添加", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary) },
                                shape = RoundedCornerShape(6.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了")
            }
        }
    )
}

@Composable
fun CodeLockDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(12.dp),
        title = { Text("提示") },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                Text("知道了")
            }
        }
    )
}

private fun vibrate(context: android.content.Context) {
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(60)
            }
        }
    } catch (_: Exception) {
        // 震动失败不影响正常拍照
    }
}