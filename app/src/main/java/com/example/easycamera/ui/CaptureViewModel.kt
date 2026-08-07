package com.example.easycamera.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easycamera.data.file.FileNameGenerator
import com.example.easycamera.data.model.CaptureMetadata
import com.example.easycamera.data.model.CaptureSessionConfig
import com.example.easycamera.data.model.CaptureState
import com.example.easycamera.data.model.LocationInfo
import com.example.easycamera.data.model.NonIdealPromptType
import com.example.easycamera.domain.CaptureCodeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.easycamera.BuildConfig

class CaptureViewModel : ViewModel() {

    private val _sessionConfig = MutableStateFlow(
        CaptureSessionConfig(date = getCurrentDateYYMMDD(), isNonIdealVersion = BuildConfig.IS_NON_IDEAL)
    )
    val sessionConfig: StateFlow<CaptureSessionConfig> = _sessionConfig.asStateFlow()

    private val _captureState = MutableStateFlow(CaptureState())
    val captureState: StateFlow<CaptureState> = _captureState.asStateFlow()

    private val _capturedMetadataList = MutableStateFlow<List<CaptureMetadata>>(emptyList())
    val capturedMetadataList: StateFlow<List<CaptureMetadata>> = _capturedMetadataList.asStateFlow()

    private val _captureMessage = MutableStateFlow<String?>(null)
    val captureMessage: StateFlow<String?> = _captureMessage.asStateFlow()

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _capturedFilePaths = MutableStateFlow<List<String>>(emptyList())
    val capturedFilePaths: StateFlow<List<String>> = _capturedFilePaths.asStateFlow()

    private val _currentLocation = MutableStateFlow<LocationInfo?>(null)
    val currentLocation: StateFlow<LocationInfo?> = _currentLocation.asStateFlow()

    private val _locationStatus = MutableStateFlow("定位中...")
    val locationStatus: StateFlow<String> = _locationStatus.asStateFlow()

    private val _locationDetermined = MutableStateFlow(false)
    val locationDetermined: StateFlow<Boolean> = _locationDetermined.asStateFlow()

    // Dialog states for manual code editing
    private val _fieldEditText = MutableStateFlow("")
    val fieldEditText: StateFlow<String> = _fieldEditText.asStateFlow()

    private val _sampleEditText = MutableStateFlow("")
    val sampleEditText: StateFlow<String> = _sampleEditText.asStateFlow()

    private val _fieldEditError = MutableStateFlow<String?>(null)
    val fieldEditError: StateFlow<String?> = _fieldEditError.asStateFlow()

    private val _sampleEditError = MutableStateFlow<String?>(null)
    val sampleEditError: StateFlow<String?> = _sampleEditError.asStateFlow()

    private val _showFieldEditDialog = MutableStateFlow(false)
    val showFieldEditDialog: StateFlow<Boolean> = _showFieldEditDialog.asStateFlow()

    private val _showSampleEditDialog = MutableStateFlow(false)
    val showSampleEditDialog: StateFlow<Boolean> = _showSampleEditDialog.asStateFlow()

    private val _codeLockMessage = MutableStateFlow<String?>(null)
    val codeLockMessage: StateFlow<String?> = _codeLockMessage.asStateFlow()

    // --- 非理想提示姊妹版状态 ---
    private val _exposureCompensation = MutableStateFlow(0)
    val exposureCompensation: StateFlow<Int> = _exposureCompensation.asStateFlow()

    private val _whiteBalanceValue = MutableStateFlow(0) // -100 冷色 ~ 0 中性 ~ +100 暖色
    val whiteBalanceValue: StateFlow<Int> = _whiteBalanceValue.asStateFlow()

    val currentPromptType: NonIdealPromptType?
        get() {
            if (!BuildConfig.IS_NON_IDEAL) return null
            val config = _sessionConfig.value
            val state = _captureState.value
            if (!config.isNonIdealVersion) return null
            val code = config.angleSequence.getOrElse(state.currentAngleIndex) { return null }
            return NonIdealPromptType.fromCode(code)
        }

    init {
        if (_sessionConfig.value.isNonIdealVersion) {
            regeneratePromptSequence()
        }
    }

    fun regeneratePromptSequence() {
        // 6种非理想原因各拍一张，不再随机
        _sessionConfig.update {
            it.copy(angleSequence = NonIdealPromptType.entries.map { p -> p.code })
        }
    }

