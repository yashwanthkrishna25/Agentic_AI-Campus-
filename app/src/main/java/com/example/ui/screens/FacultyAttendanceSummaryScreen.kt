package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FacultyAttendanceDataProvider
import com.example.model.FacultyAttendanceOverview
import com.example.model.FacultySubjectSummary
import com.example.ui.theme.*
import kotlin.math.roundToInt

enum class ChartFilterView(val title: String) {
    ALL_CHARTS("All Charts"),
    SUBJECT_BARS("Subject Comparison"),
    WEEKLY_TREND("Weekly Trends"),
    TIER_DISTRIBUTION("Tier Breakdown")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultyAttendanceSummaryScreen(
    isDarkMode: Boolean = true,
    onBack: () -> Unit = {},
    onNavigateToChat: (String) -> Unit = {},
    onTriggerToast: (String) -> Unit = {}
) {
    val overview = remember { FacultyAttendanceDataProvider.getFacultyOverview() }
    var selectedSubjectCode by remember { mutableStateOf<String?>(null) } // null = All Subjects
    var activeChartTab by remember { mutableStateOf(ChartFilterView.ALL_CHARTS) }
    var showExportReportDialog by remember { mutableStateOf(false) }
    var showNotifyRiskDialog by remember { mutableStateOf(false) }

    val filteredSubjects = remember(selectedSubjectCode) {
        if (selectedSubjectCode == null) {
            overview.subjects
        } else {
            overview.subjects.filter { it.subjectCode == selectedSubjectCode }
        }
    }

    val selectedSubject = remember(selectedSubjectCode) {
        overview.subjects.find { it.subjectCode == selectedSubjectCode }
    }

    val displayedAverage = remember(filteredSubjects) {
        if (filteredSubjects.isNotEmpty()) {
            filteredSubjects.map { it.averageAttendancePercentage }.average()
        } else 0.0
    }

    val totalConducted = remember(filteredSubjects) {
        filteredSubjects.sumOf { it.totalClassesConducted }
    }

    val totalStudents = remember(filteredSubjects) {
        filteredSubjects.sumOf { it.enrolledStudents }
    }

    val totalSafe = remember(filteredSubjects) {
        filteredSubjects.sumOf { it.studentsAbove75 }
    }

    val totalAtRisk = remember(filteredSubjects) {
        filteredSubjects.sumOf { it.studentsBelow75 }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Faculty Attendance Analytics",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                            )
                        )
                        Text(
                            text = "${overview.facultyName} • ${overview.department}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isDarkMode) TextSecondary else Color(0xFF64748B)
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("summary_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDarkMode) Color.White else Color(0xFF0F172A)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showExportReportDialog = true },
                        modifier = Modifier.testTag("export_summary_pdf_icon")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export Report",
                            tint = if (isDarkMode) CyberEmerald else Color(0xFF0D9488)
                        )
                    }
                    IconButton(
                        onClick = {
                            onNavigateToChat("Analyze my faculty attendance summary: total average is ${"%.1f".format(displayedAverage)}% with $totalAtRisk students below 75% cutoff across subjects.")
                        },
                        modifier = Modifier.testTag("summary_ai_advisor_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "AI Analysis",
                            tint = ElectricIndigo
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkMode) ObsidianSurface else LightSurface
                )
            )
        },
        containerColor = if (isDarkMode) ObsidianBg else LightBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Subject Filter Bar (Horizontal Chips)
            SubjectFilterBar(
                subjects = overview.subjects,
                selectedCode = selectedSubjectCode,
                onSelectCode = { selectedSubjectCode = it },
                isDarkMode = isDarkMode
            )

            // Grand Aggregate Attendance KPI Card
            GrandAttendancePercentageCard(
                averagePercentage = displayedAverage,
                totalConducted = totalConducted,
                totalStudents = totalStudents,
                totalSafe = totalSafe,
                totalAtRisk = totalAtRisk,
                selectedSubjectName = selectedSubject?.subjectName,
                isDarkMode = isDarkMode,
                onExportClick = { showExportReportDialog = true },
                onNotifyRiskClick = { showNotifyRiskDialog = true }
            )

            // Chart View Mode Selector Tabs
            ChartViewSelectorTabs(
                selectedView = activeChartTab,
                onSelectView = { activeChartTab = it },
                isDarkMode = isDarkMode
            )

            // Chart Renderers based on active tab
            when (activeChartTab) {
                ChartFilterView.ALL_CHARTS -> {
                    // 1. Subject Comparison Bar Chart
                    SubjectAttendanceBarChartCard(
                        subjects = overview.subjects,
                        selectedCode = selectedSubjectCode,
                        onBarClick = { selectedSubjectCode = if (selectedSubjectCode == it) null else it },
                        isDarkMode = isDarkMode
                    )

                    // 2. Weekly Trend Area Chart
                    WeeklyAttendanceTrendChartCard(
                        trendData = selectedSubject?.weeklyAttendanceTrend
                            ?: overview.subjects.first().weeklyAttendanceTrend,
                        subjectName = selectedSubject?.subjectName ?: "Aggregate (All Subjects)",
                        isDarkMode = isDarkMode
                    )

                    // 3. Tier Distribution Donut Chart
                    AttendanceTierDonutChartCard(
                        overview = overview,
                        selectedSubject = selectedSubject,
                        isDarkMode = isDarkMode
                    )

                    // 4. Period-wise Class Retention
                    PeriodAttendanceBreakdownCard(
                        periods = selectedSubject?.periodAverages
                            ?: overview.subjects.first().periodAverages,
                        subjectName = selectedSubject?.subjectName ?: "Deep Learning [2339609]",
                        isDarkMode = isDarkMode
                    )
                }

                ChartFilterView.SUBJECT_BARS -> {
                    SubjectAttendanceBarChartCard(
                        subjects = overview.subjects,
                        selectedCode = selectedSubjectCode,
                        onBarClick = { selectedSubjectCode = if (selectedSubjectCode == it) null else it },
                        isDarkMode = isDarkMode
                    )
                }

                ChartFilterView.WEEKLY_TREND -> {
                    WeeklyAttendanceTrendChartCard(
                        trendData = selectedSubject?.weeklyAttendanceTrend
                            ?: overview.subjects.first().weeklyAttendanceTrend,
                        subjectName = selectedSubject?.subjectName ?: "Aggregate (All Subjects)",
                        isDarkMode = isDarkMode
                    )
                }

                ChartFilterView.TIER_DISTRIBUTION -> {
                    AttendanceTierDonutChartCard(
                        overview = overview,
                        selectedSubject = selectedSubject,
                        isDarkMode = isDarkMode
                    )
                }
            }

            // Detailed Subject Breakdown Cards
            Text(
                text = "SUBJECT BREAKDOWN (${filteredSubjects.size})",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) TextMuted else Color(0xFF64748B),
                    letterSpacing = 1.sp
                )
            )

            filteredSubjects.forEach { subject ->
                SubjectDetailedAttendanceCard(
                    subject = subject,
                    isDarkMode = isDarkMode,
                    isSelected = subject.subjectCode == selectedSubjectCode,
                    onClick = {
                        selectedSubjectCode = if (selectedSubjectCode == subject.subjectCode) null else subject.subjectCode
                    },
                    onExportReport = {
                        selectedSubjectCode = subject.subjectCode
                        showExportReportDialog = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Export PDF Report Dialog
    if (showExportReportDialog) {
        ExportAttendanceReportDialog(
            overview = overview,
            selectedSubject = selectedSubject,
            isDarkMode = isDarkMode,
            onDismiss = { showExportReportDialog = false },
            onConfirmExport = {
                showExportReportDialog = false
                val subjectTitle = selectedSubject?.subjectName ?: "All Faculty Subjects"
                onTriggerToast("PDF Generated: KSRMCE_Attendance_Summary_${subjectTitle.take(12).replace(" ", "_")}.pdf saved!")
            }
        )
    }

    // Notify Risk Dialog
    if (showNotifyRiskDialog) {
        AlertDialog(
            onDismissRequest = { showNotifyRiskDialog = false },
            title = {
                Text(
                    text = "Attendance Risk Notice (<75%)",
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "A total of $totalAtRisk students are currently falling below the 75% regulatory cutoff.",
                        fontSize = 13.sp,
                        color = if (isDarkMode) TextSecondary else Color(0xFF475569)
                    )
                    Surface(
                        color = NeonCoral.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Action to be dispatched:",
                                fontWeight = FontWeight.Bold,
                                color = NeonCoral,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "• Automated SMS dispatched to registered Parent numbers\n• Condonation counseling alert logged to HOD desk\n• Hall-ticket advisory flagged in student app",
                                fontSize = 11.5.sp,
                                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNotifyRiskDialog = false
                        onTriggerToast("Dispatched attendance warning SMS to $totalAtRisk parents!")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCoral)
                ) {
                    Text("Dispatch Alerts", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotifyRiskDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = if (isDarkMode) ObsidianElevated else LightSurface
        )
    }
}

/**
 * Filter chips for subjects.
 */
@Composable
fun SubjectFilterBar(
    subjects: List<FacultySubjectSummary>,
    selectedCode: String?,
    onSelectCode: (String?) -> Unit,
    isDarkMode: Boolean
) {
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            FilterChip(
                selected = selectedCode == null,
                onClick = { onSelectCode(null) },
                label = { Text("All Subjects (${subjects.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                leadingIcon = {
                    if (selectedCode == null) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ElectricIndigo,
                    selectedLabelColor = Color.White,
                    containerColor = if (isDarkMode) ObsidianElevated else LightSurfaceContainer,
                    labelColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("filter_all_subjects")
            )
        }

        items(subjects.size) { index ->
            val sub = subjects[index]
            val isSelected = selectedCode == sub.subjectCode
            FilterChip(
                selected = isSelected,
                onClick = { onSelectCode(sub.subjectCode) },
                label = {
                    Text(
                        text = "${sub.subjectName.take(15)}.. [${sub.subjectCode}]",
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                leadingIcon = {
                    Surface(
                        shape = CircleShape,
                        color = if (sub.isOverallAboveTarget) CyberEmerald else NeonCoral,
                        modifier = Modifier.size(8.dp)
                    ) {}
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ElectricIndigo,
                    selectedLabelColor = Color.White,
                    containerColor = if (isDarkMode) ObsidianElevated else LightSurfaceContainer,
                    labelColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("filter_subject_${sub.subjectCode}")
            )
        }
    }
}

/**
 * Grand Attendance Percentage Card with large animated circular gauge and KPI stats.
 */
@Composable
fun GrandAttendancePercentageCard(
    averagePercentage: Double,
    totalConducted: Int,
    totalStudents: Int,
    totalSafe: Int,
    totalAtRisk: Int,
    selectedSubjectName: String?,
    isDarkMode: Boolean,
    onExportClick: () -> Unit = {},
    onNotifyRiskClick: () -> Unit = {}
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (averagePercentage / 100.0).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000),
        label = "grand_percentage_anim"
    )

    val isAboveCutoff = averagePercentage >= 75.0

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isDarkMode) ObsidianElevated else LightSurface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isAboveCutoff) ElectricIndigo.copy(alpha = 0.5f) else NeonCoral.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("grand_attendance_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = ElectricIndigo.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = if (selectedSubjectName != null) "SUBJECT TOTAL ATTENDANCE" else "FACULTY AGGREGATE ATTENDANCE",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ElectricIndigo,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    color = if (isAboveCutoff) CyberEmerald.copy(alpha = 0.15f) else NeonCoral.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isAboveCutoff) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isAboveCutoff) (if (isDarkMode) CyberEmerald else Color(0xFF0D9488)) else NeonCoral,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isAboveCutoff) "Safe (>75% Target)" else "At Risk (<75% Target)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAboveCutoff) (if (isDarkMode) CyberEmerald else Color(0xFF0D9488)) else NeonCoral,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Gauge & Big Numbers Row with Adaptive BoxWithConstraints
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isNarrow = maxWidth < 360.dp

                if (isNarrow) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Centered Gauge
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(96.dp)
                        ) {
                            val trackColor = if (isDarkMode) ObsidianBorder else Color(0xFFE2E8F0)
                            val strokeBrush = Brush.sweepGradient(
                                listOf(
                                    if (isDarkMode) CyberEmerald else Color(0xFF0D9488),
                                    ElectricIndigo,
                                    ElectricViolet,
                                    if (isDarkMode) CyberEmerald else Color(0xFF0D9488)
                                )
                            )

                            Canvas(modifier = Modifier.size(92.dp)) {
                                val strokeWidth = 9.dp.toPx()
                                drawArc(
                                    color = trackColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth)
                                )
                                drawArc(
                                    brush = strokeBrush,
                                    startAngle = -90f,
                                    sweepAngle = animatedProgress * 360f,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${"%.1f".format(averagePercentage)}%",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                        fontSize = 19.sp
                                    )
                                )
                                Text(
                                    text = "TOTAL AVG",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Stats Summary Matrix
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                KpiStatMini(
                                    label = "Classes Held",
                                    value = "$totalConducted Sessions",
                                    color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                )
                                KpiStatMini(
                                    label = "Total Students",
                                    value = "$totalStudents Enrolled",
                                    color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                KpiStatMini(
                                    label = "Eligible (>75%)",
                                    value = "$totalSafe Students",
                                    color = if (isDarkMode) CyberEmerald else Color(0xFF0D9488)
                                )
                                KpiStatMini(
                                    label = "Defaulters (<75%)",
                                    value = "$totalAtRisk Students",
                                    color = if (totalAtRisk > 0) NeonCoral else (if (isDarkMode) Color.White else Color(0xFF0F172A))
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Large Circular Canvas Gauge
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(110.dp)
                        ) {
                            val trackColor = if (isDarkMode) ObsidianBorder else Color(0xFFE2E8F0)
                            val strokeBrush = Brush.sweepGradient(
                                listOf(
                                    if (isDarkMode) CyberEmerald else Color(0xFF0D9488),
                                    ElectricIndigo,
                                    ElectricViolet,
                                    if (isDarkMode) CyberEmerald else Color(0xFF0D9488)
                                )
                            )

                            Canvas(modifier = Modifier.size(105.dp)) {
                                val strokeWidth = 11.dp.toPx()
                                drawArc(
                                    color = trackColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth)
                                )
                                val angle75Rad = Math.toRadians(180.0)
                                val radius = (size.minDimension - strokeWidth) / 2f
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val markerX = (center.x + radius * Math.cos(angle75Rad)).toFloat()
                                val markerY = (center.y + radius * Math.sin(angle75Rad)).toFloat()

                                drawArc(
                                    brush = strokeBrush,
                                    startAngle = -90f,
                                    sweepAngle = animatedProgress * 360f,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )

                                drawCircle(
                                    color = NeonAmber,
                                    radius = 4.dp.toPx(),
                                    center = Offset(markerX, markerY)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${"%.1f".format(averagePercentage)}%",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                        fontSize = 22.sp
                                    )
                                )
                                Text(
                                    text = "TOTAL AVG",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Stats Summary Matrix
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                KpiStatMini(
                                    label = "Classes Held",
                                    value = "$totalConducted Sessions",
                                    color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                )
                                KpiStatMini(
                                    label = "Total Students",
                                    value = "$totalStudents Enrolled",
                                    color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                KpiStatMini(
                                    label = "Eligible (>75%)",
                                    value = "$totalSafe Students",
                                    color = if (isDarkMode) CyberEmerald else Color(0xFF0D9488)
                                )
                                KpiStatMini(
                                    label = "Defaulters (<75%)",
                                    value = "$totalAtRisk Students",
                                    color = if (totalAtRisk > 0) NeonCoral else (if (isDarkMode) Color.White else Color(0xFF0F172A))
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = if (isDarkMode) ObsidianBorder else LightElevated)
            Spacer(modifier = Modifier.height(10.dp))

            // Regulatory banner
            Surface(
                color = if (isDarkMode) ObsidianSurface else Color(0xFFF1F5F9),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = LuminousAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AP Autonomous Mandate: 75% attendance needed for End-Sem Hall Tickets.",
                            fontSize = 11.sp,
                            color = if (isDarkMode) TextSecondary else Color(0xFF475569)
                        )
                    }

                    if (totalAtRisk > 0) {
                        TextButton(
                            onClick = onNotifyRiskClick,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = "Triage Defaulters",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCoral
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiStatMini(
    label: String,
    value: String,
    color: Color
) {
    Column {
        Text(text = label, fontSize = 10.5.sp, color = TextMuted)
        Text(text = value, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

/**
 * Chart Selector Tabs.
 */
@Composable
fun ChartViewSelectorTabs(
    selectedView: ChartFilterView,
    onSelectView: (ChartFilterView) -> Unit,
    isDarkMode: Boolean
) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDarkMode) ObsidianElevated else LightSurfaceContainer)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(ChartFilterView.values().size) { idx ->
            val view = ChartFilterView.values()[idx]
            val isSelected = selectedView == view
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) ElectricIndigo else Color.Transparent,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelectView(view) }
            ) {
                Text(
                    text = view.title,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    fontSize = 11.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else (if (isDarkMode) TextSecondary else Color(0xFF64748B)),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * 1. Subject Attendance Comparison Bar Chart Card (Interactive Custom Canvas).
 */
@Composable
fun SubjectAttendanceBarChartCard(
    subjects: List<FacultySubjectSummary>,
    selectedCode: String?,
    onBarClick: (String) -> Unit,
    isDarkMode: Boolean
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isDarkMode) ObsidianElevated else LightSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("subject_bar_chart_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Subject Attendance Comparison",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                        )
                    )
                    Text(
                        text = "Total attendance percentage comparison against 75% target",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                    )
                }

                Surface(
                    color = LuminousAmber.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Target: 75%",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = LuminousAmber
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Custom Canvas Bar Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 24.dp, top = 20.dp, start = 8.dp, end = 8.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val barCount = subjects.size
                    val spacing = 16.dp.toPx()
                    val totalSpacing = spacing * (barCount + 1)
                    val barWidth = ((width - totalSpacing) / barCount).coerceAtLeast(20.dp.toPx())

                    // Draw 75% Reference Line
                    val targetY = height * (1f - 0.75f)
                    drawLine(
                        color = LuminousAmber,
                        start = Offset(0f, targetY),
                        end = Offset(width, targetY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )

                    // Draw Bars
                    subjects.forEachIndexed { index, sub ->
                        val barHeight = (sub.averageAttendancePercentage / 100.0).toFloat() * height
                        val startX = spacing + index * (barWidth + spacing)
                        val startY = height - barHeight
                        val isHighlighted = selectedCode == null || selectedCode == sub.subjectCode

                        val barColor = when {
                            sub.averageAttendancePercentage >= 85.0 -> if (isDarkMode) CyberEmerald else Color(0xFF0D9488)
                            sub.averageAttendancePercentage >= 75.0 -> ElectricIndigo
                            sub.averageAttendancePercentage >= 65.0 -> LuminousAmber
                            else -> NeonCoral
                        }

                        val alpha = if (isHighlighted) 1.0f else 0.35f

                        // Bar background track
                        drawRoundRect(
                            color = if (isDarkMode) ObsidianBorder else Color(0xFFE2E8F0),
                            topLeft = Offset(startX, 0f),
                            size = Size(barWidth, height),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                        )

                        // Filled bar
                        drawRoundRect(
                            color = barColor.copy(alpha = alpha),
                            topLeft = Offset(startX, startY),
                            size = Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                        )
                    }
                }

                // Value labels and Subject labels rendered via Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    subjects.forEach { sub ->
                        val isSelected = selectedCode == sub.subjectCode
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { onBarClick(sub.subjectCode) }
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "${"%.1f".format(sub.averageAttendancePercentage)}%",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sub.averageAttendancePercentage >= 75.0) {
                                    if (isDarkMode) CyberEmerald else Color(0xFF0D9488)
                                } else NeonCoral
                            )
                            Text(
                                text = sub.subjectCode,
                                fontSize = 9.sp,
                                color = if (isSelected) ElectricIndigo else (if (isDarkMode) TextSecondary else Color(0xFF64748B)),
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chart Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChartLegendItem(color = if (isDarkMode) CyberEmerald else Color(0xFF0D9488), label = ">85% Distinction")
                ChartLegendItem(color = ElectricIndigo, label = "75-85% Good")
                ChartLegendItem(color = LuminousAmber, label = "65-74% Warning")
                ChartLegendItem(color = NeonCoral, label = "<65% Critical")
            }
        }
    }
}

/**
 * 2. Weekly Attendance Trend Area / Line Chart Card (Canvas).
 */
@Composable
fun WeeklyAttendanceTrendChartCard(
    trendData: List<Double>,
    subjectName: String,
    isDarkMode: Boolean
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isDarkMode) ObsidianElevated else LightSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weekly_trend_chart_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "12-Week Attendance Trajectory",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                        )
                    )
                    Text(
                        text = subjectName,
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val currentTrend = trendData.lastOrNull() ?: 0.0
                Surface(
                    color = ElectricIndigo.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "W12: ${"%.1f".format(currentTrend)}%",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricIndigo
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Smooth Trend Canvas Line & Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp, bottom = 24.dp, start = 8.dp, end = 8.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val pointsCount = trendData.size
                    if (pointsCount < 2) return@Canvas

                    val stepX = width / (pointsCount - 1)
                    val minVal = 60.0
                    val maxVal = 100.0

                    // 75% Cutoff line
                    val cutoffY = (height * (1f - ((75.0 - minVal) / (maxVal - minVal)))).toFloat()
                    drawLine(
                        color = LuminousAmber.copy(alpha = 0.7f),
                        start = Offset(0f, cutoffY),
                        end = Offset(width, cutoffY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )

                    // Line & Fill Paths
                    val linePath = Path()
                    val fillPath = Path()

                    val coordinates = trendData.mapIndexed { i, value ->
                        val x = i * stepX
                        val normalized = ((value - minVal) / (maxVal - minVal)).coerceIn(0.0, 1.0)
                        val y = (height * (1f - normalized)).toFloat()
                        Offset(x, y)
                    }

                    linePath.moveTo(coordinates[0].x, coordinates[0].y)
                    fillPath.moveTo(coordinates[0].x, height)
                    fillPath.lineTo(coordinates[0].x, coordinates[0].y)

                    for (i in 1 until coordinates.size) {
                        val prev = coordinates[i - 1]
                        val curr = coordinates[i]
                        val cx = (prev.x + curr.x) / 2f
                        linePath.cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
                        fillPath.cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
                    }

                    fillPath.lineTo(coordinates.last().x, height)
                    fillPath.close()

                    // Draw Gradient Area Fill
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            listOf(
                                ElectricIndigo.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    )

                    // Draw Continuous Stroke Line
                    drawPath(
                        path = linePath,
                        color = ElectricIndigo,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw Points
                    coordinates.forEachIndexed { idx, point ->
                        val pointColor = if (trendData[idx] >= 75.0) {
                            if (isDarkMode) CyberEmerald else Color(0xFF0D9488)
                        } else NeonCoral

                        drawCircle(
                            color = pointColor,
                            radius = 4.dp.toPx(),
                            center = point
                        )
                    }
                }

                // X-Axis Week Labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("W1", fontSize = 9.sp, color = TextMuted)
                    Text("W3", fontSize = 9.sp, color = TextMuted)
                    Text("W6 (Mid-1)", fontSize = 9.sp, color = TextMuted)
                    Text("W9", fontSize = 9.sp, color = TextMuted)
                    Text("W12 (Current)", fontSize = 9.sp, color = TextMuted)
                }
            }
        }
    }
}

