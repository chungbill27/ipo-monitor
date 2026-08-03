package com.ipomonitor.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ipomonitor.data.model.AIProvider
import com.ipomonitor.data.model.AnalysisStatus
import com.ipomonitor.data.model.GeminiModel
import com.ipomonitor.data.model.IPOEntity
import com.ipomonitor.data.model.IPOListItem
import com.ipomonitor.data.model.OpenAIModel
import com.ipomonitor.data.remote.GeminiService
import com.ipomonitor.data.repository.IPORepository
import com.ipomonitor.util.CheckFrequency
import com.ipomonitor.util.SecurePrefs
import com.ipomonitor.worker.IPOCheckWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IPOUiState(
    val isSetupComplete: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMoreHistorical: Boolean = true,
    val recentIPOs: List<IPOListItem> = emptyList(),
    val historicalIPOs: List<IPOListItem> = emptyList(),
    val selectedHkexId: Int? = null,
    val selectedRecord: IPOEntity? = null,
    val searchQuery: String = "",
    val selectedMonth: String? = null,
    val selectedIndustry: String? = null,
    val availableMonths: List<String> = emptyList(),
    val availableIndustries: List<String> = emptyList(),
    val historicalPage: Int = 0,
    val errorMessage: String? = null,
    // Settings state
    val showSettings: Boolean = false,
    val currentProvider: AIProvider = AIProvider.GEMINI,
    val currentGeminiModel: GeminiModel = GeminiModel.GEMINI_35_FLASH,
    val currentModel: OpenAIModel? = null,
    val currentFrequency: CheckFrequency = CheckFrequency.HOURS_1,
    val workHoursOnly: Boolean = false,
    val lastCheckTime: Long = 0L,
    val analysisCount: Int = 0
)

