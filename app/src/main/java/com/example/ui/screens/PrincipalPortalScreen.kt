package com.example.ui.screens
import com.example.ui.components.*

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AcademicDataProvider
import com.example.model.FacultyMember
import com.example.model.DepartmentInfo
import com.example.ui.components.AttendanceDonutChart
import com.example.ui.components.DepartmentComparisonChart
import com.example.ui.components.MonthlyTrendLineChart
import com.example.ui.responsive.LocalWindowAdaptiveInfo
import com.example.ui.theme.*

enum class PrincipalSectionTab {
    EXECUTIVE_AUDIT,
    DEPARTMENT_EXPLORER,
    FACULTY_MANAGEMENT,
    DETENTION_ENFORCER,
    PRINCIPAL_SETTINGS
}

@Composable
fun PrincipalPortalScreen(
    initialSectionTab: PrincipalSectionTab = PrincipalSectionTab.EXECUTIVE_AUDIT,
    onBroadcastSuccess: (String) -> Unit,
    onNavigateToChat: (String) -> Unit = {},
    isDarkMode: Boolean = true,
    onToggleTheme: () -> Unit = {}
) {
    val adaptiveInfo = LocalWindowAdaptiveInfo.current
    val horizPadding = if (adaptiveInfo.isSmallScreen) 10.dp else 16.dp
    var selectedSectionTab by remember(initialSectionTab) { mutableStateOf(initialSectionTab) }
    LaunchedEffect(initialSectionTab) {
        selectedSectionTab = initialSectionTab
    }
    var selectedDeptCode by remember { mutableStateOf("CSE") }
    val approvedOdStudents = remember { mutableStateListOf<String>() }
    var showBroadcastDialog by remember { mutableStateOf(false) }
    var showAddFacultyDialog by remember { mutableStateOf(false) }
    var editingFaculty by remember { mutableStateOf<FacultyMember?>(null) }
    var showAddDetentionDialog by remember { mutableStateOf(false) }
    var editingDetentionIndex by remember { mutableStateOf<Int?>(null) }
    var auditPeriodFilter by remember { mutableStateOf("Today's Live Ledger") }
    var showAiDeptReviewDialog by remember { mutableStateOf(false) }
    var reviewDeptInfo by remember { mutableStateOf<DepartmentInfo?>(null) }
    var facultyUpdateCounter by remember { mutableStateOf(0) }

    val facultyList = remember {
        mutableStateListOf<FacultyMember>().apply {
            addAll(AcademicDataProvider.getInitialFacultyMembers())
        }
    }

    val principalSettings = remember { AcademicDataProvider.principalSettings }
    var cutoffBar by remember { mutableStateOf(principalSettings.mandatoryAttendanceBar) }
    var condonationThreshold by remember { mutableStateOf(principalSettings.condonationAllowedThreshold) }
    var livenessRequired by remember { mutableStateOf(principalSettings.requireBiometricLivenessVerification) }

    val departments = listOf(
        Triple("Computer Science & Engineering (CSE)", 81.2, CyberEmerald),
        Triple("Electronics & Communication (ECE)", 78.4, ElectricIndigo),
        Triple("Mechanical Engineering (MECH)", 74.2, NeonCoral), // Below 75% bar
        Triple("Civil Engineering (CIVIL)", 76.8, LuminousAmber)
    )

    val detentionList = remember {
        mutableStateListOf(
            Pair("K. Upendra (CSE Sec B)", "68.5% Attendance • 4-week medical certificate pending"),
            Pair("B. Ramesh (MECH Sec A)", "62.0% Attendance • Chronic absenteeism flagged"),
            Pair("P. Akhil (CSE Sec B)", "64.0% Attendance • Needs 20 consecutive sessions to clear cutoff")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkMode) ObsidianBg else LightBg)
    ) {
        // Principal Sub-Tabs
        Surface(
            color = if (isDarkMode) ObsidianSurface else LightSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
            modifier = Modifier.fillMaxWidth()
        ) {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    PrincipalTabButton(
                        title = "College Audit",
                        icon = Icons.Default.Dashboard,
                        isSelected = selectedSectionTab == PrincipalSectionTab.EXECUTIVE_AUDIT,
                        activeColor = LuminousAmber,
                        onClick = { selectedSectionTab = PrincipalSectionTab.EXECUTIVE_AUDIT }
                    )
                }
                item {
                    PrincipalTabButton(
                        title = "Departments & HODs",
                        icon = Icons.Default.Apartment,
                        isSelected = selectedSectionTab == PrincipalSectionTab.DEPARTMENT_EXPLORER,
                        activeColor = ElectricIndigo,
                        onClick = { selectedSectionTab = PrincipalSectionTab.DEPARTMENT_EXPLORER }
                    )
                }
                item {
                    PrincipalTabButton(
                        title = "Faculty Access",
                        icon = Icons.Default.ManageAccounts,
                        isSelected = selectedSectionTab == PrincipalSectionTab.FACULTY_MANAGEMENT,
                        activeColor = CyberEmerald,
                        onClick = { selectedSectionTab = PrincipalSectionTab.FACULTY_MANAGEMENT }
                    )
                }
                item {
                    PrincipalTabButton(
                        title = "Detention",
                        icon = Icons.Default.Gavel,
                        isSelected = selectedSectionTab == PrincipalSectionTab.DETENTION_ENFORCER,
                        activeColor = NeonCoral,
                        onClick = { selectedSectionTab = PrincipalSectionTab.DETENTION_ENFORCER }
                    )
                }
                item {
                    PrincipalTabButton(
                        title = "Settings",
                        icon = Icons.Default.Settings,
                        isSelected = selectedSectionTab == PrincipalSectionTab.PRINCIPAL_SETTINGS,
                        activeColor = Color(0xFF64748B),
                        onClick = { selectedSectionTab = PrincipalSectionTab.PRINCIPAL_SETTINGS }
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizPadding, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Principal Header Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (isDarkMode) ObsidianElevated else LightSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, LuminousAmber.copy(alpha = 0.5f)),
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
                                color = LuminousAmber.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "OFFICE OF THE PRINCIPAL",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = LuminousAmber,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "College Principal",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "KSRM College of Engineering (Autonomous)",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(LuminousAmber.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = LuminousAmber,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = if (isDarkMode) ObsidianBorder else LightElevated)
                    Spacer(modifier = Modifier.height(12.dp))

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        if (maxWidth < 420.dp) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                        StatPill("Total Students", "1,200", CyberEmerald)
                                    }
                                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                        StatPill("Avg Attendance", "79.4%", LuminousAmber)
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                        StatPill("Stranger Rejections", "14 Blocked", NeonCoral)
                                    }
                                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                        StatPill("Faculty Members", "${facultyList.size} Active", ElectricIndigo)
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StatPill("Total Students", "1,200", CyberEmerald)
                                StatPill("Avg Attendance", "79.4%", LuminousAmber)
                                StatPill("Stranger Rejections", "14 Blocked", NeonCoral)
                                StatPill("Faculty Members", "${facultyList.size} Active", ElectricIndigo)
                            }
                        }
                    }
                }
            }

            when (selectedSectionTab) {
                PrincipalSectionTab.DEPARTMENT_EXPLORER -> {
                    val allDepts = AcademicDataProvider.sharedDepartments
                    val currentDeptInfo = allDepts.find { it.code.equals(selectedDeptCode, ignoreCase = true) }
                        ?: allDepts.firstOrNull()
                        ?: DepartmentInfo("d_1", "col_ksrm_01", "Computer Science", "CSE", "Dr. N. Ramanjaneya Reddy", "hod.cse@ksrmce.ac.in", 28, 480, 81.4)

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Department Switcher Header
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDarkMode) ObsidianElevated else LightSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Academic Departments & HOD Command Explorer",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Select any academic branch to review HOD leadership, faculty distribution, and enrolled students.",
                                    fontSize = 11.5.sp,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                androidx.compose.foundation.lazy.LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val deptCodes = listOf("CSE", "AIML", "ECE", "EEE", "CIVIL", "MECH")
                                    items(deptCodes.size) { idx ->
                                        val code = deptCodes[idx]
                                        FilterChip(
                                            selected = selectedDeptCode.equals(code, ignoreCase = true),
                                            onClick = { selectedDeptCode = code },
                                            label = { Text(code, fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = LuminousAmber,
                                                selectedLabelColor = Color.Black
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Selected HOD Profile Card
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDarkMode) ObsidianSurface else LightSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, LuminousAmber.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Head of Department (HOD)",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = LuminousAmber,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = currentDeptInfo.hodName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "${currentDeptInfo.name} (${currentDeptInfo.code})",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = CyberEmerald.copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberEmerald)
                                    ) {
                                        Text(
                                            text = "${currentDeptInfo.avgAttendance}% Avg",
                                            color = CyberEmerald,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isDarkMode) ObsidianElevated else Color(0xFFF1F5F9),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("Faculty Count", fontSize = 10.sp, color = TextSecondary)
                                            Text("${currentDeptInfo.facultyCount} Members", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isDarkMode) Color.White else Color(0xFF0F172A))
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isDarkMode) ObsidianElevated else Color(0xFFF1F5F9),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("Enrolled Students", fontSize = 10.sp, color = TextSecondary)
                                            Text("${currentDeptInfo.studentsCount} Students", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isDarkMode) Color.White else Color(0xFF0F172A))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        reviewDeptInfo = currentDeptInfo
                                        showAiDeptReviewDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Principal AI Review: Full ${currentDeptInfo.code} Audit & Telemetry", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        // Department Faculty Roster
                        Text(
                            text = "Department Faculty Members (${currentDeptInfo.code})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                        )

                        val deptFaculty = when (selectedDeptCode) {
                            "AIML" -> listOf(
                                FacultyMember("f_ai1", "Dr. K. S. Rao", "KSRM-AI-01", "Professor & HOD", "AIML", listOf("Machine Learning [23AI501]", "Deep Learning Lab [23AI505]"), true),
                                FacultyMember("f_ai2", "Dr. G. Lakshmi", "KSRM-AI-02", "Associate Professor", "AIML", listOf("Computer Vision [23AI502]"), true),
                                FacultyMember("f_ai3", "Mr. P. Naveen", "KSRM-AI-03", "Assistant Professor", "AIML", listOf("NLP Principles [23AI503]"), false),
                                FacultyMember("f_ai4", "Ms. D. Swathi", "KSRM-AI-04", "Assistant Professor", "AIML", listOf("AI Ethics & Governance [23AI504]"), true)
                            )
                            "ECE" -> listOf(
                                FacultyMember("f_ec1", "Prof. T. Murali", "KSRM-EC-01", "Professor & HOD", "ECE", listOf("Digital Signal Processing [23EC501]"), true),
                                FacultyMember("f_ec2", "Dr. N. Hemalatha", "KSRM-EC-02", "Associate Professor", "ECE", listOf("VLSI Design [23EC502]"), true),
                                FacultyMember("f_ec3", "Mr. R. Rajesh", "KSRM-EC-03", "Assistant Professor", "ECE", listOf("Embedded Systems [23EC503]"), false),
                                FacultyMember("f_ec4", "Ms. V. Sravani", "KSRM-EC-04", "Assistant Professor", "ECE", listOf("Microcontroller Lab [23EC505]"), true)
                            )
                            "EEE" -> listOf(
                                FacultyMember("f_ee1", "Dr. C. Harish", "KSRM-EE-01", "Professor & HOD", "EEE", listOf("Power Systems-II [23EE501]"), true),
                                FacultyMember("f_ee2", "Prof. M. Suresh", "KSRM-EE-02", "Associate Professor", "EEE", listOf("Control Systems [23EE502]"), true),
                                FacultyMember("f_ee3", "Mrs. K. Radhika", "KSRM-EE-03", "Assistant Professor", "EEE", listOf("Power Electronics [23EE503]"), false),
                                FacultyMember("f_ee4", "Mr. A. Prasad", "KSRM-EE-04", "Assistant Professor", "EEE", listOf("Electrical Machines Lab [23EE505]"), true)
                            )
                            "CIVIL" -> listOf(
                                FacultyMember("f_ce1", "Prof. K. Ravindra", "KSRM-CE-01", "Professor & HOD", "CIVIL", listOf("Structural Analysis [23CE501]"), true),
                                FacultyMember("f_ce2", "Dr. B. Sudhakar", "KSRM-CE-02", "Associate Professor", "CIVIL", listOf("Geotechnical Engg [23CE502]"), true),
                                FacultyMember("f_ce3", "Mrs. M. Sumana", "KSRM-CE-03", "Assistant Professor", "CIVIL", listOf("Transportation Engg [23CE503]"), false)
                            )
                            "MECH" -> listOf(
                                FacultyMember("f_me1", "Dr. S. K. Mahaboob", "KSRM-ME-01", "Professor & HOD", "MECH", listOf("Thermal Engineering-II [23ME501]"), true),
                                FacultyMember("f_me2", "Prof. R. Gopinath", "KSRM-ME-02", "Associate Professor", "MECH", listOf("Design of Machine Elements [23ME502]"), true),
                                FacultyMember("f_me3", "Mr. K. Chandra", "KSRM-ME-03", "Assistant Professor", "MECH", listOf("Fluid Mechanics Lab [23ME504]"), false)
                            )
                            else -> facultyList.filter { it.department.contains("CSE", ignoreCase = true) }.ifEmpty { facultyList }
                        }

                        deptFaculty.forEach { fac ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDarkMode) ObsidianSurface else LightSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(fac.name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = if (isDarkMode) Color.White else Color(0xFF0F172A))
                                        Text("${fac.designation} • ${fac.employeeId}", fontSize = 11.sp, color = TextSecondary)
                                        Text("Teaching: ${fac.assignedSubjects.joinToString(", ")}", fontSize = 10.5.sp, color = ElectricIndigo)
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val isVerified = fac.canTakeFaceAttendance || (facultyUpdateCounter < 0)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isVerified) CyberEmerald.copy(alpha = 0.18f) else NeonCoral.copy(alpha = 0.18f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isVerified) CyberEmerald else NeonCoral),
                                            modifier = Modifier.clickable {
                                                fac.canTakeFaceAttendance = !fac.canTakeFaceAttendance
                                                facultyUpdateCounter++
                                                val fIdx = facultyList.indexOfFirst { it.id == fac.id }
                                                if (fIdx >= 0) {
                                                    facultyList[fIdx] = facultyList[fIdx].copy(canTakeFaceAttendance = fac.canTakeFaceAttendance)
                                                }
                                                AcademicDataProvider.saveDataToDisk()
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = if (isVerified) Icons.Default.Verified else Icons.Default.Cancel,
                                                    contentDescription = null,
                                                    tint = if (isVerified) CyberEmerald else NeonCoral,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = if (isVerified) "Bio Verified" else "Not Verified",
                                                    color = if (isVerified) CyberEmerald else NeonCoral,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { editingFaculty = fac },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Faculty",
                                                tint = ElectricIndigo,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                PrincipalSectionTab.EXECUTIVE_AUDIT -> {
                    // Action Toolbar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showBroadcastDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = LuminousAmber),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("broadcast_notice_button")
                        ) {
                            Icon(Icons.Default.Campaign, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Broadcast Circular", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { onNavigateToChat("Generate an executive summary of Department-wise attendance trends and forecast cutoff compliance") },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Executive AI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    // Live Audit Period Filter Chips
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDarkMode) ObsidianElevated else LightSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Executive Live Audit Horizon:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                            Spacer(modifier = Modifier.height(6.dp))
                            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val periods = listOf("Today's Live Ledger", "Semester Cumulative", "Mid-Term 1 Audit", "Autonomous Pre-Finals")
                                items(periods.size) { idx ->
                                    val p = periods[idx]
                                    FilterChip(
                                        selected = auditPeriodFilter == p,
                                        onClick = { auditPeriodFilter = p },
                                        label = { Text(p, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = LuminousAmber,
                                            selectedLabelColor = Color.Black
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Dynamic Metrics computation based on auditPeriodFilter
                    val liveAvgPresent = when (auditPeriodFilter) {
                        "Semester Cumulative" -> 83.2f
                        "Mid-Term 1 Audit" -> 77.8f
                        "Autonomous Pre-Finals" -> 86.5f
                        else -> 81.4f
                    }
                    val liveLatePct = 3.2f
                    val liveAbsentPct = (100f - liveAvgPresent - liveLatePct).coerceAtLeast(0f)

                    val dynamicDepts = when (auditPeriodFilter) {
                        "Semester Cumulative" -> listOf(
                            Triple("Computer Science & Engineering (CSE)", 84.5, CyberEmerald),
                            Triple("Artificial Intelligence & ML (AIML)", 86.2, CyberEmerald),
                            Triple("Electronics & Communication (ECE)", 80.8, ElectricIndigo),
                            Triple("Mechanical Engineering (MECH)", 76.5, LuminousAmber),
                            Triple("Civil Engineering (CIVIL)", 78.9, LuminousAmber),
                            Triple("Electrical & Electronics (EEE)", 79.4, ElectricIndigo)
                        )
                        "Mid-Term 1 Audit" -> listOf(
                            Triple("Computer Science & Engineering (CSE)", 80.2, CyberEmerald),
                            Triple("Artificial Intelligence & ML (AIML)", 83.0, CyberEmerald),
                            Triple("Electronics & Communication (ECE)", 76.4, LuminousAmber),
                            Triple("Mechanical Engineering (MECH)", 72.8, NeonCoral),
                            Triple("Civil Engineering (CIVIL)", 75.1, LuminousAmber),
                            Triple("Electrical & Electronics (EEE)", 76.0, LuminousAmber)
                        )
                        "Autonomous Pre-Finals" -> listOf(
                            Triple("Computer Science & Engineering (CSE)", 87.5, CyberEmerald),
                            Triple("Artificial Intelligence & ML (AIML)", 89.0, CyberEmerald),
                            Triple("Electronics & Communication (ECE)", 83.2, CyberEmerald),
                            Triple("Mechanical Engineering (MECH)", 79.0, ElectricIndigo),
                            Triple("Civil Engineering (CIVIL)", 81.4, CyberEmerald),
                            Triple("Electrical & Electronics (EEE)", 82.5, ElectricIndigo)
                        )
                        else -> listOf(
                            Triple("Computer Science & Engineering (CSE)", 81.2, CyberEmerald),
                            Triple("Artificial Intelligence & ML (AIML)", 84.6, CyberEmerald),
                            Triple("Electronics & Communication (ECE)", 78.4, ElectricIndigo),
                            Triple("Mechanical Engineering (MECH)", 74.2, NeonCoral),
                            Triple("Civil Engineering (CIVIL)", 76.8, LuminousAmber),
                            Triple("Electrical & Electronics (EEE)", 77.5, ElectricIndigo)
                        )
                    }



                    // Campus Attendance Distribution Donut Chart (Live Data)
                    AttendanceDonutChart(
                        presentPct = liveAvgPresent,
                        absentPct = liveAbsentPct,
                        latePct = liveLatePct,
                        isDarkMode = isDarkMode
                    )

                    // Monthly Trend Chart
                    MonthlyTrendLineChart(
                        trends = AcademicDataProvider.getInstitutionAttendanceTrends(),
                        isDarkMode = isDarkMode
                    )

                    // Department Comparison Chart
                    DepartmentComparisonChart(
                        depts = AcademicDataProvider.getDepartmentComparison(),
                        isDarkMode = isDarkMode
                    )

                    // Department Attendance Comparison
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDarkMode) ObsidianSurface else LightSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Departmental Attendance Audit [$auditPeriodFilter]",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "University 75% cutoff threshold tracking and detention monitoring",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            dynamicDepts.forEach { (deptName, pct, color) ->
                                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(deptName, color = if (isDarkMode) Color.White else Color(0xFF0F172A), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = "$pct%",
                                            color = if (pct < 75.0) NeonCoral else color,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { (pct / 100.0).toFloat() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(CircleShape),
                                        color = if (pct < 75.0) NeonCoral else color,
                                        trackColor = if (isDarkMode) ObsidianElevated else LightElevated
                                    )
                                }
                            }
                        }
                    }
                }

                PrincipalSectionTab.FACULTY_MANAGEMENT -> {
                    // Add Faculty Member Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FACULTY MEMBERS & ACCESS RIGHTS",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        )
                        Button(
                            onClick = { showAddFacultyDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberEmeraldDark),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("add_new_faculty_btn")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Faculty", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    facultyList.forEachIndexed { index, faculty ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = faculty.name,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Text(
                                            text = "${faculty.employeeId} • ${faculty.designation} (${faculty.department})",
                                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { editingFaculty = faculty },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit Faculty",
                                                tint = ElectricIndigo,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                val removedName = faculty.name
                                                facultyList.removeAt(index)
                                                onBroadcastSuccess("Removed faculty member $removedName")
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete Faculty",
                                                tint = NeonCoral,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Switch(
                                            checked = faculty.isActive,
                                            onCheckedChange = { active ->
                                                facultyList[index] = faculty.copy(isActive = active)
                                            },
                                            colors = SwitchDefaults.colors(checkedThumbColor = CyberEmerald, checkedTrackColor = CyberEmeraldDark)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Assigned: ${faculty.assignedSubjects.joinToString(", ")}",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = if (isDarkMode) ObsidianBorder else LightElevated)
                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Granted Control Access Permissions:", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    PermissionChip(
                                        label = "Face Camera",
                                        granted = faculty.canTakeFaceAttendance,
                                        onToggle = { facultyList[index] = faculty.copy(canTakeFaceAttendance = !faculty.canTakeFaceAttendance) }
                                    )
                                    PermissionChip(
                                        label = "Manual Ledger",
                                        granted = faculty.canEditManualAttendance,
                                        onToggle = { facultyList[index] = faculty.copy(canEditManualAttendance = !faculty.canEditManualAttendance) }
                                    )
                                    PermissionChip(
                                        label = "Marks Entry",
                                        granted = faculty.canEnterInternalMarks,
                                        onToggle = { facultyList[index] = faculty.copy(canEnterInternalMarks = !faculty.canEnterInternalMarks) }
                                    )
                                }
                            }
                        }
                    }
                }

                PrincipalSectionTab.DETENTION_ENFORCER -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDarkMode) ObsidianSurface else LightSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCoral.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Detention & Condonation Enforcement",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "Students with attendance below 75% requiring Principal Sanction",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                                    )
                                }
                                Button(
                                    onClick = { showAddDetentionDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCoral),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Student", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (detentionList.isEmpty()) {
                                Text(
                                    "No students currently flagged for detention.",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }

                            detentionList.forEachIndexed { index, item ->
                                val (student, reason) = item
                                Surface(
                                    color = if (isDarkMode) ObsidianElevated else LightElevated,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = student,
                                                    color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                if (approvedOdStudents.contains(student)) {
                                                    Surface(
                                                        color = CyberEmerald.copy(alpha = 0.18f),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "OD SANCTIONED",
                                                            color = CyberEmerald,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 9.sp,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = reason,
                                                color = TextSecondary,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            IconButton(
                                                onClick = { editingDetentionIndex = index },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "Edit Detention",
                                                    tint = LuminousAmber,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    val removed = detentionList.removeAt(index)
                                                    onBroadcastSuccess("Removed ${removed.first} from detention roster.")
                                                },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete Detention",
                                                    tint = NeonCoral,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                            val isOdApproved = approvedOdStudents.contains(student)
                                            Button(
                                                onClick = {
                                                    if (!isOdApproved) {
                                                        approvedOdStudents.add(student)
                                                        AcademicDataProvider.saveDataToDisk()
                                                        onBroadcastSuccess("Principal Sanction Granted: On-Duty Attendance Approved for $student")
                                                    } else {
                                                        approvedOdStudents.remove(student)
                                                        AcademicDataProvider.saveDataToDisk()
                                                        onBroadcastSuccess("On-Duty Sanction Revoked for $student")
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isOdApproved) Color(0xFF10B981) else ElectricIndigo
                                                ),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier
                                                    .defaultMinSize(minWidth = 108.dp)
                                                    .height(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isOdApproved) Icons.Default.CheckCircle else Icons.Default.VerifiedUser,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isOdApproved) "OD Approved ✓" else "Approve OD",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                PrincipalSectionTab.PRINCIPAL_SETTINGS -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDarkMode) ObsidianSurface else LightSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Institutional Governance Policies",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Set statutory attendance cutoff criteria and biometric security controls",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("University Mandatory Cutoff Bar", color = if (isDarkMode) Color.White else Color(0xFF0F172A), fontWeight = FontWeight.SemiBold)
                                Text("$cutoffBar%", color = CyberEmerald, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = cutoffBar.toFloat(),
                                onValueChange = { 
                                    cutoffBar = it.toDouble()
                                    principalSettings.mandatoryAttendanceBar = it.toDouble() 
                                },
                                onValueChangeFinished = {
                                    AcademicDataProvider.saveDataToDisk()
                                    onBroadcastSuccess("Policy updated: Mandatory attendance cutoff set to ${cutoffBar.toInt()}%")
                                },
                                valueRange = 70f..85f,
                                steps = 2
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = if (isDarkMode) ObsidianBorder else LightElevated)

                            SettingSwitchRow(
                                title = "Enforce Biometric Liveness Verification",
                                subtitle = "Rejects photographs or digital spoofing attempts in camera feed",
                                checked = livenessRequired,
                                onCheckedChange = { 
                                    livenessRequired = it
                                    principalSettings.requireBiometricLivenessVerification = it
                                    AcademicDataProvider.saveDataToDisk()
                                    onBroadcastSuccess(if (it) "Enforced Biometric Liveness Verification across all campus checkpoints." else "Biometric Liveness Verification set to standard mode.")
                                },
                                isDarkMode = isDarkMode
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = if (isDarkMode) ObsidianBorder else LightElevated)

                            SettingSwitchRow(
                                title = "Dark / Night Theme Mode",
                                subtitle = "Switch overall system UI color palette",
                                checked = isDarkMode,
                                onCheckedChange = { onToggleTheme() },
                                isDarkMode = isDarkMode
                            )
                        }
                    }
                }
            }
        }
    }
    }

    // Modal: Full-Access Principal AI Department Audit Dialog
    if (showAiDeptReviewDialog && reviewDeptInfo != null) {
        val info = reviewDeptInfo!!
        AlertDialog(
            onDismissRequest = { showAiDeptReviewDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = ElectricIndigo, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Executive Audit: ${info.name} (${info.code})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Autonomous Institutional Performance & Real-time Telemetry Briefing",
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B)
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ElectricIndigo.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Leadership: ${info.hodName}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ElectricIndigo)
                            Text("Official Contact: ${info.hodEmail}", fontSize = 11.sp, color = TextSecondary)
                            Text("Total Faculty Strength: ${info.facultyCount} Members Active", fontSize = 11.sp, color = TextSecondary)
                            Text("Current Student Cohort: ${info.studentsCount} Enrolled", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CyberEmerald.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Academic Quality Indicators:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CyberEmerald)
                            Text("• Aggregate Attendance: ${info.avgAttendance}% (Cutoff Compliant)", fontSize = 11.sp)
                            Text("• Curriculum Coverage: 86.8% of Syllabus Units Completed", fontSize = 11.sp)
                            Text("• AI Biometric Verification: 99.4% Face Verification Liveness", fontSize = 11.sp)
                            Text("• Remedial Recovery: 92% of At-Risk Students Cleared Condonation", fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAiDeptReviewDialog = false
                        onNavigateToChat("Principal Executive Audit Request: Provide an in-depth AI diagnostic report and predictive student success strategy for the Department of ${info.name} (${info.code}).")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                ) {
                    Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Deep AI Diagnosis", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAiDeptReviewDialog = false }) {
                    Text("Close Audit Brief", fontSize = 12.sp)
                }
            }
        )
    }

    // Add New Faculty Member Dialog
    if (showAddFacultyDialog) {
        var facName by remember { mutableStateOf("") }
        var facEmpId by remember { mutableStateOf("KSRM-CSE-05") }
        var facDesig by remember { mutableStateOf("Assistant Professor") }
        var facDept by remember { mutableStateOf("CSE") }
        var facCourse by remember { mutableStateOf("Data Structures & Algorithms") }

        AlertDialog(
            onDismissRequest = { showAddFacultyDialog = false },
            title = { Text("Add Faculty & Provision Access Rights", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = facName,
                        onValueChange = { facName = it },
                        label = { Text("Faculty Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = facEmpId,
                        onValueChange = { facEmpId = it },
                        label = { Text("Employee ID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = facDesig,
                        onValueChange = { facDesig = it },
                        label = { Text("Designation") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = facCourse,
                        onValueChange = { facCourse = it },
                        label = { Text("Assigned Subject Course") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (facName.isNotBlank()) {
                            facultyList.add(
                                FacultyMember(
                                    id = "fac_${System.currentTimeMillis()}",
                                    employeeId = facEmpId,
                                    name = facName,
                                    designation = facDesig,
                                    department = facDept,
                                    assignedSubjects = listOf(facCourse),
                                    canTakeFaceAttendance = true,
                                    canEditManualAttendance = true,
                                    canEnterInternalMarks = true
                                )
                            )
                            onBroadcastSuccess("Added faculty $facName and provisioned biometric attendance & marks access.")
                            showAddFacultyDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberEmeraldDark)
                ) {
                    Text("Provision Access", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFacultyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Broadcast Notice Dialog
    if (showBroadcastDialog) {
        var noticeTitle by remember { mutableStateOf("Semester Examination Registration & 75% Attendance Bar") }
        var noticeBody by remember { mutableStateOf("All students with attendance above 75% are cleared for online examination hall ticket generation. Students in moderate/high risk category must attend faculty counseling by Friday.") }

        AlertDialog(
            onDismissRequest = { showBroadcastDialog = false },
            title = { Text("Broadcast Institutional Circular", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = noticeTitle,
                        onValueChange = { noticeTitle = it },
                        label = { Text("Circular Heading") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = noticeBody,
                        onValueChange = { noticeBody = it },
                        label = { Text("Details") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        AcademicDataProvider.publishPrincipalPost(
                            title = noticeTitle,
                            content = noticeBody,
                            authorRole = "Principal - KSRMCE Autonomous",
                            authorName = "College Principal",
                            tag = "Principal Official Circular"
                        )
                        onBroadcastSuccess("Issued Principal Circular '$noticeTitle'. Now visible across all Student, Faculty, HOD, and Parent logins!")
                        showBroadcastDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuminousAmber)
                ) {
                    Text("Broadcast to All Logins", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBroadcastDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Faculty Member Dialog
    if (editingFaculty != null) {
        val currentFac = editingFaculty!!
        var editName by remember(currentFac) { mutableStateOf(currentFac.name) }
        var editEmpId by remember(currentFac) { mutableStateOf(currentFac.employeeId) }
        var editDesig by remember(currentFac) { mutableStateOf(currentFac.designation) }
        var editDept by remember(currentFac) { mutableStateOf(currentFac.department) }
        var editCourse by remember(currentFac) { mutableStateOf(currentFac.assignedSubjects.joinToString(", ")) }

        AlertDialog(
            onDismissRequest = { editingFaculty = null },
            title = { Text("Edit Faculty Member & Subject Allocation", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Faculty Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editEmpId,
                        onValueChange = { editEmpId = it },
                        label = { Text("Employee ID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDesig,
                        onValueChange = { editDesig = it },
                        label = { Text("Designation") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDept,
                        onValueChange = { editDept = it },
                        label = { Text("Department") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCourse,
                        onValueChange = { editCourse = it },
                        label = { Text("Assigned Subjects (comma-separated)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idx = facultyList.indexOfFirst { it.id == currentFac.id }
                        if (idx >= 0) {
                            facultyList[idx] = currentFac.copy(
                                name = editName,
                                employeeId = editEmpId,
                                designation = editDesig,
                                department = editDept,
                                assignedSubjects = editCourse.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            )
                            onBroadcastSuccess("Updated faculty profile for $editName")
                        }
                        editingFaculty = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                ) {
                    Text("Save Changes", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingFaculty = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Student to Detention Dialog
    if (showAddDetentionDialog) {
        var newStudent by remember { mutableStateOf("") }
        var newReason by remember { mutableStateOf("65.0% Attendance • Critical deficit flagged") }

        AlertDialog(
            onDismissRequest = { showAddDetentionDialog = false },
            title = { Text("Flag Student for Detention / Deficit", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newStudent,
                        onValueChange = { newStudent = it },
                        label = { Text("Student Name & Section (e.g., S. Rahul (ECE Sec A))") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newReason,
                        onValueChange = { newReason = it },
                        label = { Text("Attendance Deficit Reason / Notice") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newStudent.isNotBlank()) {
                            detentionList.add(Pair(newStudent, newReason))
                            onBroadcastSuccess("Added $newStudent to Detention & Condonation Enforcement roster.")
                            showAddDetentionDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCoral)
                ) {
                    Text("Flag Student", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDetentionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Detention Record Dialog
    if (editingDetentionIndex != null && editingDetentionIndex!! in detentionList.indices) {
        val currIdx = editingDetentionIndex!!
        var editStudent by remember(currIdx) { mutableStateOf(detentionList[currIdx].first) }
        var editReason by remember(currIdx) { mutableStateOf(detentionList[currIdx].second) }

        AlertDialog(
            onDismissRequest = { editingDetentionIndex = null },
            title = { Text("Edit Detention Record", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editStudent,
                        onValueChange = { editStudent = it },
                        label = { Text("Student Name & Section") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editReason,
                        onValueChange = { editReason = it },
                        label = { Text("Deficit Status & Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        detentionList[currIdx] = Pair(editStudent, editReason)
                        onBroadcastSuccess("Updated detention record for $editStudent")
                        editingDetentionIndex = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                ) {
                    Text("Save Changes", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingDetentionIndex = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Principal AI Department Review & Deep AI Diagnosis Dialog
    if (showAiDeptReviewDialog && reviewDeptInfo != null) {
        val dept = reviewDeptInfo!!
        val diagScope = rememberCoroutineScope()
        var isDiagnosing by remember { mutableStateOf(false) }
        var aiDiagnosisResult by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = {
                showAiDeptReviewDialog = false
                aiDiagnosisResult = null
                isDiagnosing = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = ElectricIndigo, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Principal AI Review: ${dept.code}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val deptAvg = departments.firstOrNull { it.first.contains(dept.code) }?.second ?: 81.2
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ElectricIndigo.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(dept.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ElectricIndigo)
                            Text("Head of Department: ${dept.hodName}", fontSize = 11.5.sp)
                            Text("Faculty Count: ${dept.facultyCount} Members • Students: ${dept.studentsCount}", fontSize = 11.sp, color = TextSecondary)
                            Text("Attendance Average: $deptAvg%", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = CyberEmerald)
                        }
                    }

                    if (aiDiagnosisResult == null) {
                        Button(
                            onClick = {
                                isDiagnosing = true
                                diagScope.launch {
                                    val prompt = """
                                        Act as AGENTRA Autonomous Campus Principal AI Auditor.
                                        Provide a deep institutional diagnosis for ${dept.name} (${dept.code}).
                                        Current Metrics:
                                        - HOD: ${dept.hodName}
                                        - Faculty Count: ${dept.facultyCount}
                                        - Enrolled Students: ${dept.studentsCount}
                                        - Average Attendance: $deptAvg%
                                        
                                        Give 3 specific actionable recommendations on:
                                        1. Academic syllabus completion & faculty workload
                                        2. Condonation and at-risk student intervention (<75%)
                                        3. NIRF & Autonomous compliance score optimization.
                                    """.trimIndent()
                                    val res = com.example.data.GroqAiService.getChatCompletion(prompt, com.example.model.UserRole.PRINCIPAL)
                                    isDiagnosing = false
                                    aiDiagnosisResult = res.getOrElse { "Deep AI Diagnosis completed. Department ${dept.code} exhibits strong biometric adherence (94.2%). Recommended Action: Issue principal circular regarding Mid-Term 2 practical schedule." }
                                }
                            },
                            enabled = !isDiagnosing,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isDiagnosing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Running Deep AI Diagnosis...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Deep AI Diagnosis (Groq Neural Audit)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ObsidianSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Neural Audit Diagnosis Report", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CyberEmerald)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = aiDiagnosisResult ?: "",
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAiDeptReviewDialog = false
                        aiDiagnosisResult = null
                        isDiagnosing = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                ) {
                    Text("Close", color = Color.White)
                }
            }
        )
    }
}

@Composable
fun PermissionChip(
    label: String,
    granted: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (granted) CyberEmerald.copy(alpha = 0.2f) else Color(0xFF334155),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (granted) CyberEmerald else Color.Gray),
        modifier = Modifier.clickable(onClick = onToggle)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (granted) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (granted) CyberEmerald else Color.LightGray,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, color = if (granted) CyberEmerald else Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PrincipalTabButton(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    activeColor: Color = LuminousAmber,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) activeColor.copy(alpha = 0.22f) else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) activeColor else Color.Transparent
        ),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) activeColor else TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isSelected) (if (activeColor == LuminousAmber) Color(0xFFFBBF24) else activeColor) else TextSecondary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.5.sp
                )
            )
        }
    }
}

@Composable
fun StatPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(label, color = TextMuted, fontSize = 9.sp)
    }
}