/**
 * 3. Attendance Tier Distribution Donut Chart Card.
 */
@Composable
fun AttendanceTierDonutChartCard(
    overview: FacultyAttendanceOverview,
    selectedSubject: FacultySubjectSummary?,
    isDarkMode: Boolean
) {
    val distinction = selectedSubject?.tierDistinctionCount ?: overview.overallTierDistinction
    val good = selectedSubject?.tierGoodStandingCount ?: overview.overallTierGood
    val warning = selectedSubject?.tierWarningZoneCount ?: overview.overallTierWarning
    val critical = selectedSubject?.tierCriticalDetentionCount ?: overview.overallTierCritical
    val total = (distinction + good + warning + critical).coerceAtLeast(1)

    val pDistinction = (distinction.toFloat() / total) * 360f
    val pGood = (good.toFloat() / total) * 360f
    val pWarning = (warning.toFloat() / total) * 360f
    val pCritical = (critical.toFloat() / total) * 360f

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isDarkMode) ObsidianElevated else LightSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tier_donut_chart_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Attendance Tier Breakdown (Students)",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                )
            )
            Text(
                text = "Distribution across academic clearance & condonation brackets",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Donut Chart Canvas
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(130.dp)
                ) {
                    Canvas(modifier = Modifier.size(120.dp)) {
                        val strokeWidth = 14.dp.toPx()
                        var currentAngle = -90f

                        // Tier 1: Distinction (>85%)
                        drawArc(
                            color = if (isDarkMode) CyberEmerald else Color(0xFF0D9488),
                            startAngle = currentAngle,
                            sweepAngle = pDistinction,
                            useCenter = false,
                            style = Stroke(width = strokeWidth)
                        )
                        currentAngle += pDistinction

                        // Tier 2: Good Standing (75-85%)
                        drawArc(
                            color = ElectricIndigo,
                            startAngle = currentAngle,
                            sweepAngle = pGood,
                            useCenter = false,
                            style = Stroke(width = strokeWidth)
                        )
                        currentAngle += pGood

                        // Tier 3: Condonation Warning (65-74%)
                        drawArc(
                            color = LuminousAmber,
                            startAngle = currentAngle,
                            sweepAngle = pWarning,
                            useCenter = false,
                            style = Stroke(width = strokeWidth)
                        )
                        currentAngle += pWarning

                        // Tier 4: Critical (<65%)
                        drawArc(
                            color = NeonCoral,
                            startAngle = currentAngle,
                            sweepAngle = pCritical,
                            useCenter = false,
                            style = Stroke(width = strokeWidth)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$total",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            text = "STUDENTS",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Legend List with counts and percentages
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TierLegendRow(
                        color = if (isDarkMode) CyberEmerald else Color(0xFF0D9488),
                        tierName = ">85% Distinction",
                        count = distinction,
                        percentage = ((distinction.toDouble() / total) * 100).roundToInt(),
                        isDarkMode = isDarkMode
                    )
                    TierLegendRow(
                        color = ElectricIndigo,
                        tierName = "75-85% Good Standing",
                        count = good,
                        percentage = ((good.toDouble() / total) * 100).roundToInt(),
                        isDarkMode = isDarkMode
                    )
                    TierLegendRow(
                        color = LuminousAmber,
                        tierName = "65-74% Condonation",
                        count = warning,
                        percentage = ((warning.toDouble() / total) * 100).roundToInt(),
                        isDarkMode = isDarkMode
                    )
                    TierLegendRow(
                        color = NeonCoral,
                        tierName = "<65% Critical / Detain",
                        count = critical,
                        percentage = ((critical.toDouble() / total) * 100).roundToInt(),
                        isDarkMode = isDarkMode
                    )
                }
            }
        }
    }
}

