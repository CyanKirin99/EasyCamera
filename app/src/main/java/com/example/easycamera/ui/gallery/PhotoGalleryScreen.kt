package com.example.easycamera.ui.gallery

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoGalleryScreen(
    viewModel: PhotoGalleryViewModel,
    onNavigateBack: () -> Unit
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

    var showDeleteProjectConfirm by remember { mutableStateOf(false) }

    var showFieldEditDialog by remember { mutableStateOf(false) }
    var editingFieldCode by remember { mutableStateOf("") }
    var editingSampleCode by remember { mutableStateOf("") }
    var fieldEditNewValue by remember { mutableStateOf("") }
    var fieldEditError by remember { mutableStateOf<String?>(null) }
    var showOverwriteFieldConfirm by remember { mutableStateOf(false) }
    var pendingNewFieldCode by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("照片集") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", fontSize = 20.sp)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshCurrentProject() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                    IconButton(onClick = { showDeleteProjectConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除项目")
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
                .padding(horizontal = 12.dp)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.projects.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无照片。请先完成拍摄。",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                ProjectSelectorBar(
                    candidateProjects = uiState.candidateProjects,
                    selectedProject = uiState.selectedProject,
                    onProjectSelected = { viewModel.selectProject(it) },
                    onRefresh = { viewModel.refreshCurrentProject() }
                )

                Spacer(modifier = Modifier.height(8.dp))

                val selectedProject = uiState.selectedProject
                if (selectedProject != null) {
                    Text(
                        text = "当前项目：${selectedProject.projectName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "照片数量：${selectedProject.photoCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

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

                            PhotoGrid(
                                fields = organizedData,
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
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { viewModel.startExport() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        enabled = exportState !is ExportState.Exporting
                    ) {
                        if (exportState is ExportState.Exporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (exportState is ExportState.Exporting) "正在导出..." else "导出当前项目",
                            fontSize = 16.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
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
                        text = "当前样本组：${editingFieldCode}_${editingSampleCode}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = fieldEditNewValue,
                        onValueChange = {
                            if (it.length <= 2) {
                                fieldEditNewValue = it
                                fieldEditError = null
                            }
                        },
                        label = { Text("新田块编号 (1-99)") },
                        singleLine = true,
                        isError = fieldEditError != null,
                        supportingText = if (fieldEditError != null) {
                            { Text(fieldEditError!!, color = MaterialTheme.colorScheme.error) }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
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
                    "目标田块 ${pendingNewFieldCode} 的样本 ${editingSampleCode} 已存在照片。\n\n" +
                            "请选择操作方式：\n" +
                            "• 覆盖：删除目标位置现有照片，将当前照片移入\n" +
                            "• 对调：将当前照片与目标位置照片的田块编号互换"
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
                            val newCode = pendingNewFieldCode
                            pendingNewFieldCode = ""
                            viewModel.swapFieldCode(
                                oldFieldCode = editingFieldCode,
                                sampleCode = editingSampleCode,
                                newFieldCode = newCode
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("对调")
                    }
                    Button(
                        onClick = {
                            showOverwriteFieldConfirm = false
                            val newCode = pendingNewFieldCode
                            pendingNewFieldCode = ""
                            viewModel.modifyFieldCode(
                                oldFieldCode = editingFieldCode,
                                sampleCode = editingSampleCode,
                                newFieldCode = newCode,
                                overwriteDestination = true
                            )
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectSelectorBar(
    candidateProjects: List<CaptureProject>,
    selectedProject: CaptureProject?,
    onProjectSelected: (CaptureProject) -> Unit,
    onRefresh: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showNoOtherTip by remember { mutableStateOf(false) }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = {
                if (candidateProjects.isEmpty()) {
                    showNoOtherTip = true
                } else {
                    expanded = true
                }
            },
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = selectedProject?.projectName ?: "选择项目",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                candidateProjects.forEach { project ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "${project.projectName}（${project.photoCount}张）"
                            )
                        },
                        onClick = {
                            onProjectSelected(project)
                            expanded = false
                        }
                    )
                }
            }
        }

        if (candidateProjects.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                candidateProjects.forEach { project ->
                    FilterChip(
                        selected = false,
                        onClick = { onProjectSelected(project) },
                        label = { Text(project.projectName, fontSize = 11.sp) }
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
    val angles: Map<String, CapturedPhoto?>
)

data class FieldDisplay(
    val fieldCode: String,
    val samples: List<SampleDisplay>
)

private val ANGLE_ORDER = listOf("A", "B", "C", "D")

fun organizePhotos(photos: List<CapturedPhoto>): List<FieldDisplay> {
    val byField = photos.groupBy { it.fieldCode }
        .mapValues { (_, fieldPhotos) ->
            val bySample = fieldPhotos.groupBy { it.sampleCode }
                .mapValues { (_, samplePhotos) ->
                    samplePhotos.associateBy { it.angleCode }
                }

            val sampleCodes = bySample.keys.mapNotNull { it.toIntOrNull() }.sorted()
                .map { it.toString().padStart(2, '0') }

            sampleCodes.map { sampleCode ->
                val angles = bySample[sampleCode] ?: emptyMap()
                SampleDisplay(
                    sampleCode = sampleCode,
                    angles = ANGLE_ORDER.associateWith { angle ->
                        angles[angle]
                    }
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

@Composable
fun PhotoGrid(
    fields: List<FieldDisplay>,
    onDeleteSample: (String, String) -> Unit,
    onEditFieldCode: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var previewPhoto by remember { mutableStateOf<CapturedPhoto?>(null) }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        fields.forEach { field ->
            item(key = "field_${field.fieldCode}") {
                FieldSection(
                    field = field,
                    onPhotoClick = { photo -> previewPhoto = photo },
                    onDeleteSample = onDeleteSample,
                    onEditFieldCode = onEditFieldCode
                )
            }
        }
    }

    if (previewPhoto != null) {
        PhotoPreviewDialog(
            photo = previewPhoto!!,
            onDismiss = { previewPhoto = null }
        )
    }
}

@Composable
fun FieldSection(
    field: FieldDisplay,
    onPhotoClick: (CapturedPhoto) -> Unit,
    onDeleteSample: (String, String) -> Unit,
    onEditFieldCode: (String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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

            Spacer(modifier = Modifier.height(8.dp))

            field.samples.forEach { sample ->
                SampleRow(
                    sample = sample,
                    onPhotoClick = onPhotoClick,
                    onDelete = { onDeleteSample(field.fieldCode, sample.sampleCode) }
                )
                if (sample != field.samples.last()) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
fun SampleRow(
    sample: SampleDisplay,
    onPhotoClick: (CapturedPhoto) -> Unit,
    onDelete: () -> Unit
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

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(listOf("A", "B", "C", "D")) { angle ->
                val photo = sample.angles[angle]
                AngleSlot(
                    angleCode = angle,
                    photo = photo,
                    slotSize = 80.dp,
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
    onDismiss: () -> Unit
) {
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
                modifier = Modifier.padding(16.dp),
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

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("关闭")
                }
            }
        }
    }
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