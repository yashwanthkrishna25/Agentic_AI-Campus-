package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.example.data.AcademicDataProvider
import com.example.data.EmailAlertDispatcher
import com.example.model.AttendanceStatus
import com.example.model.MentorDirectMessage
import com.example.model.ParentLeaveApplication
import com.example.ui.components.OverallAttendanceCard
import com.example.ui.responsive.LocalWindowAdaptiveInfo
import com.example.ui.theme.*

enum class ParentSectionTab {
    CHILD_ATTENDANCE,
    MARKS_AND_RISK,
    LEAVE_APPLICATIONS,
    MENTOR_CONNECT,
    PARENT_SETTINGS
}

/**
 * Parent & Guardian Portal for AGENTRA AI.
 * Allows parents to track child's real-time attendance, today's period-by-period logs,
 * exam marks, apply for official leave/condonation, and chat directly with faculty mentor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(
    initialTab: ParentSectionTab = ParentSectionTab.CHILD_ATTENDANCE,
    onNavigateToChat: (String) -> Unit = {},
    isDarkMode: Boolean = true,
    onToggleTheme: () -> Unit = {}
) {
    val adaptiveInfo = LocalWindowAdaptiveInfo.current
    val horizPadding = if (adaptiveInfo.isSmallScreen) 10.dp else 14.dp
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    val todayLogs = remember { AcademicDataProvider.getTodayChildPeriodLogs() }
    val courses = AcademicDataProvider.sharedCourses

    val leaveApplications = remember {
        mutableStateListOf<ParentLeaveApplication>().apply {
            addAll(AcademicDataProvider.getParentLeaveApplications())
        }
    }

    val mentorMessages = remember {
        mutableStateListOf<MentorDirectMessage>().apply {
            addAll(AcademicDataProvider.getMentorConversation())
        }
    }

    var showApplyLeaveDialog by remember { mutableStateOf(false) }
    var mentorReplyText by remember { mutableStateOf("") }
    var smsAlertsEnabled by remember { mutableStateOf(true) }
    var dailyReportEnabled by remember { mutableStateOf(true) }
    var showSuccessToast by remember { mutableStateOf<String?>(null) }

    // Child summary statistics
    val totalAttended = courses.sumOf { it.attendedSessions }
    val totalHeld = courses.sumOf { it.totalSessions }
    val overallPercentage = if (totalHeld > 0) (totalAttended.toDouble() / totalHeld.toDouble()) * 100.0 else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkMode) ObsidianBg else LightBg)
    ) {
        // Parent Sub-Tabs Header
        Surface(
            color = if (isDarkMode) ObsidianSurface else LightSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
            modifier = Modifier.fillMaxWidth()
        ) {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    ParentTabButton(
                        title = "Attendance",
                        icon = Icons.Default.EventAvailable,
                        isSelected = selectedTab == ParentSectionTab.CHILD_ATTENDANCE,
                        onClick = { selectedTab = ParentSectionTab.CHILD_ATTENDANCE }
                    )
                }
                item {
                    ParentTabButton(
                        title = "Marks & Risk",
                        icon = Icons.Default.FactCheck,
                        isSelected = selectedTab == ParentSectionTab.MARKS_AND_RISK,
                        onClick = { selectedTab = ParentSectionTab.MARKS_AND_RISK }
                    )
                }
                item {
                    ParentTabButton(
                        title = "Leave App",
                        icon = Icons.Default.NoteAlt,
                        isSelected = selectedTab == ParentSectionTab.LEAVE_APPLICATIONS,
                        onClick = { selectedTab = ParentSectionTab.LEAVE_APPLICATIONS }
                    )
                }
                item {
                    ParentTabButton(
                        title = "Mentor",
                        icon = Icons.Default.SupportAgent,
                        isSelected = selectedTab == ParentSectionTab.MENTOR_CONNECT,
                        onClick = { selectedTab = ParentSectionTab.MENTOR_CONNECT }
                    )
                }
                item {
                    ParentTabButton(
                        title = "Settings",
                        icon = Icons.Default.Settings,
                        isSelected = selectedTab == ParentSectionTab.PARENT_SETTINGS,
                        onClick = { selectedTab = ParentSectionTab.PARENT_SETTINGS }
                    )
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 1040.dp)
                    .padding(horizontal = horizPadding, vertical = 12.dp)
            ) {
            // Child Overview Hero Banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDarkMode) ObsidianElevated else LightSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(CyberEmerald.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.School, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Surface(
                                    color = CyberEmerald.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "CHILD ON-CAMPUS STATUS",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = CyberEmerald,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    )
                                }
                                Text(
                                    text = "Sattu Penchala Prasad",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                )
                                Text(
                                    text = "239Y1A0501 • B.Tech CSE (4th Year Sec B)",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 11.sp)
                                )
                            }
                        }

                        // Live Present Indicator
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Present Today", color = Color(0xFF10B981), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = if (isDarkMode) ObsidianBorder else LightElevated)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Child stats with responsive wrap
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val isNarrow = maxWidth < 380.dp
                        val pctZone = if (overallPercentage >= 75.0) "Safe Zone" else if (overallPercentage >= 65.0) "Condonation" else "Alert"
                        val pctColor = if (overallPercentage >= 75.0) CyberEmerald else if (overallPercentage >= 65.0) LuminousAmber else CyberError

                        if (isNarrow) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Overall Attendance", color = TextMuted, fontSize = 9.5.sp)
                                        Text(
                                            text = "${String.format(java.util.Locale.getDefault(), "%.1f", overallPercentage)}% ($pctZone)",
                                            color = pctColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Current CGPA", color = TextMuted, fontSize = 9.5.sp)
                                        Text("8.42 / 10.0", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Faculty Mentor: Dr. P. V. Kishore", color = ElectricIndigo, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Overall Attendance", color = TextMuted, fontSize = 10.sp)
                                    Text(
                                        text = "${String.format(java.util.Locale.getDefault(), "%.1f", overallPercentage)}% ($pctZone)",
                                        color = pctColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Current CGPA", color = TextMuted, fontSize = 10.sp)
                                    Text("8.42 / 10.0", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Faculty Mentor", color = TextMuted, fontSize = 10.sp)
                                    Text("Dr. P. V. Kishore", color = ElectricIndigo, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Official Email Advisory Bar for Parent
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F1E3D),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Automated Parent Email Alerts", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        Text("Registered: parent.penchalaprasad@gmail.com", color = Color(0xFFBAE6FD), fontSize = 10.sp)
                    }
                    Button(
                        onClick = {
                            EmailAlertDispatcher.sendAndhraExamScheduleAlert()
                            showSuccessToast = "Official Timetable & Attendance Memo sent to your email!"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Send Copy", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (showSuccessToast != null) {
                Surface(
                    color = Color(0xFF065F46),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(showSuccessToast!!, color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Tab Content
            when (selectedTab) {
                ParentSectionTab.CHILD_ATTENDANCE -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Attendance Ring Card
                        item {
                            OverallAttendanceCard(
                                percentage = overallPercentage,
                                attendedSessions = totalAttended,
                                totalSessions = totalHeld,
                                onViewCatchupPlan = {
                                    onNavigateToChat("Parent query: Is my child Penchala Prasad's 80.15% attendance safe from detention?")
                                }
                            )
                        }

                        // Today's Period-by-Period Real-Time Log
                        item {
                            Text(
                                text = "TODAY'S PERIOD-BY-PERIOD BIOMETRIC TIMETABLE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextMuted,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                        }

                        items(todayLogs) { log ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDarkMode) ObsidianElevated else LightSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (log.status) {
                                                        AttendanceStatus.PRESENT -> Color(0xFF10B981).copy(alpha = 0.2f)
                                                        AttendanceStatus.ABSENT -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                                        AttendanceStatus.LATE -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                                        AttendanceStatus.ON_DUTY -> Color(0xFF3B82F6).copy(alpha = 0.2f)
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "P${log.periodNumber}",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = log.subjectName,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "${log.timeSlot} • ${log.facultyName}",
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = "Verified at ${log.recordedTime} via Face Biometrics",
                                                color = CyberEmerald,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = when (log.status) {
                                            AttendanceStatus.PRESENT -> Color(0xFF10B981)
                                            AttendanceStatus.ABSENT -> Color(0xFFEF4444)
                                            AttendanceStatus.LATE -> Color(0xFFF59E0B)
                                            AttendanceStatus.ON_DUTY -> Color(0xFF3B82F6)
                                        }
                                    ) {
                                        Text(
                                            text = log.status.label,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Subject-Wise Attendance Overview
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "SUBJECT-WISE ATTENDANCE SUMMARY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextMuted,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                        }

                        items(courses) { course ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDarkMode) ObsidianElevated else LightSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = course.name,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "${String.format("%.1f", course.attendancePercentage)}%",
                                            color = if (course.attendancePercentage < 75.0) NeonCoral else CyberEmerald,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${course.attendedSessions} attended of ${course.totalSessions} sessions • Faculty: ${course.facultyName}",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { (course.attendancePercentage / 100.0).toFloat() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(CircleShape),
                                        color = if (course.attendancePercentage < 75.0) NeonCoral else CyberEmerald,
                                        trackColor = if (isDarkMode) ObsidianBorder else LightElevated
                                    )
                                }
                            }
                        }
                    }
                }

                ParentSectionTab.MARKS_AND_RISK -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Academic Risk Early Warning status
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF064E3B).copy(alpha = 0.3f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyberEmerald),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("ACADEMIC RISK MODEL: LOW RISK (SAFE)", color = CyberEmerald, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(
                                            "Child maintains strong academic indicators: 80.15% attendance, 8.42 CGPA. No detention risk detected.",
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                text = "COURSE ASSESSMENT & SEMESTER MARKS",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            )
                        }

                        items(courses) { course ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDarkMode) ObsidianElevated else LightSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(course.name, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color(0xFF0F172A), fontSize = 13.sp)
                                        Text("Total: ${course.grandTotal.toInt()} / 100", color = LuminousAmber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Component badges (horizontally scrollable to prevent clipping on mobile screens)
                                    androidx.compose.foundation.lazy.LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        item { MarkBadge("Quiz 1", course.x1) }
                                        item { MarkBadge("Assign 1", course.x2) }
                                        item { MarkBadge("Mid-1", course.x3) }
                                        item { MarkBadge("Lab/Proj", course.x4) }
                                        item { MarkBadge("Seminar", course.x5) }
                                        item { MarkBadge("Mid-2", course.x6) }
                                        item { MarkBadge("End-Term", course.x7) }
                                    }
                                }
                            }
                        }
                    }
                }

                ParentSectionTab.LEAVE_APPLICATIONS -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Action: Apply New Leave
                        Button(
                            onClick = { showApplyLeaveDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberEmeraldDark),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("apply_parent_leave_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Apply Student Leave / Medical Condonation", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Text(
                            text = "OFFICIAL LEAVE & CONDONATION SUBMISSIONS",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        )

                        leaveApplications.forEach { app ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDarkMode) ObsidianElevated else LightSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(app.reasonCategory, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color(0xFF0F172A), fontSize = 13.5.sp)
                                            Text("Duration: ${app.startDate} to ${app.endDate}", color = TextSecondary, fontSize = 11.sp)
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = when (app.status) {
                                                "Condonation Granted" -> Color(0xFF10B981).copy(alpha = 0.2f)
                                                "Approved by Mentor" -> ElectricIndigo.copy(alpha = 0.2f)
                                                else -> LuminousAmber.copy(alpha = 0.2f)
                                            },
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                when (app.status) {
                                                    "Condonation Granted" -> Color(0xFF10B981)
                                                    "Approved by Mentor" -> ElectricIndigo
                                                    else -> LuminousAmber
                                                }
                                            )
                                        ) {
                                            Text(
                                                text = app.status,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = when (app.status) {
                                                        "Condonation Granted" -> Color(0xFF10B981)
                                                        "Approved by Mentor" -> ElectricIndigo
                                                        else -> LuminousAmber
                                                    },
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(app.explanation, color = TextMuted, fontSize = 11.5.sp)

                                    if (app.medicalProofAttached) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.AttachFile, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Doctor Certificate & Medical Slip Verified", color = CyberEmerald, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                ParentSectionTab.MENTOR_CONNECT -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Mentor Profile Summary
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDarkMode) ObsidianElevated else LightSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(ElectricIndigo.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.SupervisorAccount, contentDescription = null, tint = ElectricIndigo, modifier = Modifier.size(24.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Dr. P. V. Kishore", fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color(0xFF0F172A), fontSize = 14.sp)
                                        Text("Associate Professor in CSE • Official Faculty Mentor", color = TextSecondary, fontSize = 11.sp)
                                        Text("+91 94401 23456 • pvk.cse@ksrmce.ac.in", color = CyberEmerald, fontSize = 10.sp)
                                    }
                                }

                                IconButton(onClick = { showSuccessToast = "Calling Dr. P. V. Kishore (+91 94401 23456)..." }) {
                                    Icon(Icons.Default.Call, contentDescription = "Call Mentor", tint = Color(0xFF10B981))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Conversation message list
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(mentorMessages) { msg ->
                                val isParent = !msg.isFromMentor
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isParent) Arrangement.End else Arrangement.Start
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isParent) CyberEmeraldDark else ObsidianElevated,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isParent) CyberEmerald else ObsidianBorder
                                        ),
                                        modifier = Modifier.widthIn(max = 280.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = msg.senderName,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isParent) Color.White else ElectricIndigo,
                                                fontSize = 11.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(msg.messageText, color = Color.White, fontSize = 12.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(msg.timestamp, color = TextMuted, fontSize = 9.sp, modifier = Modifier.align(Alignment.End))
                                        }
                                    }
                                }
                            }
                        }

                        // Message Input
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = mentorReplyText,
                                onValueChange = { mentorReplyText = it },
                                placeholder = { Text("Message mentor about child's academics...", color = TextMuted, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(20.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = CyberEmerald,
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedContainerColor = ObsidianElevated,
                                    unfocusedContainerColor = ObsidianElevated
                                ),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (mentorReplyText.isNotBlank()) {
                                        mentorMessages.add(
                                            MentorDirectMessage(
                                                id = System.currentTimeMillis().toString(),
                                                senderName = "P. Srinivasa Rao (Parent)",
                                                messageText = mentorReplyText,
                                                timestamp = "Just now",
                                                isFromMentor = false
                                            )
                                        )
                                        mentorReplyText = ""
                                        showSuccessToast = "Message sent to Mentor Dr. Kishore!"
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(CyberEmerald)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Send", tint = ObsidianBg)
                            }
                        }
                    }
                }

                ParentSectionTab.PARENT_SETTINGS -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDarkMode) ObsidianElevated else LightSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("PARENT NOTIFICATION PREFERENCES", fontWeight = FontWeight.Bold, color = TextMuted, fontSize = 11.sp, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(12.dp))

                                ParentToggleRow(
                                    title = "Instant Period Absence SMS",
                                    subtitle = "Receive automated SMS alerts if child misses any morning/afternoon period",
                                    checked = smsAlertsEnabled,
                                    onCheckedChange = { smsAlertsEnabled = it }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = if (isDarkMode) ObsidianBorder else LightElevated)

                                ParentToggleRow(
                                    title = "Daily Attendance Digest",
                                    subtitle = "Daily summary at 05:00 PM with all periods and verification method",
                                    checked = dailyReportEnabled,
                                    onCheckedChange = { dailyReportEnabled = it }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = if (isDarkMode) ObsidianBorder else LightElevated)

                                ParentToggleRow(
                                    title = "Dark Theme",
                                    subtitle = "Toggle dark obsidian or clean day mode",
                                    checked = isDarkMode,
                                    onCheckedChange = { onToggleTheme() }
                                )
                            }
                        }

                        // Ask Parent AI Companion Button
                        Button(
                            onClick = { onNavigateToChat("Parent query: Explain KSRMCE 75% attendance detention rules and exam eligibility") },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ask AGENTRA Parent AI Companion", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
    }

    // Apply Leave Dialog
    if (showApplyLeaveDialog) {
        var leaveFrom by remember { mutableStateOf("15 Oct 2026") }
        var leaveTo by remember { mutableStateOf("16 Oct 2026") }
        var leaveReasonCategory by remember { mutableStateOf("Medical Leave") }
        var leaveExplanation by remember { mutableStateOf("") }
        var attachMedicalSlip by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showApplyLeaveDialog = false },
            title = { Text("Submit Student Leave / Condonation", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Child: Sattu Penchala Prasad (239Y1A0501)", fontSize = 12.sp, color = CyberEmerald, fontWeight = FontWeight.SemiBold)

                    OutlinedTextField(
                        value = leaveFrom,
                        onValueChange = { leaveFrom = it },
                        label = { Text("Start Date") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = leaveTo,
                        onValueChange = { leaveTo = it },
                        label = { Text("End Date") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = leaveReasonCategory,
                        onValueChange = { leaveReasonCategory = it },
                        label = { Text("Leave Category") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = leaveExplanation,
                        onValueChange = { leaveExplanation = it },
                        label = { Text("Reason Details / Doctor Consultation") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Undergoing medical observation...") }
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { attachMedicalSlip = !attachMedicalSlip }
                    ) {
                        Checkbox(checked = attachMedicalSlip, onCheckedChange = { attachMedicalSlip = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Attach Registered Medical Practitioner Certificate", fontSize = 11.5.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        leaveApplications.add(
                            ParentLeaveApplication(
                                id = "LEAVE-${System.currentTimeMillis() % 10000}",
                                childName = "Sattu Penchala Prasad",
                                rollNumber = "239Y1A0501",
                                startDate = leaveFrom,
                                endDate = leaveTo,
                                reasonCategory = leaveReasonCategory,
                                explanation = leaveExplanation.ifBlank { "Formal parent leave application submitted via AGENTRA AI." },
                                medicalProofAttached = attachMedicalSlip,
                                status = "Pending Mentor Review"
                            )
                        )
                        com.example.data.AcademicDataProvider.sharedNotifications.add(
                            0,
                            "Leave Request: Sattu Penchala Prasad (239Y1A0501) - $leaveReasonCategory ($leaveFrom to $leaveTo)"
                        )
                        com.example.data.AcademicDataProvider.saveDataToDisk()
                        showApplyLeaveDialog = false
                        showSuccessToast = "Leave application submitted to Mentor Dr. Kishore!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberEmeraldDark)
                ) {
                    Text("Submit Application", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApplyLeaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ParentTabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) CyberEmerald.copy(alpha = 0.18f) else Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) CyberEmerald else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                color = if (isSelected) CyberEmerald else TextSecondary,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun MarkBadge(label: String, score: Double) {
    Surface(
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = TextMuted, fontSize = 9.sp)
            Text(String.format("%.1f", score), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}

@Composable
fun ParentToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(subtitle, color = TextSecondary, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = CyberEmerald, checkedTrackColor = CyberEmeraldDark)
        )
    }
}
