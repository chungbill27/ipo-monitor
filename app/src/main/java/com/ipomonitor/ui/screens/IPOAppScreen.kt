package com.ipomonitor.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipomonitor.ui.screens.detail.IPODetailPane
import com.ipomonitor.ui.screens.list.IPOListPane
import com.ipomonitor.ui.screens.settings.SettingsScreen
import com.ipomonitor.ui.screens.setup.SetupScreen
import kotlinx.coroutines.launch

/**
 * Root screen that handles:
 * 1. First-launch setup (multi-model API key input)
 * 2. Main app with adaptive List-Detail layout for foldable
 * 3. Settings page (check frequency, AI model, work hours)
 */
@Composable
fun IPOAppScreen(
    viewModel: IPOViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Show error as snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            scope.launch {
                snackbarHostState.showSnackbar(msg)
                viewModel.dismissError()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            !uiState.isSetupComplete -> {
                SetupScreen(
                    onSetupComplete = { /* State auto-updates via ViewModel */ },
                    onValidateGeminiKey = { key, geminiModel -> viewModel.validateGeminiKey(key, geminiModel) },
                    onValidateOpenAIKey = { key, model -> viewModel.validateOpenAIKey(key, model) }
                )
            }
            uiState.showSettings -> {
                SettingsScreen(
                    currentProvider = uiState.currentProvider,
                    currentGeminiModel = uiState.currentGeminiModel,
                    currentOpenAIModel = uiState.currentModel,
                    currentFrequency = uiState.currentFrequency,
                    workHoursOnly = uiState.workHoursOnly,
                    lastCheckTime = uiState.lastCheckTime,
                    analysisCount = uiState.analysisCount,
                    onProviderChanged = viewModel::onProviderChanged,
                    onGeminiModelChanged = viewModel::onGeminiModelChanged,
                    onOpenAIModelChanged = viewModel::onModelChanged,
                    onFrequencyChanged = viewModel::onFrequencyChanged,
                    onWorkHoursChanged = viewModel::onWorkHoursChanged,
                    onChangeApiKey = viewModel::onChangeApiKey,
                    onBack = viewModel::onSettingsBack
                )
            }
            else -> {
                MainContent(viewModel = viewModel, uiState = uiState)
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun MainContent(
    viewModel: IPOViewModel,
    uiState: IPOUiState
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<Int>()

    // Handle back press
    androidx.activity.compose.BackHandler(navigator.canNavigateBack()) {
        navigator.navigateBack()
    }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                IPOListPane(
                    recentIPOs = uiState.recentIPOs,
                    historicalIPOs = uiState.historicalIPOs,
                    availableMonths = uiState.availableMonths,
                    availableIndustries = uiState.availableIndustries,
                    selectedMonth = uiState.selectedMonth,
                    selectedIndustry = uiState.selectedIndustry,
                    searchQuery = uiState.searchQuery,
                    selectedHkexId = uiState.selectedHkexId,
                    hasMoreHistorical = uiState.hasMoreHistorical,
                    isLoadingMore = uiState.isLoadingMore,
                    isRefreshing = uiState.isRefreshing,
                    onItemClick = { hkexId ->
                        viewModel.onItemSelected(hkexId)
                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, hkexId)
                    },
                    onAnalyzeClick = { hkexId -> viewModel.onAnalyzeClick(hkexId) },
                    onLoadMore = viewModel::onLoadMore,
                    onMonthSelected = viewModel::onMonthSelected,
                    onIndustrySelected = viewModel::onIndustrySelected,
                    onSearchChanged = viewModel::onSearchChanged,
                    onRefresh = viewModel::onRefresh,
                    onSettingsClick = viewModel::onSettingsClick
                )
            }
        },
        detailPane = {
            AnimatedPane {
                val selectedRecord = uiState.selectedRecord
                if (selectedRecord != null) {
                    IPODetailPane(
                        record = selectedRecord,
                        showBackButton = navigator.canNavigateBack(),
                        onBackClick = { navigator.navigateBack() },
                        onAnalyzeClick = { hkexId -> viewModel.onAnalyzeClick(hkexId) },
                        onRetryClick = { hkexId -> viewModel.onAnalyzeClick(hkexId) }
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "選擇一家公司查看詳情",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    )
}
