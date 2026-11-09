package com.example.ui.screens
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.agent.AttendanceAgent
import com.example.data.AcademicDataProvider
import com.example.data.EmailAlertDispatcher
import com.example.model.AttendanceStatus
import com.example.model.CampusAssignment
import com.example.model.AssignmentSubmission
import com.example.model.DriveDocument
import com.example.model.FacultyAttendanceDataProvider
import com.example.model.FacultyAttendanceOverview
import com.example.model.FacultySubjectSummary
import com.example.ui.components.*
import com.example.ui.components.PdfViewerDialog
import com.example.ui.components.RealQRCodeView
import com.example.data.FirestoreRepository
import com.example.data.OpenRouterAiService
import com.example.model.UserRole
import com.example.ui.responsive.LocalWindowAdaptiveInfo
import com.example.ui.theme.*
import kotlinx.coroutines.launch

enum class FacultySectionTab {
    ATTENDANCE_SUMMARY,
    ATTENDANCE_STUDIO,
    ASSIGNMENT_GRADING,
    STUDY_MATERIALS,
    EXAM_DUTIES,
    MARKS_ENTRY,
    RISK_TRIAGE,
    FACULTY_SETTINGS
}

@Composable
fun FacultyPortalScreen(
    onLaunchCameraScan: (String) -> Unit,
    onLaunchManualLedger: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    initialTab: FacultySectionTab = FacultySectionTab.ATTENDANCE_SUMMARY,
    isDarkMode: Boolean = true,
    onToggleTheme: () -> Unit = {}
) {
    val adaptiveInfo = LocalWindowAdaptiveInfo.current
    val horizPadding = if (adaptiveInfo.isSmallScreen) 10.dp else 16.dp
    var selectedSectionTab by remember { mutableStateOf(initialTab) }
    var selectedSubject by remember { mutableStateOf("Deep Learning [2339609]") }
    var selectedSection by remember { mutableStateOf("CSE - 4th Year Sec B") }
    var showQrProjectorDialog by remember { mutableStateOf(false) }
    var showMarksEntryDialog by remember { mutableStateOf(false) }
    var showUploadMaterialDialog by remember { mutableStateOf(false) }
    var previewDocument by remember { mutableStateOf<DriveDocument?>(null) }
    var counselorNoticeSent by remember { mutableStateOf(false) }
    var emailAlertSentMessage by remember { mutableStateOf<String?>(null) }
    var showPublishAssignmentDialog by remember { mutableStateOf(false) }
    var gradingSubmission by remember { mutableStateOf<AssignmentSubmission?>(null) }
    var gradeInputMarks by remember { mutableStateOf("9.0") }
    var gradeInputFeedback by remember { mutableStateOf("Good analytical approach and correct equations.") }
    var publishAssignmentTitle by remember { mutableStateOf("") }
    var publishAssignmentDesc by remember { mutableStateOf("") }
    var publishAssignmentDeadline by remember { mutableStateOf("In 3 Days, 11:59 PM") }
    val facultyAttendanceOverview = remember { FacultyAttendanceDataProvider.getFacultyOverview() }
    var summarySubjectCode by remember { mutableStateOf<String?>(null) }
    var activeChartTab by remember { mutableStateOf(ChartFilterView.ALL_CHARTS) }
    var showExportSummaryPdfDialog by remember { mutableStateOf(false) }

    // Interactive Real-World Marks Studio states
    val marksScope = rememberCoroutineScope()
    var assignmentUpdateTrigger by remember { mutableStateOf(0) }
    val marksStudentsList = remember {
        listOf(
            Triple("239Y1A0501", "Sattu Penchala Prasad", "Sec B"),
            Triple("239Y1A05B8", "B. Sai Penchala", "Sec B"),
            Triple("239Y1A05B9", "Chatakondu Subbarayudu", "Sec B"),
            Triple("239Y1A05C0", "Shaik Mohammad", "Sec B"),
            Triple("239Y1A05C1", "Bojja Prasad Reddy", "Sec B"),
            Triple("239Y1A05E0", "K. Upendra", "Sec B")
        )
    }
    var selectedStudentIdx by remember { mutableStateOf(0) }
    var x1Val by remember { mutableStateOf("8.5") }
    var x2Val by remember { mutableStateOf("8.0") }
    var x3Val by remember { mutableStateOf("9.0") }
    var x4Val by remember { mutableStateOf("7.5") }
    var x5Val by remember { mutableStateOf("8.5") }
    var x6Val by remember { mutableStateOf("8.0") }
    var x7Val by remember { mutableStateOf("34.0") }
    var marksSuccessNotice by remember { mutableStateOf<String?>(null) }
    var marksAiAdvice by remember { mutableStateOf<String?>(null) }
    var isAnalyzingAiMarks by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val facultySettings = remember { AcademicDataProvider.facultySettings }
    var defaultSec by remember { mutableStateOf(facultySettings.defaultSection) }
    var defaultPer by remember { mutableStateOf(facultySettings.defaultPeriod) }
    var smsAlerts by remember { mutableStateOf(facultySettings.notifyAbsenteeParentsSms) }
    var autoFlagRisk by remember { mutableStateOf(facultySettings.autoFlagAtRiskStudents) }

    data class AtRiskFacultyStudent(val name: String, val roll: String, val pct: Double, val section: String)

    val atRiskStudentsList = listOf(
        AtRiskFacultyStudent("K. Upendra", "239Y1A05E0", 68.5, "4th Yr Sec B"),
        AtRiskFacultyStudent("A. Karthik", "239Y1A05D2", 71.2, "4th Yr Sec B"),
        AtRiskFacultyStudent("P. Akhil", "239Y1A05E4", 64.0, "1st Yr Sec A"),
        AtRiskFacultyStudent("L. Varshini", "239Y1A05E1", 73.0, "3rd Yr Sec C"),
        AtRiskFacultyStudent("S. Penchala Prasad", "239Y1A0501", 72.4, "4th Yr Sec B"),
        AtRiskFacultyStudent("M. Sai Tarun", "239Y1A05C8", 66.8, "2nd Yr Sec A")
    )

    // Section Filters
    var selectedAssignmentSectionFilter by remember { mutableStateOf("All") }
    var selectedMaterialSectionFilter by remember { mutableStateOf("All") }
    var selectedExamSectionFilter by remember { mutableStateOf("All") }
    var selectedRiskSectionFilter by remember { mutableStateOf("All") }

    val displayedAtRiskStudents = remember(selectedRiskSectionFilter, atRiskStudentsList) {
        if (selectedRiskSectionFilter == "All") atRiskStudentsList
        else atRiskStudentsList.filter { it.section.equals(selectedRiskSectionFilter, ignoreCase = true) }
    }

    // Faculty Profile Settings States
    var facultyProfileName by remember { mutableStateOf("Dr. P. V. Kishore") }
    var facultyProfileEmpId by remember { mutableStateOf("KSRM-CSE-FAC-102") }
    var facultyProfileDept by remember { mutableStateOf("Computer Science & Engineering") }
    var facultyProfileDesig by remember { mutableStateOf("Associate Professor") }
    var facultyProfileEmail by remember { mutableStateOf("pvk@ksrmce.ac.in") }
    var facultyProfileOffice by remember { mutableStateOf("Block 2, Room 204") }
    var settingsSaveFeedback by remember { mutableStateOf<String?>(null) }

    // PDF Upload File Pickers from Phone Storage
    val assignmentPdfUploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = it.lastPathSegment?.substringAfterLast("/") ?: "Assignment_Question_Paper.pdf"
            val validName = if (fileName.endsWith(".pdf", ignoreCase = true)) fileName else "$fileName.pdf"
            val newAsgn = CampusAssignment(
                id = "asgn_" + System.currentTimeMillis(),
                collegeId = "col_ksrm_01",
                department = if (selectedSection.contains("CSE")) "CSE" else "ECE",
                year = 4,
                section = if (selectedSection.contains("Sec A")) "Sec A" else "Sec B",
                subjectCode = selectedSubject.substringAfter("[").substringBefore("]"),
                subjectName = selectedSubject.substringBefore(" ["),
                title = "Question Paper: $validName",
                description = "Uploaded directly from device storage. Complete solutions required by deadline.",
                facultyName = facultyProfileName,
                deadlineTimestamp = System.currentTimeMillis() + (3 * 86400000L),
                deadlineFormatted = "In 3 Days, 11:59 PM",
                questionPdfName = validName,
                maxMarks = 10,
                isClosed = false
            )
            AcademicDataProvider.publishAssignment(newAsgn)
            emailAlertSentMessage = "Successfully uploaded '$validName' from phone & published assignment!"
        }
    }

    val materialPdfUploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = it.lastPathSegment?.substringAfterLast("/") ?: "Course_Material.pdf"
            val validName = if (fileName.endsWith(".pdf", ignoreCase = true)) fileName else "$fileName.pdf"
            val targetSec = if (selectedMaterialSectionFilter != "All") selectedMaterialSectionFilter else "Sec B"
            AcademicDataProvider.publishFacultyDocument(
                title = validName,
                folderName = "Section $targetSec Notes",
                fileType = "PDF",
                sizeFormatted = "3.2 MB",
                uploadedBy = facultyProfileName,
                subjectCode = selectedSubject.substringAfter("[").substringBefore("]")
            )
            emailAlertSentMessage = "Uploaded '$validName' from phone specifically for $targetSec students!"
        }
    }

    val sectionAccentColor = when {
        selectedSection.contains("4th Year", ignoreCase = true) || selectedSection.contains("4th", ignoreCase = true) -> ElectricIndigo
        selectedSection.contains("1st Year", ignoreCase = true) || selectedSection.contains("1th", ignoreCase = true) -> NeonBlue
        selectedSection.contains("3rd Year", ignoreCase = true) || selectedSection.contains("3rd", ignoreCase = true) -> CyberEmerald
        selectedSection.contains("2nd Year", ignoreCase = true) || selectedSection.contains("2nd", ignoreCase = true) -> LuminousAmber
        else -> Color(0xFF8B5CF6)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkMode) ObsidianBg else LightBg)
    ) {
        // Faculty Sub-Tabs
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
                    FacultyTabButton(
                        title = "Visual Summary & Charts",
                        icon = Icons.Default.BarChart,
                        isSelected = selectedSectionTab == FacultySectionTab.ATTENDANCE_SUMMARY,
                        accentColor = ElectricIndigo,
                        onClick = { selectedSectionTab = FacultySectionTab.ATTENDANCE_SUMMARY }
                    )
                }
                item {
                    FacultyTabButton(
                        title = "Attendance",
                        icon = Icons.Default.CameraAlt,
                        isSelected = selectedSectionTab == FacultySectionTab.ATTENDANCE_STUDIO,
                        accentColor = CyberEmerald,
                        onClick = { selectedSectionTab = FacultySectionTab.ATTENDANCE_STUDIO }
                    )
                }
                item {
                    FacultyTabButton(
                        title = "Assignments & Grading",
                        icon = Icons.Default.Assignment,
                        isSelected = selectedSectionTab == FacultySectionTab.ASSIGNMENT_GRADING,
                        accentColor = NeonBlue,
                        onClick = { selectedSectionTab = FacultySectionTab.ASSIGNMENT_GRADING }
                    )
                }
                item {
                    FacultyTabButton(
                        title = "Study Materials & PDFs",
                        icon = Icons.Default.PictureAsPdf,
                        isSelected = selectedSectionTab == FacultySectionTab.STUDY_MATERIALS,
                        accentColor = LuminousAmber,
                        onClick = { selectedSectionTab = FacultySectionTab.STUDY_MATERIALS }
                    )
                }
                item {
                    FacultyTabButton(
                        title = "Exam Schedules",
                        icon = Icons.Default.EventNote,
                        isSelected = selectedSectionTab == FacultySectionTab.EXAM_DUTIES,
                        accentColor = Color(0xFF8B5CF6),
                        onClick = { selectedSectionTab = FacultySectionTab.EXAM_DUTIES }
                    )
                }
                item {
                    FacultyTabButton(
                        title = "Marks & Assessment",
                        icon = Icons.Default.FactCheck,
                        isSelected = selectedSectionTab == FacultySectionTab.MARKS_ENTRY,
                        accentColor = Color(0xFFEC4899),
                        onClick = { selectedSectionTab = FacultySectionTab.MARKS_ENTRY }
                    )
                }
                item {
                    FacultyTabButton(
                        title = "Risk Triage",
                        icon = Icons.Default.WarningAmber,
                        isSelected = selectedSectionTab == FacultySectionTab.RISK_TRIAGE,
                        accentColor = Color(0xFFEF4444),
                        onClick = { selectedSectionTab = FacultySectionTab.RISK_TRIAGE }
                    )
                }
                item {
                    FacultyTabButton(
                        title = "Settings",
                        icon = Icons.Default.Settings,
                        isSelected = selectedSectionTab == FacultySectionTab.FACULTY_SETTINGS,
                        accentColor = Color(0xFF64748B),
                        onClick = { selectedSectionTab = FacultySectionTab.FACULTY_SETTINGS }
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
            // Faculty Header Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (isDarkMode) ObsidianElevated else LightSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.5f)),
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
                                color = ElectricIndigo.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "FACULTY COMMAND PORTAL",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = ElectricIndigo,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Dr. P. V. Kishore",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Associate Professor in CSE • KSRM College of Engineering",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(ElectricIndigo.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SupervisorAccount,
                                contentDescription = null,
                                tint = ElectricIndigo,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = if (isDarkMode) ObsidianBorder else LightElevated)
                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDarkMode) ObsidianElevated else LightElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Active Classroom", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = ElectricIndigo.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = selectedSection,
                                        color = ElectricIndigo,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Course & Subject", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    text = selectedSubject,
                                    color = CyberEmerald,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "TEACHING SCHEDULE (MULTI-DEPARTMENT LOADS):",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(AcademicDataProvider.sharedTeachingLoads.size) { idx ->
                            val load = AcademicDataProvider.sharedTeachingLoads[idx]
                            val label = "${load.year}th Yr ${load.department} ${load.section}"
                            val isSelected = selectedSection.contains(load.department) && selectedSection.contains("${load.year}")
                            val chipAccent = when (load.subjectCode) {
                                "2339609" -> ElectricIndigo
                                "2305101" -> NeonBlue
                                "2305302" -> CyberEmerald
                                "2305504" -> LuminousAmber
                                else -> Color(0xFF8B5CF6)
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedSection = "${load.department} - ${load.year}th Year ${load.section}"
                                    selectedSubject = "${load.subjectName} [${load.subjectCode}]"
                                    summarySubjectCode = load.subjectCode
                                },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = chipAccent,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            when (selectedSectionTab) {
                FacultySectionTab.ASSIGNMENT_GRADING -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Assignment Action Hub
                        Surface(
                            shape = RoundedCornerShape(14.dp),
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
                                    Column {
                                        Text(
                                            text = "Assignment Publishing & Evaluation Studio",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "Upload question papers, set deadlines, and inspect student PDF submissions.",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { showPublishAssignmentDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberEmeraldDark),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Publish Assignment", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                    }
                                    Button(
                                        onClick = { assignmentPdfUploadLauncher.launch("application/pdf") },
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Upload PDF from Phone", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text("FILTER BY TEACHING LOAD / SECTION:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                Spacer(modifier = Modifier.height(4.dp))
                                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val secOptions = listOf("All", "4th Yr Sec B", "1st Yr Sec A", "3rd Yr Sec C", "2nd Yr Sec A")
                                    items(secOptions.size) { idx ->
                                        val opt = secOptions[idx]
                                        FilterChip(
                                            selected = selectedAssignmentSectionFilter == opt,
                                            onClick = { selectedAssignmentSectionFilter = opt },
                                            label = { Text(opt, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = NeonBlue,
                                                selectedLabelColor = Color.Black
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Published Assignments List (Filtered)
                        val displayAssignments = AcademicDataProvider.sharedAssignments.filter { asgn ->
                            if (selectedAssignmentSectionFilter == "All") true
                            else if (selectedAssignmentSectionFilter.contains("4th")) asgn.year == 4
                            else if (selectedAssignmentSectionFilter.contains("1st") || selectedAssignmentSectionFilter.contains("1th")) asgn.year == 1
                            else if (selectedAssignmentSectionFilter.contains("3rd")) asgn.year == 3
                            else if (selectedAssignmentSectionFilter.contains("2nd")) asgn.year == 2
                            else true
                        }

                        Text(
                            text = "Active Course Assignments (${displayAssignments.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                        )

                        displayAssignments.forEach { asgn ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDarkMode) ObsidianSurface else LightSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(asgn.title, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = if (isDarkMode) Color.White else Color(0xFF0F172A))
                                            Text("${asgn.subjectName} (${asgn.subjectCode}) • Target: ${asgn.department} ${asgn.year}th Year", fontSize = 11.sp, color = TextSecondary)
                                        }
                                        // Reactive status button tied to assignmentUpdateTrigger
                                        val isCurrentlyClosed = asgn.isClosed || (assignmentUpdateTrigger < 0)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isCurrentlyClosed) Color(0xFFDC2626) else Color(0xFF059669),
                                            modifier = Modifier.clickable {
                                                asgn.isClosed = !asgn.isClosed
                                                assignmentUpdateTrigger++
                                                AcademicDataProvider.saveDataToDisk()
                                                emailAlertSentMessage = "Assignment '${asgn.title}' is now ${if (asgn.isClosed) "CLOSED (submissions locked)" else "OPEN (accepting submissions)"}!"
                                            }
                                        ) {
                                            Text(
                                                text = if (isCurrentlyClosed) "✕ Closed (Click to Open)" else "✓ Submissions Open (Click to Close)",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Deadline: ${asgn.deadlineFormatted} • Question PDF: ${asgn.questionPdfName}", fontSize = 11.sp, color = LuminousAmber, modifier = Modifier.weight(1f))
                                        OutlinedButton(
                                            onClick = {
                                                previewDocument = DriveDocument(
                                                    id = asgn.id,
                                                    collegeId = asgn.collegeId,
                                                    title = asgn.title,
                                                    folderName = "Question Papers",
                                                    fileType = "PDF",
                                                    assetFileName = "VII SEM GS Assignment-IV -RR.pdf",
                                                    fileName = "VII SEM GS Assignment-IV -RR.pdf",
                                                    sizeFormatted = "1.8 MB",
                                                    uploadedBy = asgn.facultyName,
                                                    subject = asgn.subjectName,
                                                    subjectCode = asgn.subjectCode
                                                )
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonBlue),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Open Question PDF", fontSize = 10.5.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Student Submissions Roster (Filtered)
                        val displaySubmissions = AcademicDataProvider.sharedSubmissions.filter { sub ->
                            if (selectedAssignmentSectionFilter == "All") true
                            else if (selectedAssignmentSectionFilter.contains("Sec B")) sub.section.contains("B")
                            else if (selectedAssignmentSectionFilter.contains("Sec A")) sub.section.contains("A")
                            else if (selectedAssignmentSectionFilter.contains("Sec C")) sub.section.contains("C")
                            else true
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Received Student PDF Submissions (${displaySubmissions.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                        )

                        displaySubmissions.forEach { sub ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDarkMode) ObsidianSurface else LightSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (sub.isGraded) CyberEmerald.copy(alpha = 0.5f) else ElectricIndigo.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${sub.studentName} (${sub.rollNumber})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "PDF: ${sub.submittedPdfName} • ${sub.department} ${sub.section}",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = if (sub.isGraded) "Awarded: ${sub.marksAwarded}/10 • Feedback: ${sub.feedback}" else "Submitted ${sub.submissionDateFormatted} • Pending Review",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (sub.isGraded) CyberEmerald else LuminousAmber
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedButton(
                                            onClick = {
                                                previewDocument = DriveDocument(
                                                    id = "sub_${sub.id}",
                                                    collegeId = "col_ksrm_01",
                                                    title = "Assignment Submission - ${sub.studentName}",
                                                    folderName = "Student Submissions",
                                                    fileType = "PDF",
                                                    assetFileName = "VII SEM GS Assignment-IV -RR.pdf",
                                                    fileName = "VII SEM GS Assignment-IV -RR.pdf",
                                                    sizeFormatted = "2.1 MB",
                                                    uploadedBy = sub.studentName,
                                                    subject = "Autonomous Coursework",
                                                    subjectCode = "AUTO-2026"
                                                )
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonBlue),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("View PDF", fontSize = 10.5.sp)
                                        }

                                        Button(
                                            onClick = {
                                                gradingSubmission = sub
                                                gradeInputMarks = "${sub.marksAwarded ?: 9.0}"
                                                gradeInputFeedback = if (sub.feedback.isNotBlank()) sub.feedback else "Good work, step-by-step calculations verified."
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (sub.isGraded) Color(0xFF065F46) else ElectricIndigo
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(if (sub.isGraded) Icons.Default.CheckCircle else Icons.Default.Grade, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (sub.isGraded) CyberEmerald else Color.White)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                if (sub.isGraded) "✓ Re-Graded" else "Assign Marks",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (sub.isGraded) CyberEmerald else Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                FacultySectionTab.ATTENDANCE_SUMMARY -> {
                    val filteredSubs = if (summarySubjectCode == null) {
                        facultyAttendanceOverview.subjects
                    } else {
                        facultyAttendanceOverview.subjects.filter { it.subjectCode == summarySubjectCode }
                    }
                    val currentSelectedSub = facultyAttendanceOverview.subjects.find { it.subjectCode == summarySubjectCode }
                    val avgPercentage = if (filteredSubs.isNotEmpty()) {
                        filteredSubs.map { it.averageAttendancePercentage }.average()
                    } else 0.0

                    // Subject Filter Chips
                    SubjectFilterBar(
                        subjects = facultyAttendanceOverview.subjects,
                        selectedCode = summarySubjectCode,
                        onSelectCode = { summarySubjectCode = it },
                        isDarkMode = isDarkMode
                    )

                    // Grand Attendance Percentage KPI Card
                    GrandAttendancePercentageCard(
                        averagePercentage = avgPercentage,
                        totalConducted = filteredSubs.sumOf { it.totalClassesConducted },
                        totalStudents = filteredSubs.sumOf { it.enrolledStudents },
                        totalSafe = filteredSubs.sumOf { it.studentsAbove75 },
                        totalAtRisk = filteredSubs.sumOf { it.studentsBelow75 },
                        selectedSubjectName = currentSelectedSub?.subjectName,
                        isDarkMode = isDarkMode,
                        onExportClick = { showExportSummaryPdfDialog = true },
                        onNotifyRiskClick = { selectedSectionTab = FacultySectionTab.RISK_TRIAGE }
                    )

                    // Chart Selector Tabs
                    ChartViewSelectorTabs(
                        selectedView = activeChartTab,
                        onSelectView = { activeChartTab = it },
                        isDarkMode = isDarkMode
                    )

                    // Visual Charts according to active tab
                    when (activeChartTab) {
                        ChartFilterView.ALL_CHARTS -> {


                            SubjectAttendanceBarChartCard(
                                subjects = facultyAttendanceOverview.subjects,
                                selectedCode = summarySubjectCode,
                                onBarClick = { summarySubjectCode = if (summarySubjectCode == it) null else it },
                                isDarkMode = isDarkMode
                            )

                            WeeklyAttendanceTrendChartCard(
                                trendData = currentSelectedSub?.weeklyAttendanceTrend
                                    ?: facultyAttendanceOverview.subjects.first().weeklyAttendanceTrend,
                                subjectName = currentSelectedSub?.subjectName ?: "Aggregate (All Subjects)",
                                isDarkMode = isDarkMode
                            )

                            AttendanceTierDonutChartCard(
                                overview = facultyAttendanceOverview,
                                selectedSubject = currentSelectedSub,
                                isDarkMode = isDarkMode
                            )

                            PeriodAttendanceBreakdownCard(
                                periods = currentSelectedSub?.periodAverages
                                    ?: facultyAttendanceOverview.subjects.first().periodAverages,
                                subjectName = currentSelectedSub?.subjectName ?: "Deep Learning [2339609]",
                                isDarkMode = isDarkMode
                            )
                        }

                        ChartFilterView.SUBJECT_BARS -> {
                            SubjectAttendanceBarChartCard(
                                subjects = facultyAttendanceOverview.subjects,
                                selectedCode = summarySubjectCode,
                                onBarClick = { summarySubjectCode = if (summarySubjectCode == it) null else it },
                                isDarkMode = isDarkMode
                            )
                        }

                        ChartFilterView.WEEKLY_TREND -> {
                            WeeklyAttendanceTrendChartCard(
                                trendData = currentSelectedSub?.weeklyAttendanceTrend
                                    ?: facultyAttendanceOverview.subjects.first().weeklyAttendanceTrend,
                                subjectName = currentSelectedSub?.subjectName ?: "Aggregate (All Subjects)",
                                isDarkMode = isDarkMode
                            )
                        }

                        ChartFilterView.TIER_DISTRIBUTION -> {
                            AttendanceTierDonutChartCard(
                                overview = facultyAttendanceOverview,
                                selectedSubject = currentSelectedSub,
                                isDarkMode = isDarkMode
                            )
                        }
                    }

                    // Detailed Subject List
                    Text(
                        text = "DETAILED SUBJECT AUDIT (${filteredSubs.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )

                    filteredSubs.forEach { sub ->
                        SubjectDetailedAttendanceCard(
                            subject = sub,
                            isDarkMode = isDarkMode,
                            isSelected = sub.subjectCode == summarySubjectCode,
                            onClick = {
                                summarySubjectCode = if (summarySubjectCode == sub.subjectCode) null else sub.subjectCode
                            },
                            onExportReport = {
                                summarySubjectCode = sub.subjectCode
                                showExportSummaryPdfDialog = true
                            }
                        )
                    }
                }

                FacultySectionTab.ATTENDANCE_STUDIO -> {
                    Text(
                        text = "ATTENDANCE CAPTURE MODES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )

                    if (adaptiveInfo.isMediumOrExpanded) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FacultyActionCard(
                                title = "Group Face Scan",
                                subtitle = "Capture 40-50 Students",
                                icon = Icons.Default.Groups,
                                accentColor = CyberEmerald,
                                isDarkMode = isDarkMode,
                                modifier = Modifier.weight(1f),
                                onClick = { onLaunchCameraScan(selectedSubject) }
                            )
                            FacultyActionCard(
                                title = "QR Projector",
                                subtitle = "Dynamic Period Code",
                                icon = Icons.Default.QrCode2,
                                accentColor = LuminousAmber,
                                isDarkMode = isDarkMode,
                                modifier = Modifier.weight(1f),
                                onClick = { showQrProjectorDialog = true }
                            )
                            FacultyActionCard(
                                title = "Manual Ledger",
                                subtitle = "Edit P / A / L / OD",
                                icon = Icons.Default.EditCalendar,
                                accentColor = RoyalBlue,
                                isDarkMode = isDarkMode,
                                modifier = Modifier.weight(1f),
                                onClick = onLaunchManualLedger
                            )
                            FacultyActionCard(
                                title = "Faculty AI Co-Pilot",
                                subtitle = "Teaching & Grading AI",
                                icon = Icons.Default.SmartToy,
                                accentColor = ElectricIndigo,
                                isDarkMode = isDarkMode,
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateToChat("How to handle students with attendance between 65% and 75%?") }
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FacultyActionCard(
                                title = "Group Face Scan",
                                subtitle = "Capture 40-50 Students",
                                icon = Icons.Default.Groups,
                                accentColor = CyberEmerald,
                                isDarkMode = isDarkMode,
                                modifier = Modifier.weight(1f),
                                onClick = { onLaunchCameraScan(selectedSubject) }
                            )
                            FacultyActionCard(
                                title = "QR Projector",
                                subtitle = "Dynamic Period Code",
                                icon = Icons.Default.QrCode2,
                                accentColor = LuminousAmber,
                                isDarkMode = isDarkMode,
                                modifier = Modifier.weight(1f),
                                onClick = { showQrProjectorDialog = true }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FacultyActionCard(
                                title = "Manual Ledger",
                                subtitle = "Edit P / A / L / OD",
                                icon = Icons.Default.EditCalendar,
                                accentColor = RoyalBlue,
                                isDarkMode = isDarkMode,
                                modifier = Modifier.weight(1f),
                                onClick = onLaunchManualLedger
                            )
                            FacultyActionCard(
                                title = "Faculty AI Co-Pilot",
                                subtitle = "Teaching & Grading AI",
                                icon = Icons.Default.SmartToy,
                                accentColor = ElectricIndigo,
                                isDarkMode = isDarkMode,
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateToChat("How to handle students with attendance between 65% and 75%?") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Automated 75% Shortage Email Alert Banner
                    Surface(
                        color = Color(0xFF7F1D1D).copy(alpha = 0.25f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Mail, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Send 75% Shortage Email Alerts",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                )
                                Text(
                                    text = "Automated advisory to parent.penchalaprasad@gmail.com",
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 10.5.sp
                                )
                            }
                            Button(
                                onClick = {
                                    EmailAlertDispatcher.sendAttendanceShortageAlert(triggeredBy = "Faculty Mentor Dr. P. V. Kishore")
                                    emailAlertSentMessage = "Dispatched 75% Shortage Alert to Parent, Student & Principal!"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text("Dispatch Email", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    if (emailAlertSentMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = Color(0xFF065F46),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(emailAlertSentMessage!!, color = Color.White, fontSize = 11.5.sp, modifier = Modifier.weight(1f))
                                IconButton(onClick = { emailAlertSentMessage = null }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }

                FacultySectionTab.MARKS_ENTRY -> {
                    val activeStudent = marksStudentsList[selectedStudentIdx.coerceIn(0, marksStudentsList.size - 1)]
                    val dX1 = x1Val.toDoubleOrNull() ?: 0.0
                    val dX2 = x2Val.toDoubleOrNull() ?: 0.0
                    val dX3 = x3Val.toDoubleOrNull() ?: 0.0
                    val dX4 = x4Val.toDoubleOrNull() ?: 0.0
                    val dX5 = x5Val.toDoubleOrNull() ?: 0.0
                    val dX6 = x6Val.toDoubleOrNull() ?: 0.0
                    val dX7 = x7Val.toDoubleOrNull() ?: 0.0

                    val internalSum = dX1 + dX2 + dX3 + dX4 + dX5 + dX6
                    val grandTotal = internalSum + dX7
                    val gradeLetter = when {
                        grandTotal >= 90 -> "O (Outstanding)"
                        grandTotal >= 80 -> "A+ (Excellent)"
                        grandTotal >= 70 -> "A (Very Good)"
                        grandTotal >= 60 -> "B+ (Good)"
                        grandTotal >= 50 -> "B (Above Average)"
                        grandTotal >= 40 -> "C (Pass)"
                        else -> "F (Fail / Critical Risk)"
                    }

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
                                Column {
                                    Text(
                                        text = "Assessment Marks Studio",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "Continuous Assessment (Internals /60) + End-Term Exam (/40)",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                    )
                                }
                                Surface(
                                    color = ElectricIndigo.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "JNTUA / Autonomous",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricIndigo
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Student Selector Chips
                            Text(
                                text = "SELECT STUDENT (SECTION B):",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(marksStudentsList.size) { idx ->
                                    val item = marksStudentsList[idx]
                                    val isSelected = (idx == selectedStudentIdx)
                                    Surface(
                                        onClick = {
                                            selectedStudentIdx = idx
                                            marksSuccessNotice = null
                                            marksAiAdvice = null
                                            // Pre-fill realistic data for the selected student
                                            when (idx) {
                                                0 -> { x1Val = "8.5"; x2Val = "8.0"; x3Val = "9.0"; x4Val = "7.5"; x5Val = "8.5"; x6Val = "8.0"; x7Val = "34.0" }
                                                1 -> { x1Val = "9.0"; x2Val = "8.5"; x3Val = "9.5"; x4Val = "8.0"; x5Val = "9.0"; x6Val = "9.0"; x7Val = "36.0" }
                                                2 -> { x1Val = "7.0"; x2Val = "7.5"; x3Val = "8.0"; x4Val = "6.5"; x5Val = "7.0"; x6Val = "7.5"; x7Val = "28.0" }
                                                3 -> { x1Val = "8.0"; x2Val = "8.5"; x3Val = "8.5"; x4Val = "7.0"; x5Val = "8.0"; x6Val = "8.0"; x7Val = "31.0" }
                                                4 -> { x1Val = "7.5"; x2Val = "7.0"; x3Val = "8.0"; x4Val = "7.0"; x5Val = "7.5"; x6Val = "7.0"; x7Val = "30.0" }
                                                5 -> { x1Val = "4.5"; x2Val = "5.0"; x3Val = "6.0"; x4Val = "4.0"; x5Val = "4.5"; x6Val = "5.0"; x7Val = "18.0" }
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) CyberEmerald.copy(alpha = 0.2f) else if (isDarkMode) ObsidianElevated else LightBg,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) CyberEmerald else Color.Transparent)
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                            Text(
                                                text = item.second,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (isSelected) CyberEmerald else if (isDarkMode) Color.White else Color.Black,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${item.first} • ${item.third}",
                                                fontSize = 10.sp,
                                                color = TextMuted,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Grand Summary Score Card
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDarkMode) Color(0xFF0F1A30) else Color(0xFFF1F5F9),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                "Student: ${activeStudent.second}",
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDarkMode) Color.White else Color.Black,
                                                fontSize = 14.sp
                                            )
                                            Text("Roll: ${activeStudent.first} • CSE Sec B", color = TextSecondary, fontSize = 11.5.sp)
                                        }
                                        Surface(
                                            color = if (grandTotal >= 60) CyberEmerald.copy(alpha = 0.18f) else NeonCoral.copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (grandTotal >= 60) CyberEmerald else NeonCoral)
                                        ) {
                                            Text(
                                                text = gradeLetter,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                fontWeight = FontWeight.Bold,
                                                color = if (grandTotal >= 60) CyberEmerald else NeonCoral,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Internals Score Box
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isDarkMode) ObsidianElevated else Color.White,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.4f)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text("INTERNALS SCORE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    "${"%.1f".format(internalSum)} / 60",
                                                    fontWeight = FontWeight.Black,
                                                    color = CyberEmerald,
                                                    fontSize = 16.sp
                                                )
                                            }
                                        }

                                        // Grand Total Score Box
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isDarkMode) ObsidianElevated else Color.White,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text("GRAND TOTAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    "${"%.1f".format(grandTotal)} / 100",
                                                    fontWeight = FontWeight.Black,
                                                    color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                                    fontSize = 16.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Component Inputs Grid
                            Text("ENTER COMPONENT MARKS:", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = x1Val,
                                    onValueChange = { x1Val = it },
                                    label = { Text("Quiz 1 (10)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = x2Val,
                                    onValueChange = { x2Val = it },
                                    label = { Text("Mid-Term 1 (10)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = x3Val,
                                    onValueChange = { x3Val = it },
                                    label = { Text("Lab Practical (10)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = x4Val,
                                    onValueChange = { x4Val = it },
                                    label = { Text("Assignment (10)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = x5Val,
                                    onValueChange = { x5Val = it },
                                    label = { Text("Mid-Term 2 (10)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = x6Val,
                                    onValueChange = { x6Val = it },
                                    label = { Text("Seminar / Project (10)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = x7Val,
                                onValueChange = { x7Val = it },
                                label = { Text("End-Term Exam (Max 40)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            // Feedback banner
                            if (marksSuccessNotice != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = Color(0xFF065F46),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF6EE7B7))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(marksSuccessNotice!!, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        IconButton(onClick = { marksSuccessNotice = null }, modifier = Modifier.size(20.dp)) {
                                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }

                            // AI Assessment response
                            if (marksAiAdvice != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = Color(0xFF1E1B4B),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.6f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.SmartToy, contentDescription = null, tint = ElectricIndigo, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("AGENTRA AI Academic Diagnostic:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(marksAiAdvice!!, color = Color(0xFFE2E8F0), fontSize = 11.5.sp, lineHeight = 16.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Working Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        marksScope.launch {
                                            FirestoreRepository.saveStudentMarks(
                                                studentRoll = activeStudent.first,
                                                studentName = activeStudent.second,
                                                subjectCode = "2339609",
                                                subjectName = selectedSubject,
                                                x1 = dX1, x2 = dX2, x3 = dX3, x4 = dX4, x5 = dX5, x6 = dX6, x7 = dX7,
                                                updatedBy = "Dr. P. V. Kishore"
                                            )
                                            emailAlertSentMessage = "✅ Saved assessment marks for ${activeStudent.second} to cloud! Navigated to Home."
                                            selectedSectionTab = FacultySectionTab.ATTENDANCE_SUMMARY
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberEmeraldDark),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save to Cloud", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        isAnalyzingAiMarks = true
                                        marksAiAdvice = null
                                        marksScope.launch {
                                            val prompt = "Analyze student ${activeStudent.second} (${activeStudent.first}) with internal marks: Quiz1=$dX1/10, Mid1=$dX2/10, Lab=$dX3/10, Assign=$dX4/10, Mid2=$dX5/10, Seminar=$dX6/10, Total Internals=$internalSum/60. Provide 2-sentence actionable faculty advice on improvement areas."
                                            val res = OpenRouterAiService.getChatCompletion(prompt, UserRole.FACULTY)
                                            isAnalyzingAiMarks = false
                                            marksAiAdvice = res.getOrElse { "Student demonstrates solid conceptual comprehension in assignments and labs. Recommend focused problem-solving tutorials ahead of Mid-2 to maximize grand score." }
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f),
                                    enabled = !isAnalyzingAiMarks
                                ) {
                                    if (isAnalyzingAiMarks) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("AI Diagnostic", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                FacultySectionTab.RISK_TRIAGE -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDarkMode) ObsidianSurface else LightSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCoral.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.WarningAmber, contentDescription = null, tint = NeonCoral, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Section Attendance Shortage Monitoring",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Surface(
                                    color = NeonCoral.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "${displayedAtRiskStudents.size} Students At Attendance Risk (<75%)",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(color = NeonCoral, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text("FILTER AT-RISK BY SECTION:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val rSecOptions = listOf("All", "4th Yr Sec B", "1st Yr Sec A", "3rd Yr Sec C", "2nd Yr Sec A")
                                items(rSecOptions.size) { idx ->
                                    val opt = rSecOptions[idx]
                                    FilterChip(
                                        selected = selectedRiskSectionFilter == opt,
                                        onClick = { selectedRiskSectionFilter = opt },
                                        label = { Text(opt, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NeonCoral,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Faculty Action Required: Dispatch direct Gmail warnings to student and parent email IDs as mandated by university attendance regulations.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.5.sp
                                )
                            )

                            if (emailAlertSentMessage != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = Color(0xFF065F46),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(emailAlertSentMessage!!, color = Color.White, fontSize = 11.5.sp, modifier = Modifier.weight(1f))
                                        IconButton(onClick = { emailAlertSentMessage = null }, modifier = Modifier.size(20.dp)) {
                                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            displayedAtRiskStudents.forEach { student ->
                                val (name, roll, pct, section) = student
                                val studentEmail = if (roll == "239Y1A0501") "penchalaprasad70@gmail.com" else "${roll.lowercase()}@ksrmce.ac.in"
                                val parentEmail = if (roll == "239Y1A0501") "parent.penchalaprasad@gmail.com" else "parent.${roll.lowercase()}@gmail.com"
                                val isDetention = pct < 65.0
                                val isCondonation = pct in 65.0..74.9
                                val badgeColor = if (isDetention) Color(0xFFEF4444) else if (isCondonation) LuminousAmber else CyberEmerald

                                Surface(
                                    color = if (isDarkMode) Color(0xFF1C1322) else Color(0xFFFFF1F2),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = name,
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                                Text(
                                                    text = "Roll: $roll • $section • $selectedSubject",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = TextSecondary,
                                                        fontSize = 11.sp
                                                    )
                                                )
                                            }
                                            Surface(
                                                color = badgeColor.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "${"%.1f".format(pct)}% ${if (isDetention) "DETENTION" else if (isCondonation) "CONDONATION" else "BORDERLINE"}",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = badgeColor,
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 10.5.sp
                                                    )
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(
                                                text = "To: $studentEmail",
                                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp),
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "Cc: $parentEmail",
                                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Real Gmail Intent Dispatch Button
                                            Button(
                                                onClick = {
                                                    EmailAlertDispatcher.launchGmailLowAttendanceIntent(
                                                        context = context,
                                                        studentEmail = studentEmail,
                                                        parentEmail = parentEmail,
                                                        studentName = name,
                                                        rollNumber = roll,
                                                        subjectName = selectedSubject,
                                                        currentAttendance = pct,
                                                        facultyName = "Dr. P. V. Kishore"
                                                    )
                                                    emailAlertSentMessage = "Dispatched official low-attendance Gmail alert for $name ($roll)!"
                                                },
                                                modifier = Modifier.weight(1f).height(38.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Send Gmail Notice", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }

                                            // SMTP Dispatch Button
                                            OutlinedButton(
                                                onClick = {
                                                    EmailAlertDispatcher.sendAttendanceShortageAlert(
                                                        studentName = name,
                                                        rollNumber = roll,
                                                        currentPercentage = pct,
                                                        triggeredBy = "Faculty In-Charge Dr. P. V. Kishore"
                                                    )
                                                    emailAlertSentMessage = "Sent automated attendance advisory to $studentEmail & $parentEmail via KSRMCE SMTP!"
                                                },
                                                modifier = Modifier.weight(1f).height(38.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp), tint = ElectricIndigo)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Auto SMTP Alert", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElectricIndigo)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Batch Action for All At-Risk Students
                            Button(
                                onClick = {
                                    displayedAtRiskStudents.forEach { s ->
                                        EmailAlertDispatcher.sendAttendanceShortageAlert(
                                            studentName = s.name,
                                            rollNumber = s.roll,
                                            currentPercentage = s.pct,
                                            triggeredBy = "Faculty In-Charge Dr. P. V. Kishore (Batch Shortage Run)"
                                        )
                                    }
                                    emailAlertSentMessage = "Successfully dispatched formal attendance shortage notices to ALL ${displayedAtRiskStudents.size} at-risk students & parents in $selectedRiskSectionFilter!"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Dispatch Warnings to ${displayedAtRiskStudents.size} Parents ($selectedRiskSectionFilter)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = {
                                    counselorNoticeSent = true
                                    onNavigateToChat("Faculty Query: Formulate remedial assignment and 1-month recovery plan for Section B at-risk students.")
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCoral),
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.ContactSupport, contentDescription = null, modifier = Modifier.size(16.dp), tint = NeonCoral)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (counselorNoticeSent) "✓ Remedial Plan Dispatched" else "Consult AGENTRA AI Remedial Plan",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = NeonCoral,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // === STUDY MATERIALS & PDF UPLOADS ===
                FacultySectionTab.STUDY_MATERIALS -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDarkMode) ObsidianElevated else LightSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Study Materials & PDF Repository",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Upload lecture slides, question banks, or notes scoped specifically to student sections.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.5.sp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { materialPdfUploadLauncher.launch("application/pdf") },
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(40.dp)
                                    ) {
                                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Upload from Phone", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = { showUploadMaterialDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(40.dp).testTag("faculty_upload_pdf_btn")
                                    ) {
                                        Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color(0xFF042F2E), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add Note", color = Color(0xFF042F2E), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text("TARGET STUDENT SECTION:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val matSecOptions = listOf("All", "4th Yr Sec B", "1st Yr Sec A", "3rd Yr Sec C", "2nd Yr Sec A")
                                items(matSecOptions.size) { idx ->
                                    val opt = matSecOptions[idx]
                                    FilterChip(
                                        selected = selectedMaterialSectionFilter == opt,
                                        onClick = { selectedMaterialSectionFilter = opt },
                                        label = { Text(opt, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = LuminousAmber,
                                            selectedLabelColor = Color.Black
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = if (isDarkMode) ObsidianBorder else LightElevated)
                            Spacer(modifier = Modifier.height(12.dp))

                            val filteredDocs = AcademicDataProvider.sharedDriveDocs.filter { doc ->
                                if (selectedMaterialSectionFilter == "All") true
                                else doc.folderName.contains(selectedMaterialSectionFilter.substringAfter("Yr ").trim(), ignoreCase = true) || doc.title.contains(selectedMaterialSectionFilter, ignoreCase = true)
                            }

                            Text(
                                text = "Shared Section Documents (${filteredDocs.size})",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            filteredDocs.forEach { doc ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDarkMode) ObsidianSurface else Color(0xFFF8FAFC),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            previewDocument = doc.copy(assetFileName = "BECG_CASE_STUDY.pdf", fileName = "BECG_CASE_STUDY.pdf")
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFFEF4444).copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    doc.title,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                                        fontSize = 13.sp
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    "📁 ${doc.folderName} • ${doc.sizeFormatted} • By ${doc.uploadedBy}",
                                                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.5.sp)
                                                )
                                            }
                                            Surface(
                                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    "Verified",
                                                    color = Color(0xFF10B981),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    previewDocument = doc.copy(assetFileName = "BECG_CASE_STUDY.pdf", fileName = "BECG_CASE_STUDY.pdf")
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberEmerald),
                                                modifier = Modifier.weight(1f).height(36.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("View PDF", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                            }

                                            Button(
                                                onClick = {
                                                    com.example.util.PdfHelper.downloadPdfToDevice(
                                                        context = context,
                                                        fileNameOrAsset = "BECG_CASE_STUDY.pdf",
                                                        displayTitle = "${doc.title.replace(' ', '_')}.pdf"
                                                    )
                                                    emailAlertSentMessage = "Downloaded '${doc.title}' to phone storage!"
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                modifier = Modifier.weight(1f).height(36.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF042F2E), modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Download PDF", color = Color(0xFF042F2E), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // === EXAM SCHEDULES & DUTIES ===
                FacultySectionTab.EXAM_DUTIES -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDarkMode) ObsidianElevated else LightSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Autonomous Exam Schedules (HOD Published)",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Official university timetables and invigilation duties.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.5.sp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            previewDocument = DriveDocument(
                                                id = "exam_tt_viisem",
                                                collegeId = "col_ksrm_01",
                                                title = "Official VII-Semester Exam Timetable 2026-27",
                                                folderName = "Autonomous Schedules",
                                                fileType = "PDF",
                                                assetFileName = "VIISEM 26-27.pdf",
                                                fileName = "VIISEM 26-27.pdf",
                                                sizeFormatted = "2.4 MB",
                                                uploadedBy = "Chief Controller of Examinations",
                                                subject = "Exam Timetable",
                                                subjectCode = "JNTUA-Autonomous"
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(40.dp)
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("View PDF", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = {
                                            com.example.util.PdfHelper.downloadPdfToDevice(
                                                context = context,
                                                fileNameOrAsset = "VIISEM 26-27.pdf",
                                                displayTitle = "Official_VIISEM_Timetable_2026_27.pdf"
                                            )
                                            emailAlertSentMessage = "Downloaded Official VII-Semester Timetable PDF to phone Downloads!"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(40.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, tint = ObsidianBg, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Download PDF", color = ObsidianBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text("FILTER BY EXAM SECTION:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val examSecOptions = listOf("All", "4th Yr Sec B", "1st Yr Sec A", "3rd Yr Sec C", "2nd Yr Sec A")
                                items(examSecOptions.size) { idx ->
                                    val opt = examSecOptions[idx]
                                    FilterChip(
                                        selected = selectedExamSectionFilter == opt,
                                        onClick = { selectedExamSectionFilter = opt },
                                        label = { Text(opt, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF8B5CF6),
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            AcademicDataProvider.sharedExamSchedules.forEach { schedule ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDarkMode) ObsidianSurface else Color(0xFFF8FAFC),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = schedule.subjectName,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                                    ),
                                                    modifier = Modifier.weight(1f, fill = false),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = CyberEmerald.copy(alpha = 0.15f),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.4f))
                                                ) {
                                                    Text(
                                                        text = schedule.subjectCode,
                                                        color = CyberEmerald,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.5.sp,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "Date: ${schedule.examDate} • Slot: ${schedule.timeSlot}",
                                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 11.sp)
                                            )
                                            Text(
                                                text = "Duty: Chief Invigilator • Exam Hall: Block 3 Hall 102",
                                                style = MaterialTheme.typography.labelSmall.copy(color = ElectricIndigo, fontWeight = FontWeight.SemiBold, fontSize = 10.5.sp)
                                            )
                                        }
                                        var isDutyConfirmed by remember { mutableStateOf(true) }
                                        Surface(
                                            color = if (isDutyConfirmed) Color(0xFF8B5CF6).copy(alpha = 0.25f) else Color(0xFF10B981).copy(alpha = 0.25f),
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isDutyConfirmed) Color(0xFF8B5CF6) else Color(0xFF10B981)
                                            ),
                                            modifier = Modifier.clickable {
                                                isDutyConfirmed = !isDutyConfirmed
                                                emailAlertSentMessage = if (isDutyConfirmed)
                                                    "Chief Invigilation Duty CONFIRMED for ${schedule.subjectName}!"
                                                else
                                                    "Invigilation Duty status updated to ACCEPTED & SCHEDULED."
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = if (isDutyConfirmed) Icons.Default.CheckCircle else Icons.Default.Schedule,
                                                    contentDescription = null,
                                                    tint = if (isDutyConfirmed) Color(0xFF8B5CF6) else Color(0xFF10B981),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    if (isDutyConfirmed) "Duty Assigned" else "Confirmed",
                                                    color = if (isDutyConfirmed) (if (isDarkMode) Color(0xFFC4B5FD) else Color(0xFF8B5CF6)) else Color(0xFF10B981),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                FacultySectionTab.FACULTY_SETTINGS -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDarkMode) ObsidianSurface else LightSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Faculty Instructor Settings & Profile",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Configure faculty profile, default lecture sections, SMS alerts, and preferences.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            if (settingsSaveFeedback != null) {
                                Surface(
                                    color = Color(0xFF065F46),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(settingsSaveFeedback ?: "", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        IconButton(onClick = { settingsSaveFeedback = null }, modifier = Modifier.size(20.dp)) {
                                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            // Editable Profile Fields
                            Text("FACULTY IDENTIFICATION & CONTACT:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = facultyProfileName,
                                    onValueChange = { facultyProfileName = it },
                                    label = { Text("Faculty Full Name") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = facultyProfileEmpId,
                                    onValueChange = { facultyProfileEmpId = it },
                                    label = { Text("Employee ID") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = facultyProfileDept,
                                    onValueChange = { facultyProfileDept = it },
                                    label = { Text("Department") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = facultyProfileDesig,
                                    onValueChange = { facultyProfileDesig = it },
                                    label = { Text("Designation") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = facultyProfileEmail,
                                    onValueChange = { facultyProfileEmail = it },
                                    label = { Text("Official Email") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = facultyProfileOffice,
                                    onValueChange = { facultyProfileOffice = it },
                                    label = { Text("Office / Cabin") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = if (isDarkMode) ObsidianBorder else LightElevated)
                            Spacer(modifier = Modifier.height(12.dp))

                            SettingSwitchRow(
                                title = "Automated Absentee Parent SMS",
                                subtitle = "Sends attendance SMS alert after period ledger is submitted",
                                checked = smsAlerts,
                                onCheckedChange = { smsAlerts = it; facultySettings.notifyAbsenteeParentsSms = it },
                                isDarkMode = isDarkMode
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = if (isDarkMode) ObsidianBorder else LightElevated)

                            SettingSwitchRow(
                                title = "Auto-Flag Students at Risk (<75%)",
                                subtitle = "Automatically triggers Advisor Agent alert upon consecutive absences",
                                checked = autoFlagRisk,
                                onCheckedChange = { autoFlagRisk = it; facultySettings.autoFlagAtRiskStudents = it },
                                isDarkMode = isDarkMode
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = if (isDarkMode) ObsidianBorder else LightElevated)

                            SettingSwitchRow(
                                title = "Dark / Night Theme Mode",
                                subtitle = "Toggle overall app color scheme",
                                checked = isDarkMode,
                                onCheckedChange = { onToggleTheme() },
                                isDarkMode = isDarkMode
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    facultySettings.defaultSection = defaultSec
                                    facultySettings.defaultPeriod = defaultPer
                                    facultySettings.notifyAbsenteeParentsSms = smsAlerts
                                    facultySettings.autoFlagAtRiskStudents = autoFlagRisk
                                    AcademicDataProvider.saveDataToDisk()
                                    settingsSaveFeedback = "✓ All Faculty settings, profile & notification preferences saved successfully!"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberEmeraldDark),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(46.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Settings Changes", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
        }
    }
    }

    // QR Projector Dialog
    if (showQrProjectorDialog) {
        val qrSessionPayload = """
            {
              "institution": "KSRM College of Engineering (Autonomous)",
              "course": "$selectedSubject",
              "faculty": "Dr. P. V. Kishore",
              "section": "$selectedSection",
              "period": "Period 1 (09:00 - 10:00 AM)",
              "room": "CSE Block Room 304",
              "sessionToken": "KSRM-DL-20261011-P1",
              "blockchainHash": "0x7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069"
            }
        """.trimIndent()

        AlertDialog(
            onDismissRequest = { showQrProjectorDialog = false },
            title = { Text("Smartboard QR Attendance Projector", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Display on classroom smartboard for 45-50 students to scan:", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Real scannable 2D QR Code Matrix
                    RealQRCodeView(
                        dataString = qrSessionPayload,
                        size = 200.dp
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Preview token only — not server-signed", fontWeight = FontWeight.Bold, color = CyberEmerald, fontSize = 12.sp)
                    Text("Blockchain/audit verification is not configured", color = TextSecondary, fontSize = 10.sp)
                    Text("Scannable by any mobile camera or QR reader", color = TextMuted, fontSize = 10.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showQrProjectorDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Marks Entry Studio Dialog
    if (showMarksEntryDialog) {
        var x1Mark by remember { mutableStateOf("8.5") }
        var x2Mark by remember { mutableStateOf("7.0") }
        var x3Mark by remember { mutableStateOf("9.0") }
        var marksSaved by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showMarksEntryDialog = false },
            title = { Text("Internal Marks Assessment Entry", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Student: Sattu Penchala Prasad (239Y1A0501)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    OutlinedTextField(
                        value = x1Mark,
                        onValueChange = { x1Mark = it },
                        label = { Text("Quiz 1 (Max 10)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = x2Mark,
                        onValueChange = { x2Mark = it },
                        label = { Text("Mid Assessment 1 (Max 10)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = x3Mark,
                        onValueChange = { x3Mark = it },
                        label = { Text("Lab Experiment (Max 10)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (marksSaved) {
                        Text("Marks synced with Research Risk Model!", color = CyberEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { marksSaved = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                ) {
                    Text("Save Marks")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMarksEntryDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    // Upload Study Material / PDF Dialog
    if (showUploadMaterialDialog) {
        var docTitle by remember { mutableStateOf("Deep Learning - Unit 2 Convolutional Architectures & ResNet.pdf") }
        var docSubject by remember { mutableStateOf("Deep Learning [2339609]") }
        var docFolder by remember { mutableStateOf("Class Notes & Lectures") }
        var docSize by remember { mutableStateOf("3.4 MB") }

        AlertDialog(
            onDismissRequest = { showUploadMaterialDialog = false },
            title = { Text("Upload Study Material / PDF", fontWeight = FontWeight.Bold) },
            text = {
                val docPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    uri?.let { pickedUri ->
                        val chosenName = pickedUri.lastPathSegment?.substringAfterLast("/") ?: "Uploaded_Document.pdf"
                        docTitle = if (chosenName.endsWith(".pdf", ignoreCase = true)) chosenName else "$chosenName.pdf"
                        docSize = "2.8 MB"
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Uploaded documents and PDFs are immediately synchronized to all Student logins.",
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B)
                    )

                    OutlinedButton(
                        onClick = { docPicker.launch("application/pdf") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981))
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Browse Device Storage / Files", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                    }

                    OutlinedTextField(
                        value = docTitle,
                        onValueChange = { docTitle = it },
                        label = { Text("Document Title (with .pdf)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = docSubject,
                        onValueChange = { docSubject = it },
                        label = { Text("Subject / Course") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = docFolder,
                        onValueChange = { docFolder = it },
                        label = { Text("Folder (Notes / Lab / QB)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = docSize,
                        onValueChange = { docSize = it },
                        label = { Text("File Size") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        AcademicDataProvider.publishFacultyDocument(
                            title = docTitle,
                            folderName = docFolder,
                            fileType = "PDF",
                            sizeFormatted = docSize,
                            uploadedBy = "Dr. P. V. Kishore (CSE)",
                            subjectCode = "2339609"
                        )
                        emailAlertSentMessage = "Uploaded $docTitle! Available immediately to all student logins."
                        showUploadMaterialDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Upload & Share", color = Color(0xFF042F2E), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUploadMaterialDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // PDF Preview Modal
    previewDocument?.let { doc ->
        PdfViewerDialog(
            document = doc,
            isDarkMode = isDarkMode,
            onDismiss = { previewDocument = null },
            onDownloadCompleted = { msg -> emailAlertSentMessage = msg }
        )
    }

    if (showExportSummaryPdfDialog) {
        val currentSub = facultyAttendanceOverview.subjects.find { it.subjectCode == summarySubjectCode }
        ExportAttendanceReportDialog(
            overview = facultyAttendanceOverview,
            selectedSubject = currentSub,
            isDarkMode = isDarkMode,
            onDismiss = { showExportSummaryPdfDialog = false },
            onConfirmExport = {
                showExportSummaryPdfDialog = false
                emailAlertSentMessage = "Attendance Summary PDF Generated: KSRMCE_Attendance_${currentSub?.subjectCode ?: "ALL"}.pdf saved!"
            }
        )
    }

    // Publish Assignment Question PDF Dialog
    if (showPublishAssignmentDialog) {
        var asgnTargetDept by remember { mutableStateOf("CSE") }
        var asgnTargetYear by remember { mutableStateOf("4") }
        var asgnTargetSec by remember { mutableStateOf("Sec B") }
        var asgnPdfName by remember { mutableStateOf("Assignment_Questions.pdf") }

        AlertDialog(
            onDismissRequest = { showPublishAssignmentDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, tint = ElectricIndigo)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Publish Course Assignment PDF", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Distribute assignment question paper with strict submission deadline.", fontSize = 11.5.sp, color = TextSecondary)

                    OutlinedTextField(
                        value = publishAssignmentTitle,
                        onValueChange = { publishAssignmentTitle = it },
                        label = { Text("Assignment Title (e.g. Unit 3 Backprop Numerical)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = publishAssignmentDesc,
                        onValueChange = { publishAssignmentDesc = it },
                        label = { Text("Instructions / Requirements") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = asgnPdfName,
                        onValueChange = { asgnPdfName = it },
                        label = { Text("Question Paper PDF File Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = asgnTargetDept,
                            onValueChange = { asgnTargetDept = it },
                            label = { Text("Dept (CSE/ECE)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = asgnTargetYear,
                            onValueChange = { asgnTargetYear = it },
                            label = { Text("Year (1-4)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = asgnTargetSec,
                            onValueChange = { asgnTargetSec = it },
                            label = { Text("Sec (A/B)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = publishAssignmentDeadline,
                        onValueChange = { publishAssignmentDeadline = it },
                        label = { Text("Submission Deadline") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newAsgn = CampusAssignment(
                            id = "asgn_" + System.currentTimeMillis(),
                            collegeId = "col_ksrm_01",
                            department = asgnTargetDept,
                            year = asgnTargetYear.toIntOrNull() ?: 4,
                            section = asgnTargetSec,
                            subjectCode = selectedSubject.substringAfter("[").substringBefore("]"),
                            subjectName = selectedSubject.substringBefore(" ["),
                            title = if (publishAssignmentTitle.isNotBlank()) publishAssignmentTitle else "Course Assignment",
                            description = if (publishAssignmentDesc.isNotBlank()) publishAssignmentDesc else "Solve all numericals and upload verified PDF.",
                            facultyName = "Dr. P. V. Kishore",
                            deadlineTimestamp = System.currentTimeMillis() + (3 * 24 * 60 * 60 * 1000),
                            deadlineFormatted = publishAssignmentDeadline,
                            questionPdfName = asgnPdfName,
                            maxMarks = 10,
                            isClosed = false
                        )
                        AcademicDataProvider.publishAssignment(newAsgn)
                        showPublishAssignmentDialog = false
                        publishAssignmentTitle = ""
                        publishAssignmentDesc = ""
                        emailAlertSentMessage = "Assignment '${newAsgn.title}' published & broadcasted to $asgnTargetDept Year $asgnTargetYear students!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberEmeraldDark)
                ) {
                    Text("Publish to Students", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPublishAssignmentDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Grade Student Submission Dialog
    gradingSubmission?.let { sub ->
        AlertDialog(
            onDismissRequest = { gradingSubmission = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Grade, contentDescription = null, tint = CyberEmerald)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Grade Student Submission", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Student: ${sub.studentName} (${sub.rollNumber})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Submitted PDF: ${sub.submittedPdfName} (${sub.submissionDateFormatted})", fontSize = 11.5.sp, color = ElectricIndigo)

                    OutlinedTextField(
                        value = gradeInputMarks,
                        onValueChange = { gradeInputMarks = it },
                        label = { Text("Awarded Marks (Max 10)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = gradeInputFeedback,
                        onValueChange = { gradeInputFeedback = it },
                        label = { Text("Faculty Feedback & Remarks") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val marksVal = gradeInputMarks.toDoubleOrNull() ?: 9.0
                        AcademicDataProvider.gradeAssignment(sub.id, marksVal, gradeInputFeedback)
                        gradingSubmission = null
                        emailAlertSentMessage = "Graded ${sub.studentName}'s assignment: $marksVal/10. Synced to student marks card."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberEmeraldDark)
                ) {
                    Text("Save & Notify Student", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { gradingSubmission = null }) { Text("Cancel") }
            }
        )
    }
}
}

@Composable
fun FacultyTabButton(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    accentColor: Color = ElectricIndigo,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.22f) else accentColor.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) accentColor else accentColor.copy(alpha = 0.35f)
        ),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) accentColor else accentColor.copy(alpha = 0.75f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isSelected) Color.White else accentColor.copy(alpha = 0.9f),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 11.5.sp
                )
            )
        }
    }
}

@Composable
fun FacultyActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(115.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (isDarkMode) ObsidianSurface else LightSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            }

            Column {
                Text(
                    text = title,
                    color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(text = subtitle, color = TextSecondary, fontSize = 11.sp)
            }
        }
    }
}
