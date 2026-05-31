package com.example.easycamera.ui.gallery

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.easycamera.data.export.ExportResult
import com.example.easycamera.data.export.ProjectExportManager
import com.example.easycamera.data.model.CaptureProject
import com.example.easycamera.data.model.CapturedPhoto
import com.example.easycamera.data.repository.MetadataRepository
import com.example.easycamera.data.repository.PhotoGalleryRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GalleryUiState(
    val projects: List<CaptureProject> = emptyList(),
    val selectedProject: CaptureProject? = null,
    val candidateProjects: List<CaptureProject> = emptyList(),
    val photos: List<CapturedPhoto> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed class ExportState {
    object Idle : ExportState()
    object Exporting : ExportState()
    data class Ready(val tempPath: String, val suggestedName: String, val warning: String? = null) : ExportState()
    data class Success(val message: String) : ExportState()
    data class Error(val message: String) : ExportState()
}

class PhotoGalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PhotoGalleryRepository(application)
    private val metadataRepository = MetadataRepository(application)

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private val _deleteMessage = MutableStateFlow<String?>(null)
    val deleteMessage: StateFlow<String?> = _deleteMessage.asStateFlow()

    private var currentProjectName: String? = null

    fun setCurrentProjectName(name: String) {
        currentProjectName = name
        // Try to select the matching project from already loaded list
        val match = _uiState.value.projects.find { it.projectName == name }
        if (match != null) {
            _uiState.value = _uiState.value.copy(selectedProject = match)
            updateCandidateProjects()
            loadPhotosForProject(match)
        } else {
            updateCandidateProjects()
        }
    }

    fun loadProjects() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val allProjects = withContext(Dispatchers.IO) {
                repository.scanProjects()
            }
            _uiState.value = _uiState.value.copy(
                projects = allProjects,
                isLoading = false
            )

            // Always try to select the project matching current capture project on entry
            val target = if (currentProjectName != null) {
                allProjects.find { it.projectName == currentProjectName }
                    ?: _uiState.value.selectedProject
                        ?: allProjects.firstOrNull()
            } else {
                _uiState.value.selectedProject
                    ?: allProjects.firstOrNull()
            }

            val changed = target?.let { it != _uiState.value.selectedProject } ?: false
            if (target != null && changed) {
                _uiState.value = _uiState.value.copy(selectedProject = target)
                loadPhotosForProject(target)
            } else if (target != null && !changed) {
                // Refresh photos for the same selected project
                loadPhotosForProject(target)
            }
            updateCandidateProjects()
        }
    }

    private fun updateCandidateProjects() {
        val all = _uiState.value.projects
        val selected = _uiState.value.selectedProject
        val candidates = if (selected != null) {
            all.filter { it.projectName != selected.projectName }
        } else {
            all
        }
        _uiState.value = _uiState.value.copy(candidateProjects = candidates)
    }

    fun selectProject(project: CaptureProject) {
        _uiState.value = _uiState.value.copy(selectedProject = project, photos = emptyList())
        updateCandidateProjects()
        loadPhotosForProject(project)
    }

    private fun loadPhotosForProject(project: CaptureProject) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val photos = withContext(Dispatchers.IO) {
                repository.loadPhotos(project)
            }
            _uiState.value = _uiState.value.copy(
                photos = photos,
                isLoading = false
            )
        }
    }

    fun refreshCurrentProject() {
        val project = _uiState.value.selectedProject ?: return
        loadPhotosForProject(project)
    }

    fun startExport() {
        val project = _uiState.value.selectedProject
        if (project == null) {
            _exportState.value = ExportState.Error("请先选择一个项目")
            return
        }
        _exportState.value = ExportState.Exporting
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                ProjectExportManager.prepareExport(
                    getApplication(),
                    project.region,
                    project.date
                )
            }
            when (result) {
                is ExportResult.Ready -> {
                    _exportState.value = ExportState.Ready(
                        tempPath = result.tempFile.absolutePath,
                        suggestedName = result.suggestedName
                    )
                }
                is ExportResult.Warning -> {
                    _exportState.value = ExportState.Ready(
                        tempPath = result.tempFile.absolutePath,
                        suggestedName = result.suggestedName,
                        warning = result.message
                    )
                }
                is ExportResult.Error -> {
                    _exportState.value = ExportState.Error(result.message)
                }
                else -> {}
            }
        }
    }

    fun completeExport(uri: Uri) {
        viewModelScope.launch {
            val state = _exportState.value
            if (state !is ExportState.Ready) return@launch
            val ready = state as ExportState.Ready

            val tempFile = File(ready.tempPath)
            val warning = ready.warning
            val app = getApplication<Application>()

            try {
                withContext(Dispatchers.IO) {
                    app.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        tempFile.inputStream().use { input ->
                            input.copyTo(outputStream)
                        }
                    } ?: throw Exception("无法打开目标文件")
                    tempFile.delete()
                }
                val msg = if (warning != null) {
                    "导出成功。$warning"
                } else {
                    "导出成功"
                }
                _exportState.value = ExportState.Success(msg)
            } catch (e: Exception) {
                tempFile.delete()
                _exportState.value = ExportState.Error("导出失败：${e.message ?: "请重试"}")
            }
        }
    }

    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }

    fun deleteSampleGroup(fieldCode: String, sampleCode: String) {
        viewModelScope.launch {
            val project = _uiState.value.selectedProject ?: return@launch
            val result = withContext(Dispatchers.IO) {
                val filesDeleted = repository.deleteSampleGroup(project, fieldCode, sampleCode)
                if (filesDeleted) {
                    metadataRepository.deleteSampleGroup(
                        region = project.region,
                        date = project.date,
                        fieldCode = fieldCode,
                        sampleCode = sampleCode
                    )
                }
                filesDeleted
            }
            if (result) {
                _deleteMessage.value = "已删除样本组 ${fieldCode}_${sampleCode}"
                loadPhotosForProject(project)
            } else {
                _deleteMessage.value = "删除失败，请重试"
            }
        }
    }

    fun deleteProject() {
        viewModelScope.launch {
            val project = _uiState.value.selectedProject ?: return@launch
            val result = withContext(Dispatchers.IO) {
                repository.deleteProject(project)
            }
            if (result) {
                _deleteMessage.value = "已删除项目 ${project.projectName}"
                loadProjects()
            } else {
                _deleteMessage.value = "删除项目失败，请重试"
            }
        }
    }

    fun clearDeleteMessage() {
        _deleteMessage.value = null
    }
}