package com.warungerik.devidchanger

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warungerik.devidchanger.engine.DevIdChanger
import com.warungerik.devidchanger.device.DeviceIdRepository
import com.warungerik.devidchanger.model.AppLanguage
import com.warungerik.devidchanger.model.AppTab
import com.warungerik.devidchanger.model.GameVersion
import com.warungerik.devidchanger.model.MlbbCheckResult
import com.warungerik.devidchanger.model.SavedDevId
import com.warungerik.devidchanger.network.MlbbApiClient
import com.warungerik.devidchanger.network.MlbbApiException
import com.warungerik.devidchanger.root.RootDetector
import com.warungerik.devidchanger.root.RootInfo
import com.warungerik.devidchanger.storage.SavedIdStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LogType { INFO, SUCCESS, WARNING, ERROR }

data class LogEntry(
    val message: String,
    val type: LogType = LogType.INFO,
    val timestamp: Long = System.currentTimeMillis()
)

data class AppState(
    val rootInfo: RootInfo = RootInfo(),
    val selectedVersion: GameVersion = GameVersion.GLOBAL,
    val currentJsonId: String? = null,
    val newJsonId: String = "",
    val isProcessing: Boolean = false,
    val currentProcessStep: String? = null,
    val showSuccessDialog: Boolean = false,
    val isGameFolderExists: Boolean = false,
    val currentTab: AppTab = AppTab.HOME,
    val isDarkMode: Boolean = true,
    val currentLanguage: AppLanguage = AppLanguage.INDONESIAN,
    val showWelcomeDialog: Boolean = true,
    val logs: List<LogEntry> = emptyList(),
    val savedDevIds: List<SavedDevId> = emptyList(),
    val mlbbUserId: String = "",
    val mlbbServerId: String = "",
    val isMlbbChecking: Boolean = false,
    val mlbbResult: MlbbCheckResult? = null,
    val mlbbError: String? = null,
    val deviceIdFileName: String? = null,
    val deviceIdCount: Int = 0,
    val deviceIdPosition: Int = 0
)

class MainViewModel : ViewModel() {

    private val deviceIdRepository = DeviceIdRepository()

    private val _uiState = MutableStateFlow(AppState())
    val uiState: StateFlow<AppState> = _uiState.asStateFlow()

    private val strings: com.warungerik.devidchanger.ui.theme.AppStrings
        get() = com.warungerik.devidchanger.ui.theme.AppStrings(_uiState.value.currentLanguage)

    init {
        checkRootAndFetch()
    }