@Composable
private fun TierLegendRow(
    color: Color,
    tierName: String,
    count: Int,
    percentage: Int,
    isDarkMode: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = color,
                modifier = Modifier.size(8.dp)
            ) {}
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = tierName,
                fontSize = 11.sp,
                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
            )
        }

        Text(
            text = "$count ($percentage%)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * 4. Period Attendance Breakdown Matrix.
 */
@Composable
fun PeriodAttendanceBreakdownCard(
    periods: List<Pair<String, Double>>,
    subjectName: String,
    isDarkMode: Boolean
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isDarkMode) ObsidianElevated else LightSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Period-Wise Retention Dynamics",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                )
            )
            Text(
                text = "Lecture session attendance rates across morning and afternoon periods",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            periods.forEach { (periodLabel, avgPct) ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = periodLabel,
                            fontSize = 11.5.sp,
                            color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            text = "${"%.1f".format(avgPct)}%",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (avgPct >= 75.0) {
                                if (isDarkMode) CyberEmerald else Color(0xFF0D9488)
                            } else LuminousAmber
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = { (avgPct / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (avgPct >= 75.0) ElectricIndigo else LuminousAmber,
                        trackColor = if (isDarkMode) ObsidianBorder else Color(0xFFE2E8F0)
                    )
                }
            }
        }
    }
}

