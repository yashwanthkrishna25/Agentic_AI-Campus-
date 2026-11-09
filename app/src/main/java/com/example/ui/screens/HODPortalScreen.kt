package com.example.ui.screens
import com.example.ui.components.*

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AcademicDataProvider
import com.example.model.*
import com.example.ui.components.AttendanceDonutChart
import com.example.ui.components.MonthlyTrendLineChart
import com.example.ui.components.SubjectAttendanceBarChart
import com.example.ui.responsive.LocalWindowAdaptiveInfo
import com.example.ui.theme.*

enum class HODTab {
    DEPARTMENT_OVERVIEW,
    EXAM_OPERATIONS,
    BELOW_75_TRIAGE, // Academic Mentorship & Counseling Hub
    FACULTY_ALLOCATION,
    DEPARTMENT_CIRCULARS,
    HOD_SETTINGS
}

/**
 * Advanced Head of Department (HOD) Command Center Dashboard.
 * Rebuilt completely to feature:
 * 1. Live Department Operations & Class Monitoring with branch selector (CSE, ECE, MECH, CIVIL, AI&DS).
 * 2. Exam Operations Publisher: HOD can publish Exam Schedules and Semester Exam Results, which immediately sync to Student & Faculty logins.
 * 3. Academic Mentorship Hub & Student Counseling Oversight.
 * 4. Faculty Workload & Biometric Attendance Oversight.
 * 5. Full Light and Dark theme responsiveness.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HODPortalScreen(
    initialTab: HODTab = HODTab.DEPARTMENT_OVERVIEW,
    isDarkMode: Boolean = true,
    onToggleTheme: () -> Unit = {},
    onNavigateToChat: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val adaptiveInfo = LocalWindowAdaptiveInfo.current
    val horizPadding = if (adaptiveInfo.isSmallScreen) 10.dp else 16.dp
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    LaunchedEffect(initialTab) {
        selectedTab = initialTab
    }

    val departments = remember { listOf("CSE", "AIML", "ECE", "EEE", "CIVIL", "MECH") }
    var selectedDeptCode by remember { mutableStateOf("CSE") }

    val facultyList = remember { mutableStateListOf<FacultyMember>().apply { addAll(AcademicDataProvider.getFacultyDirectory()) } }
    val below75Students = remember { mutableStateListOf<EnrolledStudent>().apply { addAll(AcademicDataProvider.getStudentsBelow75Attendance()) } }
    val alertedParentRolls = remember { mutableStateListOf<String>() }

    // Academic Mentorship & Counseling states
    var selectedMentorshipSectionFilter by remember { mutableStateOf("All") }
    val assignedMentors = remember { mutableStateMapOf<String, String>() }
    val counselingStatuses = remember { mutableStateMapOf<String, String>() }

    // HOD Profile Settings states
    var hodProfileName by remember { mutableStateOf("Dr. N. Ramanjaneya Reddy") }
    var hodProfileDept by remember { mutableStateOf("Computer Science & Engineering") }
    var hodProfileEmpId by remember { mutableStateOf("KSRM-HOD-CSE-001") }
    var hodProfileCabin by remember { mutableStateOf("Block 1, Room 101") }
    var hodProfileEmail by remember { mutableStateOf("hod.cse@ksrmce.ac.in") }
    var hodProfilePhone by remember { mutableStateOf("+91 94402 12345") }
    var hodSettingsFeedback by remember { mutableStateOf<String?>(null) }
    var hodNotifyParentSms by remember { mutableStateOf(true) }
    var hodRequireBiometrics by remember { mutableStateOf(true) }

    var actionBannerMessage by remember { mutableStateOf<String?>(null) }
    var showPublishScheduleDialog by remember { mutableStateOf(false) }
    var showPublishResultDialog by remember { mutableStateOf(false) }
    var showPdfPreviewDialog by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<AndhraExamSchedule?>(null) }
    var editingResult by remember { mutableStateOf<AndhraExamResult?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkMode) ObsidianBg else LightBg)
    ) {
        // HOD Command Sub-tabs
        Surface(
            color = if (isDarkMode) ObsidianSurface else LightSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
            modifier = Modifier.fillMaxWidth()
        ) {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = selectedTab == HODTab.DEPARTMENT_OVERVIEW,
                        onClick = { selectedTab = HODTab.DEPARTMENT_OVERVIEW },
                        label = { Text("Command Center", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(15.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF8B5CF6),
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("hod_tab_overview")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTab == HODTab.EXAM_OPERATIONS,
                        onClick = { selectedTab = HODTab.EXAM_OPERATIONS },
                        label = { Text("Publish Exams & Results", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null, modifier = Modifier.size(15.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricIndigo,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("hod_tab_exams")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTab == HODTab.BELOW_75_TRIAGE,
                        onClick = { selectedTab = HODTab.BELOW_75_TRIAGE },
                        label = { Text("Mentorship Hub", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(15.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("hod_tab_triage")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTab == HODTab.FACULTY_ALLOCATION,
                        onClick = { selectedTab = HODTab.FACULTY_ALLOCATION },
                        label = { Text("Faculty (${facultyList.size})", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(15.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberEmeraldDark,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("hod_tab_faculty")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTab == HODTab.DEPARTMENT_CIRCULARS,
                        onClick = { selectedTab = HODTab.DEPARTMENT_CIRCULARS },
                        label = { Text("Notices", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(15.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LuminousAmber,
                            selectedLabelColor = Color.Black
                        ),
                        modifier = Modifier.testTag("hod_tab_notices")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTab == HODTab.HOD_SETTINGS,
                        onClick = { selectedTab = HODTab.HOD_SETTINGS },
                        label = { Text("Settings", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(15.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF64748B),
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("hod_tab_settings")
                    )
                }
            }
        }

        // Action confirmation banner
        if (actionBannerMessage != null) {
            Surface(
                color = Color(0xFF065F46),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(actionBannerMessage!!, color = Color.White, fontSize = 11.5.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { actionBannerMessage = null }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
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
            ) {
                val deptEnrolled = when (selectedDeptCode) {
                    "CSE" -> 480
                    "AIML" -> 240
                    "ECE" -> 420
                    "EEE" -> 210
                    "CIVIL" -> 180
                    "MECH" -> 220
                    else -> 300
                }
                val deptFacultyCount = when (selectedDeptCode) {
                    "CSE" -> 28
                    "AIML" -> 16
                    "ECE" -> 24
                    "EEE" -> 14
                    "CIVIL" -> 12
                    "MECH" -> 15
                    else -> 20
                }
                val deptAvgAttendance = when (selectedDeptCode) {
                    "CSE" -> 81.4f
                    "AIML" -> 84.8f
                    "ECE" -> 78.2f
                    "EEE" -> 76.5f
                    "CIVIL" -> 73.8f
                    "MECH" -> 75.1f
                    else -> 80.0f
                }
                val deptDefaulters = when (selectedDeptCode) {
                    "CSE" -> below75Students.size
                    "AIML" -> 2
                    "ECE" -> 7
                    "EEE" -> 5
                    "CIVIL" -> 8
                    "MECH" -> 6
                    else -> 4
                }
                val deptDonutPresent = deptAvgAttendance
                val deptDonutAbsent = 100f - deptDonutPresent - 3.2f
                val deptDonutLate = 3.2f

                val deptMonthlyTrends = when (selectedDeptCode) {
                    "CSE" -> listOf(Pair("Jun", 85.0f), Pair("Jul", 83.2f), Pair("Aug", 79.5f), Pair("Sep", 81.0f), Pair("Oct", 81.4f))
                    "AIML" -> listOf(Pair("Jun", 88.0f), Pair("Jul", 86.5f), Pair("Aug", 84.0f), Pair("Sep", 85.2f), Pair("Oct", 84.8f))
                    "ECE" -> listOf(Pair("Jun", 80.0f), Pair("Jul", 79.1f), Pair("Aug", 77.0f), Pair("Sep", 77.8f), Pair("Oct", 78.2f))
                    "EEE" -> listOf(Pair("Jun", 78.5f), Pair("Jul", 77.0f), Pair("Aug", 75.2f), Pair("Sep", 76.0f), Pair("Oct", 76.5f))
                    "CIVIL" -> listOf(Pair("Jun", 75.0f), Pair("Jul", 74.2f), Pair("Aug", 72.8f), Pair("Sep", 73.5f), Pair("Oct", 73.8f))
                    "MECH" -> listOf(Pair("Jun", 77.0f), Pair("Jul", 76.1f), Pair("Aug", 74.0f), Pair("Sep", 74.8f), Pair("Oct", 75.1f))
                    else -> listOf(Pair("Jun", 82.0f), Pair("Jul", 81.0f), Pair("Aug", 79.0f), Pair("Sep", 80.0f), Pair("Oct", 80.5f))
                }

                val deptSubjects = when (selectedDeptCode) {
                    "CSE" -> listOf(
                        Triple("Deep Learning", 82.5f, "2339609"),
                        Triple("Cyber Security", 74.0f, "2305701"),
                        Triple("Cloud Computing", 78.5f, "2332604"),
                        Triple("Prompt Eng Lab", 95.0f, "2339751"),
                        Triple("Organizational Behaviour", 88.0f, "23HS772")
                    )
                    "AIML" -> listOf(
                        Triple("Machine Learning", 86.0f, "23AI501"),
                        Triple("Computer Vision", 83.5f, "23AI502"),
                        Triple("NLP Principles", 85.0f, "23AI503"),
                        Triple("AI Ethics & Governance", 90.0f, "23AI504"),
                        Triple("Deep Learning Lab", 94.5f, "23AI505")
                    )
                    "ECE" -> listOf(
                        Triple("Digital Signal Processing", 79.0f, "23EC501"),
                        Triple("VLSI Design", 76.5f, "23EC502"),
                        Triple("Embedded Systems", 81.0f, "23EC503"),
                        Triple("Antennas & Propagation", 75.5f, "23EC504"),
                        Triple("Microcontroller Lab", 89.0f, "23EC505")
                    )
                    "EEE" -> listOf(
                        Triple("Power Systems-II", 77.0f, "23EE501"),
                        Triple("Control Systems", 75.5f, "23EE502"),
                        Triple("Power Electronics", 78.0f, "23EE503"),
                        Triple("Renewable Energy Systems", 82.0f, "23EE504"),
                        Triple("Electrical Machines Lab", 88.5f, "23EE505")
                    )
                    "CIVIL" -> listOf(
                        Triple("Structural Analysis", 74.0f, "23CE501"),
                        Triple("Geotechnical Engineering", 72.5f, "23CE502"),
                        Triple("Transportation Engg", 76.0f, "23CE503"),
                        Triple("Environmental Engg", 75.0f, "23CE504"),
                        Triple("Concrete Tech Lab", 86.0f, "23CE505")
                    )
                    "MECH" -> listOf(
                        Triple("Thermal Engineering-II", 75.0f, "23ME501"),
                        Triple("Design of Machine Elements", 73.5f, "23ME502"),
                        Triple("Manufacturing Technology", 78.0f, "23ME503"),
                        Triple("Fluid Mechanics Lab", 87.0f, "23ME504"),
                        Triple("CAD/CAM Engineering", 82.0f, "23ME505")
                    )
                    else -> listOf(Triple("Core Engineering", 80.0f, "23XX001"))
                }

                val deptTimetable = when (selectedDeptCode) {
                    "CSE" -> listOf(
                        Triple("09:15 - 10:15 AM", "Cyber Security [2305701]", "Dr. M. Ramanathan • 42/48 Present"),
                        Triple("10:15 - 11:15 AM", "Organizational Behaviour [23HS772]", "Prof. S. R. Varma • 44/48 Present"),
                        Triple("11:15 - 12:15 PM", "Deep Learning [2339609]", "Dr. P. V. Kishore • 38/48 Present"),
                        Triple("01:15 - 03:15 PM", "Prompt Engineering Lab [2339751]", "Mr. J. Vinay Kumar • 48/48 Present")
                    )
                    "AIML" -> listOf(
                        Triple("09:15 - 10:15 AM", "Machine Learning [23AI501]", "Dr. K. S. Rao • 39/42 Present"),
                        Triple("10:15 - 11:15 AM", "Computer Vision [23AI502]", "Dr. G. Lakshmi • 40/42 Present"),
                        Triple("11:15 - 12:15 PM", "NLP Principles [23AI503]", "Mr. P. Naveen • 41/42 Present"),
                        Triple("01:15 - 03:15 PM", "Deep Learning Lab [23AI505]", "Dr. K. S. Rao • 42/42 Present")
                    )
                    "ECE" -> listOf(
                        Triple("09:15 - 10:15 AM", "Digital Signal Processing [23EC501]", "Prof. T. Murali • 43/46 Present"),
                        Triple("10:15 - 11:15 AM", "VLSI Design [23EC502]", "Dr. N. Hemalatha • 41/46 Present"),
                        Triple("11:15 - 12:15 PM", "Embedded Systems [23EC503]", "Mr. R. Rajesh • 45/46 Present"),
                        Triple("01:15 - 03:15 PM", "Microcontroller Lab [23EC505]", "Prof. T. Murali • 46/46 Present")
                    )
                    else -> listOf(
                        Triple("09:15 - 10:15 AM", "$selectedDeptCode Core Technical-I", "Senior Faculty • In Progress"),
                        Triple("10:15 - 11:15 AM", "$selectedDeptCode Core Technical-II", "Associate Professor • Scheduled"),
                        Triple("11:15 - 12:15 PM", "$selectedDeptCode Advanced Lab", "Assistant Professor • Scheduled")
                    )
                }

                when (selectedTab) {
            // === 1. COMMAND CENTER OVERVIEW ===
            HODTab.DEPARTMENT_OVERVIEW -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Department & HOD Executive Header Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (isDarkMode) ObsidianElevated else LightSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f)),
                            shadowElevation = 4.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Surface(
                                            color = Color(0xFF8B5CF6).copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "HOD COMMAND CENTER",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color(0xFF8B5CF6),
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.sp
                                                )
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Dr. N. Ramanjaneya Reddy",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                                fontSize = 17.sp
                                            )
                                        )
                                        Text(
                                            text = "Head of Department • Computer Science & Engineering",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = if (isDarkMode) TextSecondary else Color(0xFF475569),
                                                fontSize = 11.5.sp
                                            )
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF8B5CF6).copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Engineering,
                                            contentDescription = null,
                                            tint = Color(0xFF8B5CF6),
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Department selector chips
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Dept:", color = if (isDarkMode) TextSecondary else Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 6.dp))
                                    androidx.compose.foundation.lazy.LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        items(departments) { dept ->
                                            val isSelected = selectedDeptCode == dept
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isSelected) Color(0xFF8B5CF6) else (if (isDarkMode) ObsidianSurface else LightElevated),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFF8B5CF6) else if (isDarkMode) ObsidianBorder else Color(0xFFCBD5E1)),
                                                modifier = Modifier.clickable { selectedDeptCode = dept }
                                            ) {
                                                Text(
                                                    text = dept,
                                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.White else (if (isDarkMode) TextSecondary else Color(0xFF0F172A))
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = if (isDarkMode) ObsidianBorder else LightElevated)
                                Spacer(modifier = Modifier.height(12.dp))

                                // KPI Stat Counters with responsive wrapping
                                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                    if (maxWidth < 420.dp) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Total Enrolled", color = TextSecondary, fontSize = 10.sp)
                                                    Text("$deptEnrolled", color = CyberEmerald, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                                }
                                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                                    Text("Faculty Strength", color = TextSecondary, fontSize = 10.sp)
                                                    Text("$deptFacultyCount", color = ElectricIndigo, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                                }
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Avg Attendance", color = TextSecondary, fontSize = 10.sp)
                                                    Text("${"%.1f".format(deptAvgAttendance)}%", color = Color(0xFF10B981), fontSize = 16.sp, fontWeight = FontWeight.Black)
                                                }
                                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                                    Text("Mentorship Support", color = TextSecondary, fontSize = 10.sp)
                                                    Text("$deptDefaulters", color = NeonCoral, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                                }
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("Total Enrolled", color = TextSecondary, fontSize = 10.5.sp)
                                                Text("$deptEnrolled", color = CyberEmerald, fontSize = 17.sp, fontWeight = FontWeight.Black)
                                            }
                                            Column {
                                                Text("Faculty Strength", color = TextSecondary, fontSize = 10.5.sp)
                                                Text("$deptFacultyCount", color = ElectricIndigo, fontSize = 17.sp, fontWeight = FontWeight.Black)
                                            }
                                            Column {
                                                Text("Avg Attendance", color = TextSecondary, fontSize = 10.5.sp)
                                                Text("${"%.1f".format(deptAvgAttendance)}%", color = Color(0xFF10B981), fontSize = 17.sp, fontWeight = FontWeight.Black)
                                            }
                                            Column {
                                                Text("Mentorship Support", color = TextSecondary, fontSize = 10.5.sp)
                                                Text("$deptDefaulters", color = NeonCoral, fontSize = 17.sp, fontWeight = FontWeight.Black)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }



                    // Fast Action Bar: Publish Exam / Publish Results / HOD AI Bot
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showPublishScheduleDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("hod_publish_exam_btn")
                                ) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Publish Timetable", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }

                                Button(
                                    onClick = { showPublishResultDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("hod_publish_results_btn")
                                ) {
                                    Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF042F2E))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Publish Results", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF042F2E), maxLines = 1)
                                }
                            }

                            Button(
                                onClick = { onNavigateToChat("Hello HOD AI Chatbot. Assist with department operations, syllabus compliance, and faculty workloads.") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                            ) {
                                Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ask HOD AI Operations Advisor ✨", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    // Department Attendance Distribution Donut Chart (Dynamic)
                    item {
                        AttendanceDonutChart(
                            presentPct = deptDonutPresent,
                            absentPct = deptDonutAbsent,
                            latePct = deptDonutLate,
                            isDarkMode = isDarkMode
                        )
                    }

                    // Department Monthly Trend Chart (Dynamic)
                    item {
                        MonthlyTrendLineChart(
                            trends = deptMonthlyTrends,
                            isDarkMode = isDarkMode
                        )
                    }

                    // Department Subject Compliance Bar Chart (Dynamic)
                    item {
                        SubjectAttendanceBarChart(
                            subjects = deptSubjects,
                            isDarkMode = isDarkMode
                        )
                    }

                    // Live Period Schedule & Class Surveillance (Dynamic)
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
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
                                        text = "Today's Live Class Timetable ($selectedDeptCode)",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                            fontWeight = FontWeight.Bold
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
                                    )
                                    Surface(
                                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            "In Progress",
                                            color = Color(0xFF10B981),
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                deptTimetable.forEach { period ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            period.first,
                                            color = if (isDarkMode) Color(0xFFA5B4FC) else ElectricIndigo,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.width(100.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                period.second,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp,
                                                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                            )
                                            Text(
                                                period.third,
                                                fontSize = 10.5.sp,
                                                color = if (isDarkMode) TextSecondary else Color(0xFF475569)
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = if (isDarkMode) ObsidianBorder else LightElevated, thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }
            }

            // === 2. EXAM OPERATIONS: PUBLISH SCHEDULES & RESULTS ===
            HODTab.EXAM_OPERATIONS -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDarkMode) ObsidianElevated else LightSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Autonomous Exam Operations",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                            )
                                        )
                                        Text(
                                            text = "Published timetables and marks sync instantly to Student and Faculty logins.",
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.5.sp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { showPublishScheduleDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(42.dp)
                                    ) {
                                        Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Publish Timetable", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { showPublishResultDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(42.dp)
                                    ) {
                                        Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF042F2E))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Publish Results", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF042F2E))
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Currently Published Exam Timetables (${AcademicDataProvider.sharedExamSchedules.size})",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                            )
                        )
                    }

                    items(AcademicDataProvider.sharedExamSchedules) { schedule ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDarkMode) ObsidianSurface else LightSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = ElectricIndigo.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                schedule.subjectCode,
                                                color = if (isDarkMode) Color(0xFFA5B4FC) else ElectricIndigo,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            schedule.subjectName,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "📅 ${schedule.examDate} • ⏰ ${schedule.timeSlot} • [${schedule.examType}]",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 11.sp)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            "Live in Portals",
                                            color = Color(0xFF10B981),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { editingSchedule = schedule },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Schedule", tint = Color(0xFF8B5CF6), modifier = Modifier.size(17.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            AcademicDataProvider.sharedExamSchedules.remove(schedule)
                                            actionBannerMessage = "Removed timetable for ${schedule.subjectName}"
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Schedule", tint = Color(0xFFEF4444), modifier = Modifier.size(17.dp))
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Published Semester Grade Memos (${AcademicDataProvider.sharedExamResults.size})",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                            )
                        )
                    }

                    items(AcademicDataProvider.sharedExamResults) { res ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDarkMode) ObsidianSurface else LightSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        res.subjectName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                        )
                                    )
                                    Text(
                                        "Code: ${res.subjectCode} • Internal: ${res.internalMarks} • End-Term: ${res.endTermMarks} • Total: ${res.totalMarks}",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 11.sp)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = if (res.resultStatus == "PASSED") Color(0xFF10B981).copy(alpha = 0.2f) else NeonCoral.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            "Grade: ${res.gradeLetter} (${res.resultStatus})",
                                            color = if (res.resultStatus == "PASSED") Color(0xFF10B981) else NeonCoral,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { editingResult = res },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Result", tint = Color(0xFF8B5CF6), modifier = Modifier.size(17.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            AcademicDataProvider.sharedExamResults.remove(res)
                                            actionBannerMessage = "Removed grade memo for ${res.subjectName}"
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Result", tint = Color(0xFFEF4444), modifier = Modifier.size(17.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // === 3. ACADEMIC PERFORMANCE & MENTORSHIP HUB ===
            HODTab.BELOW_75_TRIAGE -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDarkMode) ObsidianElevated else LightSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(CyberEmerald.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.School, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(24.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Academic Performance & Mentorship Hub",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                        )
                                        Text(
                                            "Proactive academic counseling, remedial roadmaps, and dedicated faculty mentor allocation.",
                                            fontSize = 11.5.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Year & Section Filter Chips
                                Text("Filter by Class & Section:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val mentorshipSections = listOf("All Sections", "4th Yr CSE-B", "3rd Yr CSE-A", "2nd Yr CSE-B", "1st Yr CSE-A")
                                    items(mentorshipSections) { sec ->
                                        val isSelected = selectedMentorshipSectionFilter == sec
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedMentorshipSectionFilter = sec },
                                            label = { Text(sec, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = CyberEmeraldDark,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Filtered students list
                    val filteredMentorshipStudents = below75Students.filter { s ->
                        when (selectedMentorshipSectionFilter) {
                            "4th Yr CSE-B" -> s.section.contains("B", ignoreCase = true)
                            "3rd Yr CSE-A" -> s.section.contains("A", ignoreCase = true)
                            "2nd Yr CSE-B" -> s.section.contains("B", ignoreCase = true)
                            "1st Yr CSE-A" -> s.section.contains("A", ignoreCase = true)
                            else -> true
                        }
                    }

                    items(filteredMentorshipStudents) { student ->
                        val currentMentor = assignedMentors[student.rollNumber] ?: "Unassigned"
                        val currentRemedial = counselingStatuses[student.rollNumber] ?: "Standard Tracking"
                        val isAlerted = alertedParentRolls.contains(student.rollNumber)

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDarkMode) ObsidianSurface else LightSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(ElectricIndigo.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                student.rollNumber.takeLast(2),
                                                color = if (isDarkMode) Color(0xFFA5B4FC) else ElectricIndigo,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                student.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                            )
                                            Text(
                                                "Roll: ${student.rollNumber} • ${student.department} (${student.section})",
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }

                                    Surface(
                                        color = if (student.attendancePercentage < 65.0) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            "${student.attendancePercentage.toInt()}% Attd",
                                            color = if (student.attendancePercentage < 65.0) Color(0xFFEF4444) else Color(0xFFF59E0B),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Status Badges
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = if (currentMentor != "Unassigned") ElectricIndigo.copy(alpha = 0.15f) else Color(0xFF64748B).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(13.dp), tint = if (currentMentor != "Unassigned") ElectricIndigo else Color(0xFF64748B))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "Mentor: $currentMentor",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isDarkMode) Color.White else Color(0xFF1E293B)
                                            )
                                        }
                                    }

                                    Surface(
                                        color = if (currentRemedial != "Standard Tracking") CyberEmerald.copy(alpha = 0.15f) else Color(0xFF64748B).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(13.dp), tint = if (currentRemedial != "Standard Tracking") CyberEmerald else Color(0xFF64748B))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "Plan: $currentRemedial",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isDarkMode) Color.White else Color(0xFF1E293B)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Action Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            assignedMentors[student.rollNumber] = "Dr. P. V. Kishore (CSE Lead)"
                                            actionBannerMessage = "Assigned Dr. P. V. Kishore as academic mentor to ${student.name}."
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (currentMentor != "Unassigned") Color(0xFF4338CA) else ElectricIndigo
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            if (currentMentor != "Unassigned") "✓ Mentor Set" else "Assign Mentor",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            counselingStatuses[student.rollNumber] = "Active 30-Day Recovery"
                                            actionBannerMessage = "Enrolled ${student.name} into Active 30-Day Recovery Roadmap."
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (currentRemedial != "Standard Tracking") Color(0xFF065F46) else CyberEmeraldDark
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            if (currentRemedial != "Standard Tracking") "✓ In Recovery" else "Remedial Plan",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            if (isAlerted) {
                                                alertedParentRolls.remove(student.rollNumber)
                                            } else {
                                                alertedParentRolls.add(student.rollNumber)
                                                actionBannerMessage = "Dispatched SMS & Email Notice to Parent of ${student.name} (${student.rollNumber})"
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isAlerted) Color(0xFF065F46) else NeonCoral
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            if (isAlerted) "✓ Notice Sent" else "Alert Parent",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // === 4. FACULTY WORKLOAD & ALLOCATION ===
            HODTab.FACULTY_ALLOCATION -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "CSE Department Faculty Strength & Teaching Roster",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                            )
                        )
                    }

                    items(facultyList) { faculty ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDarkMode) ObsidianSurface else LightSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp)
                                    ) {
                                        Text(
                                            faculty.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "${faculty.designation} • ID: ${faculty.employeeId}",
                                            fontSize = 11.5.sp,
                                            color = if (isDarkMode) Color(0xFFA5B4FC) else ElectricIndigo,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    val teachingHours = (faculty.assignedSubjects.size * 5).coerceIn(15, 25)
                                    Surface(
                                        color = CyberEmerald.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            "$teachingHours hrs/wk",
                                            color = CyberEmerald,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            maxLines = 1
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Allocated: ${faculty.assignedSubjects.joinToString(", ")}",
                                    fontSize = 11.sp,
                                    color = if (isDarkMode) TextSecondary else Color(0xFF475569)
                                )
                            }
                        }
                    }
                }
            }

            // === 5. DEPARTMENT CIRCULARS ===
            HODTab.DEPARTMENT_CIRCULARS -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Department Circulars & Official Notices (${AcademicDataProvider.sharedFeedPosts.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                            )
                        )
                    }

                    items(AcademicDataProvider.sharedFeedPosts) { post ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDarkMode) ObsidianSurface else LightSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = LuminousAmber.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            post.tag,
                                            color = LuminousAmber,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(post.date, fontSize = 11.sp, color = TextSecondary)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    post.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    post.content,
                                    fontSize = 12.sp,
                                    color = if (isDarkMode) TextSecondary else Color(0xFF475569),
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "— ${post.authorName} (${post.authorRole})",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CyberEmerald
                                )
                            }
                        }
                    }
                }
            }

            // === 6. HOD PROFILE & DEPARTMENT SETTINGS ===
            HODTab.HOD_SETTINGS -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDarkMode) ObsidianElevated else LightSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF8B5CF6).copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(24.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "HOD Departmental & System Settings",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                        )
                                        Text(
                                            "Configure department profile, threshold automations, and live audit rules.",
                                            fontSize = 11.5.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                if (hodSettingsFeedback != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        color = Color(0xFF065F46),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(hodSettingsFeedback!!, color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text("HOD Profile Details", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isDarkMode) Color.White else Color(0xFF0F172A))
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = hodProfileName,
                                    onValueChange = { hodProfileName = it },
                                    label = { Text("Head of Department Name") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = hodProfileDept,
                                        onValueChange = { hodProfileDept = it },
                                        label = { Text("Department") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = hodProfileEmpId,
                                        onValueChange = { hodProfileEmpId = it },
                                        label = { Text("Employee ID") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = hodProfileCabin,
                                        onValueChange = { hodProfileCabin = it },
                                        label = { Text("Office Cabin") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = hodProfilePhone,
                                        onValueChange = { hodProfilePhone = it },
                                        label = { Text("Direct Line") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = hodProfileEmail,
                                    onValueChange = { hodProfileEmail = it },
                                    label = { Text("Official University Email") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        AcademicDataProvider.saveDataToDisk()
                                        hodSettingsFeedback = "✓ Department Settings Saved & Synchronized to Academic Server!"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(44.dp)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save HOD Settings", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
    }

    // Modal: Publish Exam Schedule Dialog
    if (showPublishScheduleDialog) {
        var exSubjectCode by remember { mutableStateOf("2339609") }
        var exSubjectName by remember { mutableStateOf("Deep Learning & Neural Networks") }
        var exDate by remember { mutableStateOf("28 Nov 2026") }
        var exTimeSlot by remember { mutableStateOf("10:00 AM - 01:00 PM") }
        var exType by remember { mutableStateOf("Autonomous End-Semester Theory") }

        AlertDialog(
            onDismissRequest = { showPublishScheduleDialog = false },
            title = { Text("Publish Exam Timetable to Campus", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "This schedule will immediately appear in Student & Faculty Exam Timetable views.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                    OutlinedTextField(
                        value = exSubjectCode,
                        onValueChange = { exSubjectCode = it },
                        label = { Text("Subject Code") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = exSubjectName,
                        onValueChange = { exSubjectName = it },
                        label = { Text("Subject Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = exDate,
                        onValueChange = { exDate = it },
                        label = { Text("Exam Date") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = exTimeSlot,
                        onValueChange = { exTimeSlot = it },
                        label = { Text("Time Slot") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = exType,
                        onValueChange = { exType = it },
                        label = { Text("Exam Classification") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        AcademicDataProvider.publishExamSchedule(
                            subjectCode = exSubjectCode,
                            subjectName = exSubjectName,
                            examDate = exDate,
                            timeSlot = exTimeSlot,
                            examType = exType
                        )
                        actionBannerMessage = "Published Exam Timetable for $exSubjectName. Broadcasted to Students & Faculty!"
                        showPublishScheduleDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                ) {
                    Text("Publish to Students & Faculty", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPublishScheduleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal: Publish Exam Results Dialog
    if (showPublishResultDialog) {
        var resSubjectCode by remember { mutableStateOf("2339751") }
        var resSubjectName by remember { mutableStateOf("Prompt Engineering & GenAI Lab") }
        var resInternal by remember { mutableStateOf("29.0") }
        var resEndTerm by remember { mutableStateOf("68.0") }

        AlertDialog(
            onDismissRequest = { showPublishResultDialog = false },
            title = { Text("Publish Official Exam Result", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Grades, SGPA points, and Pass/Fail status will sync immediately to Student grade memos.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                    OutlinedTextField(
                        value = resSubjectCode,
                        onValueChange = { resSubjectCode = it },
                        label = { Text("Subject Code") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = resSubjectName,
                        onValueChange = { resSubjectName = it },
                        label = { Text("Subject Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = resInternal,
                            onValueChange = { resInternal = it },
                            label = { Text("Internal (/30)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = resEndTerm,
                            onValueChange = { resEndTerm = it },
                            label = { Text("End-Term (/70)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intVal = resInternal.toDoubleOrNull() ?: 28.0
                        val extVal = resEndTerm.toDoubleOrNull() ?: 60.0
                        AcademicDataProvider.publishExamResult(
                            subjectCode = resSubjectCode,
                            subjectName = resSubjectName,
                            internalMarks = intVal,
                            endTermMarks = extVal
                        )
                        actionBannerMessage = "Published Grade Memo for $resSubjectName! Synced to Student & Faculty portals."
                        showPublishResultDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Publish Results", color = Color(0xFF042F2E), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPublishResultDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Exam Timetable Dialog with In-Place Color Feedback
    editingSchedule?.let { schedule ->
        var editDate by remember { mutableStateOf(schedule.examDate) }
        var editTime by remember { mutableStateOf(schedule.timeSlot) }
        var editType by remember { mutableStateOf(schedule.examType) }

        AlertDialog(
            onDismissRequest = { editingSchedule = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EditCalendar, contentDescription = null, tint = Color(0xFF8B5CF6))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Exam Schedule", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "${schedule.subjectName} (${schedule.subjectCode})",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B5CF6),
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = editDate,
                        onValueChange = { editDate = it },
                        label = { Text("Exam Date") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editTime,
                        onValueChange = { editTime = it },
                        label = { Text("Time Slot") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editType,
                        onValueChange = { editType = it },
                        label = { Text("Exam Type (e.g., Mid-I, Mid-II, Regular)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idx = AcademicDataProvider.sharedExamSchedules.indexOf(schedule)
                        if (idx != -1) {
                            AcademicDataProvider.sharedExamSchedules[idx] = schedule.copy(
                                examDate = editDate,
                                timeSlot = editTime,
                                examType = editType
                            )
                        }
                        actionBannerMessage = "Updated Exam Schedule for ${schedule.subjectName}! Live changes synced."
                        editingSchedule = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                ) {
                    Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingSchedule = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Grade Memo Result Dialog
    editingResult?.let { res ->
        var editInternal by remember { mutableStateOf(res.internalMarks.toString()) }
        var editEndTerm by remember { mutableStateOf(res.endTermMarks.toString()) }
        var editStatus by remember { mutableStateOf(res.resultStatus) }

        AlertDialog(
            onDismissRequest = { editingResult = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FactCheck, contentDescription = null, tint = Color(0xFF10B981))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Semester Grade Memo", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "${res.subjectName} (${res.subjectCode})",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        fontSize = 13.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editInternal,
                            onValueChange = { editInternal = it },
                            label = { Text("Internal Marks") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editEndTerm,
                            onValueChange = { editEndTerm = it },
                            label = { Text("End-Term Marks") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = editStatus,
                        onValueChange = { editStatus = it },
                        label = { Text("Result Status (PASSED / WITHHELD / FAILED)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intVal = editInternal.toDoubleOrNull() ?: res.internalMarks
                        val extVal = editEndTerm.toDoubleOrNull() ?: res.endTermMarks
                        val totVal = intVal + extVal
                        val grade = if (totVal >= 85) "A+" else if (totVal >= 75) "A" else if (totVal >= 65) "B" else "C"
                        val idx = AcademicDataProvider.sharedExamResults.indexOf(res)
                        if (idx != -1) {
                            AcademicDataProvider.sharedExamResults[idx] = res.copy(
                                internalMarks = intVal,
                                endTermMarks = extVal,
                                totalMarks = totVal,
                                gradeLetter = grade,
                                resultStatus = editStatus.uppercase()
                            )
                        }
                        actionBannerMessage = "Updated Grade Memo for ${res.subjectName}! Live changes synced."
                        editingResult = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Save Memo", color = Color(0xFF042F2E), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingResult = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