@HiltViewModel
class IPOViewModel @Inject constructor(
    private val application: Application,
    private val repository: IPORepository,
    private val securePrefs: SecurePrefs,
    private val geminiService: GeminiService
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(IPOUiState())
    val uiState: StateFlow<IPOUiState> = _uiState.asStateFlow()

    // Cutoff date: 2026-06-01 (normalized YYYYMMDD for DAO comparison)
    private val cutoffDate = "20260601"

    init {
        checkSetupStatus()
    }

    private fun checkSetupStatus() {
        val hasKey = securePrefs.isSetupComplete()
        _uiState.update { it.copy(
            isSetupComplete = hasKey,
            currentProvider = securePrefs.getProvider(),
            currentGeminiModel = securePrefs.getGeminiModel(),
            currentModel = securePrefs.getOpenAIModel(),
            currentFrequency = securePrefs.getCheckFrequency(),
            workHoursOnly = securePrefs.isWorkHoursOnly(),
            lastCheckTime = securePrefs.getLastCheckTime(),
            analysisCount = securePrefs.getAnalysisCount()
        )}
        if (hasKey) {
            loadData()
        }
    }

    // ============ Setup (Multi-model) ============

    suspend fun validateGeminiKey(apiKey: String, model: GeminiModel = GeminiModel.GEMINI_35_FLASH): Boolean {
        val isValid = geminiService.validateApiKey(apiKey)
        if (isValid) {
            securePrefs.setProvider(AIProvider.GEMINI)
            securePrefs.setGeminiApiKey(apiKey)
            securePrefs.setGeminiModel(model)
            securePrefs.setSetupComplete(true)
            _uiState.update { it.copy(
                isSetupComplete = true,
                currentProvider = AIProvider.GEMINI,
                currentGeminiModel = model
            )}
            IPOCheckWorker.schedule(application, securePrefs.getCheckFrequency())
            loadData()
        }
        return isValid
    }

    suspend fun validateOpenAIKey(apiKey: String, model: OpenAIModel): Boolean {
        val isValid = geminiService.validateOpenAIKey(apiKey)
        if (isValid) {
            securePrefs.setProvider(AIProvider.OPENAI)
            securePrefs.setOpenAIApiKey(apiKey)
            securePrefs.setOpenAIModel(model)
            securePrefs.setSetupComplete(true)
            _uiState.update { it.copy(
                isSetupComplete = true,
                currentProvider = AIProvider.OPENAI,
                currentModel = model
            )}
            IPOCheckWorker.schedule(application, securePrefs.getCheckFrequency())
            loadData()
        }
        return isValid
    }

    // ============ Data Loading ============

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                // Sync from HKEX
                repository.syncFromHKEX()

                // Load recent IPOs (after cutoff)
                val recent = repository.getIPOsAfterDate(cutoffDate)
                val months = repository.getAvailableMonths()
                val industries = repository.getAvailableIndustries()

                _uiState.update { it.copy(
                    recentIPOs = recent,
                    availableMonths = months,
                    availableIndustries = industries,
                    isRefreshing = false,
                    lastCheckTime = securePrefs.getLastCheckTime()
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isRefreshing = false,
                    errorMessage = "同步失敗: ${e.message}"
                )}
            }
        }
    }

    fun onRefresh() {
        loadData()
    }

    fun onLoadMore() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            try {
                val currentPage = _uiState.value.historicalPage
                val state = _uiState.value
                val moreItems = repository.getIPOsBeforeDate(
                    cutoffDate,
                    offset = currentPage * 10,
                    limit = 10,
                    searchQuery = state.searchQuery,
                    month = state.selectedMonth,
                    industry = state.selectedIndustry
                )
                _uiState.update { it.copy(
                    historicalIPOs = it.historicalIPOs + moreItems,
                    historicalPage = currentPage + 1,
                    hasMoreHistorical = moreItems.size == 10,
                    isLoadingMore = false
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    // ============ Filtering ============

    fun onSearchChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        viewModelScope.launch { reloadFiltered() }
    }

    fun onMonthSelected(month: String?) {
        _uiState.update { it.copy(selectedMonth = month) }
        viewModelScope.launch { reloadFiltered() }
    }

    fun onIndustrySelected(industry: String?) {
        _uiState.update { it.copy(selectedIndustry = industry) }
        viewModelScope.launch { reloadFiltered() }
    }

    private suspend fun reloadFiltered() {
        val state = _uiState.value
        val recent = repository.getIPOsAfterDate(
            cutoffDate,
            state.searchQuery,
            state.selectedMonth,
            state.selectedIndustry
        )
        val historical = if (state.historicalPage > 0) {
            repository.getIPOsBeforeDate(
                cutoffDate,
                offset = 0,
                limit = state.historicalPage * 10,
                searchQuery = state.searchQuery,
                month = state.selectedMonth,
                industry = state.selectedIndustry
            )
        } else emptyList()

        _uiState.update { it.copy(
            recentIPOs = recent,
            historicalIPOs = historical
        )}
    }

    // ============ Manual Analysis ============

    fun onAnalyzeClick(hkexId: Int) {
        viewModelScope.launch {
            // Update status to QUEUED
            repository.updateStatus(hkexId, AnalysisStatus.QUEUED.name)
            refreshItemStatus(hkexId, AnalysisStatus.QUEUED.name)

            try {
                // Update to ANALYZING
                repository.updateStatus(hkexId, AnalysisStatus.ANALYZING.name)
                refreshItemStatus(hkexId, AnalysisStatus.ANALYZING.name)

                // Perform analysis
                val success = repository.analyzeIPO(hkexId)
                val newStatus = if (success) AnalysisStatus.COMPLETED.name else AnalysisStatus.FAILED.name
                repository.updateStatus(hkexId, newStatus)
                refreshItemStatus(hkexId, newStatus)

                if (success) {
                    securePrefs.incrementAnalysisCount()
                    _uiState.update { it.copy(analysisCount = securePrefs.getAnalysisCount()) }
                    // Refresh selected record if viewing this item
                    if (_uiState.value.selectedHkexId == hkexId) {
                        repository.observeRecord(hkexId).first()?.let { record ->
                            _uiState.update { it.copy(selectedRecord = record) }
                        }
                    }
                }
            } catch (e: Exception) {
                repository.updateStatus(hkexId, AnalysisStatus.FAILED.name)
                refreshItemStatus(hkexId, AnalysisStatus.FAILED.name)
                _uiState.update { it.copy(errorMessage = "分析失敗: ${e.message}") }
            }
        }
    }

    private fun refreshItemStatus(hkexId: Int, newStatus: String) {
        _uiState.update { state ->
            state.copy(
                recentIPOs = state.recentIPOs.map {
                    if (it.hkexId == hkexId) it.copy(status = newStatus) else it
                },
                historicalIPOs = state.historicalIPOs.map {
                    if (it.hkexId == hkexId) it.copy(status = newStatus) else it
                }
            )
        }
    }

    // ============ Settings ============

    fun onSettingsClick() {
        _uiState.update { it.copy(
            showSettings = true,
            lastCheckTime = securePrefs.getLastCheckTime(),
            analysisCount = securePrefs.getAnalysisCount()
        )}
    }

    fun onSettingsBack() {
        _uiState.update { it.copy(showSettings = false) }
    }

    fun onProviderChanged(provider: AIProvider) {
        securePrefs.setProvider(provider)
        _uiState.update { it.copy(currentProvider = provider) }
    }

    fun onGeminiModelChanged(model: GeminiModel) {
        securePrefs.setGeminiModel(model)
        _uiState.update { it.copy(currentGeminiModel = model) }
    }

    fun onModelChanged(model: OpenAIModel) {
        securePrefs.setOpenAIModel(model)
        _uiState.update { it.copy(currentModel = model) }
    }

    fun onFrequencyChanged(frequency: CheckFrequency) {
        securePrefs.setCheckFrequency(frequency)
        _uiState.update { it.copy(currentFrequency = frequency) }
        // Reschedule WorkManager with new frequency
        IPOCheckWorker.reschedule(application, frequency)
    }

    fun onWorkHoursChanged(enabled: Boolean) {
        securePrefs.setWorkHoursOnly(enabled)
        _uiState.update { it.copy(workHoursOnly = enabled) }
    }

    fun onChangeApiKey() {
        securePrefs.setSetupComplete(false)
        _uiState.update { it.copy(
            isSetupComplete = false,
            showSettings = false
        )}
    }

    // ============ Selection ============

    fun onItemSelected(hkexId: Int) {
        _uiState.update { it.copy(selectedHkexId = hkexId) }
        viewModelScope.launch {
            repository.observeRecord(hkexId).collect { record ->
                _uiState.update { it.copy(selectedRecord = record) }
            }
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedHkexId = null, selectedRecord = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
