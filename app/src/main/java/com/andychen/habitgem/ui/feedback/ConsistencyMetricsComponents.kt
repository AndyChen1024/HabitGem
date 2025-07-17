package com.andychen.habitgem.ui.feedback

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andychen.habitgem.R
import com.andychen.habitgem.domain.model.Trend
import com.andychen.habitgem.ui.theme.HabitGemTheme
import kotlin.math.roundToInt

/**
 * 一致性指标组件
 * 
 * 显示习惯的一致性指标
 * 
 * @param consistencyScore 一致性分数 (0.0-1.0)
 * @param trend 趋势
 * @param modifier 修饰符
 */
@Composable
fun ConsistencyMetricCard(
    consistencyScore: Float,
    trend: Trend,
    modifier: Modifier = Modifier
) {
    val animatedConsistencyScore = remember { Animatable(0f) }
    
    LaunchedEffect(consistencyScore) {
        animatedConsistencyScore.animateTo(
            targetValue = consistencyScore,
            animationSpec = tween(1000, easing = FastOutSlowInEasing)
        )
    }
    
    Card(
        modifier = modifier,
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "习惯一致性",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 一致性仪表盘
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                // 绘制仪表盘
                Canvas(modifier = Modifier.fillMaxWidth()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.width / 2 - 16.dp.toPx()
                    
                    // 绘制背景圆弧
                    drawArc(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                    )
                    
                    // 绘制进度圆弧
                    drawArc(
                        color = when {
                            animatedConsistencyScore.value >= 0.8f -> Color(0xFF1E8E3E) // 深绿
                            animatedConsistencyScore.value >= 0.6f -> Color(0xFF34A853) // 绿色
                            animatedConsistencyScore.value >= 0.4f -> Color(0xFFFBBC04) // 黄色
                            else -> Color(0xFFEA4335) // 红色
                        },
                        startAngle = 135f,
                        sweepAngle = 270f * animatedConsistencyScore.value,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                    )
                    
                    // 绘制刻度
                    for (i in 0..10) {
                        val angle = 135f + 270f * (i / 10f)
                        val angleInRadians = Math.toRadians(angle.toDouble())
                        val startRadius = radius - 32.dp.toPx()
                        val endRadius = if (i % 5 == 0) radius - 16.dp.toPx() else radius - 24.dp.toPx()
                        
                        val startX = center.x + (startRadius * kotlin.math.cos(angleInRadians)).toFloat()
                        val startY = center.y + (startRadius * kotlin.math.sin(angleInRadians)).toFloat()
                        val endX = center.x + (endRadius * kotlin.math.cos(angleInRadians)).toFloat()
                        val endY = center.y + (endRadius * kotlin.math.sin(angleInRadians)).toFloat()
                        
                        drawLine(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = if (i % 5 == 0) 2.dp.toPx() else 1.dp.toPx()
                        )
                    }
                }
                
                // 显示分数
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${(animatedConsistencyScore.value * 100).roundToInt()}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "分",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 趋势指示器
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = when (trend) {
                        Trend.IMPROVING -> painterResource(id = R.drawable.ic_thumbs_up)
                        Trend.DECLINING -> painterResource(id = R.drawable.ic_thumbs_up) // 应替换为向下图标
                        else -> painterResource(id = R.drawable.ic_thumbs_up) // 应替换为水平图标
                    },
                    contentDescription = "趋势",
                    tint = when (trend) {
                        Trend.IMPROVING -> Color(0xFF1E8E3E)
                        Trend.DECLINING -> Color(0xFFEA4335)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = when (trend) {
                        Trend.IMPROVING -> "上升趋势"
                        Trend.DECLINING -> "下降趋势"
                        Trend.STABLE -> "保持稳定"
                        Trend.FLUCTUATING -> "波动较大"
                        Trend.NOT_ENOUGH_DATA -> "数据不足"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (trend) {
                        Trend.IMPROVING -> Color(0xFF1E8E3E)
                        Trend.DECLINING -> Color(0xFFEA4335)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 一致性评级
            Text(
                text = when {
                    consistencyScore >= 0.9f -> "卓越"
                    consistencyScore >= 0.8f -> "优秀"
                    consistencyScore >= 0.7f -> "良好"
                    consistencyScore >= 0.6f -> "中等"
                    consistencyScore >= 0.4f -> "有待提高"
                    else -> "需要关注"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = when {
                    consistencyScore >= 0.8f -> Color(0xFF1E8E3E)
                    consistencyScore >= 0.6f -> Color(0xFF34A853)
                    consistencyScore >= 0.4f -> Color(0xFFFBBC04)
                    else -> Color(0xFFEA4335)
                }
            )
        }
    }
}

/**
 * 周期性表现比较组件
 * 
 * 比较不同时间段的习惯表现
 * 
 * @param currentPeriodRate 当前周期完成率
 * @param previousPeriodRate 上一周期完成率
 * @param periodType 周期类型 (如 "周", "月")
 * @param modifier 修饰符
 */
@Composable
fun PeriodComparisonCard(
    currentPeriodRate: Float,
    previousPeriodRate: Float,
    periodType: String,
    modifier: Modifier = Modifier
) {
    val difference = currentPeriodRate - previousPeriodRate
    val percentDifference = if (previousPeriodRate > 0) {
        (difference / previousPeriodRate * 100).roundToInt()
    } else if (currentPeriodRate > 0) {
        100
    } else {
        0
    }
    
    Card(
        modifier = modifier,
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
                text = "周期比较",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 当前周期
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "本$periodType",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "${(currentPeriodRate * 100).roundToInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // 上一周期
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "上$periodType",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "${(previousPeriodRate * 100).roundToInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 变化指示器
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = when {
                        difference > 0 -> painterResource(id = R.drawable.ic_thumbs_up) // 应替换为向上图标
                        difference < 0 -> painterResource(id = R.drawable.ic_thumbs_up) // 应替换为向下图标
                        else -> painterResource(id = R.drawable.ic_thumbs_up) // 应替换为水平图标
                    },
                    contentDescription = "变化",
                    tint = when {
                        difference > 0 -> Color(0xFF1E8E3E)
                        difference < 0 -> Color(0xFFEA4335)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = when {
                        difference > 0 -> "提升 $percentDifference%"
                        difference < 0 -> "下降 ${-percentDifference}%"
                        else -> "持平"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        difference > 0 -> Color(0xFF1E8E3E)
                        difference < 0 -> Color(0xFFEA4335)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ConsistencyMetricCardPreview() {
    HabitGemTheme {
        Surface {
            ConsistencyMetricCard(
                consistencyScore = 0.75f,
                trend = Trend.IMPROVING,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PeriodComparisonCardPreview() {
    HabitGemTheme {
        Surface {
            PeriodComparisonCard(
                currentPeriodRate = 0.8f,
                previousPeriodRate = 0.65f,
                periodType = "周",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}