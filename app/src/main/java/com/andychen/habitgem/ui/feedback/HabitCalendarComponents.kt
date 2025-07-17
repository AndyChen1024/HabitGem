package com.andychen.habitgem.ui.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andychen.habitgem.ui.theme.HabitGemTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * 习惯表现日历组件
 * 
 * 显示月视图的习惯完成情况
 * 
 * @param dataByDate 日期到完成情况的映射 (true=完成, false=未完成)
 * @param month 月份 (1-12)
 * @param year 年份
 * @param modifier 修饰符
 * @param onDateSelected 点击日期的回调
 */
@Composable
fun HabitCalendarView(
    dataByDate: Map<LocalDate, Boolean>,
    month: Int,
    year: Int,
    modifier: Modifier = Modifier,
    onDateSelected: (LocalDate) -> Unit = {}
) {
    // 计算当月第一天和最后一天
    val firstDayOfMonth = LocalDate.of(year, month, 1)
    val lastDayOfMonth = firstDayOfMonth.withDayOfMonth(firstDayOfMonth.lengthOfMonth())
    
    // 计算日历需要显示的第一天（可能是上个月的）
    val firstDayOfCalendar = firstDayOfMonth.minusDays((firstDayOfMonth.dayOfWeek.value - 1).toLong())
    
    // 计算总共需要显示的周数
    val weeksToShow = (lastDayOfMonth.toEpochDay() - firstDayOfCalendar.toEpochDay()) / 7 + 1
    
    Column(modifier = modifier) {
        // 月份标题
        Text(
            text = "${year}年${month}月",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 星期标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // 日历网格
        repeat(weeksToShow.toInt()) { weekIndex ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(7) { dayIndex ->
                    val date = firstDayOfCalendar.plusDays((weekIndex * 7 + dayIndex).toLong())
                    val isCurrentMonth = date.month.value == month
                    val isCompleted = dataByDate[date] == true
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(2.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    !isCurrentMonth -> Color.Transparent
                                    isCompleted -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .clickable(enabled = isCurrentMonth) { onDateSelected(date) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                !isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                isCompleted -> MaterialTheme.colorScheme.onPrimary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 图例
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Text(
                text = "已完成",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Text(
                text = "未完成",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 习惯完成率热力图
 * 
 * 显示不同日期的习惯完成情况
 * 
 * @param dataByDate 日期到完成率的映射
 * @param startDate 开始日期
 * @param endDate 结束日期
 * @param modifier 修饰符
 * @param onDateSelected 点击日期的回调
 */
@Composable
fun HabitHeatmap(
    dataByDate: Map<LocalDate, Float>,
    startDate: LocalDate,
    endDate: LocalDate,
    modifier: Modifier = Modifier,
    onDateSelected: (LocalDate) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    
    Column(modifier = modifier) {
        // 月份标签
        val months = mutableSetOf<String>()
        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            months.add("${currentDate.year}年${currentDate.monthValue}月")
            currentDate = currentDate.plusMonths(1).withDayOfMonth(1)
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            months.forEach { month ->
                Text(
                    text = month,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // 星期标签
        Row(
            modifier = Modifier
                .padding(end = 8.dp, bottom = 4.dp)
        ) {
            // 星期标签列
            Column(
                modifier = Modifier.width(24.dp),
                horizontalAlignment = Alignment.End
            ) {
                DayOfWeek.values().forEach { day ->
                    Text(
                        text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .height(24.dp)
                            .padding(end = 4.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
            
            // 热力图
            Row(
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .padding(start = 4.dp)
            ) {
                // 计算需要显示的周数
                val totalDays = endDate.toEpochDay() - startDate.toEpochDay() + 1
                val weeks = (totalDays / 7 + 2).toInt() // 额外添加一些空间
                
                // 计算起始日期所在周的第一天（周日）
                var weekStart = startDate.minusDays(startDate.dayOfWeek.value % 7L)
                
                // 绘制每周的方块
                repeat(weeks) {
                    Column {
                        // 每周7天
                        repeat(7) { dayIndex ->
                            val date = weekStart.plusDays(dayIndex.toLong())
                            val completionRate = dataByDate[date] ?: 0f
                            
                            // 只显示在范围内的日期
                            if (date in startDate..endDate) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            getHeatmapColor(completionRate)
                                        )
                                        .clickable { onDateSelected(date) }
                                )
                            } else {
                                // 范围外的日期显示为空白
                                Spacer(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                    
                    // 移动到下一周
                    weekStart = weekStart.plusWeeks(1)
                }
            }
        }
        
        // 图例
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "完成率: ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // 热力图颜色图例
            listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { rate ->
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(getHeatmapColor(rate))
                )
            }
            
            Text(
                text = " 低 → 高",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 根据完成率获取热力图颜色
 */
private fun getHeatmapColor(completionRate: Float): Color {
    return when {
        completionRate >= 0.9f -> Color(0xFF1E8E3E) // 深绿
        completionRate >= 0.7f -> Color(0xFF34A853) // 绿色
        completionRate >= 0.5f -> Color(0xFF81C995) // 浅绿
        completionRate >= 0.3f -> Color(0xFFCEEAD6) // 非常浅的绿
        completionRate > 0f -> Color(0xFFE6F4EA) // 几乎白色的绿
        else -> Color(0xFFEEEEEE) // 灰色（未完成）
    }
}

@Preview(showBackground = true)
@Composable
fun HabitCalendarViewPreview() {
    HabitGemTheme {
        Surface {
            val today = LocalDate.now()
            val dataByDate = (1..28).associate {
                today.withDayOfMonth(it) to (it % 3 != 0)
            }
            
            HabitCalendarView(
                dataByDate = dataByDate,
                month = today.monthValue,
                year = today.year,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HabitHeatmapPreview() {
    HabitGemTheme {
        Surface {
            val today = LocalDate.now()
            val startDate = today.minusMonths(2).withDayOfMonth(1)
            val endDate = today
            
            val dataByDate = mutableMapOf<LocalDate, Float>()
            var currentDate = startDate
            
            while (!currentDate.isAfter(endDate)) {
                dataByDate[currentDate] = when {
                    currentDate.dayOfWeek == DayOfWeek.SATURDAY || currentDate.dayOfWeek == DayOfWeek.SUNDAY -> 0.9f
                    currentDate.dayOfMonth % 5 == 0 -> 0.7f
                    currentDate.dayOfMonth % 3 == 0 -> 0.5f
                    currentDate.dayOfMonth % 7 == 0 -> 0.3f
                    else -> 0.1f
                }
                currentDate = currentDate.plusDays(1)
            }
            
            HabitHeatmap(
                dataByDate = dataByDate,
                startDate = startDate,
                endDate = endDate,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}