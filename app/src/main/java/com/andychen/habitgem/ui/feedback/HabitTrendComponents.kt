package com.andychen.habitgem.ui.feedback

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andychen.habitgem.domain.model.DataPoint
import com.andychen.habitgem.ui.theme.HabitGemTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * 习惯趋势图表
 * 
 * 显示习惯完成率的趋势线图
 * 
 * @param dataPoints 数据点列表
 * @param modifier 修饰符
 * @param onPointSelected 点击数据点的回调
 */
@Composable
fun HabitTrendChart(
    dataPoints: List<DataPoint>,
    modifier: Modifier = Modifier,
    onPointSelected: (DataPoint) -> Unit = {}
) {
    if (dataPoints.isEmpty()) {
        return
    }
    
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 绘制图表
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        // 计算点击位置最近的数据点
                        val width = size.width
                        val pointSpacing = width / (dataPoints.size - 1)
                        val index = (offset.x / pointSpacing).roundToInt()
                        
                        if (index in dataPoints.indices) {
                            selectedPointIndex = index
                            onPointSelected(dataPoints[index])
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val pointSpacing = width / (dataPoints.size - 1)
            
            // 绘制网格线
            val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            
            // 水平网格线
            for (i in 0..4) {
                val y = height * (1 - i / 4f)
                drawLine(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashPathEffect
                )
            }
            
            // 绘制趋势线
            val path = Path()
            val points = dataPoints.mapIndexed { index, dataPoint ->
                Offset(
                    x = index * pointSpacing,
                    y = height * (1 - dataPoint.value)
                )
            }
            
            // 移动到第一个点
            if (points.isNotEmpty()) {
                path.moveTo(points.first().x, points.first().y)
                
                // 连接所有点
                for (i in 1 until points.size) {
                    path.lineTo(points[i].x, points[i].y)
                }
                
                // 绘制路径
                drawPath(
                    path = path,
                    color = MaterialTheme.colorScheme.primary,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
                
                // 绘制渐变填充
                val fillPath = Path().apply {
                    // 复制趋势线路径
                    addPath(path)
                    // 添加闭合路径
                    lineTo(points.last().x, height)
                    lineTo(points.first().x, height)
                    close()
                }
                
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.0f)
                        )
                    )
                )
                
                // 绘制数据点
                points.forEachIndexed { index, point ->
                    val isSelected = index == selectedPointIndex
                    
                    // 绘制点
                    drawCircle(
                        color = if (isSelected) MaterialTheme.colorScheme.primary 
                               else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                        center = point
                    )
                    
                    // 如果选中，绘制外圈
                    if (isSelected) {
                        drawCircle(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            radius = 12.dp.toPx(),
                            center = point
                        )
                    }
                }
            }
        }
        
        // 绘制日期标签
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 只显示第一个、中间和最后一个日期
            val indicesToShow = listOf(0, dataPoints.size / 2, dataPoints.size - 1)
                .filter { it in dataPoints.indices }
                .distinct()
            
            indicesToShow.forEach { index ->
                val dataPoint = dataPoints[index]
                val position = index.toFloat() / (dataPoints.size - 1)
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            start = if (index == 0) 0.dp else 4.dp,
                            end = if (index == dataPoints.size - 1) 0.dp else 4.dp
                        ),
                    contentAlignment = when {
                        index == 0 -> Alignment.BottomStart
                        index == dataPoints.size - 1 -> Alignment.BottomEnd
                        else -> Alignment.BottomCenter
                    }
                ) {
                    Text(
                        text = dataPoint.label ?: dataPoint.date.format(DateTimeFormatter.ofPattern("MM/dd")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // 显示选中的数据点信息
        selectedPointIndex?.let { index ->
            val dataPoint = dataPoints[index]
            
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 4.dp
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = dataPoint.date.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Text(
                            text = "${(dataPoint.value * 100).roundToInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HabitTrendChartPreview() {
    HabitGemTheme {
        Surface {
            HabitTrendChart(
                dataPoints = listOf(
                    DataPoint(LocalDate.now().minusDays(30), 0.5f, "6/1"),
                    DataPoint(LocalDate.now().minusDays(25), 0.6f, "6/5"),
                    DataPoint(LocalDate.now().minusDays(20), 0.4f, "6/10"),
                    DataPoint(LocalDate.now().minusDays(15), 0.7f, "6/15"),
                    DataPoint(LocalDate.now().minusDays(10), 0.8f, "6/20"),
                    DataPoint(LocalDate.now().minusDays(5), 0.9f, "6/25"),
                    DataPoint(LocalDate.now(), 0.85f, "6/30")
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(16.dp)
            )
        }
    }
}