    fun updateExposureCompensation(ev: Int) {
        _exposureCompensation.value = ev.coerceIn(-12, 12) // 宽范围，实际限制由设备EV范围决定
    }

    fun updateWhiteBalanceValue(value: Int) {
        _whiteBalanceValue.value = value.coerceIn(-100, 100)
    }

    val previewFileName: String
        get() {
            val config = _sessionConfig.value
            val state = _captureState.value
            val angle = config.angleSequence.getOrElse(state.currentAngleIndex) { "?" }
            val loc = _currentLocation.value
            return FileNameGenerator.generateFileName(
                region = config.region,
                date = config.date,
                fieldCode = CaptureCodeManager.formatCode(state.fieldCode),
                sampleCode = CaptureCodeManager.formatCode(state.sampleCode),
                angleCode = angle,
                longitude = loc?.longitude,
                latitude = loc?.latitude
            )
        }

    val currentAngleLabel: String
        get() {
            val config = _sessionConfig.value
            val state = _captureState.value
            val code = config.angleSequence.getOrElse(state.currentAngleIndex) { "?" }
            if (BuildConfig.IS_NON_IDEAL) {
                val prompt = NonIdealPromptType.fromCode(code)
                return prompt?.label ?: code
            }
            return code
        }

    val currentPromptInstruction: String
        get() {
            if (!BuildConfig.IS_NON_IDEAL) return ""
            val config = _sessionConfig.value
            val state = _captureState.value
            if (!config.isNonIdealVersion) return ""
            val code = config.angleSequence.getOrElse(state.currentAngleIndex) { return "" }
            val prompt = NonIdealPromptType.fromCode(code) ?: return ""
            return when (prompt) {
                NonIdealPromptType.MOTION_BLUR -> "请在按下快门时轻微晃动手机，模拟运动模糊"
                NonIdealPromptType.TOO_FAR -> "请将手机远离植株，让主体偏小"
                NonIdealPromptType.TOO_CLOSE -> "请将手机凑近植株，让主体超出画面或过近"
                NonIdealPromptType.BAD_ANGLE -> "请从侧面或非正对角度拍摄"
                NonIdealPromptType.OCCLUSION -> "用杂物遮挡镜头或在视野中保留无关杂物，使主体不在中间"
                NonIdealPromptType.BAD_EXPOSURE -> "请调节下方曝光和白平衡参数后拍摄，让画面曝光异常"
            }
        }

    val progressLabel: String
        get() {
            val state = _captureState.value
            val config = _sessionConfig.value
            val total = config.angleSequence.size
            val done = state.capturedAngles.size
            return if (state.isGroupComplete) "${total}/${total}" else "${done}/${total}"
        }

    val currentAngleIsCaptured: Boolean
        get() {
            val config = _sessionConfig.value
            val state = _captureState.value
            val currentAngle = config.angleSequence.getOrElse(state.currentAngleIndex) { return false }
            return currentAngle in state.capturedAngles
        }

    fun clearCaptureMessage() {
        _captureMessage.value = null
        _codeLockMessage.value = null
    }

    fun updateLocation(location: LocationInfo?, errorStatus: String? = null) {
        _currentLocation.value = location
        _locationDetermined.value = true
        _locationStatus.value = when {
            location != null -> "定位：${String.format("%.6f", location.longitude)}, ${String.format("%.6f", location.latitude)}"
            errorStatus != null -> errorStatus
            else -> "定位不可用，将使用 NA_NA"
        }
    }

    fun setLocationStatus(status: String) {
        _locationStatus.value = status
    }

    private val hasPhotos: Boolean
        get() = _captureState.value.capturedAngles.isNotEmpty()

    // Session config updates
    fun updateRegion(region: String) {
        if (hasPhotos) {
            _codeLockMessage.value = "当前样本组已有照片。为了避免编号和文件不一致，请先点击「重拍本组」清空当前样本组后再修改采样信息。"
            return
        }
        _sessionConfig.update { it.copy(region = region) }
        resetCodesAndState()
    }

    fun updateDate(date: String) {
        if (hasPhotos) {
            _codeLockMessage.value = "当前样本组已有照片。为了避免编号和文件不一致，请先点击「重拍本组」清空当前样本组后再修改采样信息。"
            return
        }
        _sessionConfig.update { it.copy(date = date) }
        resetCodesAndState()
    }

