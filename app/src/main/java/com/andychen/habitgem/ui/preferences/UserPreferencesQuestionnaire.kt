package com.andychen.habitgem.ui.preferences

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.andychen.habitgem.domain.model.GoalType
import com.andychen.habitgem.domain.model.HabitCategory
import com.andychen.habitgem.domain.model.TimeSlot
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Main entry point for the user preferences questionnaire
 */
@Composable
fun UserPreferencesQuestionnaire(
    viewModel: UserPreferencesViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    if (uiState.isComplete) {
        LaunchedEffect(key1 = true) {
            onComplete()
        }
    }
    
    Scaffold(
        topBar = {
            QuestionnaireTopBar(
                currentStep = uiState.currentStep,
                onBackClick = { viewModel.previousStep() },
                showBackButton = uiState.currentStep != QuestionnaireStep.CATEGORIES
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Progress indicator
                    LinearProgressIndicator(
                        progress = { (uiState.currentStep.ordinal + 1) / QuestionnaireStep.values().size.toFloat() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Content based on current step
                    Box(modifier = Modifier.weight(1f)) {
                        QuestionnaireContent(
                            uiState = uiState,
                            viewModel = viewModel
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Navigation buttons
                    QuestionnaireNavigation(
                        currentStep = uiState.currentStep,
                        canProceed = viewModel.canProceedFromCurrentStep(),
                        onNext = { viewModel.nextStep() },
                        onSave = { viewModel.savePreferences() },
                        isSaving = uiState.isSaving
                    )
                }
            }
        }
    }
}

/**
 * Top app bar for the questionnaire
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionnaireTopBar(
    currentStep: QuestionnaireStep,
    onBackClick: () -> Unit,
    showBackButton: Boolean
) {
    TopAppBar(
        title = {
            Text(
                text = when (currentStep) {
                    QuestionnaireStep.CATEGORIES -> "选择习惯类别"
                    QuestionnaireStep.GOALS -> "设置目标类型"
                    QuestionnaireStep.DIFFICULTY -> "设置难度偏好"
                    QuestionnaireStep.REMINDERS -> "设置提醒"
                    QuestionnaireStep.TIME_AVAILABILITY -> "设置可用时间"
                    QuestionnaireStep.SUMMARY -> "确认偏好"
                }
            )
        },
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            }
        }
    )
}

/**
 * Content for each step of the questionnaire
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun QuestionnaireContent(
    uiState: UserPreferencesUiState,
    viewModel: UserPreferencesViewModel
) {
    AnimatedContent(
        targetState = uiState.currentStep,
        transitionSpec = {
            if (targetState.ordinal > initialState.ordinal) {
                slideInHorizontally(animationSpec = tween(300)) { width -> width } + 
                fadeIn(animationSpec = tween(300)) togetherWith 
                slideOutHorizontally(animationSpec = tween(300)) { width -> -width } + 
                fadeOut(animationSpec = tween(300))
            } else {
                slideInHorizontally(animationSpec = tween(300)) { width -> -width } + 
                fadeIn(animationSpec = tween(300)) togetherWith 
                slideOutHorizontally(animationSpec = tween(300)) { width -> width } + 
                fadeOut(animationSpec = tween(300))
            }
        },
        label = "QuestionnaireStepTransition"
    ) { step ->
        when (step) {
            QuestionnaireStep.CATEGORIES -> CategoriesStep(
                selectedCategories = uiState.selectedCategories,
                onCategoryToggle = { viewModel.toggleCategory(it) }
            )
            QuestionnaireStep.GOALS -> GoalsStep(
                selectedGoalTypes = uiState.selectedGoalTypes,
                onGoalTypeToggle = { viewModel.toggleGoalType(it) }
            )
            QuestionnaireStep.DIFFICULTY -> DifficultyStep(
                difficultyPreference = uiState.difficultyPreference,
                onDifficultyChange = { viewModel.updateDifficultyPreference(it) }
            )
            QuestionnaireStep.REMINDERS -> RemindersStep(
                reminderEnabled = uiState.reminderEnabled,
                reminderTime = uiState.reminderTime,
                vibrationEnabled = uiState.vibrationEnabled,
                reminderSound = uiState.reminderSound,
                onReminderSettingsChange = { enabled, time, sound, vibration ->
                    viewModel.updateReminderSettings(enabled, time, sound, vibration)
                }
            )
            QuestionnaireStep.TIME_AVAILABILITY -> TimeAvailabilityStep(
                timeAvailability = uiState.timeAvailability,
                onTimeAvailabilityChange = { day, slots ->
                    viewModel.updateTimeAvailability(day, slots)
                }
            )
            QuestionnaireStep.SUMMARY -> SummaryStep(uiState = uiState)
        }
    }
}

/**
 * Navigation buttons for the questionnaire
 */
@Composable
fun QuestionnaireNavigation(
    currentStep: QuestionnaireStep,
    canProceed: Boolean,
    onNext: () -> Unit,
    onSave: () -> Unit,
    isSaving: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        if (currentStep == QuestionnaireStep.SUMMARY) {
            Button(
                onClick = onSave,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("保存偏好设置")
                }
            }
        } else {
            Button(
                onClick = onNext,
                enabled = canProceed,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("下一步")
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

/**
 * Step 1: Select habit categories
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoriesStep(
    selectedCategories: List<HabitCategory>,
    onCategoryToggle: (HabitCategory) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "您对哪些类型的习惯感兴趣？",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "请选择您感兴趣的习惯类别，这将帮助我们为您推荐合适的习惯。",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HabitCategory.values().forEach { category ->
                CategoryChip(
                    category = category,
                    selected = selectedCategories.contains(category),
                    onToggle = { onCategoryToggle(category) }
                )
            }
        }
        
        if (selectedCategories.isEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "请至少选择一个类别",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * Step 2: Select goal types
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalsStep(
    selectedGoalTypes: List<GoalType>,
    onGoalTypeToggle: (GoalType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "您希望通过养成习惯达成什么目标？",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "请选择您希望通过养成习惯达成的目标类型，这将帮助我们为您提供更精准的建议。",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GoalType.values().forEach { goalType ->
                GoalChip(
                    goalType = goalType,
                    selected = selectedGoalTypes.contains(goalType),
                    onToggle = { onGoalTypeToggle(goalType) }
                )
            }
        }
        
        if (selectedGoalTypes.isEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "请至少选择一个目标类型",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * Step 3: Set difficulty preference
 */
@Composable
fun DifficultyStep(
    difficultyPreference: Int,
    onDifficultyChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "您偏好什么难度的习惯？",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "请选择您偏好的习惯难度，这将帮助我们为您推荐合适的习惯。",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        Slider(
            value = difficultyPreference.toFloat(),
            onValueChange = { onDifficultyChange(it.toInt()) },
            valueRange = 1f..5f,
            steps = 3,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "简单",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "适中",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "挑战",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = when (difficultyPreference) {
                        1 -> "简单习惯"
                        2 -> "较简单习惯"
                        3 -> "适中难度习惯"
                        4 -> "较有挑战习惯"
                        else -> "高挑战习惯"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = when (difficultyPreference) {
                        1 -> "适合初学者，容易坚持，如每天喝水、深呼吸等。"
                        2 -> "稍有挑战但仍易于坚持，如每天步行10分钟、阅读5页书等。"
                        3 -> "平衡挑战与可行性，如每天冥想15分钟、学习新语言20分钟等。"
                        4 -> "需要一定毅力，如每天锻炼30分钟、写作500字等。"
                        else -> "高度挑战，需要强大的意志力，如每天跑步5公里、学习编程1小时等。"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * Step 4: Set reminder preferences
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersStep(
    reminderEnabled: Boolean,
    reminderTime: LocalTime,
    vibrationEnabled: Boolean,
    reminderSound: String,
    onReminderSettingsChange: (Boolean?, LocalTime?, String?, Boolean?) -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "您希望如何接收提醒？",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "设置提醒可以帮助您更好地坚持习惯。",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (reminderEnabled) 
                                Icons.Default.NotificationsActive 
                            else 
                                Icons.Default.NotificationsOff,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                        Text(
                            text = "启用提醒",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { onReminderSettingsChange(it, null, null, null) }
                    )
                }
                
                AnimatedVisibility(visible = reminderEnabled) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTimePicker = true },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                                Text(
                                    text = "默认提醒时间",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            
                            Text(
                                text = reminderTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "震动",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            
                            Switch(
                                checked = vibrationEnabled,
                                onCheckedChange = { onReminderSettingsChange(null, null, null, it) }
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "提示：您可以在创建习惯时为每个习惯单独设置提醒时间。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
    
    if (showTimePicker) {
        val timePickerState = remember { 
            TimePickerState(
                initialHour = reminderTime.hour,
                initialMinute = reminderTime.minute,
                is24Hour = true
            )
        }
        
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = MaterialTheme.shapes.large,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "选择提醒时间",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    TimePicker(state = timePickerState)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("取消")
                        }
                        
                        TextButton(
                            onClick = {
                                onReminderSettingsChange(
                                    null,
                                    LocalTime.of(timePickerState.hour, timePickerState.minute),
                                    null,
                                    null
                                )
                                showTimePicker = false
                            }
                        ) {
                            Text("确定")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Step 5: Set time availability
 */
@Composable
fun TimeAvailabilityStep(
    timeAvailability: Map<DayOfWeek, List<TimeSlot>>,
    onTimeAvailabilityChange: (DayOfWeek, List<TimeSlot>) -> Unit
) {
    // This is a simplified version. In a real app, you would want a more sophisticated UI
    // for selecting time slots for each day of the week.
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "您在一周中什么时间有空？",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "请选择您在一周中有空的时间段，这将帮助我们为您安排合适的习惯时间。",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // For simplicity, we'll just show the current selections
        // In a real app, you would implement a more interactive UI
        DayOfWeek.values().forEach { day ->
            val slots = timeAvailability[day] ?: emptyList()
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = day.getDisplayName(TextStyle.FULL, Locale.CHINESE),
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    if (slots.isNotEmpty()) {
                        Text(
                            text = "${slots.size}个时间段",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = "无可用时间",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "注意：这是一个简化版的时间选择界面。在实际应用中，您可以为每天设置具体的时间段。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

/**
 * Step 6: Summary of selected preferences
 */
@Composable
fun SummaryStep(uiState: UserPreferencesUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "您的偏好设置摘要",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "请确认以下偏好设置，这些设置将用于为您推荐合适的习惯。",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Categories
        SummarySection(
            title = "习惯类别",
            content = uiState.selectedCategories.joinToString(", ") { 
                getCategoryDisplayName(it)
            }
        )
        
        // Goals
        SummarySection(
            title = "目标类型",
            content = uiState.selectedGoalTypes.joinToString(", ") { 
                getGoalTypeDisplayName(it)
            }
        )
        
        // Difficulty
        SummarySection(
            title = "难度偏好",
            content = when (uiState.difficultyPreference) {
                1 -> "简单习惯"
                2 -> "较简单习惯"
                3 -> "适中难度习惯"
                4 -> "较有挑战习惯"
                else -> "高挑战习惯"
            }
        )
        
        // Reminders
        SummarySection(
            title = "提醒设置",
            content = if (uiState.reminderEnabled) {
                "已启用，默认时间：${uiState.reminderTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"
            } else {
                "未启用"
            }
        )
        
        // Time availability
        SummarySection(
            title = "可用时间",
            content = "${uiState.timeAvailability.values.sumOf { it.size }}个时间段"
        )
    }
}

/**
 * Helper function to get display name for habit category
 */
fun getCategoryDisplayName(category: HabitCategory): String {
    return when (category) {
        HabitCategory.HEALTH -> "健康"
        HabitCategory.FITNESS -> "健身"
        HabitCategory.MINDFULNESS -> "正念"
        HabitCategory.PRODUCTIVITY -> "生产力"
        HabitCategory.LEARNING -> "学习"
        HabitCategory.SOCIAL -> "社交"
        HabitCategory.CREATIVITY -> "创造力"
        HabitCategory.FINANCE -> "财务"
        HabitCategory.OTHER -> "其他"
    }
}

/**
 * Helper function to get display name for goal type
 */
fun getGoalTypeDisplayName(goalType: GoalType): String {
    return when (goalType) {
        GoalType.HEALTH_IMPROVEMENT -> "改善健康"
        GoalType.SKILL_DEVELOPMENT -> "技能发展"
        GoalType.PRODUCTIVITY_BOOST -> "提高生产力"
        GoalType.STRESS_REDUCTION -> "减轻压力"
        GoalType.RELATIONSHIP_BUILDING -> "建立关系"
        GoalType.PERSONAL_GROWTH -> "个人成长"
        GoalType.OTHER -> "其他"
    }
}

/**
 * Summary section component
 */
@Composable
fun SummarySection(
    title: String,
    content: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * Category selection chip
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryChip(
    category: HabitCategory,
    selected: Boolean,
    onToggle: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onToggle,
        label = { 
            Text(getCategoryDisplayName(category))
        },
        leadingIcon = {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null
                )
            }
        }
    )
}

/**
 * Goal type selection chip
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalChip(
    goalType: GoalType,
    selected: Boolean,
    onToggle: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onToggle,
        label = { 
            Text(getGoalTypeDisplayName(goalType))
        },
        leadingIcon = {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null
                )
            }
        }
    )
}