    fun dismissWelcomeDialog() {
        _uiState.update { it.copy(showWelcomeDialog = false) }
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun setDarkMode(isDark: Boolean) {
        _uiState.update { it.copy(isDarkMode = isDark) }
    }

    fun selectLanguage(language: AppLanguage) {
        _uiState.update { it.copy(currentLanguage = language) }
    }

    fun checkRootAndFetch() {
        viewModelScope.launch(Dispatchers.IO) {
            val root = RootDetector.detect()
            _uiState.update { it.copy(rootInfo = root) }

            if (root.isRooted) {
                addLog(strings.logRootVerified(root.rootType.displayName, root.rootVersion ?: ""), LogType.SUCCESS)
                refreshCurrentId()
            } else {
                addLog(strings.logRootNotFound, LogType.ERROR)
            }
        }
    }

    fun selectVersion(version: GameVersion) {
        _uiState.update { it.copy(selectedVersion = version) }
        refreshCurrentId()
    }

    fun updateNewJsonId(input: String) {
        _uiState.update { it.copy(newJsonId = input) }
    }

    fun loadDeviceIdFile(context: Context, uri: android.net.Uri) {
        viewModelScope.launch {
            val result = deviceIdRepository.loadFromUri(context, uri)

            result.onSuccess { count ->
                val currentId = deviceIdRepository.current()

                _uiState.update {
                    it.copy(
                        newJsonId = currentId ?: "",
                        deviceIdCount = count,
                        deviceIdPosition = deviceIdRepository.position()
                    )
                }
            }

            result.onFailure { error ->
                addLog(
                    "Gagal memuat Device ID: ${error.message ?: "Unknown error"}",
                    LogType.ERROR
                )
            }
        }
    }

    fun nextDeviceId() {
        val id = deviceIdRepository.next() ?: return

        _uiState.update {
            it.copy(
                newJsonId = id,
                deviceIdPosition = deviceIdRepository.position()
            )
        }
    }

    fun previousDeviceId() {
        val id = deviceIdRepository.previous() ?: return

        _uiState.update {
            it.copy(
                newJsonId = id,
                deviceIdPosition = deviceIdRepository.position()
            )
        }
    }

    fun updateMlbbUserId(input: String) {
        _uiState.update { it.copy(mlbbUserId = input, mlbbError = null) }
    }

    fun updateMlbbServerId(input: String) {
        _uiState.update { it.copy(mlbbServerId = input, mlbbError = null) }
    }

    fun clearMlbbResult() {
        _uiState.update { it.copy(mlbbResult = null, mlbbError = null) }
    }

    fun checkMlbbAccount() {
        val userId = _uiState.value.mlbbUserId.trim()
        val serverId = _uiState.value.mlbbServerId.trim()
        val currentStr = strings

        if (userId.isEmpty() || serverId.isEmpty()) {
            _uiState.update {
                it.copy(mlbbError = currentStr.mlbbErrorInputEmpty)
            }
            return
        }

        _uiState.update {
            it.copy(isMlbbChecking = true, mlbbResult = null, mlbbError = null)
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = MlbbApiClient.checkAccount(userId, serverId)
                _uiState.update {
                    it.copy(
                        isMlbbChecking = false,
                        mlbbResult = MlbbCheckResult(
                            userId = userId,
                            serverId = serverId,
                            username = user.username,
                            country = user.country
                        )
                    )
                }
            } catch (e: MlbbApiException) {
                _uiState.update {
                    it.copy(
                        isMlbbChecking = false,
                        mlbbResult = null,
                        mlbbError = e.message ?: currentStr.mlbbErrorGeneric
                    )
                }
            } catch (e: Exception) {
                android.util.Log.w("MainViewModel", "Unexpected MLBB check error", e)
                _uiState.update {
                    it.copy(
                        isMlbbChecking = false,
                        mlbbResult = null,
                        mlbbError = currentStr.mlbbErrorGeneric
                    )
                }
            }
        }
    }

    fun generateNewGuestId() {
        val isUS = _uiState.value.selectedVersion.isUS
        val charPool = "0123456789abcdef"
        val length = if (isUS) 84 else 40
        val randomId = (1..length)
            .map { charPool.random() }
            .joinToString("")
        _uiState.update { it.copy(newJsonId = randomId) }
        addLog(strings.logRandomGuestCreated(randomId), LogType.INFO)
    }

    fun refreshCurrentId() {
        viewModelScope.launch(Dispatchers.IO) {
            val version = _uiState.value.selectedVersion
            val exists = DevIdChanger.isGameFolderExists(version)
            val currentId = if (exists) DevIdChanger.getCurrentJsonId(version) else null

            _uiState.update {
                it.copy(
                    isGameFolderExists = exists,
                    currentJsonId = currentId
                )
            }

            if (!exists) {
                addLog(strings.logFolderNotFound(version.displayName, version.packageName), LogType.WARNING)
            }
        }
    }