    fun updateOperator(operator: String) {
        if (hasPhotos) {
            _codeLockMessage.value = "当前样本组已有照片。为了避免编号和文件不一致，请先点击「重拍本组」清空当前样本组后再修改采样信息。"
            return
        }
        _sessionConfig.update { it.copy(operator = operator) }
        resetCodesAndState()
    }

    fun updateBbch(bbch: String) {
        _sessionConfig.update { it.copy(bbch = bbch) }
    }

    fun updatePlantHeight(height: String) {
        _sessionConfig.update { it.copy(plantHeight = height) }
    }

    private fun resetCodesAndState() {
        _captureState.value = CaptureState()
        _capturedMetadataList.value = emptyList()
        _capturedFilePaths.value = emptyList()
        _captureMessage.value = null
    }

    // Field code +/- operations
    fun incrementFieldCode() {
        if (hasPhotos) {
            _codeLockMessage.value = "当前样本组已有照片。为了避免编号和文件不一致，请先点击「重拍本组」清空当前样本组后再修改编号。"
            return
        }
        val current = _captureState.value.fieldCode
        if (current < 99) {
            _captureState.update { CaptureCodeManager.onFieldCodeChanged(it, current + 1) }
            _capturedMetadataList.value = emptyList()
            _capturedFilePaths.value = emptyList()
            _captureMessage.value = null
        }
    }

    fun decrementFieldCode() {
        if (hasPhotos) {
            _codeLockMessage.value = "当前样本组已有照片。为了避免编号和文件不一致，请先点击「重拍本组」清空当前样本组后再修改编号。"
            return
        }
        val current = _captureState.value.fieldCode
        if (current > 1) {
            _captureState.update { CaptureCodeManager.onFieldCodeChanged(it, current - 1) }
            _capturedMetadataList.value = emptyList()
            _capturedFilePaths.value = emptyList()
            _captureMessage.value = null
        }
    }

    // Sample code +/- operations
    fun incrementSampleCode() {
        if (hasPhotos) {
            _codeLockMessage.value = "当前样本组已有照片。为了避免编号和文件不一致，请先点击「重拍本组」清空当前样本组后再修改编号。"
            return
        }
        val current = _captureState.value.sampleCode
        if (current < 99) {
            _captureState.update { CaptureCodeManager.onSampleCodeChanged(it, current + 1) }
            _capturedMetadataList.value = emptyList()
            _capturedFilePaths.value = emptyList()
            _captureMessage.value = null
        }
    }

    fun decrementSampleCode() {
        if (hasPhotos) {
            _codeLockMessage.value = "当前样本组已有照片。为了避免编号和文件不一致，请先点击「重拍本组」清空当前样本组后再修改编号。"
            return
        }
        val current = _captureState.value.sampleCode
        if (current > 1) {
            _captureState.update { CaptureCodeManager.onSampleCodeChanged(it, current - 1) }
            _capturedMetadataList.value = emptyList()
            _capturedFilePaths.value = emptyList()
            _captureMessage.value = null
        }
    }

    // Angle selection
    fun selectAngleIndex(index: Int) {
        val maxIdx = _sessionConfig.value.angleSequence.size - 1
        _captureState.update { CaptureCodeManager.onAngleIndexChanged(it, index, maxIdx) }
        _captureMessage.value = null
    }

    // Field edit dialog
    fun openFieldEditDialog() {
        if (hasPhotos) {
            _codeLockMessage.value = "当前样本组已有照片。为了避免编号和文件不一致，请先点击「重拍本组」清空当前样本组后再修改编号。"
            return
        }
        _fieldEditText.value = _captureState.value.fieldCode.toString()
        _fieldEditError.value = null
        _showFieldEditDialog.value = true
    }

    fun closeFieldEditDialog() {
        _showFieldEditDialog.value = false
        _fieldEditError.value = null
    }

    fun updateFieldEditText(text: String) {
        val filtered = text.filter { it.isDigit() }
        _fieldEditText.value = filtered
        _fieldEditError.value = null
    }