/**
 * Detailed Subject Attendance Card.
 */
@Composable
fun SubjectDetailedAttendanceCard(
    subject: FacultySubjectSummary,
    isDarkMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onExportReport: () -> Unit
) {
    val isSafe = subject.isOverallAboveTarget

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDarkMode) ObsidianSurface else LightSurface,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) ElectricIndigo else (if (isDarkMode) ObsidianBorder else LightElevated)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("subject_card_${subject.subjectCode}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subject.subjectName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                        )
                    )
                    Text(
                        text = "Code: ${subject.subjectCode} • ${subject.section}",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.5.sp)
                    )
                }

                // Mini Percentage Badge
                Surface(
                    color = if (isSafe) CyberEmerald.copy(alpha = 0.15f) else NeonCoral.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${"%.1f".format(subject.averageAttendancePercentage)}%",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSafe) (if (isDarkMode) CyberEmerald else Color(0xFF0D9488)) else NeonCoral
                            )
                        )
                        Text(
                            text = if (isSafe) "SAFE" else "AT RISK",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSafe) (if (isDarkMode) CyberEmerald else Color(0xFF0D9488)) else NeonCoral
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = if (isDarkMode) ObsidianBorder else LightElevated)
            Spacer(modifier = Modifier.height(10.dp))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Conducted", fontSize = 10.sp, color = TextMuted)
                    Text("${subject.totalClassesConducted} Classes", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color(0xFF0F172A))
                }
                Column {
                    Text("Students", fontSize = 10.sp, color = TextMuted)
                    Text("${subject.enrolledStudents} Enrolled", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color(0xFF0F172A))
                }
                Column {
                    Text("Above 75%", fontSize = 10.sp, color = TextMuted)
                    Text("${subject.studentsAbove75} Safe", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) CyberEmerald else Color(0xFF0D9488))
                }
                Column {
                    Text("Below 75%", fontSize = 10.sp, color = TextMuted)
                    Text("${subject.studentsBelow75} At Risk", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (subject.studentsBelow75 > 0) NeonCoral else (if (isDarkMode) Color.White else Color(0xFF0F172A)))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Peak Day & Low Day Info in perfectly aligned 2-row table
            Surface(
                color = if (isDarkMode) ObsidianElevated else Color(0xFFF8FAFC),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Peak Attendance",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted
                        )
                        Text(
                            text = subject.highestDayInfo,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) CyberEmerald else Color(0xFF0D9488)
                        )
                    }
                    HorizontalDivider(color = if (isDarkMode) ObsidianBorder.copy(alpha = 0.5f) else Color(0xFFE2E8F0), thickness = 0.5.dp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lowest Attendance",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted
                        )
                        Text(
                            text = subject.lowestDayInfo,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCoral
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onExportReport,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export Subject Report", fontSize = 11.5.sp)
                }
            }
        }
    }
}

