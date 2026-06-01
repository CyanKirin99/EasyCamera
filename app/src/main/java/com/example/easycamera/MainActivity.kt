package com.example.easycamera

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import android.view.KeyEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.easycamera.camera.CameraPreview
import com.example.easycamera.camera.rememberImageCaptureState
import com.example.easycamera.camera.takePhoto
import com.example.easycamera.data.location.LocationProvider
import com.example.easycamera.data.imports.ImportPlan
import com.example.easycamera.data.imports.ImportResult
import com.example.easycamera.data.imports.ProjectImportManager
import com.example.easycamera.data.model.CaptureMetadata
import com.example.easycamera.data.model.CaptureSessionConfig
import com.example.easycamera.data.model.CaptureState
import com.example.easycamera.data.repository.MetadataRepository
import com.example.easycamera.domain.CaptureCodeManager
import com.example.easycamera.ui.CaptureViewModel
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
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

    val imageCaptureState = rememberImageCaptureState()

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
            val loc = locationProvider.getLocation()
            viewModel.updateLocation(loc)
        }
    }

    var showPhotoGallery by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    var showImportDialog by remember { mutableStateOf(false) }
    var pendingImportPlan by remember { mutableStateOf<ImportPlan?>(null) }
    var importResultText by remember { mutableStateOf<String?>(null) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val tempFile = File(context.cacheDir, "import_temp.zip")
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tempFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    val result = ProjectImportManager.analyze(context, tempFile)
                    withContext(Dispatchers.Main) {
                        when (result) {
                            is ImportResult.Ready -> {
                                if (result.plan.hasConflicts) {
                                    pendingImportPlan = result.plan
                                    showImportDialog = true
                                } else {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val execResult = ProjectImportManager.executeImport(
                                            context, result.plan, overwriteExisting = false
                                        )
                                        withContext(Dispatchers.Main) {
                                            importResultText = when (execResult) {
                                                is ImportResult.Success ->
                                                    "导入完成：成功 ${execResult.importedCount} 张，跳过 ${execResult.skippedCount} 张"
                                                is ImportResult.Error -> execResult.message
                                                else -> "导入失败"
                                            }
                                        }
                                    }
                                }
                            }
                            is ImportResult.Error -> { importResultText = result.message }
                            else -> {}
                        }
                        tempFile.delete()
                    }
                } catch (e: Exception) {
                    importResultText = "读取文件失败：${e.message ?: "未知错误"}"
                    tempFile.delete()
                }
            }
        }
    }

    var dontShowNAWarning by remember { mutableStateOf(false) }
    var showNAWarningDialog by remember { mutableStateOf(false) }
    var pendingNAConfirm by remember { mutableStateOf<(() -> Unit)?>(null) }

    var showRetakeConfirmDialog by remember { mutableStateOf(false) }

    var showOverwriteConfirmDialog by remember { mutableStateOf(false) }
    var pendingOverwriteDoCapture by remember { mutableStateOf<(() -> Unit)?>(null) }
    var overwriteExistingMatch by remember { mutableStateOf<CaptureMetadata?>(null) }

    val locationText by viewModel.locationStatus.collectAsState()
    val locationDetermined by viewModel.locationDetermined.collectAsState()
    val isLocationNA = remember(locationText) {
        locationText.contains("NA") || locationText.contains("不可用")
    }

    val metadataRepository = remember { MetadataRepository(context) }

    var showExitConfirm by remember { mutableStateOf(false) }

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
                onNavigateBack = { showPhotoGallery = false }
            )
        } else {
        BackHandler { showExitConfirm = true }

        val performCapture = {
            val ic = imageCaptureState.value
            if (ic != null && !captureState.isGroupComplete && !isCapturing) {
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

        // 全局音量键监听：按音量+/-触发拍照
        val activityContext = LocalContext.current
        DisposableEffect(Unit) {
            val activity = activityContext as? ComponentActivity
            if (activity != null) {
                val decorView = activity.window.decorView
                val keyListener = View.OnKeyListener { _, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_DOWN &&
                        (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
                    ) {
                        performCapture()
                        true
                    } else {
                        false
                    }
                }
                decorView.setOnKeyListener(keyListener)
                decorView.isFocusable = true
                decorView.requestFocus()
                onDispose {
                    decorView.setOnKeyListener(null)
                }
            } else {
                onDispose { }
            }
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
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
                            val loc = locationProvider.getLocation()
                            viewModel.updateLocation(loc)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (captureState.isGroupComplete) {
                CompactGroupConfirmContent(
                    captureState = captureState,
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
                    .clipToBounds(),
                contentAlignment = Alignment.Center
            ) {
                if (cameraPermissionGranted) {
                    CameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        imageCaptureState = imageCaptureState
                    )
                } else {
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
                }
                if (capturedPreviewPath != null) {
                    val previewPath = capturedPreviewPath!!
                    val painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(context)
                            .data(File(previewPath))
                            .crossfade(true)
                            .build()
                    )
                    Image(
                        painter = painter,
                        contentDescription = "已拍摄照片，点击重拍",
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { showRetakeConfirmDialog = true },
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

            Spacer(modifier = Modifier.height(2.dp))

            CompactCodeAngleBar(
                captureState = captureState,
                onSelectAngle = { viewModel.selectAngleIndex(it) },
                onDecrementField = { viewModel.decrementFieldCode() },
                onIncrementField = { viewModel.incrementFieldCode() },
                onDecrementSample = { viewModel.decrementSampleCode() },
                onIncrementSample = { viewModel.incrementSampleCode() },
                progressLabel = viewModel.progressLabel
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (captureState.isGroupComplete) {
                // CompactGroupConfirmContent already has retake/confirm buttons
            } else {
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

            Spacer(modifier = Modifier.height(10.dp))

            CompactBottomBar(
                captureMessage = captureMessage,
                metadataWriteFailed = metadataWriteFailed,
                previewFileName = viewModel.previewFileName,
                onOpenGallery = { showPhotoGallery = true },
                onImport = {
                    importLauncher.launch(arrayOf("application/zip"))
                }
            )

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

    if (showFieldEditDialog) {
        FieldEditDialog(viewModel)
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

    LaunchedEffect(importResultText) {
        if (importResultText != null) {
            Toast.makeText(context, importResultText, Toast.LENGTH_LONG).show()
            importResultText = null
        }
    }

    if (showImportDialog && pendingImportPlan != null) {
        val plan = pendingImportPlan!!
        var overwrite by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = {
                showImportDialog = false
                pendingImportPlan = null
            },
            shape = RoundedCornerShape(12.dp),
            title = { Text("导入检测到冲突") },
            text = {
                Column {
                    Text(
                        text = "压缩包中包含 ${plan.entries.size} 张照片，其中 ${plan.entries.count { it.exists }} 张已存在于本地。",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { overwrite = !overwrite }
                    ) {
                        Checkbox(
                            checked = overwrite,
                            onCheckedChange = { overwrite = !overwrite }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("覆盖已存在的文件")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showImportDialog = false
                    pendingImportPlan = null
                    coroutineScope.launch(Dispatchers.IO) {
                        val execResult = ProjectImportManager.executeImport(
                            context, plan, overwriteExisting = overwrite
                        )
                        withContext(Dispatchers.Main) {
                            importResultText = when (execResult) {
                                is ImportResult.Success ->
                                    "导入完成：成功 ${execResult.importedCount} 张，跳过 ${execResult.skippedCount} 张"
                                is ImportResult.Error -> execResult.message
                                else -> "导入失败"
                            }
                        }
                    }
                }) {
                    Text("开始导入")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportDialog = false
                    pendingImportPlan = null
                }) {
                    Text("取消")
                }
            }
        )
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
    val regionOptions = listOf("JL", "XT", "JS")
    val operatorOptions = listOf("黄添", "史俊尧", "苏辰晔", "王宇杰", "张浩然")
    val locationText by locationStatus.collectAsState()
    val context = LocalContext.current

    var regionExpanded by remember { mutableStateOf(false) }
    var operatorExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box {
            AssistChip(
                onClick = { regionExpanded = true },
                label = { Text(sessionConfig.region, fontSize = 14.sp) },
                leadingIcon = { Text("地区", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(34.dp)
            )
            DropdownMenu(
                expanded = regionExpanded,
                onDismissRequest = { regionExpanded = false },
                shape = RoundedCornerShape(8.dp)
            ) {
                regionOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            regionExpanded = false
                            if (option != sessionConfig.region) onRegionSelected(option)
                        }
                    )
                }
            }
        }

        // Date chip — opens Material3 DatePickerDialog
        var showDatePicker by remember { mutableStateOf(false) }
        val dateSdf = remember { SimpleDateFormat("yyMMdd", Locale.getDefault()) }
        val displaySdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

        Box {
            val parsedDate = remember(sessionConfig.date) {
                try { dateSdf.parse(sessionConfig.date) } catch (_: Exception) { null }
            }

            AssistChip(
                onClick = { showDatePicker = true },
                label = {
                    Text(
                        if (parsedDate != null) displaySdf.format(parsedDate) else sessionConfig.date,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = { Text("日期", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(34.dp)
            )
        }

        if (showDatePicker) {
            val cal = Calendar.getInstance()
            val parsed = try { dateSdf.parse(sessionConfig.date) } catch (_: Exception) { null }
            if (parsed != null) cal.time = parsed
            // Set to noon to avoid UTC date boundary shift when DatePicker converts to UTC
            cal.set(Calendar.HOUR_OF_DAY, 12)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = cal.timeInMillis,
                selectableDates = object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        return utcTimeMillis <= System.currentTimeMillis()
                    }
                }
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

        Box {
            AssistChip(
                onClick = { operatorExpanded = true },
                label = { Text(sessionConfig.operator, fontSize = 14.sp, maxLines = 1) },
                leadingIcon = { Text("拍摄人", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(34.dp)
            )
            DropdownMenu(
                expanded = operatorExpanded,
                onDismissRequest = { operatorExpanded = false },
                shape = RoundedCornerShape(8.dp)
            ) {
                operatorOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            operatorExpanded = false
                            if (option != sessionConfig.operator) onOperatorSelected(option)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = locationText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.6f, fill = false)
        )

        IconButton(
            onClick = onRefreshLocation,
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "获取定位",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CompactCodeAngleBar(
    captureState: CaptureState,
    onSelectAngle: (Int) -> Unit,
    onDecrementField: () -> Unit,
    onIncrementField: () -> Unit,
    onDecrementSample: () -> Unit,
    onIncrementSample: () -> Unit,
    progressLabel: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("田块", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(
                    onClick = onDecrementField,
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                    modifier = Modifier.height(18.dp)
                ) { Text("-", fontSize = 16.sp) }
                Text(
                    text = CaptureCodeManager.formatCode(captureState.fieldCode),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 1.dp)
                )
                TextButton(
                    onClick = onIncrementField,
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                    modifier = Modifier.height(18.dp)
                ) { Text("+", fontSize = 16.sp) }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("样本", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(
                    onClick = onDecrementSample,
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                    modifier = Modifier.height(18.dp)
                ) { Text("-", fontSize = 16.sp) }
                Text(
                    text = CaptureCodeManager.formatCode(captureState.sampleCode),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 1.dp)
                )
                TextButton(
                    onClick = onIncrementSample,
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                    modifier = Modifier.height(18.dp)
                ) { Text("+", fontSize = 16.sp) }
            }

            Text(
                text = progressLabel,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val angleSequence = listOf("A", "B", "C", "D")
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
                    onClick = { onSelectAngle(index) },
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer(scaleX = angleScale, scaleY = angleScale),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = bgColor,
                        contentColor = textColor
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    interactionSource = angleInteractionSource
                ) {
                    Text(
                        text = if (isCaptured) "$angle ✓" else angle,
                        fontSize = 13.sp,
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
            val angleSequence = listOf("A", "B", "C", "D")
            for (row in 0..1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (col in 0..1) {
                        val index = row * 2 + col
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
    onOpenGallery: () -> Unit,
    onImport: () -> Unit
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            OutlinedButton(
                onClick = onImport,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("导入", fontSize = 13.sp)
            }

            OutlinedButton(
                onClick = onOpenGallery,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("照片集", fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun FieldEditDialog(viewModel: CaptureViewModel) {
    val editText by viewModel.fieldEditText.collectAsState()
    val editError by viewModel.fieldEditError.collectAsState()

    AlertDialog(
        onDismissRequest = { viewModel.closeFieldEditDialog() },
        shape = RoundedCornerShape(12.dp),
        title = { Text("手动修改田块编号") },
        text = {
            Column {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { viewModel.updateFieldEditText(it) },
                    label = { Text("请输入田块编号 (1-99)") },
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
            Button(onClick = { viewModel.confirmFieldEdit() }, shape = RoundedCornerShape(8.dp)) {
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