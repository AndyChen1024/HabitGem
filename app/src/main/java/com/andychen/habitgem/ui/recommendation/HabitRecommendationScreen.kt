package com.andychen.habitgem.ui.recommendation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andychen.habitgem.R
import com.andychen.habitgem.domain.model.Frequency
import com.andychen.habitgem.domain.model.HabitCategory
import com.andychen.habitgem.domain.model.HabitRecommendation
import com.andychen.habitgem.ui.theme.HabitGemTheme
import java.time.DayOfWeek
import java.util.UUID

/**
 * Screen for displaying AI habit recommendations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitRecommendationScreen(
    onNavigateBack: () -> Unit,
    onCreateManualHabit: () -> Unit,
    onAcceptRecommendation: (HabitRecommendation) -> Unit,
    viewModel: HabitRecommendationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("新建习惯") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab row for switching between manual creation and AI recommendations
            TabRow(
                selectedTabIndex = if (uiState.showAiRecommendations) 1 else 0
            ) {
                Tab(
                    selected = !uiState.showAiRecommendations,
                    onClick = { viewModel.setShowAiRecommendations(false) },
                    text = { Text("手动创建") }
                )
                Tab(
                    selected = uiState.showAiRecommendations,
                    onClick = { viewModel.setShowAiRecommendations(true) },
                    text = { Text("AI推荐") }
                )
            }
            
            // Content based on selected tab
            if (uiState.showAiRecommendations) {
                // AI Recommendations tab content
                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        uiState.isLoading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        uiState.error != null -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "无法加载推荐",
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = uiState.error ?: "未知错误",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { viewModel.loadRecommendations() }) {
                                    Text("重试")
                                }
                            }
                        }
                        uiState.recommendations.isEmpty() -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "暂无推荐习惯",
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = onCreateManualHabit) {
                                    Text("手动创建习惯")
                                }
                            }
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                contentPadding = PaddingValues(vertical = 16.dp)
                            ) {
                                items(uiState.recommendations) { recommendation ->
                                    RecommendedHabitCard(
                                        recommendation = recommendation,
                                        onAccept = { onAcceptRecommendation(recommendation) },
                                        onViewDetails = { viewModel.showHabitDetails(recommendation) },
                                        hasDetailedEvidence = uiState.loadedEvidenceIds.contains(recommendation.id)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Manual creation tab content (placeholder - will be implemented in another task)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "手动创建习惯界面将在另一个任务中实现",
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
    
    // Habit details dialog
    if (uiState.selectedRecommendation != null) {
        HabitDetailsDialog(
            recommendation = uiState.selectedRecommendation!!,
            habitEvidence = if (uiState.isLoadingEvidence) null else uiState.habitEvidence,
            onDismiss = { viewModel.dismissHabitDetails() },
            onAccept = { 
                onAcceptRecommendation(uiState.selectedRecommendation!!)
                viewModel.dismissHabitDetails()
            },
            onLearnMore = {
                // Only load evidence if not already loading
                if (!uiState.isLoadingEvidence) {
                    viewModel.loadHabitEvidence(uiState.selectedRecommendation!!.id)
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HabitRecommendationScreenPreview() {
    HabitGemTheme {
        HabitRecommendationScreen(
            onNavigateBack = {},
            onCreateManualHabit = {},
            onAcceptRecommendation = {}
        )
    }
}