@Composable
private fun ChartLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = color,
            modifier = Modifier.size(8.dp)
        ) {}
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 9.5.sp, color = TextMuted)
    }
}

/**
 * Official Attendance Summary Export Dialog.
 */
@Composable
fun ExportAttendanceReportDialog(
    overview: FacultyAttendanceOverview,
    selectedSubject: FacultySubjectSummary?,
    isDarkMode: Boolean,
    onDismiss: () -> Unit,
    onConfirmExport: () -> Unit
) {
    val reportTitle = selectedSubject?.subjectName ?: "Faculty Aggregate Report (All Subjects)"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = ElectricIndigo,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Export Attendance Summary PDF",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = if (isDarkMode) ObsidianSurface else Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "K.S.R.M. COLLEGE OF ENGINEERING",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = ElectricIndigo
                        )
                        Text(
                            text = "Autonomous • Kadapa, Andhra Pradesh",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "OFFICIAL FACULTY ATTENDANCE AUDIT SUMMARY",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                        )
                    }
                }

                Text(
                    text = "Report Scope: $reportTitle",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                )

                Text(
                    text = "Faculty In-Charge: ${overview.facultyName} (${overview.designation})\n" +
                            "Department: ${overview.department}\n" +
                            "Academic Cycle: ${overview.academicYear}\n" +
                            "Audit Date: Today (Synchronized from AI biometric attendance ledger)",
                    fontSize = 11.sp,
                    color = if (isDarkMode) TextSecondary else Color(0xFF475569)
                )

                HorizontalDivider(color = if (isDarkMode) ObsidianBorder else LightElevated)

                Surface(
                    color = if (isDarkMode) ObsidianElevated else Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Regulatory Compliance Check:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isDarkMode) LuminousAmber else Color(0xFFB45309)
                        )
                        Text(
                            text = "• JNTUA / Autonomous Condonation threshold: 75.0%\n" +
                                    "• Medical condonation bracket: 65.0% - 74.9%\n" +
                                    "• Detention threshold (<65%): Formally registered for Dean scrutiny",
                            fontSize = 10.5.sp,
                            color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmExport,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("confirm_export_pdf_button")
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Generate & Download PDF", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        containerColor = if (isDarkMode) ObsidianElevated else LightSurface
    )
}