    fun executeInjection() {
        val currentState = _uiState.value
        val newId = currentState.newJsonId.trim()
        val version = currentState.selectedVersion
        val currentStr = strings

        if (!currentState.rootInfo.isRooted) {
            addLog(currentStr.logRootRequired, LogType.ERROR)
            return
        }

        if (newId.isEmpty()) {
            addLog(currentStr.logInputEmpty, LogType.ERROR)
            return
        }

        val sanitizedId = newId.filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' || it == '-' }
        if (sanitizedId != newId) {
            addLog(currentStr.logInputInvalid, LogType.ERROR)
            return
        }

        _uiState.update { it.copy(isProcessing = true, currentProcessStep = currentStr.stepStoppingGame) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                addLog("====================================================", LogType.INFO)
                addLog(currentStr.logStartInjection(version.displayName), LogType.INFO)

                _uiState.update { it.copy(currentProcessStep = currentStr.stepStoppingGame) }
                DevIdChanger.stopGame(version, currentStr) { msg ->
                    addLog(msg, LogType.INFO)
                }

                _uiState.update { it.copy(currentProcessStep = currentStr.stepCreatingBackup) }
                DevIdChanger.backupFiles(version, currentStr) { msg ->
                    addLog(msg, LogType.SUCCESS)
                }

                _uiState.update { it.copy(currentProcessStep = currentStr.stepInjectingId) }
                val injected = DevIdChanger.injectDeviceId(version, newId, currentStr) { msg ->
                    val type = when {
                        msg.startsWith("Error") || msg.startsWith("Gagal") -> LogType.ERROR
                        msg.startsWith("Peringatan") || msg.startsWith("Warning") -> LogType.WARNING
                        else -> LogType.INFO
                    }
                    addLog(msg, type)
                }
                if (!injected) {
                    return@launch
                }

                _uiState.update { it.copy(currentProcessStep = currentStr.stepFixingPermissions) }
                DevIdChanger.fixPermissions(version, currentStr) { msg ->
                    addLog(msg, LogType.INFO)
                }

                _uiState.update { it.copy(currentProcessStep = currentStr.stepFinishing) }
                val updatedId = DevIdChanger.getCurrentJsonId(version)
                _uiState.update { it.copy(currentJsonId = updatedId) }

                addLog(currentStr.logDoneSuccess, LogType.SUCCESS)
                addLog(currentStr.logOpenGameInstruction, LogType.SUCCESS)
                addLog("====================================================", LogType.INFO)

                _uiState.update { it.copy(showSuccessDialog = true) }


            } catch (e: Exception) {
                addLog(currentStr.logErrorInjection(e.localizedMessage ?: "Unknown error"), LogType.ERROR)
            } finally {
                _uiState.update { it.copy(isProcessing = false, currentProcessStep = null) }
            }
        }
    }

    fun dismissSuccessDialog() {
        _uiState.update { it.copy(showSuccessDialog = false) }
    }

    fun clearLogs() {
        _uiState.update { it.copy(logs = emptyList()) }
    }

    fun loadSavedDevIds(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val storage = SavedIdStorage(context)
            val list = storage.getSavedIds()
            _uiState.update { it.copy(savedDevIds = list) }
        }
    }

    fun saveDevId(context: Context, label: String, devId: String) {
        if (devId.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val storage = SavedIdStorage(context)
            val updated = storage.saveId(label, devId)
            _uiState.update { it.copy(savedDevIds = updated) }
            addLog(strings.logDevIdSaved(label), LogType.SUCCESS)
        }
    }

    fun deleteSavedDevId(context: Context, id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val storage = SavedIdStorage(context)
            val updated = storage.deleteId(id)
            _uiState.update { it.copy(savedDevIds = updated) }
            addLog(strings.logDevIdDeleted, LogType.INFO)
        }
    }

    fun applySavedDevId(devId: String) {
        _uiState.update { it.copy(newJsonId = devId) }
        addLog(strings.logDevIdApplied, LogType.INFO)
    }

    private fun addLog(message: String, type: LogType = LogType.INFO) {
        _uiState.update {
            it.copy(logs = it.logs + LogEntry(message, type))
        }
    }
}