    fun confirmFieldEdit() {
        if (hasPhotos) {
            _codeLockMessage.value = "当前样本组已有照片。为了避免编号和文件不一致，请先点击「重拍本组」清空当前样本组后再修改编号。"
            _showFieldEditDialog.value = false
            return
        }
        val text = _fieldEditText.value
        if (text.isEmpty()) {
            _fieldEditError.value = "编号不能为空"
            return
        }
        val intVal = text.toIntOrNull()
        if (intVal == null || intVal < 1) {
            _fieldEditError.value = "编号不能小于 1"
            return
        }
        if (intVal > 99) {
            _fieldEditError.value = "编号不能大于 99"
            return
        }
        if (intVal != _captureState.value.fieldCode) {
            _captureState.update { CaptureCodeManager.onFieldCodeChanged(it, intVal) }
            _capturedMetadataList.value = emptyList()
            _capturedFilePaths.value = emptyList()
            _captureMessage.value = null
        }
        _showFieldEditDialog.value = false
        _fieldEditError.value = null
    }

    // Sample edit dialog
    fun openSampleEditDialog() {
        if (hasPhotos) {
            _codeLockMessage.value = "当前样本组已有照片。为了避免编号和文件不一致，请先点击「重拍本组」清空当前样本组后再修改编号。"
            return
        }
        _sampleEditText.value = _captureState.value.sampleCode.toString()
        _sampleEditError.value = null
        _showSampleEditDialog.value = true
    }

    fun closeSampleEditDialog() {
        _showSampleEditDialog.value = false
        _sampleEditError.value = null
    }

    fun updateSampleEditText(text: String) {
        val filtered = text.filter { it.isDigit() }
        _sampleEditText.value = filtered
        _sampleEditError.value = null
    }

    fun confirmSampleEdit() {
        if (hasPhotos) {
            _codeLockMessage.value = "当前样本组已有照片。为了避免编号和文件不一致，请先点击「重拍本组」清空当前样本组后再修改编号。"
            _showSampleEditDialog.value = false
            return
        }
        val text = _sampleEditText.value
        if (text.isEmpty()) {
            _sampleEditError.value = "编号不能为空"
            return
        }
        val intVal = text.toIntOrNull()
        if (intVal == null || intVal < 1) {
            _sampleEditError.value = "编号不能小于 1"
            return
        }
        if (intVal > 99) {
            _sampleEditError.value = "编号不能大于 99"
            return
        }
        if (intVal != _captureState.value.sampleCode) {
            _captureState.update { CaptureCodeManager.onSampleCodeChanged(it, intVal) }
            _capturedMetadataList.value = emptyList()
            _capturedFilePaths.value = emptyList()
            _captureMessage.value = null
        }
        _showSampleEditDialog.value = false
        _sampleEditError.value = null
    }

    // Real photo capture flow

    /** Called by UI after CameraX takePicture succeeds.
     *  Updates capture state and records metadata + file path.
     *  @param onAfterCapture optional suspend callback invoked in IO dispatcher for side effects like CSV writing. */
    fun onPhotoCaptured(filePath: String, onAfterCapture: (suspend (CaptureMetadata) -> Unit)? = null) {
        val config = _sessionConfig.value
        val state = _captureState.value

        val newState = CaptureCodeManager.simulateCapture(state, config.angleSequence)
        if (newState == null) {
            // Should not happen if UI checked before capture, but guard anyway
            _captureMessage.value = "该角度已拍摄，如需重拍请点击重拍本组"
            _isCapturing.value = false
            return
        }

        val currentAngle = config.angleSequence.getOrElse(state.currentAngleIndex) { "?" }
        val timestamp = getCurrentTimestamp()
        val loc = _currentLocation.value
        val lonStr = if (loc != null) String.format("%.6f", loc.longitude) else "NA"
        val latStr = if (loc != null) String.format("%.6f", loc.latitude) else "NA"

        val metadata = CaptureMetadata(
            region = config.region,
            date = config.date,
            fieldCode = CaptureCodeManager.formatCode(state.fieldCode),
            sampleCode = CaptureCodeManager.formatCode(state.sampleCode),
            angleCode = currentAngle,
            longitude = lonStr,
            latitude = latStr,
            operator = config.operator,
            captureTime = timestamp,
            filename = filePath.substringAfterLast(File.separatorChar),
            relativePath = "EasyCamera/${config.region}_${config.date}/images/${filePath.substringAfterLast(File.separatorChar)}",
            filePath = filePath,
            retakeGroupId = "",
            bbch = config.bbch,
            plantHeight = config.plantHeight,
            extraFields = config.extraFields
        )

        _capturedMetadataList.update { list -> list + metadata }
        _capturedFilePaths.update { list -> list + filePath }
        _captureState.value = newState
        _captureMessage.value = null
        _isCapturing.value = false

        if (onAfterCapture != null) {
            viewModelScope.launch(Dispatchers.IO) {
                onAfterCapture(metadata)
            }
        }
    }

