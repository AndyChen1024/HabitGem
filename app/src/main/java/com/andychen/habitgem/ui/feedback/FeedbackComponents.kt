package com.andychen.habitgem.ui.feedback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.andychen.habitgem.R
import com.andychen.habitgem.domain.model.AnimationType
import com.andychen.habitgem.domain.model.FeedbackMessage
import com.andychen.habitgem.domain.model.FeedbackType
import com.andychen.habitgem.ui.theme.HabitGemTheme
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 反馈消息组件
 * 
 * 显示习惯完成后的反馈消息，包括文本和表情符号
 * 
 * @param message 反馈消息文本
 * @param emoji 表情符号
 * @param modifier 修饰符
 */
@Composable
fun FeedbackMessageComponent(
    message: String,
    emoji: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (emoji != null) {
            Text(
                text = emoji,
                fontSize = 24.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 反馈动画组件
 * 
 * 根据动画类型显示不同的动画效果
 * 
 * @param animationType 动画类型
 * @param modifier 修饰符
 */
@Composable
fun FeedbackAnimation(
    animationType: AnimationType,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(200.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when (animationType) {
            AnimationType.CONFETTI -> ConfettiAnimation()
            AnimationType.FIREWORKS -> FireworksAnimation()
            AnimationType.SPARKLE -> SparkleAnimation()
            AnimationType.THUMBS_UP -> ThumbsUpAnimation()
            AnimationType.NONE -> { /* No animation */ }
        }
    }
}

/**
 * 五彩纸屑动画
 */
@Composable
fun ConfettiAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti_progress"
    )
    
    val confettiColors = listOf(
        Color(0xFFFF5252), // Red
        Color(0xFFFFEB3B), // Yellow
        Color(0xFF4CAF50), // Green
        Color(0xFF2196F3), // Blue
        Color(0xFF9C27B0)  // Purple
    )
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        
        // Generate 30 confetti pieces
        repeat(30) { i ->
            val color = confettiColors[i % confettiColors.size]
            val x = Random.nextFloat() * canvasWidth
            val y = (animationProgress + i / 30f) % 1f * canvasHeight * 1.5f - 100f
            val width = Random.nextFloat() * 15f + 5f
            val height = Random.nextFloat() * 10f + 5f
            val rotation = Random.nextFloat() * 360f
            
            rotate(rotation) {
                drawRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(width, height)
                )
            }
        }
    }
}

/**
 * 烟花动画
 */
@Composable
fun FireworksAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "fireworks")
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fireworks_progress"
    )
    
    val fireworkColors = listOf(
        Color(0xFFFF5252), // Red
        Color(0xFFFFEB3B), // Yellow
        Color(0xFF4CAF50), // Green
        Color(0xFF2196F3), // Blue
        Color(0xFF9C27B0)  // Purple
    )
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val centerX = canvasWidth / 2
        val centerY = canvasHeight / 2
        
        // Draw 3 fireworks with different timings
        repeat(3) { fireworkIndex ->
            val fireworkProgress = (animationProgress + fireworkIndex * 0.33f) % 1f
            val fireworkColor = fireworkColors[(fireworkIndex * 2) % fireworkColors.size]
            
            if (fireworkProgress < 0.7f) {
                val explosionProgress = fireworkProgress / 0.7f
                val particleCount = 20
                
                // Draw explosion particles
                repeat(particleCount) { i ->
                    val angle = (i.toFloat() / particleCount) * 2 * PI.toFloat()
                    val distance = explosionProgress * 100f
                    val x = centerX + cos(angle) * distance
                    val y = centerY + sin(angle) * distance
                    val alpha = 1f - explosionProgress
                    
                    drawCircle(
                        color = fireworkColor.copy(alpha = alpha),
                        radius = (1f - explosionProgress) * 5f + 2f,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}

/**
 * 闪光动画
 */
@Composable
fun SparkleAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "sparkle")
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkle_progress"
    )
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val centerX = canvasWidth / 2
        val centerY = canvasHeight / 2
        
        // Draw main star
        val mainStarRadius = 40f
        drawCircle(
            color = Color(0xFFFFC107),
            radius = mainStarRadius,
            center = Offset(centerX, centerY)
        )
        
        // Draw sparkles
        repeat(12) { i ->
            val angle = (i.toFloat() / 12) * 2 * PI.toFloat()
            val pulseOffset = sin((animationProgress * 2 * PI + i * 0.5).toFloat()) * 20f
            val distance = 60f + pulseOffset
            
            val x = centerX + cos(angle) * distance
            val y = centerY + sin(angle) * distance
            val sparkleSize = 5f + sin((animationProgress * 2 * PI + i).toFloat()) * 3f
            
            drawCircle(
                color = Color(0xFFFFD54F),
                radius = sparkleSize,
                center = Offset(x, y)
            )
        }
    }
}

/**
 * 点赞动画
 */
