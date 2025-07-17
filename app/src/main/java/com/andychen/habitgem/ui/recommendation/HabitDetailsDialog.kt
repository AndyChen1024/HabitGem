package com.andychen.habitgem.ui.recommendation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.andychen.habitgem.domain.model.Frequency
import com.andychen.habitgem.domain.model.HabitCategory
import com.andychen.habitgem.domain.model.HabitEvidence
import com.andychen.habitgem.domain.model.HabitRecommendation
import com.andychen.habitgem.ui.theme.HabitGemTheme
import java.time.DayOfWeek
import java.util.UUID

/**
 * Dialog to display detailed information about a recommended habit
 */
@Composable
fun HabitDetailsDialog(
    recommendation: HabitRecommendation,
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
    habitEvidence: HabitEvidence? = null,
    onLearnMore: () -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Text(
                    text = recommendation.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Category chip
                Chip(
                    onClick = { },
                    enabled = false,
                    colors = ChipDefaults.chipColors(
                        disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        disabledLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text(recommendation.category.name)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Description
                Text(
                    text = recommendation.description,
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Difficulty
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "难度: ",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Display difficulty as stars
                    Row {
                        repeat(5) { index ->
                            Icon(
                                imageVector = if (index < recommendation.difficulty) {
                                    androidx.compose.material.icons.Icons.Filled.Star
                                } else {
                                    androidx.compose.material.icons.Icons.Outlined.Star
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Time commitment
                Row {
                    Text(
                        text = "预计时间: ",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${recommendation.estimatedTimePerDay}分钟/天",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Suggested frequency
                Row {
                    Text(
                        text = "建议频率: ",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatFrequency(recommendation.suggestedFrequency),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Recommendation reason
                Text(
                    text = "推荐理由",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = recommendation.recommendationReason,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Scientific basis section
                var showFullEvidence by remember { mutableStateOf(false) }
                var isLoadingEvidence by remember { mutableStateOf(false) }
                val uriHandler = LocalUriHandler.current
                
                // Track loading state changes
                LaunchedEffect(habitEvidence) {
                    if (habitEvidence != null) {
                        isLoadingEvidence = false
                    }
                }
                
                // Scientific basis header with "Learn More" button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    onClick = { 
                        if (habitEvidence != null) {
                            showFullEvidence = !showFullEvidence 
                        } else {
                            isLoadingEvidence = true
                            onLearnMore()
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Outlined.Science,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "科学依据",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        if (isLoadingEvidence && habitEvidence == null) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (habitEvidence == null) "了解更多" else if (showFullEvidence) "收起详情" else "展开详情",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (habitEvidence != null && showFullEvidence) 
                                        Icons.Default.KeyboardArrowUp 
                                    else 
                                        Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (showFullEvidence) "收起" else "展开",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Basic scientific basis
                Text(
                    text = recommendation.scientificBasis,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // Expanded scientific evidence
                AnimatedVisibility(
                    visible = showFullEvidence && habitEvidence != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    habitEvidence?.let { evidence ->
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            // Benefits summary with icon
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "益处总结",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Benefits in a card for better visual separation
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Text(
                                    text = evidence.benefitsSummary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // References with icon
                            if (evidence.references.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Outlined.MenuBook,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "研究参考",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                // References in a card with clickable items
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        evidence.references.forEachIndexed { index, reference ->
                                            Row(
                                                verticalAlignment = Alignment.Top,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "${index + 1}. ",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                
                                                Column {
                                                    Text(
                                                        text = reference,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    
                                                    // Extract DOI or URL if present
                                                    val doiPattern = "doi:([\\w./\\-]+)".toRegex()
                                                    val urlPattern = "https?://[\\w\\d.\\-/]+".toRegex()
                                                    
                                                    val doiMatch = doiPattern.find(reference)
                                                    val urlMatch = urlPattern.find(reference)
                                                    
                                                    if (doiMatch != null || urlMatch != null) {
                                                        val link = doiMatch?.value ?: urlMatch?.value
                                                        val url = when {
                                                            doiMatch != null -> "https://doi.org/${doiMatch.groupValues[1]}"
                                                            else -> urlMatch?.value
                                                        }
                                                        
                                                        if (url != null) {
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            TextButton(
                                                                onClick = { uriHandler.openUri(url) },
                                                                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                                                            ) {
                                                                Text(
                                                                    text = "查看原文",
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = MaterialTheme.colorScheme.primary,
                                                                    textDecoration = TextDecoration.Underline
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            
                                            if (index < evidence.references.size - 1) {
                                                Divider(
                                                    modifier = Modifier.padding(vertical = 4.dp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(onClick = onAccept) {
                        Text("接受推荐")
                    }
                }
            }
        }
    }
}

/**
 * Format frequency to a human-readable string
 */
@Composable
fun formatFrequency(frequency: Frequency): String {
    return when (frequency) {
        is Frequency.Daily -> {
            if (frequency.timesPerDay == 1) "每天一次" else "每天${frequency.timesPerDay}次"
        }
        is Frequency.Weekly -> {
            val days = frequency.daysOfWeek.joinToString(", ") { formatDayOfWeek(it) }
            "每周${frequency.daysOfWeek.size}次 (${days})"
        }
        is Frequency.Monthly -> {
            val days = frequency.daysOfMonth.joinToString(", ") { "${it}号" }
            "每月${frequency.daysOfMonth.size}次 (${days})"
        }
        is Frequency.Interval -> {
            "每${frequency.everyNDays}天一次"
        }
    }
}

/**
 * Format day of week to Chinese
 */
@Composable
fun formatDayOfWeek(dayOfWeek: DayOfWeek): String {
    return when (dayOfWeek) {
        DayOfWeek.MONDAY -> "周一"
        DayOfWeek.TUESDAY -> "周二"
        DayOfWeek.WEDNESDAY -> "周三"
        DayOfWeek.THURSDAY -> "周四"
        DayOfWeek.FRIDAY -> "周五"
        DayOfWeek.SATURDAY -> "周六"
        DayOfWeek.SUNDAY -> "周日"
    }
}

@Preview(showBackground = true)
@Composable
fun HabitDetailsDialogPreview() {
    HabitGemTheme {
        val recommendation = HabitRecommendation(
            id = UUID.randomUUID().toString(),
            name = "晨间冥想",
            description = "每天早晨进行10分钟冥想，提高专注力和减轻压力",
            category = HabitCategory.MINDFULNESS,
            difficulty = 3,
            recommendationReason = "冥想可以减轻压力，提高专注力，是开始新一天的理想方式。研究表明，早晨冥想可以设定积极的基调，帮助你在一天中保持平静和专注。",
            scientificBasis = "多项研究表明，定期冥想可以减轻焦虑，改善注意力，并促进情绪平衡。2019年发表在《神经科学杂志》上的一项研究发现，每天10分钟的冥想可以显著提高工作记忆和注意力。",
            suggestedFrequency = Frequency.Daily(),
            estimatedTimePerDay = 10
        )
        
        val evidence = HabitEvidence(
            habitId = recommendation.id,
            scientificBasis = "多项研究表明，定期冥想可以减轻焦虑，改善注意力，并促进情绪平衡。2019年发表在《神经科学杂志》上的一项研究发现，每天10分钟的冥想可以显著提高工作记忆和注意力。此外，冥想还与降低压力激素水平、改善睡眠质量和增强免疫功能相关。",
            references = listOf(
                "Davidson, R. J., et al. (2003). Alterations in Brain and Immune Function Produced by Mindfulness Meditation. Psychosomatic Medicine, 65(4), 564-570.",
                "Tang, Y. Y., et al. (2019). The neuroscience of mindfulness meditation. Nature Reviews Neuroscience, 16(4), 213-225.",
                "Goyal, M., et al. (2014). Meditation Programs for Psychological Stress and Well-being. JAMA Internal Medicine, 174(3), 357-368."
            ),
            benefitsSummary = "定期冥想练习与以下益处相关联：\n- 降低焦虑和抑郁症状\n- 提高注意力和工作记忆\n- 改善情绪调节能力\n- 降低压力激素水平\n- 改善睡眠质量\n- 增强免疫系统功能\n- 减少慢性疼痛"
        )
        
        HabitDetailsDialog(
            recommendation = recommendation,
            habitEvidence = evidence,
            onDismiss = {},
            onAccept = {},
            onLearnMore = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HabitDetailsDialogWithoutEvidencePreview() {
    HabitGemTheme {
        val recommendation = HabitRecommendation(
            id = UUID.randomUUID().toString(),
            name = "晨间冥想",
            description = "每天早晨进行10分钟冥想，提高专注力和减轻压力",
            category = HabitCategory.MINDFULNESS,
            difficulty = 3,
            recommendationReason = "冥想可以减轻压力，提高专注力，是开始新一天的理想方式。研究表明，早晨冥想可以设定积极的基调，帮助你在一天中保持平静和专注。",
            scientificBasis = "多项研究表明，定期冥想可以减轻焦虑，改善注意力，并促进情绪平衡。2019年发表在《神经科学杂志》上的一项研究发现，每天10分钟的冥想可以显著提高工作记忆和注意力。",
            suggestedFrequency = Frequency.Daily(),
            estimatedTimePerDay = 10
        )
        
        HabitDetailsDialog(
            recommendation = recommendation,
            habitEvidence = null,
            onDismiss = {},
            onAccept = {},
            onLearnMore = {}
        )
    }
}