    /** Called by UI when CameraX capture fails. */
    fun onPhotoCaptureError(message: String) {
        _captureMessage.value = message
        _isCapturing.value = false
    }

    fun setCaptureMessage(message: String?) {
        _captureMessage.value = message
    }

    /** Check if current angle can be captured. Sets message if not. */
    fun tryStartCapture(): Boolean {
        if (_isCapturing.value) return false

        val state = _captureState.value
        val config = _sessionConfig.value
        val currentAngle = config.angleSequence.getOrElse(state.currentAngleIndex) { return false }

        if (currentAngle in state.capturedAngles) {
            _captureMessage.value = "该角度已拍摄，如需重拍请点击重拍本组"
            return false
        }

        if (state.isGroupComplete) {
            _captureMessage.value = "本组已完成拍摄，请确认后进入下一样本"
            return false
        }

        _isCapturing.value = true
        _captureMessage.value = null
        return true
    }

    /** Force-removes the current angle from capturedAngles so tryStartCapture will allow capture. */
    fun forceAllowCaptureForCurrentAngle() {
        val state = _captureState.value
        val config = _sessionConfig.value
        val currentAngle = config.angleSequence.getOrElse(state.currentAngleIndex) { return }
        val curFieldCode = CaptureCodeManager.formatCode(state.fieldCode)
        val curSampleCode = CaptureCodeManager.formatCode(state.sampleCode)

        if (currentAngle in state.capturedAngles) {
            val newCaptured = state.capturedAngles - currentAngle
            _captureState.update {
                it.copy(capturedAngles = newCaptured, isGroupComplete = false)
            }

            val metadataList = _capturedMetadataList.value
            val removeIndex = metadataList.indexOfFirst { meta ->
                meta.fieldCode == curFieldCode &&
                        meta.sampleCode == curSampleCode &&
                        meta.angleCode == currentAngle
            }
            if (removeIndex >= 0) {
                _capturedMetadataList.update { list ->
                    list.filterIndexed { idx, _ -> idx != removeIndex }
                }
                _capturedFilePaths.update { paths ->
                    paths.filterIndexed { idx, _ -> idx != removeIndex }
                }
            }
        }
        _captureMessage.value = null
    }

    // Group actions
    fun confirmGroup() {
        _captureState.update { CaptureCodeManager.confirmGroup(it) }
        _capturedMetadataList.value = emptyList()
        _capturedFilePaths.value = emptyList()
        _captureMessage.value = null
    }

    fun retakeGroup() {
        _captureState.update { CaptureCodeManager.retakeGroup(it) }
        _capturedMetadataList.value = emptyList()
        _capturedFilePaths.value = emptyList()
        _captureMessage.value = null
        _codeLockMessage.value = null
        if (BuildConfig.IS_NON_IDEAL) {
            regeneratePromptSequence()
            _exposureCompensation.value = 0
            _whiteBalanceValue.value = 0
        }
    }

    data class UndoInfo(
        val filePath: String,
        val metadata: CaptureMetadata
    )

    /** Removes the last captured photo from state and returns info needed to clean up disk/DB. */
    fun undoLastCapture(): UndoInfo? {
        val state = _captureState.value
        val config = _sessionConfig.value
        val metadataList = _capturedMetadataList.value
        val filePaths = _capturedFilePaths.value

        if (metadataList.isEmpty() || filePaths.isEmpty()) return null

        val lastMetadata = metadataList.last()
        val lastFilePath = filePaths.last()

        _capturedMetadataList.update { it.dropLast(1) }
        _capturedFilePaths.update { it.dropLast(1) }

        val newCapturedAngles = state.capturedAngles - lastMetadata.angleCode
        val angleIndex = config.angleSequence.indexOf(lastMetadata.angleCode).coerceAtLeast(0)
        _captureState.value = state.copy(
            capturedAngles = newCapturedAngles,
            currentAngleIndex = angleIndex,
            isGroupComplete = false
        )
        _captureMessage.value = null
        _isCapturing.value = false
        _codeLockMessage.value = null

        return UndoInfo(filePath = lastFilePath, metadata = lastMetadata)
    }

    data class CaptureAngleInfo(
        val angleCode: String,
        val filename: String
    )

    private fun getCurrentDateYYMMDD(): String {
        val sdf = SimpleDateFormat("yyMMdd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}