@Composable
fun ThumbsUpAnimation() {
    var animationPlayed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (animationPlayed) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "thumbs_up_scale"
    )
    
    LaunchedEffect(key1 = true) {
        animationPlayed = true
        delay(300)
        animationPlayed = false
    }
    
    Box(
        modifier = Modifier
            .scale(scale)
            .size(80.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_thumbs_up),
            contentDescription = "Thumbs Up",
            tint = Color.White,
            modifier = Modifier.size(48.dp)
        )
    }
}

/**
 * 反馈对话框
 * 
 * 显示习惯完成后的反馈对话框，包括反馈消息和动画
 * 
 * @param feedbackMessage 反馈消息
 * @param onDismiss 关闭对话框的回调
 * @param showAnimation 是否显示动画
 */
@Composable
fun FeedbackDialog(
    feedbackMessage: FeedbackMessage,
    onDismiss: () -> Unit,
    showAnimation: Boolean = true
) {
    var showContent by remember { mutableStateOf(false) }
    
    LaunchedEffect(key1 = feedbackMessage) {
        showContent = true
    }
    
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
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title based on feedback type
                Text(
                    text = when (feedbackMessage.type) {
                        FeedbackType.COMPLETION -> "习惯完成！"
                        FeedbackType.STREAK -> "连续打卡！"
                        FeedbackType.MILESTONE -> "里程碑达成！"
                        FeedbackType.MISSED -> "别担心"
                        else -> "反馈"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Animation
                AnimatedVisibility(
                    visible = showContent && showAnimation,
                    enter = fadeIn(tween(300)) + scaleIn(tween(300)),
                    exit = fadeOut(tween(300)) + scaleOut(tween(300))
                ) {
                    feedbackMessage.animationType?.let { animationType ->
                        FeedbackAnimation(
                            animationType = animationType,
                            modifier = Modifier
                                .height(180.dp)
                                .fillMaxWidth()
                        )
                    }
                }
                
                // Message
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(500)) + expandIn(tween(500, easing = FastOutSlowInEasing)),
                    exit = fadeOut(tween(300)) + shrinkOut(tween(300))
                ) {
                    FeedbackMessageComponent(
                        message = feedbackMessage.message,
                        emoji = feedbackMessage.emoji,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Dismiss button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(48.dp)
                ) {
                    Text(text = "继续")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * 内联反馈消息
 * 
 * 在界面中内联显示的反馈消息，不使用对话框
 * 
 * @param feedbackMessage 反馈消息
 * @param modifier 修饰符
 */
@Composable
fun InlineFeedbackMessage(
    feedbackMessage: FeedbackMessage,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (feedbackMessage.type) {
        FeedbackType.COMPLETION -> MaterialTheme.colorScheme.primaryContainer
        FeedbackType.STREAK -> MaterialTheme.colorScheme.secondaryContainer
        FeedbackType.MILESTONE -> MaterialTheme.colorScheme.tertiaryContainer
        FeedbackType.MISSED -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    
    val textColor = when (feedbackMessage.type) {
        FeedbackType.COMPLETION -> MaterialTheme.colorScheme.onPrimaryContainer
        FeedbackType.STREAK -> MaterialTheme.colorScheme.onSecondaryContainer
        FeedbackType.MILESTONE -> MaterialTheme.colorScheme.onTertiaryContainer
        FeedbackType.MISSED -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji or icon
            if (feedbackMessage.emoji != null) {
                Text(
                    text = feedbackMessage.emoji,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
            
            // Message
            Text(
                text = feedbackMessage.message,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FeedbackDialogPreview() {
    HabitGemTheme {
        Surface {
            FeedbackDialog(
                feedbackMessage = FeedbackMessage(
                    message = "恭喜您连续完成这个习惯7天了！坚持就是胜利！",
                    type = FeedbackType.STREAK,
                    emoji = "🔥",
                    animationType = AnimationType.SPARKLE
                ),
                onDismiss = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InlineFeedbackMessagePreview() {
    HabitGemTheme {
        Surface {
            Column {
                InlineFeedbackMessage(
                    feedbackMessage = FeedbackMessage(
                        message = "做得好！您已经完成了今天的晨间冥想。",
                        type = FeedbackType.COMPLETION,
                        emoji = "👍",
                        animationType = AnimationType.THUMBS_UP
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                InlineFeedbackMessage(
                    feedbackMessage = FeedbackMessage(
                        message = "太棒了！您已经连续7天完成这个习惯了！",
                        type = FeedbackType.STREAK,
                        emoji = "🔥",
                        animationType = AnimationType.SPARKLE
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                InlineFeedbackMessage(
                    feedbackMessage = FeedbackMessage(
                        message = "恭喜达成30天里程碑！您的习惯已经形成！",
                        type = FeedbackType.MILESTONE,
                        emoji = "🏆",
                        animationType = AnimationType.FIREWORKS
                    )
                )
            }
        }
    }
}