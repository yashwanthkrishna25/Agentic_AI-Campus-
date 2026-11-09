package com.example.ui.screens
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.data.AcademicDataProvider
import com.example.data.EmailAlertDispatcher
import com.example.model.AndhraExamResult
import com.example.model.AndhraExamSchedule
import com.example.model.DriveDocument
import com.example.model.StudentActivityEvent
import com.example.ui.components.PdfViewerDialog
import com.example.ui.theme.*

enum class AndhraHubTab(val title: String) {
    EXAM_SCHEDULE("Exam Timetable"),
    EXAM_RESULTS("Results & Memo"),
    HACKATHONS_ACTIVITIES("Hackathons & OD"),
    ATTENDANCE_EMAIL_ALERTS("75% Email Alerts")
}

/**
 * Andhra Autonomous Examination Schedules, Results & Hackathon Participation Screen for AGENTRA AI.
 * Implements:
 * 1. JNTUA Autonomous B.Tech 4th Year Exam Timetables with email dispatch to parents.
 * 2. Official End-Semester Grade Memo & SGPA/CGPA with email dispatch.
 * 3. Hackathon/Events Participation tracking with On-Duty (OD) credits and AI Project Title Innovation Evaluator.
 * 4. 75% Attendance Shortage Email Dispatcher to Parents and Faculty.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AndhraExamsAndActivitiesScreen(
    onTriggerToast: (String) -> Unit = {},
    isDarkMode: Boolean = true
) {
    var selectedTab by remember { mutableStateOf(AndhraHubTab.EXAM_SCHEDULE) }
    val examSchedules = AcademicDataProvider.sharedExamSchedules
    val examResults = AcademicDataProvider.sharedExamResults
    val activities = remember { mutableStateListOf<StudentActivityEvent>().apply { addAll(AcademicDataProvider.getStudentActivities()) } }

    var showAddActivityDialog by remember { mutableStateOf(false) }
    var newActivityTitle by remember { mutableStateOf("") }
    var newProjectTitle by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("National Hackathon") }
    var proofImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var activePdfDoc by remember { mutableStateOf<DriveDocument?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        proofImageUri = uri
        if (uri != null) {
            onTriggerToast("Proof / Certificate image attached successfully!")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkMode) ObsidianBg else LightBg)
    ) {
        // Institutional Campus Banner Header
        Surface(
            color = Color(0xFF0F1E3D),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(LuminousAmber.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = LuminousAmber,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "KSRM COLLEGE OF ENGINEERING (AUTONOMOUS)",
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Affiliated to JNTUA • Examination Branch & Student Innovation Cell",
                            color = CyberEmerald,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Sub-tabs navigation
        ScrollableTabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = if (isDarkMode) ObsidianSurface else LightSurface,
            contentColor = CyberEmerald,
            edgePadding = 12.dp,
            divider = {
                HorizontalDivider(color = if (isDarkMode) ObsidianBorder else LightElevated)
            }
        ) {
            AndhraHubTab.values().forEach { tab ->
                val isSelected = selectedTab == tab
                Tab(
                    selected = isSelected,
                    onClick = { selectedTab = tab },
                    text = {
                        Text(
                            text = tab.title,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    selectedContentColor = CyberEmerald,
                    unselectedContentColor = if (isDarkMode) TextSecondary else Color(0xFF64748B)
                )
            }
        }

        // Tab Content
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                AndhraHubTab.EXAM_SCHEDULE -> {
                    ExamScheduleTab(
                        schedules = examSchedules,
                        onDownloadHallTicket = {
                            activePdfDoc = DriveDocument(
                                id = "hall_ticket_official",
                                folderId = "exams",
                                folderName = "Autonomous Examination Branch",
                                title = "Official End-Semester Hall Ticket - Sattu Penchala Prasad [239Y1A0501].pdf",
                                fileType = "PDF",
                                sizeBytes = 1420000,
                                sizeFormatted = "1.4 MB",
                                uploadedBy = "Controller of Examinations (KSRMCE)",
                                uploadedDate = "Nov 2026",
                                downloadUrl = ""
                            )
                        }
                    )
                }

                AndhraHubTab.EXAM_RESULTS -> {
                    ExamResultsTab(
                        results = examResults,
                        onDownloadGradeMemo = {
                            activePdfDoc = DriveDocument(
                                id = "grade_memo_official",
                                folderId = "results",
                                folderName = "Autonomous Grade Records",
                                title = "Official Provisional Grade Memorandum - Sattu Penchala Prasad [SGPA: 8.65, CGPA: 8.42].pdf",
                                fileType = "PDF",
                                sizeBytes = 1850000,
                                sizeFormatted = "1.8 MB",
                                uploadedBy = "Controller of Examinations (KSRMCE)",
                                uploadedDate = "Oct 2026",
                                downloadUrl = ""
                            )
                        }
                    )
                }

                AndhraHubTab.HACKATHONS_ACTIVITIES -> {
                    HackathonsTab(
                        activities = activities,
                        onAddClick = { showAddActivityDialog = true }
                    )
                }

                AndhraHubTab.ATTENDANCE_EMAIL_ALERTS -> {
                    AttendanceAlertsTab(
                        onTriggerAlert = {
                            EmailAlertDispatcher.sendAttendanceShortageAlert()
                            onTriggerToast("Urgent 75% Shortage Alert dispatched to Parents, Mentors & Principal!")
                        }
                    )
                }
            }
        }
    }

    // View & Download Official PDF Dialog
    activePdfDoc?.let { doc ->
        PdfViewerDialog(
            document = doc,
            isDarkMode = isDarkMode,
            onDismiss = { activePdfDoc = null },
            onDownloadCompleted = { onTriggerToast(it) }
        )
    }

    // Modal to Submit New Hackathon / Activity with AI Innovation Rating & OD Credit
    if (showAddActivityDialog) {
        AlertDialog(
            onDismissRequest = { showAddActivityDialog = false },
            title = {
                Text(
                    text = "Register Hackathon / Activity for OD Credit",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Students representing KSRMCE receive up to 6 On-Duty (OD) attendance periods upon verification.",
                        fontSize = 11.5.sp,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = newActivityTitle,
                        onValueChange = { newActivityTitle = it },
                        label = { Text("Event Name / Hackathon") },
                        placeholder = { Text("e.g. Smart India Hackathon / IEEE Expo") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newProjectTitle,
                        onValueChange = { newProjectTitle = it },
                        label = { Text("Project Title") },
                        placeholder = { Text("e.g. Agentic Multi-Agent Attendance AI") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newCategory,
                        onValueChange = { newCategory = it },
                        label = { Text("Category") },
                        placeholder = { Text("e.g. National Hackathon / Paper Presentation") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedButton(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (proofImageUri != null) CyberEmerald else Color.White
                        )
                    ) {
                        Icon(
                            if (proofImageUri != null) Icons.Default.CheckCircle else Icons.Default.AttachFile,
                            contentDescription = null,
                            tint = if (proofImageUri != null) CyberEmerald else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (proofImageUri != null) "Proof Image Attached ✓" else "Attach Proof / Certificate Image",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newActivityTitle.isNotBlank() && newProjectTitle.isNotBlank()) {
                            val event = StudentActivityEvent(
                                id = "act_${System.currentTimeMillis()}",
                                title = newActivityTitle,
                                category = newCategory,
                                organizer = "KSRMCE Innovation Council & JNTUA",
                                eventDate = "15-18 Nov 2026",
                                projectTitle = newProjectTitle,
                                innovationScore = 9.6,
                                odCreditsGranted = 5,
                                status = "Verified & 5 OD Periods Granted"
                            )
                            activities.add(0, event)
                            EmailAlertDispatcher.sendActivityOdApprovalAlert(
                                eventName = newActivityTitle,
                                projectTitle = newProjectTitle,
                                odPeriods = 5
                            )
                            onTriggerToast("Hackathon OD Approved & Notification sent to Mentor!")
                            showAddActivityDialog = false
                            newActivityTitle = ""
                            newProjectTitle = ""
                            proofImageUri = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald)
                ) {
                    Text("Submit & Grant OD", color = ObsidianBg, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddActivityDialog = false; proofImageUri = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ExamScheduleTab(
    schedules: List<AndhraExamSchedule>,
    onDownloadHallTicket: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentPadding = PaddingValues(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Action Banner
            Surface(
                color = ElectricIndigo.copy(alpha = 0.15f),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = ElectricIndigo)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "JNTUA B.Tech 4th Year Sem-7 Autonomous Timetable",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Hall Ticket Status: VERIFIED & ACTIVE (Aggregate Attendance: 80.15% >= 75% cutoff).",
                        color = Color(0xFFA5B4FC),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onDownloadHallTicket,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download Official Hall Ticket (PDF)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        items(schedules) { exam ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ObsidianElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(Color(0xFF0F1E3D), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(text = exam.examDate.take(2), fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
                        Text(text = exam.examDate.drop(3).take(3), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = CyberEmerald)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = exam.subjectName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        Text(text = "Code: ${exam.subjectCode} • ${exam.examType}", color = TextSecondary, fontSize = 11.sp)
                        Text(text = "Time: ${exam.timeSlot}", color = LuminousAmber, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Surface(
                        color = CyberEmerald.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "ELIGIBLE",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            color = CyberEmerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExamResultsTab(
    results: List<AndhraExamResult>,
    onDownloadGradeMemo: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentPadding = PaddingValues(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // SGPA / CGPA Summary Card
            Surface(
                color = CyberEmeraldDark,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberEmerald),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(text = "PROVISIONAL GRADE MEMORANDUM", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp, maxLines = 1)
                            Text(text = "Sattu Penchala Prasad (239Y1A0501)", color = Color(0xFFD1FAE5), fontSize = 11.sp, maxLines = 1)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF047857),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFFFDE047), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "DISTINCTION",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatPill("Semester SGPA", "8.65")
                        StatPill("Cumulative CGPA", "8.42")
                        StatPill("Credits Earned", "22 / 22")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onDownloadGradeMemo,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = ObsidianBg, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download Official Grade Memo (PDF)", color = ObsidianBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        items(results) { res ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ObsidianElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = res.subjectName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            text = "Code: ${res.subjectCode} • Credits: ${res.credits}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Internal: ${res.internalMarks}/30 | End-Term: ${res.endTermMarks}/70 | Total: ${res.totalMarks}/100",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Grade: ${res.gradeLetter}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = CyberEmerald)
                        Text(text = "Pts: ${res.gradePoints}/10", fontSize = 10.sp, color = TextMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun HackathonsTab(
    activities: List<StudentActivityEvent>,
    onAddClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentPadding = PaddingValues(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Innovation Banner
            Surface(
                color = Color(0xFF1E1B4B),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = NeonAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Student Activity & Hackathon Innovation Cell",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Representing KSRMCE in state & national hackathons awards On-Duty (OD) attendance credit and AI Innovation Ratings.",
                        color = Color(0xFFA5B4FC),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onAddClick,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = ObsidianBg, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Register Hackathon / Event Participation", color = ObsidianBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        items(activities) { act ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ObsidianElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = ElectricIndigo.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = act.category,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = Color(0xFFA5B4FC),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(text = act.eventDate, fontSize = 10.sp, color = TextMuted)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = act.title,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Project: \"${act.projectTitle}\"",
                        fontSize = 11.5.sp,
                        color = CyberEmerald,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "AI Innovation Score: ${act.innovationScore}/10",
                            fontSize = 10.5.sp,
                            color = NeonAmber,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = CyberEmerald.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "+${act.odCreditsGranted} Periods OD Credited",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = CyberEmerald,
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

@Composable
private fun AttendanceAlertsTab(
    onTriggerAlert: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            color = Color(0xFF7F1D1D).copy(alpha = 0.2f),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Statutory 75% Cutoff Email Alert System",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "In compliance with JNTUA and KSRMCE Autonomous regulations, students falling below 75% aggregate attendance or below 75% in any individual course receive automated high-priority email notices to Parents, Faculty Mentors, and Principal's Office.",
                    color = Color(0xFFFCA5A5),
                    fontSize = 11.5.sp,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onTriggerAlert,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Dispatch Automated < 75% Alert Now", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = ObsidianElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = "CURRENT AUDIT METRICS:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextMuted)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Student:", fontSize = 12.sp, color = TextSecondary)
                    Text(text = "SATTU PENCHALA PRASAD (239Y1A0501)", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Aggregate Attendance:", fontSize = 12.sp, color = TextSecondary)
                    Text(text = "80.15% (Safe margin: 5.15%)", fontSize = 12.sp, color = CyberEmerald, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Borderline Subject:", fontSize = 12.sp, color = TextSecondary)
                    Text(text = "Cyber Security (75.0% - Warning)", fontSize = 12.sp, color = NeonAmber, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Parent Email:", fontSize = 12.sp, color = TextSecondary)
                    Text(text = "parent.penchalaprasad@gmail.com", fontSize = 12.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Faculty Mentor:", fontSize = 12.sp, color = TextSecondary)
                    Text(text = "Dr. P. V. Kishore (pvk.cse@ksrmce.ac.in)", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.White)
        Text(text = label, fontSize = 10.sp, color = Color(0xFFD1FAE5))
    }
}
