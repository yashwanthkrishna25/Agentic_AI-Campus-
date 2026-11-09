package com.example.ui.screens
import com.example.ui.components.*

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.FirestoreRepository
import com.example.model.AppIssueReport
import com.example.model.CollegeOrganization
import com.example.model.DepartmentInfo
import com.example.model.SubjectCourse
import com.example.model.TimetableSlot
import com.example.ui.components.AttendanceDonutChart
import com.example.ui.components.DepartmentComparisonChart
import com.example.ui.components.MonthlyTrendLineChart
import com.example.ui.responsive.LocalWindowAdaptiveInfo
import com.example.ui.theme.*
import kotlinx.coroutines.launch

enum class AdminTab {
    INSTITUTION_PROFILE,
    DEPARTMENTS_COURSES,
    TIMETABLE_ALLOCATIONS,
    CAMPUS_ANALYTICS,
    ORGANIZATIONS,
    DIAGNOSTICS_ISSUES,
    ADMIN_SETTINGS
}

data class CampusFacility(
    val id: String,
    val name: String,
    val category: String,
    val location: String,
    val status: String, // "Operational", "Under Maintenance", "Inactive"
    val inChargeStaff: String,
    val lastAudited: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPortalScreen(
    initialTab: AdminTab = AdminTab.INSTITUTION_PROFILE,
    isDarkMode: Boolean = true,
    onToggleTheme: () -> Unit = {},
    onNavigateToChat: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val adaptiveInfo = LocalWindowAdaptiveInfo.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    LaunchedEffect(initialTab) {
        selectedTab = initialTab
    }

    // Multi-College Directory States
    val colleges = remember {
        mutableStateListOf<CollegeOrganization>().apply {
            addAll(AcademicDataProvider.getInitialColleges())
        }
    }
    var showAddCollegeDialog by remember { mutableStateOf(false) }
    var editingCollege by remember { mutableStateOf<CollegeOrganization?>(null) }

    // Institutional Profile States
    var instName by remember { mutableStateOf("K.S.R.M. College of Engineering (Autonomous)") }
    var instCode by remember { mutableStateOf("KSRMCE-9Y") }
    var principalName by remember { mutableStateOf("Dr. V. S. S. Murthy, M.Tech, Ph.D.") }
    var instEmail by remember { mutableStateOf("principal@ksrmce.ac.in") }
    var instAddress by remember { mutableStateOf("Yerramasupalli, Kadapa, Andhra Pradesh 516003") }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    // Department & Course States
    val departments = AcademicDataProvider.sharedDepartments
    val courses = AcademicDataProvider.sharedCourses
    var showAddDeptDialog by remember { mutableStateOf(false) }
    var editingDept by remember { mutableStateOf<DepartmentInfo?>(null) }
    var selectedInspectDept by remember { mutableStateOf<DepartmentInfo?>(null) }
    var showDeptAccessDialog by remember { mutableStateOf(false) }
    var newDeptName by remember { mutableStateOf("") }
    var newDeptCode by remember { mutableStateOf("") }
    var newDeptHod by remember { mutableStateOf("") }

    var showAddCourseDialog by remember { mutableStateOf(false) }
    var editingCourse by remember { mutableStateOf<SubjectCourse?>(null) }
    var newCourseCode by remember { mutableStateOf("") }
    var newCourseName by remember { mutableStateOf("") }
    var newCourseFaculty by remember { mutableStateOf("") }
    var newCourseCredits by remember { mutableStateOf("3") }

    // Timetable States
    val timetable = remember { mutableStateListOf<TimetableSlot>().apply { addAll(AcademicDataProvider.getWeeklyTimetable()) } }
    var selectedTimetableDept by remember { mutableStateOf("CSE") }
    var selectedDayFilter by remember { mutableStateOf("All") }
    var showAddSlotDialog by remember { mutableStateOf(false) }
    var newSlotDay by remember { mutableStateOf("Monday") }
    var newSlotPeriod by remember { mutableStateOf("1") }
    var newSlotTime by remember { mutableStateOf("09:00 - 10:00 AM") }
    var newSlotSubject by remember { mutableStateOf("Deep Learning") }
    var newSlotFaculty by remember { mutableStateOf("Dr. P. V. Kishore") }
    var newSlotRoom by remember { mutableStateOf("Lab 402") }

    // Campus Infrastructure & Facilities States
    val campusFacilities = remember {
        mutableStateListOf(
            CampusFacility("fac_1", "High-Performance AI & GPU Computing Cluster", "Computing Infrastructure", "Main Block - Lab 402", "Operational", "Dr. P. V. Kishore", "01 Nov 2026"),
            CampusFacility("fac_2", "Autonomous IoT Biometric Turnstiles", "Security & Attendance", "Campus Gates 1, 2, 3", "Operational", "Security Lead Officer", "31 Oct 2026"),
            CampusFacility("fac_3", "Central University Digital Library Server Node", "Network & Systems", "Library 2nd Floor", "Under Maintenance", "Prof. S. R. Varma", "28 Oct 2026"),
            CampusFacility("fac_4", "ECE Advanced Microcontroller & Robotics Lab", "Department Labs", "ECE Block - Lab 204", "Operational", "Prof. T. Murali", "30 Oct 2026"),
            CampusFacility("fac_5", "Smart Audio-Visual Seminar Hall 1 & 2", "Auditoriums", "Administrative Block", "Operational", "Facilities Dean", "25 Oct 2026"),
            CampusFacility("fac_6", "Campus Solar Microgrid & 250kVA Generator UPS", "Power & Utility", "Utility Complex", "Operational", "Dr. C. Harish (EEE)", "01 Nov 2026")
        )
    }
    var editingFacility by remember { mutableStateOf<CampusFacility?>(null) }
    var showAddFacilityDialog by remember { mutableStateOf(false) }

    // Issues & Alerts States
    val issueReports = remember { mutableStateListOf<AppIssueReport>().apply { addAll(AcademicDataProvider.getInitialIssueReports()) } }
    var showReportIssueDialog by remember { mutableStateOf(false) }
    var newIssueTitle by remember { mutableStateOf("") }
    var newIssueDesc by remember { mutableStateOf("") }
    var newIssueLocation by remember { mutableStateOf("Mobile App Feature") }

    // Settings States
    val adminSettings = remember { AcademicDataProvider.adminSettings }
    var autoBackup by remember { mutableStateOf(adminSettings.autoBackupFirestoreDaily) }
    var emailAlerts by remember { mutableStateOf(adminSettings.receiveIssueAlertsViaEmail) }
    var timetableAutoLock by remember { mutableStateOf(true) }
    var biometricThreshold by remember { mutableStateOf("75.0%") }
    var adminProfileName by remember { mutableStateOf("Er. T. Sreenivasulu Reddy") }
    var adminProfileEmpId by remember { mutableStateOf("KSRM-ADM-001") }
    var adminProfilePhone by remember { mutableStateOf("+91 94401 23456") }
    var adminProfileEmail by remember { mutableStateOf("admin.ops@ksrmce.ac.in") }
    var adminEndpointUrl by remember { mutableStateOf("https://firestore.googleapis.com/v1/projects/agentra-campus") }
    var adminSettingsSavedBanner by remember { mutableStateOf<String?>(null) }

    var feedbackBanner by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        FirestoreRepository.observeSystemAlerts().collect { list ->
            issueReports.clear()
            issueReports.addAll(list)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkMode) ObsidianBg else LightBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
        // Tab Navigation
        Surface(
            color = if (isDarkMode) ObsidianSurface else LightSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = selectedTab == AdminTab.INSTITUTION_PROFILE,
                        onClick = { selectedTab = AdminTab.INSTITUTION_PROFILE },
                        label = { Text("Profile", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberViolet,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("admin_tab_profile")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTab == AdminTab.DEPARTMENTS_COURSES,
                        onClick = { selectedTab = AdminTab.DEPARTMENTS_COURSES },
                        label = { Text("Departments & Courses", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberViolet,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("admin_tab_departments")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTab == AdminTab.TIMETABLE_ALLOCATIONS,
                        onClick = { selectedTab = AdminTab.TIMETABLE_ALLOCATIONS },
                        label = { Text("Timetable & Staff", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberViolet,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("admin_tab_timetable")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTab == AdminTab.CAMPUS_ANALYTICS,
                        onClick = { selectedTab = AdminTab.CAMPUS_ANALYTICS },
                        label = { Text("Attendance Charts", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberViolet,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("admin_tab_analytics")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTab == AdminTab.ORGANIZATIONS,
                        onClick = { selectedTab = AdminTab.ORGANIZATIONS },
                        label = { Text("Colleges Directory (${colleges.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.CorporateFare, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberViolet,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("admin_tab_colleges")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTab == AdminTab.DIAGNOSTICS_ISSUES,
                        onClick = { selectedTab = AdminTab.DIAGNOSTICS_ISSUES },
                        label = { Text("Facilities (${campusFacilities.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Hub, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberViolet,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("admin_tab_facilities")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTab == AdminTab.ADMIN_SETTINGS,
                        onClick = { selectedTab = AdminTab.ADMIN_SETTINGS },
                        label = { Text("Settings", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberViolet,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("admin_tab_settings")
                    )
                }
            }
        }

        if (feedbackBanner != null) {
            Surface(
                color = CyberViolet.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberViolet),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberSuccess, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(feedbackBanner!!, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { feedbackBanner = null }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
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
                when (selectedTab) {
            AdminTab.INSTITUTION_PROFILE -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Profile Header Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDarkMode) ObsidianElevated else LightSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberViolet.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Surface(
                                            color = CyberViolet.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "COLLEGE ADMIN PORTAL",
                                                color = CyberViolet,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = instName,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDarkMode) Color.White else LightTextPrimary
                                            )
                                        )
                                        Text(
                                            text = "Code: $instCode • Affiliated to JNTUA • NAAC Grade A+",
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                        )
                                    }

                                    IconButton(
                                        onClick = { showEditProfileDialog = true },
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(CyberViolet.copy(alpha = 0.2f))
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = CyberViolet)
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Fast stats
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Departments", color = TextSecondary, fontSize = 11.sp)
                                        Text("${departments.size}", color = CyberViolet, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text("Students", color = TextSecondary, fontSize = 11.sp)
                                        Text("${departments.sumOf { it.studentsCount }}", color = CyberSuccess, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text("Faculty", color = TextSecondary, fontSize = 11.sp)
                                        Text("${departments.sumOf { it.facultyCount }}", color = CyberWarning, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text("Campus Status", color = TextSecondary, fontSize = 11.sp)
                                        Text("ACTIVE", color = CyberSuccess, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Institutional Details
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDarkMode) ObsidianElevated else LightSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Governance & Accreditation", fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else LightTextPrimary)

                                DetailItem(Icons.Default.Person, "College Principal", principalName, isDarkMode)
                                DetailItem(Icons.Default.Email, "Official Email", instEmail, isDarkMode)
                                DetailItem(Icons.Default.LocationOn, "Campus Location", instAddress, isDarkMode)
                                DetailItem(Icons.Default.Verified, "Accreditation", "NAAC A+ (Cycle 2) • NBA Accredited (CSE, ECE, MECH)", isDarkMode)
                            }
                        }
                    }
                }
            }

            AdminTab.DEPARTMENTS_COURSES -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Academic Departments (${departments.size})",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkMode) Color.White else LightTextPrimary
                                )
                            )
                            Button(
                                onClick = { showAddDeptDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberViolet),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Dept", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    items(departments) { dept ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDarkMode) ObsidianElevated else LightSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightBorder),
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
                                            text = "${dept.name} (${dept.code})",
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkMode) Color.White else LightTextPrimary,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "HOD: ${dept.hodName} • ${dept.hodEmail}",
                                            color = TextSecondary,
                                            fontSize = 11.5.sp
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = CyberViolet.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "${dept.avgAttendance}% Att.",
                                                color = CyberViolet,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        IconButton(onClick = { editingDept = dept }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Dept", tint = CyberViolet, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(onClick = {
                                            departments.remove(dept)
                                            feedbackBanner = "Removed ${dept.name} (${dept.code})"
                                        }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Dept", tint = CyberError, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Students: ${dept.studentsCount}", color = TextIceBlue, fontSize = 11.sp)
                                    Text("Faculty: ${dept.facultyCount}", color = TextIceBlue, fontSize = 11.sp)
                                    Button(
                                        onClick = {
                                            selectedInspectDept = dept
                                            showDeptAccessDialog = true
                                            feedbackBanner = "Viewing full ledger access for ${dept.name} (${dept.code})"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberViolet),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Inspect Access", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Courses section
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Active Courses Catalogue (${courses.size})",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkMode) Color.White else LightTextPrimary
                                )
                            )
                            Button(
                                onClick = { showAddCourseDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberViolet),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Course", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    items(courses) { course ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDarkMode) ObsidianElevated else LightSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${course.name} [${course.code}]",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkMode) Color.White else LightTextPrimary,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Faculty: ${course.facultyName} • ${course.courseType} (${course.credits} Credits)",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${course.attendedSessions}/${course.totalSessions} sessions",
                                        color = CyberViolet,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(onClick = { editingCourse = course }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Course", tint = CyberViolet, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(onClick = {
                                        courses.remove(course)
                                        feedbackBanner = "Removed course ${course.name}"
                                    }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Course", tint = CyberError, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AdminTab.TIMETABLE_ALLOCATIONS -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Class Timetable & Staff Allocations",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkMode) Color.White else LightTextPrimary
                                    )
                                )
                                Text(
                                    text = "$selectedTimetableDept Department - Autonomous Schedule & Staff Roster",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        com.example.util.PdfHelper.downloadPdfToDevice(
                                            context = context,
                                            fileNameOrAsset = "VIISEM 26-27.pdf",
                                            displayTitle = "${selectedTimetableDept}_Official_Timetable.pdf"
                                        )
                                        feedbackBanner = "Downloaded $selectedTimetableDept Timetable PDF to phone Downloads!"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, tint = ObsidianBg, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Download Timetable PDF",
                                        color = ObsidianBg,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Button(
                                    onClick = { showAddSlotDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberViolet),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Slot", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Department Filter Chips
                    item {
                        Text("Select Department:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val ttDepts = listOf("CSE", "AIML", "ECE", "EEE", "CIVIL", "MECH")
                            items(ttDepts) { deptCode ->
                                val isSelected = selectedTimetableDept == deptCode
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedTimetableDept = deptCode },
                                    label = { Text(deptCode, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyberViolet,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // Day Filter Chips
                    item {
                        Text("Filter by Day of Week:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val days = listOf("All", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
                            items(days) { day ->
                                FilterChip(
                                    selected = selectedDayFilter == day,
                                    onClick = { selectedDayFilter = day },
                                    label = { Text(day, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyberViolet,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    val dynamicSlots = when (selectedTimetableDept) {
                        "AIML" -> listOf(
                            TimetableSlot("t_ai1", "Monday", 1, "09:00 - 10:00 AM", "23AI501", "Machine Learning", "Dr. K. S. Rao", "AI Lab 1"),
                            TimetableSlot("t_ai2", "Monday", 2, "10:00 - 11:00 AM", "23AI502", "Computer Vision", "Dr. G. Lakshmi", "Room 304"),
                            TimetableSlot("t_ai3", "Tuesday", 1, "09:00 - 10:00 AM", "23AI503", "NLP Principles", "Mr. P. Naveen", "Room 304"),
                            TimetableSlot("t_ai4", "Wednesday", 3, "11:15 - 01:15 PM", "23AI505", "Deep Learning Lab", "Dr. K. S. Rao", "AI Lab 1")
                        )
                        "ECE" -> listOf(
                            TimetableSlot("t_ec1", "Monday", 1, "09:00 - 10:00 AM", "23EC501", "Digital Signal Processing", "Prof. T. Murali", "ECE 201"),
                            TimetableSlot("t_ec2", "Tuesday", 2, "10:00 - 11:00 AM", "23EC502", "VLSI Design", "Dr. N. Hemalatha", "VLSI Lab"),
                            TimetableSlot("t_ec3", "Thursday", 1, "09:00 - 10:00 AM", "23EC503", "Embedded Systems", "Mr. R. Rajesh", "Embedded Lab"),
                            TimetableSlot("t_ec4", "Friday", 3, "11:15 - 01:15 PM", "23EC505", "Microcontroller Lab", "Prof. T. Murali", "ECE 205")
                        )
                        "EEE" -> listOf(
                            TimetableSlot("t_ee1", "Monday", 1, "09:00 - 10:00 AM", "23EE501", "Power Systems-II", "Dr. C. Harish", "EEE 101"),
                            TimetableSlot("t_ee2", "Wednesday", 2, "10:00 - 11:00 AM", "23EE502", "Control Systems", "Prof. M. Suresh", "EEE 102"),
                            TimetableSlot("t_ee3", "Thursday", 3, "11:15 - 12:15 PM", "23EE503", "Power Electronics", "Mrs. K. Radhika", "Machines Lab")
                        )
                        else -> timetable
                    }

                    val filteredSlots = if (selectedDayFilter == "All") dynamicSlots else dynamicSlots.filter { it.day.equals(selectedDayFilter, ignoreCase = true) }

                    items(filteredSlots) { slot ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDarkMode) ObsidianElevated else LightSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Surface(
                                        color = CyberViolet.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "P${slot.period}",
                                            color = CyberViolet,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "${slot.subjectName} (${slot.subjectCode})",
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkMode) Color.White else LightTextPrimary,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "${slot.day} • ${slot.timeSlot} • ${slot.roomNumber}",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "Faculty: ${slot.facultyName}",
                                            color = TextIceBlue,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        timetable.remove(slot)
                                        feedbackBanner = "Removed slot for ${slot.subjectName}"
                                    }
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = CyberError, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    // Department Teaching Staff Roster Section
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Department Teaching Staff Roster ($selectedTimetableDept)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color.White else LightTextPrimary
                            )
                        )
                    }

                    val deptStaffList = when (selectedTimetableDept) {
                        "AIML" -> listOf(
                            Triple("Dr. K. S. Rao", "Professor & HOD", "Machine Learning • 16 hrs/wk"),
                            Triple("Dr. G. Lakshmi", "Associate Professor", "Computer Vision • 14 hrs/wk"),
                            Triple("Mr. P. Naveen", "Assistant Professor", "NLP Principles • 18 hrs/wk"),
                            Triple("Ms. D. Swathi", "Assistant Professor", "AI Ethics • 12 hrs/wk")
                        )
                        "ECE" -> listOf(
                            Triple("Prof. T. Murali", "Professor & HOD", "Digital Signal Processing • 16 hrs/wk"),
                            Triple("Dr. N. Hemalatha", "Associate Professor", "VLSI Design • 14 hrs/wk"),
                            Triple("Mr. R. Rajesh", "Assistant Professor", "Embedded Systems • 18 hrs/wk"),
                            Triple("Ms. V. Sravani", "Assistant Professor", "Microcontroller Lab • 12 hrs/wk")
                        )
                        "EEE" -> listOf(
                            Triple("Dr. C. Harish", "Professor & HOD", "Power Systems-II • 16 hrs/wk"),
                            Triple("Prof. M. Suresh", "Associate Professor", "Control Systems • 14 hrs/wk"),
                            Triple("Mrs. K. Radhika", "Assistant Professor", "Power Electronics • 18 hrs/wk")
                        )
                        else -> listOf(
                            Triple("Dr. P. V. Kishore", "Professor & HOD", "Deep Learning & Neural Networks • 16 hrs/wk"),
                            Triple("Prof. S. R. Varma", "Associate Professor", "Organizational Behaviour • 14 hrs/wk"),
                            Triple("Dr. M. Ramanathan", "Professor", "Cyber Security • 16 hrs/wk"),
                            Triple("Mr. J. Vinay Kumar", "Assistant Professor", "Prompt Engineering Lab • 18 hrs/wk")
                        )
                    }

                    items(deptStaffList) { (sName, sDesig, sLoad) ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDarkMode) ObsidianElevated else LightSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(sName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isDarkMode) Color.White else LightTextPrimary)
                                    Text(sDesig, fontSize = 11.sp, color = TextSecondary)
                                    Text(sLoad, fontSize = 10.5.sp, color = CyberViolet, fontWeight = FontWeight.Medium)
                                }
                                Surface(
                                    color = CyberSuccess.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Allocated",
                                        color = CyberSuccess,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AdminTab.CAMPUS_ANALYTICS -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {


                    item {
                        Text(
                            text = "Institutional Compliance & Charts",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color.White else LightTextPrimary
                            )
                        )
                        Text(
                            text = "Real-time metrics compiled across all departments for current semester",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    // Department Comparison Chart
                    item {
                        DepartmentComparisonChart(
                            depts = AcademicDataProvider.getDepartmentComparison(),
                            isDarkMode = isDarkMode
                        )
                    }

                    // Donut Chart with Live Computed Campus Data
                    item {
                        val liveCourses = AcademicDataProvider.sharedCourses
                        val liveAvgPresent = if (liveCourses.isNotEmpty()) {
                            (liveCourses.map { it.attendancePercentage.toFloat() }.average().toFloat()).coerceIn(40f, 95f)
                        } else 81.2f
                        val liveLatePct = 3.6f
                        val liveAbsentPct = (100f - liveAvgPresent - liveLatePct).coerceAtLeast(0f)

                        AttendanceDonutChart(
                            presentPct = liveAvgPresent,
                            absentPct = liveAbsentPct,
                            latePct = liveLatePct,
                            isDarkMode = isDarkMode
                        )
                    }

                    // Trend Line Chart
                    item {
                        MonthlyTrendLineChart(
                            trends = AcademicDataProvider.getInstitutionAttendanceTrends(),
                            isDarkMode = isDarkMode
                        )
                    }
                }
            }

            AdminTab.ORGANIZATIONS -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Multi-College Directory & Campuses (${colleges.size})",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkMode) Color.White else LightTextPrimary
                                    )
                                )
                                Text(
                                    text = "Manage affiliated campuses, institutional codes, and admin quotas",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            Button(
                                onClick = { showAddCollegeDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberViolet),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add College", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    items(colleges) { col ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDarkMode) ObsidianElevated else LightSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightBorder),
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
                                            text = col.name,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkMode) Color.White else LightTextPrimary,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Code: ${col.code} • ${col.city} • ${col.adminEmail}",
                                            color = TextSecondary,
                                            fontSize = 11.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    val isActive = col.status.equals("Active", ignoreCase = true) || col.status.contains("Verified", ignoreCase = true)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Surface(
                                            color = if (isActive) CyberSuccess.copy(alpha = 0.2f) else CyberWarning.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isActive) CyberSuccess else CyberWarning),
                                            modifier = Modifier.clickable {
                                                val idx = colleges.indexOfFirst { it.id == col.id }
                                                if (idx >= 0) {
                                                    val newStatus = if (isActive) "Inactive" else "Active & Verified"
                                                    colleges[idx] = col.copy(status = newStatus)
                                                    feedbackBanner = "Changed ${col.name} status to $newStatus"
                                                }
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isActive) CyberSuccess else CyberWarning)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isActive) "Active & Verified" else "Inactive",
                                                    color = if (isActive) CyberSuccess else CyberWarning,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        IconButton(onClick = { editingCollege = col }, modifier = Modifier.size(30.dp)) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit College", tint = CyberViolet, modifier = Modifier.size(15.dp))
                                        }
                                        IconButton(onClick = {
                                            colleges.remove(col)
                                            feedbackBanner = "Removed college ${col.name}"
                                        }, modifier = Modifier.size(30.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete College", tint = CyberError, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Departments: ${col.departmentsCount}", color = TextIceBlue, fontSize = 11.5.sp)
                                    Text("Enrolled: ${col.studentsCount}", color = TextIceBlue, fontSize = 11.5.sp)
                                    Text("Federation: Connected", color = CyberSuccess, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            AdminTab.DIAGNOSTICS_ISSUES -> {
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
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Hub, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Campus Infrastructure & Facilities",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = if (isDarkMode) Color.White else LightTextPrimary
                                            )
                                            Text(
                                                text = "Real-time monitoring, state control, and maintenance audits.",
                                                color = TextSecondary,
                                                fontSize = 11.5.sp
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { showAddFacilityDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberViolet),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add Facility", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Status Overview Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val opCount = campusFacilities.count { it.status == "Operational" }
                                    val maintCount = campusFacilities.count { it.status == "Under Maintenance" }
                                    val inactCount = campusFacilities.count { it.status == "Inactive" }

                                    Surface(
                                        color = CyberSuccess.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("$opCount Active", color = CyberSuccess, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("Operational", color = TextSecondary, fontSize = 9.5.sp)
                                        }
                                    }

                                    Surface(
                                        color = CyberWarning.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("$maintCount In Service", color = CyberWarning, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("Maintenance", color = TextSecondary, fontSize = 9.5.sp)
                                        }
                                    }

                                    Surface(
                                        color = CyberError.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("$inactCount Offline", color = CyberError, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("Standby", color = TextSecondary, fontSize = 9.5.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    items(campusFacilities) { fac ->
                        val isOp = fac.status == "Operational"
                        val isMaint = fac.status == "Under Maintenance"

                        val statusBg = when (fac.status) {
                            "Operational" -> Color(0xFF065F46)
                            "Under Maintenance" -> Color(0xFF78350F)
                            else -> Color(0xFF7F1D1D)
                        }

                        val statusTint = when (fac.status) {
                            "Operational" -> CyberSuccess
                            "Under Maintenance" -> CyberWarning
                            else -> CyberError
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDarkMode) ObsidianElevated else LightSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, statusTint.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            text = fac.name,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkMode) Color.White else LightTextPrimary,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "${fac.category} • 📍 ${fac.location}",
                                            color = TextSecondary,
                                            fontSize = 11.5.sp
                                        )
                                        Text(
                                            text = "In-Charge: ${fac.inChargeStaff} • Audited: ${fac.lastAudited}",
                                            color = TextIceBlue,
                                            fontSize = 10.5.sp
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        // Interactive Toggle Status Button (Changes words AND colors on click)
                                        Button(
                                            onClick = {
                                                val nextStatus = when (fac.status) {
                                                    "Operational" -> "Under Maintenance"
                                                    "Under Maintenance" -> "Inactive"
                                                    else -> "Operational"
                                                }
                                                val idx = campusFacilities.indexOfFirst { it.id == fac.id }
                                                if (idx >= 0) {
                                                    campusFacilities[idx] = fac.copy(status = nextStatus)
                                                    feedbackBanner = "Toggled ${fac.name} state to '$nextStatus'"
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = statusBg),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(statusTint)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = when (fac.status) {
                                                    "Operational" -> "✓ Operational"
                                                    "Under Maintenance" -> "⚠ In Service"
                                                    else -> "🛠 Standby"
                                                },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }

                                        IconButton(
                                            onClick = { editingFacility = fac },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Facility", tint = CyberViolet, modifier = Modifier.size(16.dp))
                                        }

                                        IconButton(
                                            onClick = {
                                                campusFacilities.remove(fac)
                                                feedbackBanner = "Removed facility ${fac.name}"
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Facility", tint = CyberError, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AdminTab.ADMIN_SETTINGS -> {
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
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightBorder),
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
                                            .background(CyberViolet.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Tune, contentDescription = null, tint = CyberViolet, modifier = Modifier.size(24.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "College Administration & Security Settings",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDarkMode) Color.White else LightTextPrimary
                                            )
                                        )
                                        Text("Configure campus policies, cloud sync parameters, and administrator identity.", color = TextSecondary, fontSize = 11.5.sp)
                                    }
                                }

                                if (adminSettingsSavedBanner != null) {
                                    Spacer(modifier = Modifier.height(12.dp))
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
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(adminSettingsSavedBanner ?: "", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text("Administrator Identity & Office Profile", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isDarkMode) Color.White else LightTextPrimary)
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = adminProfileName,
                                    onValueChange = { adminProfileName = it },
                                    label = { Text("Chief Administrator Name") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = adminProfileEmpId,
                                        onValueChange = { adminProfileEmpId = it },
                                        label = { Text("Employee ID") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = adminProfilePhone,
                                        onValueChange = { adminProfilePhone = it },
                                        label = { Text("Contact Number") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = adminProfileEmail,
                                    onValueChange = { adminProfileEmail = it },
                                    label = { Text("Official Operations Email") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = adminEndpointUrl,
                                    onValueChange = { adminEndpointUrl = it },
                                    label = { Text("Cloud Sync API Endpoint") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = if (isDarkMode) ObsidianBorder else LightElevated)
                                Spacer(modifier = Modifier.height(12.dp))

                                SettingToggleCard(
                                    title = "Daily Cloud Auto-Backup",
                                    description = "Automatically snapshot Firestore attendance matrices at 11:59 PM daily.",
                                    checked = autoBackup,
                                    onCheckedChange = {
                                        autoBackup = it
                                        adminSettings.autoBackupFirestoreDaily = it
                                    },
                                    isDarkMode = isDarkMode
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                SettingToggleCard(
                                    title = "Admin Email Alert Notifications",
                                    description = "Dispatch instant alert when system error or anomaly is detected.",
                                    checked = emailAlerts,
                                    onCheckedChange = {
                                        emailAlerts = it
                                        adminSettings.receiveIssueAlertsViaEmail = it
                                    },
                                    isDarkMode = isDarkMode
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                SettingToggleCard(
                                    title = "Timetable Auto-Lock",
                                    description = "Disallow schedule modifications during active lecture hours without Principal key.",
                                    checked = timetableAutoLock,
                                    onCheckedChange = {
                                        timetableAutoLock = it
                                    },
                                    isDarkMode = isDarkMode
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        AcademicDataProvider.saveDataToDisk()
                                        adminSettingsSavedBanner = "✓ Campus Admin Configuration & Security Settings Saved to Local Storage & Cloud!"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberViolet),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(44.dp)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save Admin Configuration", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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

    // Modal: Full-Access Department Inspection Dialog
    if (showDeptAccessDialog && selectedInspectDept != null) {
        val dept = selectedInspectDept!!
        AlertDialog(
            onDismissRequest = { showDeptAccessDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Apartment, contentDescription = null, tint = ElectricIndigo, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${dept.name} (${dept.code})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Institutional Academic Branch & Administrative Ledger Overview",
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
                            Text("Head of Department: ${dept.hodName}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ElectricIndigo)
                            Text("Official Email: ${dept.hodEmail}", fontSize = 11.sp, color = TextSecondary)
                            Text("Total Faculty Strength: ${dept.facultyCount} Registered Members", fontSize = 11.sp, color = TextSecondary)
                            Text("Enrolled Student Roster: ${dept.studentsCount} Active Students", fontSize = 11.sp, color = TextSecondary)
                            Text("Overall Attendance Index: ${dept.avgAttendance}% (Cutoff Compliant)", fontSize = 11.sp, color = CyberEmerald, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CyberViolet.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberViolet.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Autonomous Branch Access Privileges:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CyberViolet)
                            Text("• Face Attendance Biometrics: Full Autonomous Ledger Enabled", fontSize = 11.sp)
                            Text("• Dynamic Period Allocations: Editable by Timetable Officer", fontSize = 11.sp)
                            Text("• Semester Exam Duty Roster: Synchronized with HOD Portal", fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeptAccessDialog = false
                        onNavigateToChat?.invoke("Admin AI Query: Review curriculum workload balance and faculty allocations for department ${dept.code}.")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberViolet)
                ) {
                    Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI Department Audit", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeptAccessDialog = false }) {
                    Text("Close Roster", fontSize = 12.sp)
                }
            }
        )
    }

    // Modal: Edit Facility Dialog
    if (editingFacility != null) {
        var fName by remember(editingFacility) { mutableStateOf(editingFacility!!.name) }
        var fLoc by remember(editingFacility) { mutableStateOf(editingFacility!!.location) }
        var fStaff by remember(editingFacility) { mutableStateOf(editingFacility!!.inChargeStaff) }
        var fCat by remember(editingFacility) { mutableStateOf(editingFacility!!.category) }

        AlertDialog(
            onDismissRequest = { editingFacility = null },
            title = { Text("Edit Campus Facility Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = fName, onValueChange = { fName = it }, label = { Text("Facility Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = fCat, onValueChange = { fCat = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = fLoc, onValueChange = { fLoc = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = fStaff, onValueChange = { fStaff = it }, label = { Text("In-Charge Staff") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idx = campusFacilities.indexOfFirst { it.id == editingFacility!!.id }
                        if (idx >= 0) {
                            campusFacilities[idx] = editingFacility!!.copy(name = fName, location = fLoc, inChargeStaff = fStaff, category = fCat)
                            feedbackBanner = "Updated facility $fName"
                        }
                        editingFacility = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberViolet)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingFacility = null }) { Text("Cancel") }
            }
        )
    }

    // Modal: Add Facility Dialog
    if (showAddFacilityDialog) {
        var fName by remember { mutableStateOf("") }
        var fCat by remember { mutableStateOf("Computing Infrastructure") }
        var fLoc by remember { mutableStateOf("Main Campus Block") }
        var fStaff by remember { mutableStateOf("Faculty Coordinator") }

        AlertDialog(
            onDismissRequest = { showAddFacilityDialog = false },
            title = { Text("Provision New Campus Facility", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = fName, onValueChange = { fName = it }, label = { Text("Facility Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = fCat, onValueChange = { fCat = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = fLoc, onValueChange = { fLoc = it }, label = { Text("Campus Location") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = fStaff, onValueChange = { fStaff = it }, label = { Text("In-Charge Staff Member") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (fName.isNotBlank()) {
                            campusFacilities.add(
                                CampusFacility(
                                    id = "fac_${System.currentTimeMillis()}",
                                    name = fName,
                                    category = fCat,
                                    location = fLoc,
                                    status = "Operational",
                                    inChargeStaff = fStaff,
                                    lastAudited = "02 Nov 2026"
                                )
                            )
                            feedbackBanner = "Added new facility $fName"
                            showAddFacilityDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberViolet)
                ) {
                    Text("Provision Facility", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFacilityDialog = false }) { Text("Cancel") }
            }
        )
    }
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Institution Profile", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = instName,
                        onValueChange = { instName = it },
                        label = { Text("Institution Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = principalName,
                        onValueChange = { principalName = it },
                        label = { Text("Principal Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = instEmail,
                        onValueChange = { instEmail = it },
                        label = { Text("Principal Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = instAddress,
                        onValueChange = { instAddress = it },
                        label = { Text("Campus Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEditProfileDialog = false
                        feedbackBanner = "Institution profile updated successfully"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberViolet)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = ObsidianSurface
        )
    }

    // Add Department Dialog
    if (showAddDeptDialog) {
        AlertDialog(
            onDismissRequest = { showAddDeptDialog = false },
            title = { Text("Add New Department", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newDeptName,
                        onValueChange = { newDeptName = it },
                        label = { Text("Department Name (e.g. Chemical Eng)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newDeptCode,
                        onValueChange = { newDeptCode = it },
                        label = { Text("Department Code (e.g. CHEM)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newDeptHod,
                        onValueChange = { newDeptHod = it },
                        label = { Text("HOD Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newDeptName.isNotBlank() && newDeptCode.isNotBlank()) {
                            departments.add(
                                DepartmentInfo(
                                    id = "dept_${System.currentTimeMillis()}",
                                    collegeId = "col_ksrm_01",
                                    name = newDeptName,
                                    code = newDeptCode.uppercase(),
                                    hodName = if (newDeptHod.isNotBlank()) newDeptHod else "Dr. Incharge HOD",
                                    hodEmail = "hod.${newDeptCode.lowercase()}@ksrmce.ac.in",
                                    facultyCount = 18,
                                    studentsCount = 320,
                                    avgAttendance = 80.0
                                )
                            )
                            feedbackBanner = "Added department ${newDeptCode.uppercase()}"
                            newDeptName = ""
                            newDeptCode = ""
                            newDeptHod = ""
                            showAddDeptDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberViolet)
                ) {
                    Text("Add Department")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDeptDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = ObsidianSurface
        )
    }

    // Add Course Dialog
    if (showAddCourseDialog) {
        AlertDialog(
            onDismissRequest = { showAddCourseDialog = false },
            title = { Text("Add New Course", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newCourseName,
                        onValueChange = { newCourseName = it },
                        label = { Text("Course Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCourseCode,
                        onValueChange = { newCourseCode = it },
                        label = { Text("Course Code (e.g. 2305801)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCourseFaculty,
                        onValueChange = { newCourseFaculty = it },
                        label = { Text("Assigned Faculty Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCourseCredits,
                        onValueChange = { newCourseCredits = it },
                        label = { Text("Credits") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCourseName.isNotBlank() && newCourseCode.isNotBlank()) {
                            courses.add(
                                SubjectCourse(
                                    code = newCourseCode,
                                    name = newCourseName,
                                    courseType = "LECTURE",
                                    sectionTag = "B.TECH - CSE B/S 7th Sem",
                                    facultyName = if (newCourseFaculty.isNotBlank()) newCourseFaculty else "Assigned Faculty",
                                    totalSessions = 40,
                                    attendedSessions = 34,
                                    credits = newCourseCredits.toIntOrNull() ?: 3
                                )
                            )
                            feedbackBanner = "Added course $newCourseName"
                            newCourseName = ""
                            newCourseCode = ""
                            newCourseFaculty = ""
                            showAddCourseDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberViolet)
                ) {
                    Text("Add Course")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCourseDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = ObsidianSurface
        )
    }

    // Add Timetable Slot Dialog
    if (showAddSlotDialog) {
        AlertDialog(
            onDismissRequest = { showAddSlotDialog = false },
            title = { Text("Add Timetable Slot", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newSlotDay,
                        onValueChange = { newSlotDay = it },
                        label = { Text("Day of Week (e.g. Monday)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSlotPeriod,
                        onValueChange = { newSlotPeriod = it },
                        label = { Text("Period (1-6)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSlotTime,
                        onValueChange = { newSlotTime = it },
                        label = { Text("Time Slot") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSlotSubject,
                        onValueChange = { newSlotSubject = it },
                        label = { Text("Subject Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSlotFaculty,
                        onValueChange = { newSlotFaculty = it },
                        label = { Text("Faculty Assigned") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSlotRoom,
                        onValueChange = { newSlotRoom = it },
                        label = { Text("Classroom / Lab") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSlotSubject.isNotBlank()) {
                            timetable.add(
                                TimetableSlot(
                                    id = "tt_${System.currentTimeMillis()}",
                                    day = newSlotDay,
                                    period = newSlotPeriod.toIntOrNull() ?: 1,
                                    timeSlot = newSlotTime,
                                    subjectCode = "23CS" + (100..999).random(),
                                    subjectName = newSlotSubject,
                                    facultyName = newSlotFaculty,
                                    roomNumber = newSlotRoom
                                )
                            )
                            feedbackBanner = "Timetable slot added for $newSlotDay P$newSlotPeriod"
                            showAddSlotDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberViolet)
                ) {
                    Text("Add Slot")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSlotDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = ObsidianSurface
        )
    }

    // Report Issue Dialog
    if (showReportIssueDialog) {
        AlertDialog(
            onDismissRequest = { showReportIssueDialog = false },
            title = { Text("Log Campus Issue Ticket", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newIssueTitle,
                        onValueChange = { newIssueTitle = it },
                        label = { Text("Issue Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newIssueDesc,
                        onValueChange = { newIssueDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newIssueLocation,
                        onValueChange = { newIssueLocation = it },
                        label = { Text("Location / Screen") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newIssueTitle.isNotBlank()) {
                            issueReports.add(
                                0,
                                AppIssueReport(
                                    id = "iss_${System.currentTimeMillis()}",
                                    title = newIssueTitle,
                                    description = newIssueDesc,
                                    locationScreen = newIssueLocation,
                                    reportedByRole = "College Admin",
                                    severity = "High",
                                    isResolved = false,
                                    dateReported = "Today"
                                )
                            )
                            feedbackBanner = "Ticket logged: $newIssueTitle"
                            newIssueTitle = ""
                            newIssueDesc = ""
                            showReportIssueDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberViolet)
                ) {
                    Text("Submit Ticket")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportIssueDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = ObsidianSurface
        )
    }

    // Edit Department Dialog
    if (editingDept != null) {
        val currDept = editingDept!!
        var editName by remember(currDept) { mutableStateOf(currDept.name) }
        var editCode by remember(currDept) { mutableStateOf(currDept.code) }
        var editHod by remember(currDept) { mutableStateOf(currDept.hodName) }
        var editEmail by remember(currDept) { mutableStateOf(currDept.hodEmail) }

        AlertDialog(
            onDismissRequest = { editingDept = null },
            title = { Text("Edit Department Information", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Department Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCode,
                        onValueChange = { editCode = it },
                        label = { Text("Department Code") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editHod,
                        onValueChange = { editHod = it },
                        label = { Text("HOD Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("HOD Official Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idx = departments.indexOfFirst { it.id == currDept.id }
                        if (idx >= 0) {
                            departments[idx] = currDept.copy(
                                name = editName,
                                code = editCode.uppercase(),
                                hodName = editHod,
                                hodEmail = editEmail
                            )
                            feedbackBanner = "Updated department $editName"
                        }
                        editingDept = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberViolet)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingDept = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = ObsidianSurface
        )
    }

    // Edit Course Dialog
    if (editingCourse != null) {
        val currCourse = editingCourse!!
        var editName by remember(currCourse) { mutableStateOf(currCourse.name) }
        var editCode by remember(currCourse) { mutableStateOf(currCourse.code) }
        var editFaculty by remember(currCourse) { mutableStateOf(currCourse.facultyName) }
        var editCredits by remember(currCourse) { mutableStateOf(currCourse.credits.toString()) }

        AlertDialog(
            onDismissRequest = { editingCourse = null },
            title = { Text("Edit Course Curriculum", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Course Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCode,
                        onValueChange = { editCode = it },
                        label = { Text("Course Code") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editFaculty,
                        onValueChange = { editFaculty = it },
                        label = { Text("Assigned Faculty") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCredits,
                        onValueChange = { editCredits = it },
                        label = { Text("Credits") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idx = courses.indexOfFirst { it.code == currCourse.code }
                        if (idx >= 0) {
                            courses[idx] = currCourse.copy(
                                name = editName,
                                code = editCode,
                                facultyName = editFaculty,
                                credits = editCredits.toIntOrNull() ?: currCourse.credits
                            )
                            feedbackBanner = "Updated course $editName"
                        }
                        editingCourse = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberViolet)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCourse = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = ObsidianSurface
        )
    }

    // Add College Dialog
    if (showAddCollegeDialog) {
        var colName by remember { mutableStateOf("") }
        var colCode by remember { mutableStateOf("") }
        var colCity by remember { mutableStateOf("") }
        var colEmail by remember { mutableStateOf("") }
        var colDepts by remember { mutableStateOf("6") }
        var colStudents by remember { mutableStateOf("1200") }

        AlertDialog(
            onDismissRequest = { showAddCollegeDialog = false },
            title = { Text("Add College to Multi-Campus Directory", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = colName,
                        onValueChange = { colName = it },
                        label = { Text("College Name (e.g., K.L.M. College of Engineering)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = colCode,
                        onValueChange = { colCode = it },
                        label = { Text("Institutional Code (e.g., KLMCE-3T)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = colCity,
                        onValueChange = { colCity = it },
                        label = { Text("Campus City / District") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = colEmail,
                        onValueChange = { colEmail = it },
                        label = { Text("Admin Office Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (colName.isNotBlank() && colCode.isNotBlank()) {
                            colleges.add(
                                CollegeOrganization(
                                    id = "col_${System.currentTimeMillis()}",
                                    name = colName,
                                    code = colCode.uppercase(),
                                    city = if (colCity.isNotBlank()) colCity else "Kadapa",
                                    adminEmail = if (colEmail.isNotBlank()) colEmail else "admin.${colCode.lowercase()}@campus.edu",
                                    departmentsCount = colDepts.toIntOrNull() ?: 6,
                                    studentsCount = colStudents.toIntOrNull() ?: 1200,
                                    status = "Active"
                                )
                            )
                            feedbackBanner = "Registered campus $colName in multi-college directory"
                            showAddCollegeDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberViolet)
                ) {
                    Text("Add College")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCollegeDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = ObsidianSurface
        )
    }

    // Edit College Dialog
    if (editingCollege != null) {
        val currCol = editingCollege!!
        var editName by remember(currCol) { mutableStateOf(currCol.name) }
        var editCode by remember(currCol) { mutableStateOf(currCol.code) }
        var editCity by remember(currCol) { mutableStateOf(currCol.city) }
        var editEmail by remember(currCol) { mutableStateOf(currCol.adminEmail) }
        var editDepts by remember(currCol) { mutableStateOf(currCol.departmentsCount.toString()) }
        var editStudents by remember(currCol) { mutableStateOf(currCol.studentsCount.toString()) }
        var editStatus by remember(currCol) { mutableStateOf(currCol.status) }

        AlertDialog(
            onDismissRequest = { editingCollege = null },
            title = { Text("Edit Campus Institutional Details", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("College Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCode,
                        onValueChange = { editCode = it },
                        label = { Text("College Code") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCity,
                        onValueChange = { editCity = it },
                        label = { Text("City / Region") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Admin Contact Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editDepts,
                            onValueChange = { editDepts = it },
                            label = { Text("Departments") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editStudents,
                            onValueChange = { editStudents = it },
                            label = { Text("Students") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Active & Verified Status", color = TextSecondary, fontSize = 12.sp)
                        Switch(
                            checked = editStatus.equals("Active", ignoreCase = true) || editStatus.contains("Verified", ignoreCase = true),
                            onCheckedChange = { checked ->
                                editStatus = if (checked) "Active & Verified" else "Inactive"
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CyberSuccess
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idx = colleges.indexOfFirst { it.id == currCol.id }
                        if (idx >= 0) {
                            colleges[idx] = currCol.copy(
                                name = editName,
                                code = editCode.uppercase(),
                                city = editCity,
                                adminEmail = editEmail,
                                departmentsCount = editDepts.toIntOrNull() ?: currCol.departmentsCount,
                                studentsCount = editStudents.toIntOrNull() ?: currCol.studentsCount,
                                status = editStatus
                            )
                            feedbackBanner = "Updated institution $editName"
                        }
                        editingCollege = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberViolet)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCollege = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = ObsidianSurface
        )
    }
}

@Composable
private fun DetailItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isDarkMode: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = CyberViolet, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(label, color = TextSecondary, fontSize = 11.sp)
            Text(value, color = if (isDarkMode) Color.White else LightTextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SettingToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isDarkMode: Boolean
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDarkMode) ObsidianElevated else LightSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else LightTextPrimary, fontSize = 13.sp)
                Text(description, color = TextSecondary, fontSize = 11.5.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = CyberViolet
                )
            )
        }
    }
}
