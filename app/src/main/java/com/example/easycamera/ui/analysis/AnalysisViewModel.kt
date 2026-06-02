package com.example.easycamera.ui.analysis

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.easycamera.data.model.FieldStats
import com.example.easycamera.data.model.PhotoLocation
import com.example.easycamera.data.model.ProjectStats
import com.example.easycamera.data.model.RegionTimeSeries
import com.example.easycamera.data.repository.AnalysisRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AnalysisUiState(
    val region: String = "",
    val date: String = "",
    val photoLocations: List<PhotoLocation> = emptyList(),
    val projectStats: ProjectStats? = null,
    val regionTimeSeries: RegionTimeSeries? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class AnalysisViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AnalysisRepository(application)

    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    fun loadProject(region: String, date: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                region = region,
                date = date,
                isLoading = true,
                errorMessage = null
            )

            val locations = withContext(Dispatchers.IO) {
                repository.loadPhotoLocations(region, date)
            }

            val stats = withContext(Dispatchers.IO) {
                repository.computeProjectStats(region, date)
            }

            val timeSeries = withContext(Dispatchers.IO) {
                repository.loadRegionTimeSeries(region)
            }

            _uiState.value = _uiState.value.copy(
                photoLocations = locations,
                projectStats = stats,
                regionTimeSeries = timeSeries,
                isLoading = false
            )
        }
    }

    fun refresh() {
        loadProject(_uiState.value.region, _uiState.value.date)
    }
}