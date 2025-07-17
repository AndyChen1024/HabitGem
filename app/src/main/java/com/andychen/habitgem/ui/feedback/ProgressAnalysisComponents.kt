package com.andychen.habitgem.ui.feedback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andychen.habitgem.R
import com.andychen.habitgem.domain.model.DataPoint
import com.andychen.habitgem.domain.model.ProgressAnalysis
import com.andychen.habitgem.ui.theme.HabitGemTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * 进度分析卡片
 * 
 * 显示习惯的进度分析，包括完成率、连续天数、洞察和建议
 * 
 * @param progressAnalysis 进度分析数据
 * @param modifier 修饰符
 */
@Composable
fun ProgressAnalysisCard(
    progressAnalysis: ProgressAnalysis,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题
            Text(
                text = "习惯进度分析",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 统计数据
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 完成率
                CompletionRateIndicator(
                    completionRate = progressAnalysis.completionRate,
                    modifier = Modifier.weight(1f)
                )
                
                // 连续天数
                StreakIndicator(
                    streak = progressAnalysis.streak,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 可视化数据
            if (progressAnalysis.visualData.isNotEmpty()) {
                Text(
                    text = "最近7天完成情况",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                WeeklyCompletionChart(
                    dataPoints = progressAnalysis.visualData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(vertical = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // 洞察
            InsightCard(
                insight = progressAnalysis.insight,
                suggestion = progressAnalysis.suggestion
            )
        }
    }
}

/**
 * 完成率指示器
 * 
 * @param completionRate 完成率 (0.0-1.0)
 * @param modifier 修饰符
 */
@Composable
fun CompletionRateIndicator(
    completionRate: Float,
    modifier: Modifier = Modifier
) {
    var animatedCompletionRate by remember { mutableStateOf(0f) }
    
    LaunchedEffect(completionRate) {
        animatedCompletionRate = completionRate
    }
    
    val animatedProgress by animateFloatAsState(
        targetValue = animatedCompletionRate,
        animationSpec = tween(1000),
        label = "completion_rate_animation"
    )
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            // 背景圆环
            CircularProgressIndicator(
                progress = 1f,
                color = MaterialTheme.colorScheme.surfaceVariant,
                strokeWidth = 8.dp,
                size = 80.dp
            )
            
            // 进度圆环
            CircularProgressIndicator(
                progress = animatedProgress,
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 8.dp,
                size = 80.dp
            )
            
            // 百分比文本
            Text(
                text = "${(animatedProgress * 100).roundToInt()}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        Text(
            text = "完成率",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * 连续天数指示器
 * 
 * @param streak 连续天数
 * @param modifier 修饰符
 */
@Composable
fun StreakIndicator(
    streak: Int,
    modifier: Modifier = Modifier
) {
    var animatedStreak by remember { mutableStateOf(0) }
    
    LaunchedEffect(streak) {
        animatedStreak = streak
    }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .padding(8.dp)
                .clip(CircleShape)
                .background(
                    if (streak >= 7) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$animatedStreak",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (streak >= 7) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = "连续天数",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * 周完成情况图表
 * 
 * @param dataPoints 数据点列表
 * @param modifier 修饰符
 */
@Composable
fun WeeklyCompletionChart(
    dataPoints: List<DataPoint>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            val barWidth = width / (dataPoints.size * 2)
            val spacing = width / dataPoints.size
            
            // 绘制底部日期标签线
            drawLine(
                color = MaterialTheme.colorScheme.outlineVariant,
                start = Offset(0f, height - 20.dp.toPx()),
                end = Offset(width, height - 20.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
            
            // 绘制柱状图
            dataPoints.forEachIndexed { index, dataPoint ->
                val x = spacing * (index + 0.5f)
                val barHeight = if (dataPoint.value > 0) height * 0.7f else 0f
                
                // 绘制柱子
                drawLine(
                    color = if (dataPoint.value > 0) MaterialTheme.colorScheme.primary 
                           else MaterialTheme.colorScheme.surfaceVariant,
                    start = Offset(x, height - 20.dp.toPx()),
                    end = Offset(x, height - 20.dp.toPx() - barHeight),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round
                )
            }
        }
        
        // 绘制日期标签
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            dataPoints.forEach { dataPoint ->
                Text(
                    text = dataPoint.label ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 洞察卡片
 * 
 * @param insight 洞察文本
 * @param suggestion 建议文本
 * @param modifier 修饰符
 */
@Composable
fun InsightCard(
    insight: String,
    suggestion: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 洞察部分
            Row(
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground), // 替换为实际的洞察图标
                    contentDescription = "洞察",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = insight,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            
            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
            )
            
            // 建议部分
            Row(
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground), // 替换为实际的建议图标
                    contentDescription = "建议",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = suggestion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

/**
 * 自定义圆形进度指示器
 */
@Composable
fun CircularProgressIndicator(
    progress: Float,
    color: Color,
    strokeWidth: androidx.compose.ui.unit.Dp,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val sweepAngle = progress * 360f
            
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeWidth.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

/**
 * 进度分析详情屏幕
 * 
 * @param progressAnalysis 进度分析数据
 * @param habitName 习惯名称
 * @param onBackClick 返回按钮点击回调
 * @param modifier 修饰符
 */
@Composable
fun ProgressAnalysisDetailScreen(
    progressAnalysis: ProgressAnalysis,
    habitName: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 标题
        Text(
            text = "$habitName 进度分析",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 统计数据卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "统计数据",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 完成率
                    CompletionRateIndicator(
                        completionRate = progressAnalysis.completionRate,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // 连续天数
                    StreakIndicator(
                        streak = progressAnalysis.streak,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 可视化数据卡片
        if (progressAnalysis.visualData.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "最近7天完成情况",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    WeeklyCompletionChart(
                        dataPoints = progressAnalysis.visualData,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 日期范围
                    val startDate = progressAnalysis.visualData.firstOrNull()?.date
                    val endDate = progressAnalysis.visualData.lastOrNull()?.date
                    
                    if (startDate != null && endDate != null) {
                        val dateFormatter = DateTimeFormatter.ofPattern("MM月dd日")
                        Text(
                            text = "${startDate.format(dateFormatter)} - ${endDate.format(dateFormatter)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // 洞察和建议卡片
        InsightCard(
            insight = progressAnalysis.insight,
            suggestion = progressAnalysis.suggestion
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProgressAnalysisCardPreview() {
    HabitGemTheme {
        Surface {
            ProgressAnalysisCard(
                progressAnalysis = ProgressAnalysis(
                    completionRate = 0.75f,
                    streak = 12,
                    insight = "您的完成率非常高 (75%)，表明这个习惯已经很好地融入了您的日常生活。",
                    suggestion = "尝试在早晨完成这个习惯，研究表明这可以提高成功率。",
                    visualData = listOf(
                        DataPoint(LocalDate.now().minusDays(6), 1f, "周一"),
                        DataPoint(LocalDate.now().minusDays(5), 1f, "周二"),
                        DataPoint(LocalDate.now().minusDays(4), 0f, "周三"),
                        DataPoint(LocalDate.now().minusDays(3), 1f, "周四"),
                        DataPoint(LocalDate.now().minusDays(2), 1f, "周五"),
                        DataPoint(LocalDate.now().minusDays(1), 0f, "周六"),
                        DataPoint(LocalDate.now(), 1f, "周日")
                    )
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProgressAnalysisDetailScreenPreview() {
    HabitGemTheme {
        Surface {
            ProgressAnalysisDetailScreen(
                progressAnalysis = ProgressAnalysis(
                    completionRate = 0.75f,
                    streak = 12,
                    insight = "您的完成率非常高 (75%)，表明这个习惯已经很好地融入了您的日常生活。您在周一和周四的完成率最高，这些天可能是您最适合执行这个习惯的时间。",
                    suggestion = "尝试在早晨完成这个习惯，研究表明这可以提高成功率。与朋友一起锻炼可以提高坚持度，考虑邀请朋友一起参与。",
                    visualData = listOf(
                        DataPoint(LocalDate.now().minusDays(6), 1f, "周一"),
                        DataPoint(LocalDate.now().minusDays(5), 1f, "周二"),
                        DataPoint(LocalDate.now().minusDays(4), 0f, "周三"),
                        DataPoint(LocalDate.now().minusDays(3), 1f, "周四"),
                        DataPoint(LocalDate.now().minusDays(2), 1f, "周五"),
                        DataPoint(LocalDate.now().minusDays(1), 0f, "周六"),
                        DataPoint(LocalDate.now(), 1f, "周日")
                    )
                ),
                habitName = "晨间冥想",
                onBackClick = {}
            )
        }
    }
}