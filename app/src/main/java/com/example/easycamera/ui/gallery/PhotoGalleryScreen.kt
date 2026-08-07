package com.example.easycamera.ui.gallery

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.easycamera.data.model.CaptureProject
import com.example.easycamera.data.model.CapturedPhoto
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoGalleryScreen(
    viewModel: PhotoGalleryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAnalysis: (region: String, date: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val exportState by viewModel.exportState.collectAsState()

    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.completeExport(uri)
        } else {
            viewModel.resetExportState()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadProjects()
    }

    LaunchedEffect(exportState) {
        when (val state = exportState) {
            is ExportState.Ready -> {
                exportLauncher.launch(state.suggestedName)
            }
            is ExportState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.resetExportState()
            }
            is ExportState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.resetExportState()
            }
            else -> {}
        }
    }

    val deleteMessage by viewModel.deleteMessage.collectAsState()
    LaunchedEffect(deleteMessage) {
        if (deleteMessage != null) {
            Toast.makeText(context, deleteMessage, Toast.LENGTH_LONG).show()
            viewModel.clearDeleteMessage()
        }
    }

    val importMessage by viewModel.importMessage.collectAsState()
    LaunchedEffect(importMessage) {
        if (importMessage != null) {
            Toast.makeText(context, importMessage, Toast.LENGTH_LONG).show()
            viewModel.clearImportMessage()
        }
    }

    var showDeleteProjectConfirm by remember { mutableStateOf(false) }

    var showProjectContent by remember { mutableStateOf(true) }

    var showFieldEditDialog by remember { mutableStateOf(false) }
    var editingFieldCode by remember { mutableStateOf("") }
    var editingSampleCode by remember { mutableStateOf("") }
    var fieldEditNewValue by remember { mutableStateOf("") }
    var fieldEditError by remember { mutableStateOf<String?>(null) }
    var showOverwriteFieldConfirm by remember { mutableStateOf(false) }
    var pendingNewFieldCode by remember { mutableStateOf("") }
    var showSwapConfirmDialog by remember { mutableStateOf(false) }
    var showForceOverwriteConfirmDialog by remember { mutableStateOf(false) }
    var showCsvViewer by remember { mutableStateOf(false) }

    // Sample code editing state (also supports field code change)
    var showSampleCodeEditDialog by remember { mutableStateOf(false) }
    var editingSampleCodeFieldCode by remember { mutableStateOf("") }
    var editingSampleCodeOldCode by remember { mutableStateOf("") }
    var sampleCodeEditNewFieldCode by remember { mutableStateOf("") }
    var sampleCodeEditNewSampleCode by remember { mutableStateOf("") }
    var sampleCodeEditError by remember { mutableStateOf<String?>(null) }
    var showOverwriteSampleConfirm by remember { mutableStateOf(false) }
    var pendingNewFieldCodeSample by remember { mutableStateOf<Pair<String, String>>(Pair("", "")) }
    var showSampleSwapConfirmDialog by remember { mutableStateOf(false) }
    var showSampleForceOverwriteConfirmDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    var showImportDialog by remember { mutableStateOf(false) }
    var pendingImportPlan by remember { mutableStateOf<com.example.easycamera.data.imports.ImportPlan?>(null) }
    var pendingImportTempFile by remember { mutableStateOf<File?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val tempFile = File(context.cacheDir, "import_temp_${System.currentTimeMillis()}.zip")
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tempFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    val result = com.example.easycamera.data.imports.ProjectImportManager.analyze(context, tempFile)
                    withContext(Dispatchers.Main) {
                        when (result) {
                            is com.example.easycamera.data.imports.ImportResult.Ready -> {
                                if (result.plan.hasConflicts) {
                                    pendingImportPlan = result.plan
                                    pendingImportTempFile = tempFile
                                    showImportDialog = true
                                } else {
                                    viewModel.executeImport(result.plan, overwriteExisting = false, tempFile = tempFile)
                                }
                            }
                            is com.example.easycamera.data.imports.ImportResult.Error -> {
                                viewModel.showToast(result.message)
                                tempFile.delete()
                            }
                            else -> {}
                        }
                    }
                } catch (e: Exception) {
                    viewModel.showToast("读取文件失败：${e.message ?: "未知错误"}")
                    tempFile.delete()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("数据库") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", fontSize = 20.sp)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshCurrentProject() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 10.dp)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // 筛选项目按钮 + 导入项目按钮 左右并排
                var filterExpanded by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { filterExpanded = !filterExpanded },
                        modifier = Modifier.weight(1f).height(34.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = if (filterExpanded) "收起筛选" else "筛选项目",
                            fontSize = 12.sp
                        )
                    }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/zip")) },
                        modifier = Modifier.weight(1f).height(34.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Text(text = "导入项目", fontSize = 12.sp)
                    }
                }

                // 筛选条件折叠区
                AnimatedVisibility(visible = filterExpanded) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var filterRegionExpanded by remember { mutableStateOf(false) }
                        Box {
                            AssistChip(
                                onClick = { filterRegionExpanded = true },
                                label = {
                                    Text(
                                        if (uiState.filterRegion.isNotEmpty()) uiState.filterRegion else "全部地区",
                                        fontSize = 12.sp
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(28.dp)
                            )
                            DropdownMenu(
                                expanded = filterRegionExpanded,
                                onDismissRequest = { filterRegionExpanded = false },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("全部地区") },
                                    onClick = {
                                        filterRegionExpanded = false
                                        viewModel.setFilterRegion("")
                                    }
                                )
                                uiState.allRegions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            filterRegionExpanded = false
                                            viewModel.setFilterRegion(option)
                                        }
                                    )
                                }
                            }
                        }
                        var filterDateExpanded by remember { mutableStateOf(false) }
                        Box {
                            AssistChip(
                                onClick = { filterDateExpanded = true },
                                label = {
                                    Text(
                                        if (uiState.filterDate.isNotEmpty()) uiState.filterDate else "全部日期",
                                        fontSize = 12.sp
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(28.dp)
                            )
                            DropdownMenu(
                                expanded = filterDateExpanded,
                                onDismissRequest = { filterDateExpanded = false },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("全部日期") },
                                    onClick = {
                                        filterDateExpanded = false
                                        viewModel.setFilterDate("")
                                    }
                                )
                                uiState.allDates.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            filterDateExpanded = false
                                            viewModel.setFilterDate(option)
                                        }
                                    )
                                }
                            }
                        }
                        var filterOperatorExpanded by remember { mutableStateOf(false) }
                        Box {
                            AssistChip(
                                onClick = { filterOperatorExpanded = true },
                                label = {
                                    Text(
                                        if (uiState.filterOperator.isNotEmpty()) uiState.filterOperator else "全部人员",
                                        fontSize = 12.sp
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(28.dp)
                            )
                            DropdownMenu(
                                expanded = filterOperatorExpanded,
                                onDismissRequest = { filterOperatorExpanded = false },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("全部人员") },
                                    onClick = {
                                        filterOperatorExpanded = false
                                        viewModel.setFilterOperator("")
                                    }
                                )
                                uiState.allOperators.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            filterOperatorExpanded = false
                                            viewModel.setFilterOperator(option)
                                        }
                                    )
                                }
                            }
                        }
                        if (uiState.filterRegion.isNotEmpty() || uiState.filterDate.isNotEmpty() || uiState.filterOperator.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    viewModel.setFilterRegion("")
                                    viewModel.setFilterDate("")
                                    viewModel.setFilterOperator("")
                                },
                                modifier = Modifier.height(26.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Text("清除", fontSize = 10.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                ProjectSelectorBar(
                    candidateProjects = uiState.candidateProjects,
                    selectedProject = uiState.selectedProject,
                    onProjectSelected = { viewModel.selectProject(it) },
                    onRefresh = { viewModel.refreshCurrentProject() },
                    onDeleteProject = { showDeleteProjectConfirm = true },
                    onToggleCollapse = { showProjectContent = !showProjectContent }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Project content (or empty message)
                if (uiState.projects.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无项目。请先导入或创建项目。",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val selectedProject = uiState.selectedProject
                    if (selectedProject != null && showProjectContent) {
                        // 项目信息 + 查看元数据按钮 同一行
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "当前项目：${selectedProject.projectName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                val photoCount = uiState.photos.size
                                val fieldCount = uiState.photos.map { it.fieldCode }.distinct().size
                                val sampleCount = uiState.photos.map { "${it.fieldCode}_${it.sampleCode}" }.distinct().size
                                Text(
                                    text = "田块: $fieldCount    样本: $sampleCount    照片: $photoCount",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            OutlinedButton(
                                onClick = { showCsvViewer = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("查看元数据", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            if (uiState.photos.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "该项目暂无照片。请先完成拍摄。",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                val organizedData = remember(uiState.photos) {
                                    organizePhotos(uiState.photos)
                                }

                                // 田块数轴 - 用于快捷跳转到指定田块
                                val gridListState = rememberLazyListState()
                                val coroutineScope = rememberCoroutineScope()
                                val fieldCodes = remember(organizedData) {
                                    organizedData.map { it.fieldCode }
                                }

                                // 田块数轴滚轴
                                if (fieldCodes.size > 1) {
                                    Text(
                                        text = "田块导航：",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(fieldCodes) { fc ->
                                            val isActive = gridListState.firstVisibleItemIndex.let { idx ->
                                                idx < fieldCodes.size && fieldCodes[idx] == fc
                                            }
                                            FilterChip(
                                                selected = isActive,
                                                onClick = {
                                                    val targetIndex = fieldCodes.indexOf(fc)
                                                    if (targetIndex >= 0) {
                                                        coroutineScope.launch {
                                                            gridListState.animateScrollToItem(targetIndex)
                                                        }
                                                    }
                                                },
                                                label = {
                                                    Text(
                                                        text = fc,
                                                        fontSize = 13.sp
                                                    )
                                                },
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                PhotoGrid(
                                    fields = organizedData,
                                    gridListState = gridListState,
                                    onDeleteSample = { fieldCode, sampleCode ->
                                        viewModel.deleteSampleGroup(fieldCode, sampleCode)
                                    },
                                    onEditFieldCode = { fieldCode, sampleCode ->
                                        editingFieldCode = fieldCode
                                        editingSampleCode = sampleCode
                                        fieldEditNewValue = fieldCode
                                        fieldEditError = null
                                        showFieldEditDialog = true
                                    },
                                    onEditSampleCode = { fieldCode, sampleCode ->
                                        editingSampleCodeFieldCode = fieldCode
                                        editingSampleCodeOldCode = sampleCode
                                        sampleCodeEditNewFieldCode = fieldCode
                                        sampleCodeEditNewSampleCode = sampleCode
                                        sampleCodeEditError = null
                                        showSampleCodeEditDialog = true
                                    },
                                    onUpdateBbchPlantHeight = { photo, bbch, plantHeight ->
                                        viewModel.updateBbchAndPlantHeight(photo, bbch, plantHeight)
                                    },
                                    onUpdateSampleBbchPlantHeight = { region, date, fieldCode, sampleCode, bbch, plantHeight ->
                                        viewModel.updateSampleBbchAndPlantHeight(region, date, fieldCode, sampleCode, bbch, plantHeight)
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        val selectedProjectForAnalysis = uiState.selectedProject
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (selectedProjectForAnalysis != null) {
                                OutlinedButton(
                                    onClick = {
                                        onNavigateToAnalysis(selectedProjectForAnalysis.region, selectedProjectForAnalysis.date)
                                    },
                                    modifier = Modifier.weight(1f).height(34.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        text = "分析当前项目",
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { viewModel.startExport() },
                                modifier = Modifier.weight(1f).height(34.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                enabled = exportState !is ExportState.Exporting
                            ) {
                                if (exportState is ExportState.Exporting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = if (exportState is ExportState.Exporting) "正在导出..." else "导出当前项目",
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showDeleteProjectConfirm && uiState.selectedProject != null) {
        AlertDialog(
            onDismissRequest = { showDeleteProjectConfirm = false },
            shape = RoundedCornerShape(12.dp),
            title = { Text("删除项目") },
            text = { Text("确定要删除项目「${uiState.selectedProject!!.projectName}」吗？\n\n此操作将删除该项目所有照片和元数据，且不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteProjectConfirm = false
                    viewModel.deleteProject()
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteProjectConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showImportDialog && pendingImportPlan != null) {
        val plan = pendingImportPlan!!
        var overwrite by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = {
                showImportDialog = false
                pendingImportPlan = null
                pendingImportTempFile?.delete()
                pendingImportTempFile = null
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
                    val p = plan
                    val tf = pendingImportTempFile
                    pendingImportPlan = null
                    pendingImportTempFile = null
                    viewModel.executeImport(p, overwriteExisting = overwrite, tempFile = tf)
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

    if (showFieldEditDialog) {
        AlertDialog(
            onDismissRequest = {
                showFieldEditDialog = false
                fieldEditError = null
            },
            shape = RoundedCornerShape(12.dp),
            title = { Text("修改田块编号") },
            text = {
                Column {
                    Text(
                        text = "当前田块编号：${editingFieldCode}",
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
                                val current = fieldEditNewValue.toIntOrNull() ?: 1
                                if (current > 1) {
                                    fieldEditNewValue = (current - 1).toString().padStart(2, '0')
                                    fieldEditError = null
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("−", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = fieldEditNewValue.padStart(2, '0'),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(48.dp),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        OutlinedButton(
                            onClick = {
                                val current = fieldEditNewValue.toIntOrNull() ?: 1
                                if (current < 99) {
                                    fieldEditNewValue = (current + 1).toString().padStart(2, '0')
                                    fieldEditError = null
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("+", fontSize = 20.sp)
                        }
                    }
                    if (fieldEditError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = fieldEditError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newCode = fieldEditNewValue.trim().padStart(2, '0')
                        if (newCode.length < 1 || newCode.toIntOrNull() == null || newCode.toInt() !in 1..99) {
                            fieldEditError = "请输入有效的编号（1-99）"
                            return@Button
                        }
                        if (newCode == editingFieldCode.padStart(2, '0')) {
                            showFieldEditDialog = false
                            return@Button
                        }
                        if (viewModel.checkFieldSampleConflict(newCode, editingSampleCode)) {
                            pendingNewFieldCode = newCode
                            showFieldEditDialog = false
                            showOverwriteFieldConfirm = true
                        } else {
                            showFieldEditDialog = false
                            viewModel.modifyFieldCode(
                                oldFieldCode = editingFieldCode,
                                sampleCode = editingSampleCode,
                                newFieldCode = newCode,
                                overwriteDestination = false
                            )
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showFieldEditDialog = false
                    fieldEditError = null
                }) {
                    Text("取消")
                }
            }
        )
    }

    if (showOverwriteFieldConfirm) {
        AlertDialog(
            onDismissRequest = {
                showOverwriteFieldConfirm = false
                pendingNewFieldCode = ""
            },
            shape = RoundedCornerShape(12.dp),
            title = { Text("目标已有照片") },
            text = {
                Text(
                    "目标田块 ${pendingNewFieldCode} 已存在照片。\n\n" +
                            "请选择操作方式：\n\n" +
                            "• 对调：将田块 ${editingFieldCode.padStart(2, '0')} 与田块 ${pendingNewFieldCode} 的所有照片编号互换\n" +
                            "• 覆盖：田块 ${pendingNewFieldCode} 的所有照片将被完全删除且不可恢复，当前田块 ${editingFieldCode.padStart(2, '0')} 的照片将移入"
                )
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            showOverwriteFieldConfirm = false
                            showSwapConfirmDialog = true
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("对调")
                    }
                    Button(
                        onClick = {
                            showOverwriteFieldConfirm = false
                            showForceOverwriteConfirmDialog = true
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("覆盖")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showOverwriteFieldConfirm = false
                    pendingNewFieldCode = ""
                }) {
                    Text("取消")
                }
            }
        )
    }

    if (showSwapConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showSwapConfirmDialog = false
            },
            shape = RoundedCornerShape(12.dp),
            title = { Text("确认对调") },
            text = {
                Text(
                    "此操作将田块 ${editingFieldCode.padStart(2, '0')} 与田块 ${pendingNewFieldCode} 的所有照片编号互换，是否确认？",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showSwapConfirmDialog = false
                    val newCode = pendingNewFieldCode
                    pendingNewFieldCode = ""
                    viewModel.swapFieldCode(
                        oldFieldCode = editingFieldCode,
                        newFieldCode = newCode
                    )
                }) {
                    Text("确认对调")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSwapConfirmDialog = false
                }) {
                    Text("取消")
                }
            }
        )
    }

    if (showForceOverwriteConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showForceOverwriteConfirmDialog = false
            },
            shape = RoundedCornerShape(12.dp),
            title = { Text("确认覆盖") },
            text = {
                Text(
                    "此操作将彻底删除田块 ${pendingNewFieldCode} 的所有照片且不可恢复，当前田块 ${editingFieldCode.padStart(2, '0')} 的照片将移入，是否确认？",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showForceOverwriteConfirmDialog = false
                    val newCode = pendingNewFieldCode
                    pendingNewFieldCode = ""
                    viewModel.modifyFieldCode(
                        oldFieldCode = editingFieldCode,
                        sampleCode = editingSampleCode,
                        newFieldCode = newCode,
                        overwriteDestination = true
                    )
                }) {
                    Text("确认覆盖", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showForceOverwriteConfirmDialog = false
                }) {
                    Text("取消")
                }
            }
        )
    }

    // --- Sample Code Edit Dialog (also supports field code change) ---
    if (showSampleCodeEditDialog) {
        AlertDialog(
            onDismissRequest = {
                showSampleCodeEditDialog = false
                sampleCodeEditError = null
            },
            shape = RoundedCornerShape(12.dp),
            title = { Text("修改田块/样本编号") },
            text = {
                Column {
                    Text(
                        text = "当前：田块 ${editingSampleCodeFieldCode}  样本 ${editingSampleCodeOldCode}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    // Field code selector
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
                                val current = sampleCodeEditNewFieldCode.toIntOrNull() ?: 1
                                if (current > 1) {
                                    sampleCodeEditNewFieldCode = (current - 1).toString().padStart(2, '0')
                                    sampleCodeEditError = null
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("−", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = sampleCodeEditNewFieldCode.padStart(2, '0'),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(48.dp),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        OutlinedButton(
                            onClick = {
                                val current = sampleCodeEditNewFieldCode.toIntOrNull() ?: 1
                                if (current < 99) {
                                    sampleCodeEditNewFieldCode = (current + 1).toString().padStart(2, '0')
                                    sampleCodeEditError = null
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("+", fontSize = 20.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Sample code selector
                    Text(
                        text = "目标样本编号：",
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
                                val current = sampleCodeEditNewSampleCode.toIntOrNull() ?: 1
                                if (current > 1) {
                                    sampleCodeEditNewSampleCode = (current - 1).toString().padStart(2, '0')
                                    sampleCodeEditError = null
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("−", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = sampleCodeEditNewSampleCode.padStart(2, '0'),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(48.dp),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        OutlinedButton(
                            onClick = {
                                val current = sampleCodeEditNewSampleCode.toIntOrNull() ?: 1
                                if (current < 99) {
                                    sampleCodeEditNewSampleCode = (current + 1).toString().padStart(2, '0')
                                    sampleCodeEditError = null
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("+", fontSize = 20.sp)
                        }
                    }
                    if (sampleCodeEditError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = sampleCodeEditError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newField = sampleCodeEditNewFieldCode.trim().padStart(2, '0')
                        val newSample = sampleCodeEditNewSampleCode.trim().padStart(2, '0')
                        if (newField.length < 1 || newField.toIntOrNull() == null || newField.toInt() !in 1..99) {
                            sampleCodeEditError = "请输入有效的田块编号（1-99）"
                            return@Button
                        }
                        if (newSample.length < 1 || newSample.toIntOrNull() == null || newSample.toInt() !in 1..99) {
                            sampleCodeEditError = "请输入有效的样本编号（1-99）"
                            return@Button
                        }
                        val sameField = newField == editingSampleCodeFieldCode.padStart(2, '0')
                        val sameSample = newSample == editingSampleCodeOldCode.padStart(2, '0')
                        if (sameField && sameSample) {
                            showSampleCodeEditDialog = false
                            return@Button
                        }
                        if (viewModel.checkFieldSampleConflict(newField, newSample)) {
                            pendingNewFieldCodeSample = Pair(newField, newSample)
                            showSampleCodeEditDialog = false
                            showOverwriteSampleConfirm = true
                        } else {
                            showSampleCodeEditDialog = false
                            viewModel.modifyFieldAndSampleCode(
                                oldFieldCode = editingSampleCodeFieldCode,
                                oldSampleCode = editingSampleCodeOldCode,
                                newFieldCode = newField,
                                newSampleCode = newSample,
                                overwriteDestination = false
                            )
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSampleCodeEditDialog = false
                    sampleCodeEditError = null
                }) {
                    Text("取消")
                }
            }
        )
    }

    if (showOverwriteSampleConfirm) {
        val (targetField, targetSample) = pendingNewFieldCodeSample
        val sameField = targetField == editingSampleCodeFieldCode.padStart(2, '0')
        AlertDialog(
            onDismissRequest = {
                showOverwriteSampleConfirm = false
                pendingNewFieldCodeSample = Pair("", "")
            },
            shape = RoundedCornerShape(12.dp),
            title = { Text("目标已有照片") },
            text = {
                if (sameField) {
                    Text(
                        "田块 ${editingSampleCodeFieldCode} 的样本 ${targetSample} 已存在照片。\n\n" +
                                "请选择操作方式：\n\n" +
                                "• 对调：将样本 ${editingSampleCodeOldCode.padStart(2, '0')} 与样本 ${targetSample} 的照片编号互换\n" +
                                "• 覆盖：样本 ${targetSample} 的所有照片将被完全删除且不可恢复，当前样本 ${editingSampleCodeOldCode.padStart(2, '0')} 的照片将移入"
                    )
                } else {
                    Text(
                        "田块 ${targetField} 的样本 ${targetSample} 已存在照片。\n\n" +
                                "请选择操作方式：\n\n" +
                                "• 对调：将田块 ${editingSampleCodeFieldCode} 样本 ${editingSampleCodeOldCode.padStart(2, '0')} 与田块 ${targetField} 样本 ${targetSample} 的照片编号互换\n" +
                                "• 覆盖：田块 ${targetField} 样本 ${targetSample} 的所有照片将被完全删除且不可恢复，当前田块 ${editingSampleCodeFieldCode} 样本 ${editingSampleCodeOldCode.padStart(2, '0')} 的照片将移入"
                    )
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            showOverwriteSampleConfirm = false
                            showSampleSwapConfirmDialog = true
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("对调")
                    }
                    Button(
                        onClick = {
                            showOverwriteSampleConfirm = false
                            showSampleForceOverwriteConfirmDialog = true
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("覆盖")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showOverwriteSampleConfirm = false
                    pendingNewFieldCodeSample = Pair("", "")
                }) {
                    Text("取消")
                }
            }
        )
    }

    if (showSampleSwapConfirmDialog) {
        val (targetField, targetSample) = pendingNewFieldCodeSample
        val sameField = targetField == editingSampleCodeFieldCode.padStart(2, '0')
        AlertDialog(
            onDismissRequest = {
                showSampleSwapConfirmDialog = false
            },
            shape = RoundedCornerShape(12.dp),
            title = { Text("确认对调") },
            text = {
                if (sameField) {
                    Text(
                        "此操作将样本 ${editingSampleCodeOldCode.padStart(2, '0')} 与样本 ${targetSample} 的所有照片编号互换，是否确认？",
                        fontSize = 14.sp
                    )
                } else {
                    Text(
                        "此操作将田块 ${editingSampleCodeFieldCode} 样本 ${editingSampleCodeOldCode.padStart(2, '0')} 与田块 ${targetField} 样本 ${targetSample} 的所有照片编号互换，是否确认？",
                        fontSize = 14.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showSampleSwapConfirmDialog = false
                    val (tf, ts) = pendingNewFieldCodeSample
                    pendingNewFieldCodeSample = Pair("", "")
                    if (sameField) {
                        viewModel.swapSampleCode(
                            fieldCode = editingSampleCodeFieldCode,
                            sampleCodeA = editingSampleCodeOldCode,
                            sampleCodeB = ts
                        )
                    } else {
                        viewModel.swapFieldSampleCode(
                            oldFieldCode = editingSampleCodeFieldCode,
                            oldSampleCode = editingSampleCodeOldCode,
                            newFieldCode = tf,
                            newSampleCode = ts
                        )
                    }
                }) {
                    Text("确认对调")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSampleSwapConfirmDialog = false
                }) {
                    Text("取消")
                }
            }
        )
    }

    if (showSampleForceOverwriteConfirmDialog) {
        val (targetField, targetSample) = pendingNewFieldCodeSample
        val sameField = targetField == editingSampleCodeFieldCode.padStart(2, '0')
        AlertDialog(
            onDismissRequest = {
                showSampleForceOverwriteConfirmDialog = false
            },
            shape = RoundedCornerShape(12.dp),
            title = { Text("确认覆盖") },
            text = {
                if (sameField) {
                    Text(
                        "此操作将彻底删除样本 ${targetSample} 的所有照片且不可恢复，当前样本 ${editingSampleCodeOldCode.padStart(2, '0')} 的照片将移入，是否确认？",
                        fontSize = 14.sp
                    )
                } else {
                    Text(
                        "此操作将彻底删除田块 ${targetField} 样本 ${targetSample} 的所有照片且不可恢复，当前田块 ${editingSampleCodeFieldCode} 样本 ${editingSampleCodeOldCode.padStart(2, '0')} 的照片将移入，是否确认？",
                        fontSize = 14.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showSampleForceOverwriteConfirmDialog = false
                    val (tf, ts) = pendingNewFieldCodeSample
                    pendingNewFieldCodeSample = Pair("", "")
                    viewModel.modifyFieldAndSampleCode(
                        oldFieldCode = editingSampleCodeFieldCode,
                        oldSampleCode = editingSampleCodeOldCode,
                        newFieldCode = tf,
                        newSampleCode = ts,
                        overwriteDestination = true
                    )
                }) {
                    Text("确认覆盖", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSampleForceOverwriteConfirmDialog = false
                }) {
                    Text("取消")
                }
            }
        )
    }

    if (showCsvViewer && uiState.selectedProject != null) {
        CsvViewerDialog(
            context = context,
            region = uiState.selectedProject!!.region,
            date = uiState.selectedProject!!.date,
            onDismiss = { showCsvViewer = false }
        )
    }
}

@Composable
fun ProjectSelectorBar(
    candidateProjects: List<CaptureProject>,
    selectedProject: CaptureProject?,
    onProjectSelected: (CaptureProject) -> Unit,
    onRefresh: () -> Unit,
    onDeleteProject: () -> Unit,
    onToggleCollapse: () -> Unit
) {
    var showNoOtherTip by remember { mutableStateOf(false) }

    Spacer(modifier = Modifier.height(4.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Button(
            onClick = onToggleCollapse,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Text(
                text = selectedProject?.projectName ?: "选择项目",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp
            )
        }

        if (selectedProject != null) {
            IconButton(
                onClick = onDeleteProject,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除项目",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        if (candidateProjects.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(candidateProjects) { project ->
                    FilterChip(
                        selected = false,
                        onClick = { onProjectSelected(project) },
                        label = { Text(project.projectName, fontSize = 11.sp) },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
        }

        if (showNoOtherTip) {
            Text(
                text = "暂无其他项目可切换",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
        }
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
}

data class SampleDisplay(
    val sampleCode: String,
    val fieldCode: String = "",
    val angles: Map<String, CapturedPhoto?>,
    val bbch: String = "",
    val plantHeight: String = ""
)

data class FieldDisplay(
    val fieldCode: String,
    val samples: List<SampleDisplay>
)

fun organizePhotos(photos: List<CapturedPhoto>): List<FieldDisplay> {
    val byField = photos.groupBy { it.fieldCode }
        .mapValues { (fieldCode, fieldPhotos) ->
            val bySample = fieldPhotos.groupBy { it.sampleCode }
                .mapValues { (_, samplePhotos) ->
                    samplePhotos.associateBy { it.angleCode }
                }

            val sampleCodes = bySample.keys.mapNotNull { it.toIntOrNull() }.sorted()
                .map { it.toString().padStart(2, '0') }

            sampleCodes.map { sampleCode ->
                val angles = bySample[sampleCode] ?: emptyMap()
                // Get sample-level BBCH/plantHeight: use first non-empty value from any angle
                val allPhotos = angles.values.filterNotNull()
                val bbchVal = allPhotos.firstOrNull { it.bbch.isNotBlank() }?.bbch
                    ?: allPhotos.firstOrNull()?.bbch ?: ""
                val phVal = allPhotos.firstOrNull { it.plantHeight.isNotBlank() }?.plantHeight
                    ?: allPhotos.firstOrNull()?.plantHeight ?: ""
                SampleDisplay(
                    sampleCode = sampleCode,
                    fieldCode = fieldCode,
                    angles = angles,
                    bbch = bbchVal,
                    plantHeight = phVal
                )
            }
        }

    val fieldCodes = byField.keys.mapNotNull { it.toIntOrNull() }.sorted()
        .map { it.toString().padStart(2, '0') }

    return fieldCodes.map { fieldCode ->
        FieldDisplay(
            fieldCode = fieldCode,
            samples = byField[fieldCode] ?: emptyList()
        )
    }
}

/**
 * Dialog that displays the metadata CSV content in a scrollable table format.
 * Safely limits rows to prevent OOM. Data is sorted by field → sample → angle.
 * Includes a horizontal field-code selector (滚轴) for quick navigation.
 */
@Composable
fun CsvViewerDialog(
    context: Context,
    region: String,
    date: String,
    onDismiss: () -> Unit
) {
    val repo = remember { com.example.easycamera.data.repository.MetadataRepository(context) }
    var csvData by remember { mutableStateOf<List<List<String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isTruncated by remember { mutableStateOf(false) }
    var selectedFieldCode by remember { mutableStateOf<String?>(null) }

    // Read and sort CSV data
    LaunchedEffect(region, date) {
        withContext(Dispatchers.IO) {
            val file = repo.getMetadataFile(region, date)
            if (file.exists()) {
                try {
                    val allLines = com.example.easycamera.data.file.CsvUtils.readAllLines(file)
                    if (allLines.size > 1) {
                        val header = allLines.first()
                        val dataRows = allLines.drop(1)
                            // Sort by field_code (col 2) → sample_code (col 3) → angle_code (col 4)
                            .sortedBy { row ->
                                val fc = row.getOrElse(2) { "" }.padStart(2, '0')
                                val sc = row.getOrElse(3) { "" }.padStart(2, '0')
                                val ac = row.getOrElse(4) { "" }
                                "$fc-$sc-$ac"
                            }
                        val maxRows = 500
                        if (dataRows.size > maxRows) {
                            isTruncated = true
                            csvData = listOf(header) + dataRows.take(maxRows)
                        } else {
                            csvData = listOf(header) + dataRows
                        }
                    } else {
                        csvData = allLines
                    }
                } catch (e: Exception) {
                    csvData = emptyList()
                }
            }
            isLoading = false
        }
    }

    // Get unique field codes from data rows
    val fieldCodes = remember(csvData) {
        if (csvData.size < 2) emptyList()
        else csvData.drop(1)
            .map { it.getOrElse(2) { "" } }
            .distinct()
            .mapNotNull { it.toIntOrNull() }
            .sorted()
            .map { it.toString().padStart(2, '0') }
    }

    // Filter data by selected field code
    val filteredData = remember(csvData, selectedFieldCode) {
        if (selectedFieldCode == null || csvData.size < 2) csvData
        else {
            listOf(csvData.first()) + csvData.drop(1).filter { row ->
                row.getOrElse(2) { "" } == selectedFieldCode
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxSize(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Title row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "元数据表格 - ${region}_${date}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (selectedFieldCode != null) {
                        TextButton(
                            onClick = { selectedFieldCode = null },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("显示全部", fontSize = 12.sp)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "关闭")
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Field code scroll wheel (滚轴)
                if (fieldCodes.isNotEmpty()) {
                    Text(
                        text = "选择田块：",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(fieldCodes) { fc ->
                            val isSelected = fc == selectedFieldCode
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedFieldCode = if (isSelected) null else fc
                                },
                                label = {
                                    Text(
                                        text = "田块${fc}",
                                        fontSize = 13.sp
                                    )
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (filteredData.isEmpty() || filteredData.size < 2) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("暂无元数据", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    if (isTruncated) {
                        Text(
                            text = "显示前500行（文件行数超过500，已截断）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (selectedFieldCode != null) {
                        Text(
                            text = "当前筛选：田块${selectedFieldCode}（共 ${filteredData.size - 1} 条）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Scrollable table with frozen header
                    val headers = filteredData.firstOrNull() ?: emptyList()
                    val vScrollState = rememberScrollState()
                    val hScrollState = rememberScrollState()

                    // 根据字段名估算列宽
                    fun colMinWidth(header: String): androidx.compose.ui.unit.Dp {
                        return when {
                            header.contains("longitude") || header.contains("latitude")
                                || header.contains("经度") || header.contains("纬度") -> 110.dp
                            header.contains("filename") || header.contains("文件") -> 160.dp
                            header.contains("timestamp") || header.contains("时间") -> 130.dp
                            header.contains("region") || header.contains("date") -> 80.dp
                            header.contains("photographer") || header.contains("拍摄")
                                || header.contains("notes") || header.contains("备注")
                                || header.contains("crop") || header.contains("作物") -> 90.dp
                            else -> 64.dp
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        Column {
                            // Header row — frozen (inside horizontal scroll)
                            Row(
                                modifier = Modifier
                                    .horizontalScroll(hScrollState)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(vertical = 6.dp)
                            ) {
                                Text(
                                    text = "#",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .width(36.dp)
                                        .padding(horizontal = 6.dp)
                                )
                                headers.forEach { header ->
                                    Text(
                                        text = header.replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .width(colMinWidth(header))
                                            .padding(horizontal = 6.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            HorizontalDivider()

                            // Data rows — vertically scrollable
                            Row(modifier = Modifier.horizontalScroll(hScrollState)) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(vScrollState)
                                ) {
                                    filteredData.drop(1).forEachIndexed { index, row ->
                                        val bgColor = if (index % 2 == 0) Color.Transparent
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(bgColor)
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier
                                                    .width(36.dp)
                                                    .padding(horizontal = 6.dp)
                                            )
                                            headers.indices.forEach { colIdx ->
                                                Text(
                                                    text = row.getOrElse(colIdx) { "" },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier
                                                        .width(colMinWidth(headers[colIdx]))
                                                        .padding(horizontal = 6.dp),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoGrid(
    fields: List<FieldDisplay>,
    gridListState: androidx.compose.foundation.lazy.LazyListState,
    onDeleteSample: (String, String) -> Unit,
    onEditFieldCode: (String, String) -> Unit,
    onEditSampleCode: (String, String) -> Unit,
    onUpdateBbchPlantHeight: (CapturedPhoto, String, String) -> Unit,
    onUpdateSampleBbchPlantHeight: (String, String, String, String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var previewPhoto by remember { mutableStateOf<CapturedPhoto?>(null) }
    var editingSample by remember { mutableStateOf<SampleDisplay?>(null) }
    val editingFieldIndex = remember { mutableStateOf(-1) }

    // When editing starts, remember the field index; when it ends, scroll back to it
    LaunchedEffect(editingSample) {
        if (editingSample != null) {
            editingFieldIndex.value = fields.indexOfFirst { it.fieldCode == editingSample!!.fieldCode }
        } else if (editingFieldIndex.value >= 0 && editingFieldIndex.value < fields.size) {
            // Small delay to allow UI to settle from dialog dismiss and data refresh
            kotlinx.coroutines.delay(200)
            if (editingSample == null) { // re-check in case user opened another dialog
                val target = editingFieldIndex.value
                if (target >= 0 && target < gridListState.layoutInfo.totalItemsCount) {
                    gridListState.animateScrollToItem(target)
                }
                editingFieldIndex.value = -1
            }
        }
    }

    LazyColumn(
        state = gridListState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 60.dp)
    ) {
        fields.forEach { field ->
            item(key = "field_${field.fieldCode}") {
                FieldSection(
                    field = field,
                    onPhotoClick = { photo -> previewPhoto = photo },
                    onDeleteSample = onDeleteSample,
                    onEditFieldCode = onEditFieldCode,
                    onEditSampleCode = onEditSampleCode,
                    onEditSample = { editingSample = it }
                )
            }
        }
    }

    if (previewPhoto != null) {
        PhotoPreviewDialog(
            photo = previewPhoto!!,
            onDismiss = { previewPhoto = null },
            onUpdate = onUpdateBbchPlantHeight
        )
    }

    if (editingSample != null) {
        SampleEditDialog(
            sample = editingSample!!,
            onDismiss = { editingSample = null },
            onSave = { bbch, plantHeight ->
                val sample = editingSample!!
                val firstPhoto = sample.angles.values.filterNotNull().firstOrNull()
                if (firstPhoto != null) {
                    onUpdateSampleBbchPlantHeight(
                        firstPhoto.region, firstPhoto.date,
                        firstPhoto.fieldCode, firstPhoto.sampleCode,
                        bbch, plantHeight
                    )
                }
                editingSample = null
            }
        )
    }
}

@Composable
fun FieldSection(
    field: FieldDisplay,
    onPhotoClick: (CapturedPhoto) -> Unit,
    onDeleteSample: (String, String) -> Unit,
    onEditFieldCode: (String, String) -> Unit,
    onEditSampleCode: (String, String) -> Unit,
    onEditSample: (SampleDisplay) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "田块 ${field.fieldCode}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = {
                        field.samples.firstOrNull()?.let { sample ->
                            onEditFieldCode(field.fieldCode, sample.sampleCode)
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("编辑田块", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            field.samples.forEach { sample ->
                SampleRow(
                    sample = sample,
                    onPhotoClick = onPhotoClick,
                    onDelete = { onDeleteSample(field.fieldCode, sample.sampleCode) },
                    onEditSampleCode = { onEditSampleCode(field.fieldCode, sample.sampleCode) },
                    onEditSample = { onEditSample(sample) }
                )
                if (sample != field.samples.last()) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun SampleRow(
    sample: SampleDisplay,
    onPhotoClick: (CapturedPhoto) -> Unit,
    onDelete: () -> Unit,
    onEditSampleCode: () -> Unit,
    onEditSample: (SampleDisplay) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Text(
                text = "样本 ${sample.sampleCode}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onEditSampleCode,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                modifier = Modifier.height(22.dp)
            ) {
                Text("编辑样本", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
            }
            // BBCH display
            if (sample.bbch.isNotBlank()) {
                InfoChip("BBCH", sample.bbch)
                Spacer(modifier = Modifier.width(4.dp))
            }
            // Plant height display
            if (sample.plantHeight.isNotBlank()) {
                InfoChip("株高", "${sample.plantHeight}cm")
                Spacer(modifier = Modifier.width(4.dp))
            }
            // Edit icon
            IconButton(
                onClick = { onEditSample(sample) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "编辑BBCH/株高",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "删除样本组",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        // 使用实际角度 key 列表，按拍摄时间排序；无照片时用所有 key
        val angleKeys = remember(sample.angles) {
            sample.angles.entries
                .filter { it.value != null }
                .sortedBy { (_, photo) -> photo?.lastModified ?: 0L }
                .map { it.key }
                .ifEmpty { sample.angles.keys.toList() }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(angleKeys) { angle ->
                val photo = sample.angles[angle]
                AngleSlot(
                    angleCode = angle,
                    photo = photo,
                    slotSize = 72.dp,
                    onPhotoClick = { photo?.let(onPhotoClick) }
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            shape = RoundedCornerShape(12.dp),
            title = { Text("删除样本组") },
            text = { Text("确定要删除样本组 ${sample.sampleCode} 吗？\n\n此操作将删除该样本组所有角度的照片，且不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun AngleSlot(
    angleCode: String,
    photo: CapturedPhoto?,
    slotSize: Dp,
    onPhotoClick: () -> Unit
) {
    val shape = RoundedCornerShape(8.dp)

    if (photo != null) {
        Box(
            modifier = Modifier
                .size(slotSize)
                .clip(shape)
                .clickable(onClick = onPhotoClick)
        ) {
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(File(photo.filePath))
                    .crossfade(true)
                    .size(160)
                    .build()
            )

            Image(
                painter = painter,
                contentDescription = "角度 $angleCode",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .background(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = angleCode,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    } else {
        Box(
            modifier = Modifier
                .size(slotSize)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = angleCode,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PhotoPreviewDialog(
    photo: CapturedPhoto,
    onDismiss: () -> Unit,
    onUpdate: (CapturedPhoto, String, String) -> Unit = { _, _, _ -> }
) {
    var bbchText by remember { mutableStateOf(photo.bbch) }
    var plantHeightText by remember { mutableStateOf(photo.plantHeight) }
    var showSuccess by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(File(photo.filePath))
                        .crossfade(true)
                        .build()
                )

                Image(
                    painter = painter,
                    contentDescription = photo.filename,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = photo.filename,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    InfoChip("田块", photo.fieldCode)
                    InfoChip("样本", photo.sampleCode)
                    InfoChip("角度", photo.angleCode)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // BBCH input
                OutlinedTextField(
                    value = bbchText,
                    onValueChange = { bbchText = it },
                    label = { Text("BBCH") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Plant Height input
                OutlinedTextField(
                    value = plantHeightText,
                    onValueChange = { plantHeightText = it },
                    label = { Text("株高 (cm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (showSuccess) {
                    Text(
                        text = "保存成功",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("关闭")
                    }
                    Button(
                        onClick = {
                            onUpdate(photo, bbchText, plantHeightText)
                            showSuccess = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("保存修改")
                    }
                }
            }
        }
    }
}

@Composable
fun SampleEditDialog(
    sample: SampleDisplay,
    onDismiss: () -> Unit,
    onSave: (bbch: String, plantHeight: String) -> Unit
) {
    var bbchText by remember { mutableStateOf(sample.bbch) }
    var plantHeightText by remember { mutableStateOf(sample.plantHeight) }
    var showSuccess by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(text = "正在编辑田块${sample.fieldCode} 样本${sample.sampleCode}")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = bbchText,
                    onValueChange = { bbchText = it },
                    label = { Text("BBCH") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = plantHeightText,
                    onValueChange = { plantHeightText = it },
                    label = { Text("株高 (cm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                if (showSuccess) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "保存成功",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(bbchText, plantHeightText)
                    showSuccess = true
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("取消")
            }
        }
    )
}

@Composable
fun InfoChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}