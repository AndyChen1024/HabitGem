package com.andychen.habitgem.ui.feedback

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andychen.habitgem.domain.model.DataPoint
import com.andychen.habitgem.domain.model.HabitInsight
import com.andychen.habitgem.domain.model.Trend
import com.andychen.habitgem.ui.theme.HabitGemTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 习惯数据探索屏幕
 * 
 * 提供交互式数据探索功能，包括趋势图表、热力图、日历视图和一致性指标
 * 
 * @param habitName 习惯名称
 * @param habitInsight 习惯洞察数据
 * @param dataPoints 趋势数据点
 * @param completionByDate 日期到完成情况的映射
 * @param onBackClick 返回按钮点击回调
 * @param modifier 修饰符
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDataExplorationScreen(
    habitName: String,
    habitInsight: HabitInsight,
    dataPoints: List<DataPoint>,
    completionByDate: Map<LocalDate, Boolean>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 计算热力图数据
    val heatmapData = completionByDate.mapValues { if (it.value) 1f else 0f }
    
    // 计算周期比较数据
    val today = LocalDate.now()
    val currentWeekStart = today.minusDays(today.dayOfWeek.value - 1L)
    val previousWeekStart = currentWeekStart.minusWeeks(1)
    
    val currentWeekData = completionByDate.filter { 
        it.key >= currentWeekStart && it.key < currentWeekStart.plusWeeks(1) 
    }
    val previousWeekData = completionByDate.filter { 
        it.key >= previousWeekStart && it.key < currentWeekStart 
    }
    
    val currentWeekRate = if (currentWeekData.isNotEmpty()) {
        currentWeekData.count { it.value }.toFloat() / currentWeekData.size
    } else 0f
    
    val previousWeekRate = if (previousWeekData.isNotEmpty()) {
        previousWeekData.count { it.value }.toFloat() / previousWeekData.size
    } else 0f
    
    // 选项卡状态
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("趋势", "日历", "热力图", "一致性")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "$habitName 数据分析") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 选项卡
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(text = title) }
                    )
                }
            }
            
            // 选项卡内容
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                when (selectedTabIndex) {
                    0 -> {
                        // 趋势视图
                        Text(
                            text = "习惯完成趋势",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        HabitTrendChart(
                            dataPoints = dataPoints,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        PeriodComparisonCard(
                            currentPeriodRate = currentWeekRate,
                            previousPeriodRate = previousWeekRate,
                            periodType = "周",
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 洞察卡片
                        InsightCard(
                            insight = habitInsight.insightMessage,
                            suggestion = "基于您的习惯数据，我们建议您在${habitInsight.bestPerformingDays.firstOrNull()?.name ?: "适合的时间"}完成这个习惯，以提高成功率。"
                        )
                    }
                    1 -> {
                        // 日历视图
                        Text(
                            text = "月度完成情况",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val today = LocalDate.now()
                        
                        HabitCalendarView(
                            dataByDate = completionByDate,
                            month = today.monthValue,
                            year = today.year,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // 上个月日历
                        val lastMonth = today.minusMonths(1)
                        
                        Text(
                            text = "上月完成情况",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        HabitCalendarView(
                            dataByDate = completionByDate,
                            month = lastMonth.monthValue,
                            year = lastMonth.year,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    2 -> {
                        // 热力图视图
                        Text(
                            text = "习惯热力图",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "过去90天的习惯完成情况",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val today = LocalDate.now()
                        val startDate = today.minusDays(90)
                        
                        HabitHeatmap(
                            dataByDate = heatmapData,
                            startDate = startDate,
                            endDate = today,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // 热力图分析
                        Text(
                            text = "热力图分析",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 计算每周几的完成率
                        val dayOfWeekRates = DayOfWeek.values().associateWith { dayOfWeek ->
                            val dayRecords = completionByDate.filter { it.key.dayOfWeek == dayOfWeek }
                            if (dayRecords.isNotEmpty()) {
                                dayRecords.count { it.value }.toFloat() / dayRecords.size
                            } else 0f
                        }
                        
                        val bestDay = dayOfWeekRates.maxByOrNull { it.value }?.key
                        val worstDay = dayOfWeekRates.minByOrNull { it.value }?.key
                        
                        if (bestDay != null && worstDay != null) {
                            Text(
                                text = "您在${bestDay.name}的完成率最高，而在${worstDay.name}的完成率最低。考虑在${worstDay.name}设置额外的提醒或调整习惯执行时间。",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    3 -> {
                        // 一致性指标视图
                        ConsistencyMetricCard(
                            consistencyScore = habitInsight.consistencyScore,
                            trend = habitInsight.completionTrend,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // 一致性分析
                        Text(
                            text = "一致性分析",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "一致性是习惯养成的关键。您的习惯一致性得分为${(habitInsight.consistencyScore * 100).toInt()}分，" +
                                  when {
                                      habitInsight.consistencyScore >= 0.8f -> "这是一个非常优秀的分数，表明您已经很好地将这个习惯融入日常生活。"
                                      habitInsight.consistencyScore >= 0.6f -> "这是一个良好的分数，表明您正在建立稳定的习惯模式。"
                                      habitInsight.consistencyScore >= 0.4f -> "这是一个中等的分数，表明您的习惯养成还有提升空间。"
                                      else -> "这个分数表明您在习惯养成方面面临挑战，考虑调整习惯难度或设置更多提醒。"
                                  },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "您的习惯趋势为" + 
                                  when (habitInsight.completionTrend) {
                                      Trend.IMPROVING -> "上升，这是一个积极的信号，表明您正在建立越来越强的习惯模式。"
                                      Trend.DECLINING -> "下降，这可能是重新评估和调整的好时机。"
                                      Trend.STABLE -> "稳定，这表明您的习惯执行保持一致。"
                                      Trend.FLUCTUATING -> "波动，这表明您的习惯执行不够稳定，可能需要更规律的时间安排。"
                                      Trend.NOT_ENOUGH_DATA -> "数据不足，继续记录您的习惯，我们将提供更详细的分析。"
                                  },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 最佳表现日
                        if (habitInsight.bestPerformingDays.isNotEmpty()) {
                            Text(
                                text = "您在${habitInsight.bestPerformingDays.joinToString("、") { it.name }}表现最佳。考虑在这些天安排更多习惯，或者分析这些天的成功因素，应用到其他天。",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HabitDataExplorationScreenPreview() {
    HabitGemTheme {
        Surface {
            val today = LocalDate.now()
            
            // 生成示例数据
            val dataPoints = (0..30).map { i ->
                val date = today.minusDays((30 - i).toLong())
                val value = when {
                    i < 10 -> 0.3f + (i / 30f)
                    i < 20 -> 0.5f + ((i - 10) / 20f)
                    else -> 0.7f + ((i - 20) / 50f)
                }.coerceIn(0f, 1f)
                
                DataPoint(
                    date = date,
                    value = value,
                    label = date.format(DateTimeFormatter.ofPattern("MM/dd"))
                )
            }
            
            val completionByDate = (0..90).associate { i ->
                val date = today.minusDays((90 - i).toLong())
                date to (i % 3 != 0 && i % 7 != 0)
            }
            
            HabitDataExplorationScreen(
                habitName = "晨间冥想",
                habitInsight = HabitInsight(
                    habitId = "1",
                    bestPerformingDays = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                    completionTrend = Trend.IMPROVING,
                    consistencyScore = 0.75f,
                    insightMessage = "您的习惯坚持度正在稳步提高，并且保持了很高的一致性。您在周一和周三表现最好，考虑在这些天安排更多习惯。"
                ),
                dataPoints = dataPoints,
                completionByDate = completionByDate,
                onBackClick = {}
            )
        }
    }
}