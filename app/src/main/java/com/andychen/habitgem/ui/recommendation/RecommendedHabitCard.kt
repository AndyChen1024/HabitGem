package com.andychen.habitgem.ui.recommendation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andychen.habitgem.domain.model.Frequency
import com.andychen.habitgem.domain.model.HabitCategory
import com.andychen.habitgem.domain.model.HabitRecommendation
import com.andychen.habitgem.ui.theme.HabitGemTheme
import java.time.DayOfWeek
import java.util.UUID

/**
 * A card component that displays a recommended habit
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendedHabitCard(
    recommendation: HabitRecommendation,
    onAccept: () -> Unit,
    onViewDetails: () -> Unit,
    hasDetailedEvidence: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Habit name and category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = recommendation.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                
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
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Description
            Text(
                text = recommendation.description,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Difficulty level
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "难度: ",
                    style = MaterialTheme.typography.bodyMedium
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
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Estimated time
                Text(
                    text = "${recommendation.estimatedTimePerDay}分钟/天",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Recommendation reason and scientific basis indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "推荐理由: ${recommendation.recommendationReason}",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Scientific basis indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.alpha(0.7f)
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Outlined.Science,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (hasDetailedEvidence) "科学依据已加载" else "查看科学依据",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (hasDetailedEvidence) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onViewDetails,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("查看详情")
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(onClick = onAccept) {
                    Text("接受")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RecommendedHabitCardPreview() {
    HabitGemTheme {
        RecommendedHabitCard(
            recommendation = HabitRecommendation(
                id = UUID.randomUUID().toString(),
                name = "晨间冥想",
                description = "每天早晨进行10分钟冥想，提高专注力和减轻压力",
                category = HabitCategory.MINDFULNESS,
                difficulty = 3,
                recommendationReason = "冥想可以减轻压力，提高专注力",
                scientificBasis = "研究表明，定期冥想可以减轻焦虑，改善注意力，并促进情绪平衡",
                suggestedFrequency = Frequency.Daily(),
                estimatedTimePerDay = 10
            ),
            onAccept = {},
            onViewDetails = {}
        )
    }
}