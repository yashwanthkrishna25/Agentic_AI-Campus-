package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.ui.responsive.LocalWindowAdaptiveInfo
import com.example.ui.theme.*
import com.example.util.PomodoroSoundHelper
import com.example.util.PomodoroTone
import kotlinx.coroutines.delay

enum class PomodoroMode(val label: String, val defaultMinutes: Int, val accentColor: Color) {
    FOCUS("Focus Session", 25, CyberEmerald),
    SHORT_BREAK("Short Break", 5, ElectricIndigo),
    LONG_BREAK("Deep Rest", 15, LuminousAmber)
}

data class PomodoroTask(
    val id: String,
    val title: String,
    val subject: String,
    val isCompleted: Boolean = false
)

/**
 * Dedicated Pomodoro Study Session Screen.
 * Opened directly when a student taps "Pomodoro Study Sessions" from the navigation drawer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(
    onNavigateBack: () -> Unit,
    isDarkMode: Boolean = true
) {
    val context = LocalContext.current
    val adaptiveInfo = LocalWindowAdaptiveInfo.current
    val horizPadding = if (adaptiveInfo.isSmallScreen) 10.dp else 16.dp
    var activeMode by remember { mutableStateOf(PomodoroMode.FOCUS) }
    var selectedDurationMinutes by remember { mutableIntStateOf(25) }
    var timeRemainingSeconds by remember { mutableIntStateOf(25 * 60) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var completedSessionsCount by remember { mutableIntStateOf(2) }
    var selectedSubject by remember { mutableStateOf("Deep Learning [2339609]") }
    var stopwatchSoundActive by remember { mutableStateOf(true) }

    val studyTasks = remember {
        mutableStateListOf(
            PomodoroTask("1", "Revise Backpropagation & Loss Gradients", "Deep Learning", true),
            PomodoroTask("2", "Solve 5 Mid-Term Question Bank Problems", "Cyber Security", false),
            PomodoroTask("3", "Review Prompt Engineering In-Context Few-Shot Templates", "Prompt Engineering", false)
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            PomodoroSoundHelper.stopAmbientFocusSound()
        }
    }

    // Timer countdown effect: plays mechanical stopwatch tick on every second while actively running
    LaunchedEffect(isTimerRunning, timeRemainingSeconds) {
        if (isTimerRunning && timeRemainingSeconds > 0) {
            delay(1000L)
            timeRemainingSeconds--
            if (stopwatchSoundActive) {
                PomodoroSoundHelper.playStopwatchTick(context)
            }
            if (timeRemainingSeconds == 0) {
                isTimerRunning = false
                PomodoroSoundHelper.playTone(context, PomodoroTone.COMPLETED)
                if (activeMode == PomodoroMode.FOCUS) {
                    completedSessionsCount++
                    activeMode = PomodoroMode.SHORT_BREAK
                    selectedDurationMinutes = 5
                    timeRemainingSeconds = 5 * 60
                } else {
                    activeMode = PomodoroMode.FOCUS
                    selectedDurationMinutes = 25
                    timeRemainingSeconds = 25 * 60
                }
            }
        }
    }

    val totalDurationSeconds = selectedDurationMinutes * 60
    val progressFraction = if (totalDurationSeconds > 0) {
        (timeRemainingSeconds.toFloat() / totalDurationSeconds.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 300),
        label = "pomodoro_progress"
    )

    val minutes = timeRemainingSeconds / 60
    val seconds = timeRemainingSeconds % 60
    val formattedTime = "%02d:%02d".format(minutes, seconds)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Pomodoro Study Session",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                            )
                        )
                        Text(
                            text = "KSRMCE Autonomous • AI Productivity Lab",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CyberEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("pomodoro_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Student Dashboard",
                            tint = if (isDarkMode) Color.White else Color(0xFF0F172A)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { stopwatchSoundActive = !stopwatchSoundActive },
                        modifier = Modifier.testTag("pomodoro_ambient_sound_btn")
                    ) {
                        Icon(
                            imageVector = if (stopwatchSoundActive) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Stopwatch Ticking Sound",
                            tint = if (stopwatchSoundActive) CyberEmerald else Color.Gray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkMode) ObsidianSurface else LightSurface
                )
            )
        },
        containerColor = if (isDarkMode) ObsidianBg else LightBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizPadding, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Mode Select Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    color = if (isDarkMode) ObsidianElevated else LightSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isDarkMode) ObsidianBorder else LightElevated
                    )
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        PomodoroMode.entries.forEach { mode ->
                            val isSelected = activeMode == mode
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) mode.accentColor else Color.Transparent,
                                modifier = Modifier
                                    .clickable {
                                        activeMode = mode
                                        isTimerRunning = false
                                        selectedDurationMinutes = mode.defaultMinutes
                                        timeRemainingSeconds = mode.defaultMinutes * 60
                                    }
                                    .testTag("pomodoro_mode_${mode.name.lowercase()}")
                            ) {
                                Text(
                                    text = mode.label,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = if (isSelected) Color(0xFF042F2E) else (if (isDarkMode) TextSecondary else Color(0xFF475569)),
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                        fontSize = 11.5.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Subject Selector Pill
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isDarkMode) ObsidianElevated else LightSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = activeMode.accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Active Study Target",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp)
                            )
                            Text(
                                text = selectedSubject,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = activeMode.accentColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${completedSessionsCount} Done Today",
                            color = activeMode.accentColor,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Central Circular Timer Display
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (isDarkMode) ObsidianElevated else LightSurface,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, activeMode.accentColor.copy(alpha = 0.6f)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 28.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(200.dp)
                    ) {
                        // Background track
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.size(190.dp),
                            color = if (isDarkMode) ObsidianBorder else Color(0xFFE2E8F0),
                            strokeWidth = 10.dp
                        )
                        // Active countdown track
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.size(190.dp),
                            color = activeMode.accentColor,
                            strokeWidth = 10.dp
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = formattedTime,
                                style = MaterialTheme.typography.displayMedium.copy(
                                    color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 44.sp,
                                    letterSpacing = 1.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isTimerRunning) "TIMER ACTIVE" else "PAUSED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isTimerRunning) activeMode.accentColor else Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Timer Action Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reset
                        OutlinedButton(
                            onClick = {
                                PomodoroSoundHelper.playTone(context, PomodoroTone.RESET)
                                isTimerRunning = false
                                timeRemainingSeconds = selectedDurationMinutes * 60
                            },
                            shape = CircleShape,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("pomodoro_reset_btn"),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset Timer", modifier = Modifier.size(20.dp))
                        }

                        // Play / Pause Main Button
                        Button(
                            onClick = {
                                if (!isTimerRunning && stopwatchSoundActive) {
                                    PomodoroSoundHelper.playStopwatchTick(context)
                                }
                                isTimerRunning = !isTimerRunning
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = activeMode.accentColor),
                            modifier = Modifier
                                .height(52.dp)
                                .widthIn(min = 120.dp, max = 160.dp)
                                .testTag("pomodoro_toggle_btn")
                        ) {
                            Icon(
                                imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isTimerRunning) "Pause" else "Start",
                                tint = Color(0xFF042F2E),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isTimerRunning) "PAUSE" else "START FOCUS",
                                color = Color(0xFF042F2E),
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }

                        // Skip to Next
                        OutlinedButton(
                            onClick = {
                                isTimerRunning = false
                                if (activeMode == PomodoroMode.FOCUS) {
                                    activeMode = PomodoroMode.SHORT_BREAK
                                    selectedDurationMinutes = 5
                                } else {
                                    activeMode = PomodoroMode.FOCUS
                                    selectedDurationMinutes = 25
                                }
                                timeRemainingSeconds = selectedDurationMinutes * 60
                            },
                            shape = CircleShape,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("pomodoro_skip_btn"),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
                            )
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Skip Mode", modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Compact Mechanical Stopwatch Ticking Sound Pill (Small size, perfectly centered)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (stopwatchSoundActive) activeMode.accentColor.copy(alpha = 0.16f) else (if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF1F5F9)),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (stopwatchSoundActive) activeMode.accentColor.copy(alpha = 0.7f) else (if (isDarkMode) ObsidianBorder else LightElevated)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { stopwatchSoundActive = !stopwatchSoundActive }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (stopwatchSoundActive) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = null,
                                tint = if (stopwatchSoundActive) activeMode.accentColor else TextMuted,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (stopwatchSoundActive) "Stopwatch Ticking: ON (Running Clock Only)" else "Stopwatch Sound: MUTED (Tap to Unmute)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (stopwatchSoundActive) (if (isDarkMode) Color.White else Color(0xFF0F172A)) else TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Preset Duration Chips
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15, 25, 45, 60).forEach { mins ->
                            val isSelected = selectedDurationMinutes == mins
                            SuggestionChip(
                                onClick = {
                                    isTimerRunning = false
                                    selectedDurationMinutes = mins
                                    timeRemainingSeconds = mins * 60
                                },
                                label = { Text("${mins}m", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (isSelected) activeMode.accentColor.copy(alpha = 0.2f) else Color.Transparent,
                                    labelColor = if (isSelected) activeMode.accentColor else (if (isDarkMode) TextSecondary else Color(0xFF64748B))
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) activeMode.accentColor else (if (isDarkMode) ObsidianBorder else LightElevated)
                                )
                            )
                        }
                    }
                }
            }

            // Study Task Checklist Section
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (isDarkMode) ObsidianSurface else LightSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's Study Checklist",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                            )
                        )
                        Text(
                            text = "${studyTasks.count { it.isCompleted }}/${studyTasks.size} Done",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CyberEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    studyTasks.forEachIndexed { index, task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    studyTasks[index] = task.copy(isCompleted = !task.isCompleted)
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = task.isCompleted,
                                onCheckedChange = { checked ->
                                    studyTasks[index] = task.copy(isCompleted = checked)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = CyberEmerald,
                                    checkmarkColor = Color(0xFF042F2E)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (task.isCompleted) TextMuted else (if (isDarkMode) Color.White else Color(0xFF0F172A)),
                                        textDecoration = if (task.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp
                                    )
                                )
                                Text(
                                    text = task.subject,
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}
