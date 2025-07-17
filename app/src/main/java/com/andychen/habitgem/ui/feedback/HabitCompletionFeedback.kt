package com.andychen.habitgem.ui.feedback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.andychen.habitgem.domain.model.FeedbackMessage
import com.andychen.habitgem.domain.model.FeedbackType
import kotlinx.coroutines.delay

/**
 * 习惯完成反馈容器
 * 
 * 这个组件用于在习惯完成时显示反馈，可以选择使用对话框或内联消息
 * 
 * @param feedbackMessage 反馈消息，如果为null则不显示反馈
 * @param onDismiss 关闭反馈的回调
 * @param useDialog 是否使用对话框显示反馈
 * @param autoHideDelay 自动隐藏内联消息的延迟时间（毫秒），仅在useDialog为false时有效
 * @param content 内容
 */
@Composable
fun HabitCompletionFeedbackContainer(
    feedbackMessage: FeedbackMessage?,
    onDismiss: () -> Unit,
    useDialog: Boolean = true,
    autoHideDelay: Long = 3000,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Main content
        content()
        
        // Feedback
        if (feedbackMessage != null) {
            if (useDialog) {
                FeedbackDialog(
                    feedbackMessage = feedbackMessage,
                    onDismiss = onDismiss
                )
            } else {
                // Auto-hide inline feedback after delay
                var showFeedback by remember { mutableStateOf(true) }
                
                LaunchedEffect(feedbackMessage) {
                    showFeedback = true
                    delay(autoHideDelay)
                    showFeedback = false
                    onDismiss()
                }
                
                AnimatedVisibility(
                    visible = showFeedback,
                    enter = fadeIn(tween(300)) + slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(300)
                    ),
                    exit = fadeOut(tween(300)) + slideOutVertically(
                        targetOffsetY = { -it },
                        animationSpec = tween(300)
                    ),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .zIndex(10f)
                ) {
                    InlineFeedbackMessage(feedbackMessage = feedbackMessage)
                }
            }
        }
    }
}

/**
 * 习惯完成反馈显示器
 * 
 * 这个组件用于在习惯完成时显示反馈，根据反馈类型选择合适的显示方式
 * 
 * @param feedbackMessage 反馈消息
 * @param onDismiss 关闭反馈的回调
 */
@Composable
fun HabitCompletionFeedback(
    feedbackMessage: FeedbackMessage,
    onDismiss: () -> Unit
) {
    // 根据反馈类型选择显示方式
    // 里程碑和连续打卡使用对话框，普通完成使用内联消息
    val useDialog = when (feedbackMessage.type) {
        FeedbackType.MILESTONE, FeedbackType.STREAK -> true
        else -> false
    }
    
    if (useDialog) {
        FeedbackDialog(
            feedbackMessage = feedbackMessage,
            onDismiss = onDismiss
        )
    } else {
        var showFeedback by remember { mutableStateOf(true) }
        
        LaunchedEffect(feedbackMessage) {
            showFeedback = true
            delay(3000) // 显示3秒
            showFeedback = false
            onDismiss()
        }
        
        AnimatedVisibility(
            visible = showFeedback,
            enter = fadeIn(tween(300)) + slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(300)
            ),
            exit = fadeOut(tween(300)) + slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(300)
            )
        ) {
            InlineFeedbackMessage(feedbackMessage = feedbackMessage)
        }
    }
}

/**
 * 习惯完成动画显示器
 * 
 * 这个组件用于在习惯完成时显示全屏动画效果
 * 
 * @param feedbackMessage 反馈消息
 * @param onAnimationComplete 动画完成的回调
 * @param showDuration 动画显示时间（毫秒）
 */
@Composable
fun HabitCompletionAnimation(
    feedbackMessage: FeedbackMessage,
    onAnimationComplete: () -> Unit,
    showDuration: Long = 2000
) {
    var showAnimation by remember { mutableStateOf(true) }
    
    LaunchedEffect(feedbackMessage) {
        showAnimation = true
        delay(showDuration)
        showAnimation = false
        onAnimationComplete()
    }
    
    AnimatedVisibility(
        visible = showAnimation,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(300))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            feedbackMessage.animationType?.let { animationType ->
                FeedbackAnimation(
                    animationType = animationType,
                    modifier = Modifier.size(200.dp)
                )
            }
        }
    }
}