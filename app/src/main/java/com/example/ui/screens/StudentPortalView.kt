package com.example.ui.screens

import android.content.Context
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
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
import com.example.agent.AcademicRiskAgent
import com.example.agent.AdvisorAgent
import com.example.data.AcademicDataProvider
import com.example.data.SessionManager
import com.example.model.*
import com.example.ui.components.MonthlyTrendLineChart
import com.example.ui.components.OverallAttendanceCard
import com.example.ui.components.SubjectAttendanceBarChart
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ui.components.PdfViewerDialog
import com.example.ui.components.SubjectAttendanceCard
import com.example.ui.responsive.LocalWindowAdaptiveInfo
import com.example.ui.theme.*
import com.example.util.PdfHelper

enum class StudentSectionTab(val label: String) {
    MY_ATTENDANCE("Attendance"),
    COURSES_ASSIGNMENTS("Courses & Tasks"),
    MY_MARKS("Marks & Results"),
    STUDY_BUDDY_AI("AI Study Buddy"),
    TASKS_EXAMS("Daily To-Do & Timetable"),
    STUDENT_SETTINGS("Settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentPortalView(
    onScanClassQr: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
    val adaptiveInfo = LocalWindowAdaptiveInfo.current
    val horizPadding = if (adaptiveInfo.isSmallScreen) 10.dp else 16.dp

    var currentProfile by remember { mutableStateOf(SessionManager.getCurrentProfile()) }
    var selectedSection by remember { mutableStateOf(StudentSectionTab.MY_ATTENDANCE) }
    var selectedCourseForDetail by remember { mutableStateOf<SubjectCourse?>(null) }
    var actionBannerMessage by remember { mutableStateOf<String?>(null) }
    var viewingPdfDoc by remember { mutableStateOf<DriveDocument?>(null) }
    var uploadedStudyDocName by remember { mutableStateOf("Deep Learning Unit 1 & Backpropagation Notes.pdf") }

    // Dialog states
    var submittingAssignment by remember { mutableStateOf<CampusAssignment?>(null) }
    var submitStudentName by remember { mutableStateOf(currentProfile.name) }
    var submitRollNo by remember { mutableStateOf(currentProfile.rollNumber) }
    var submitSectionTag by remember { mutableStateOf(currentProfile.section) }
    var submitPdfFileName by remember { mutableStateOf("${currentProfile.rollNumber}_Solution.pdf") }

    var showUploadDriveDialog by remember { mutableStateOf(false) }
    var uploadDocTitle by remember { mutableStateOf("") }
    var uploadDocType by remember { mutableStateOf("PDF") }
    var driveCategoryFilter by remember { mutableStateOf("All") }
    var driveSearchQuery by remember { mutableStateOf("") }

    var showHallTicketDialog by remember { mutableStateOf(false) }
    var showCertificateDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskDueDate by remember { mutableStateOf("Tomorrow, 10:00 AM") }
    var newTaskCategory by remember { mutableStateOf("Exam Prep") }
    var taskFilter by remember { mutableStateOf("ALL") }

    // Profile Settings States
    var editStudentName by remember { mutableStateOf(currentProfile.name) }
    var editRollNumber by remember { mutableStateOf(currentProfile.rollNumber) }
    var editDepartment by remember { mutableStateOf(currentProfile.department) }
    var editSection by remember { mutableStateOf(currentProfile.section) }
    var editEmail by remember { mutableStateOf(currentProfile.email) }
    var editPhone by remember { mutableStateOf("+91 98480 22338") }

    var showDocAiDialog by remember { mutableStateOf(false) }
    var docAiSummaryResult by remember { mutableStateOf<String?>(null) }

    // Phone File Pickers for PDFs
    val assignmentPdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            val nameIndex = cursor?.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            val pickedName = if (cursor != null && cursor.moveToFirst() && nameIndex != null && nameIndex >= 0) {
                cursor.getString(nameIndex)
            } else {
                "${currentProfile.rollNumber}_Solution.pdf"
            }
            cursor?.close()
            submitPdfFileName = pickedName
            actionBannerMessage = "Attached PDF from phone: $pickedName"
        }
    }

    val studyPdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            val nameIndex = cursor?.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            val pickedName = if (cursor != null && cursor.moveToFirst() && nameIndex != null && nameIndex >= 0) {
                cursor.getString(nameIndex)
            } else {
                "StudyNotes_${System.currentTimeMillis()}.pdf"
            }
            cursor?.close()
            uploadedStudyDocName = pickedName
            showDocAiDialog = true
        }
    }

    val campusDrivePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            val nameIndex = cursor?.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            val pickedName = if (cursor != null && cursor.moveToFirst() && nameIndex != null && nameIndex >= 0) {
                cursor.getString(nameIndex)
            } else {
                "CampusDoc_${System.currentTimeMillis()}.pdf"
            }
            cursor?.close()
            AcademicDataProvider.uploadPrivateDriveDoc(
                ownerId = currentProfile.id,
                title = pickedName,
                fileType = if (pickedName.endsWith(".pdf", ignoreCase = true)) "PDF" else "DOC"
            )
            actionBannerMessage = "Uploaded '$pickedName' from phone successfully!"
        }
    }

    val courses = AcademicDataProvider.sharedCourses
    val assignments = AcademicDataProvider.sharedAssignments
    val submissions = AcademicDataProvider.sharedSubmissions
    val resources = AcademicDataProvider.sharedCourseResources
    val examMarks = AcademicDataProvider.sharedExamMarks
    val studentTasks = AcademicDataProvider.sharedStudentTasks
    val privateDriveDocs = AcademicDataProvider.getPrivateDriveDocsForUser(currentProfile.id)
    val settings = remember { AcademicDataProvider.studentSettings }

    var notify75 by remember { mutableStateOf(settings.notifyBelow75) }
    var selfCheckIn by remember { mutableStateOf(settings.biometricSelfCheckIn) }
    var studyReminders by remember { mutableStateOf(settings.dailyStudyReminders) }
    var offlineSync by remember { mutableStateOf(settings.autoSyncOfflineNotes) }

    val totalAttended = courses.sumOf { it.attendedSessions }
    val totalHeld = courses.sumOf { it.totalSessions }
    val overallPercentage = if (totalHeld > 0) (totalAttended.toDouble() / totalHeld.toDouble()) * 100.0 else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkMode) ObsidianBg else LightBg)
    ) {
        // User Info Header Card
        Surface(
            color = if (isDarkMode) ObsidianElevated else LightSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizPadding, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(CyberEmeraldDark, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentProfile.name.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = currentProfile.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${currentProfile.rollNumber} • ${currentProfile.department} (${currentProfile.section})",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CyberEmerald.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberEmerald)
                ) {
                    Text(
                        text = "CGPA ${"%.2f".format(currentProfile.cgpa)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = CyberEmerald,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Student Sub-tabs Scrollable Row
        Surface(
            color = if (isDarkMode) ObsidianSurface else LightSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
            modifier = Modifier.fillMaxWidth()
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedSection.ordinal,
                containerColor = if (isDarkMode) ObsidianSurface else LightSurface,
                contentColor = CyberEmerald,
                edgePadding = 8.dp
            ) {
                StudentSectionTab.values().forEach { tab ->
                    Tab(
                        selected = selectedSection == tab,
                        onClick = { selectedSection = tab },
                        text = { Text(tab.label, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    StudentSectionTab.MY_ATTENDANCE -> Icons.Default.EventAvailable
                                    StudentSectionTab.COURSES_ASSIGNMENTS -> Icons.Default.MenuBook
                                    StudentSectionTab.MY_MARKS -> Icons.Default.FactCheck
                                    StudentSectionTab.STUDY_BUDDY_AI -> Icons.AutoMirrored.Filled.Chat
                                    StudentSectionTab.TASKS_EXAMS -> Icons.Default.Checklist
                                    StudentSectionTab.STUDENT_SETTINGS -> Icons.Default.Tune
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }

        // Status Banner
        if (actionBannerMessage != null) {
            Surface(
                color = Color(0xFF065F46),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = actionBannerMessage!!,
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { actionBannerMessage = null },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // Content Body
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            when (selectedSection) {
                StudentSectionTab.MY_ATTENDANCE -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 1040.dp)
                            .padding(horizontal = horizPadding, vertical = 12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            OverallAttendanceCard(
                                percentage = overallPercentage,
                                attendedSessions = totalAttended,
                                totalSessions = totalHeld,
                                isDarkMode = isDarkMode,
                                onViewCatchupPlan = { onNavigateToChat("Help me create an attendance recovery plan for ${currentProfile.name}") }
                            )
                        }

                        item {
                            MonthlyTrendLineChart(
                                trends = AcademicDataProvider.getInstitutionAttendanceTrends(),
                                isDarkMode = isDarkMode
                            )
                        }

                        item {
                            SubjectAttendanceBarChart(
                                subjects = courses.map { Triple(it.name, it.attendancePercentage.toFloat(), it.code) },
                                isDarkMode = isDarkMode
                            )
                        }

                        item {
                            Text(
                                text = "Enrolled Subject Attendance",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        items(courses) { course ->
                            SubjectAttendanceCard(
                                course = course,
                                isDarkMode = isDarkMode,
                                onTakeAttendance = onScanClassQr,
                                onOpenChatPlan = { c -> onNavigateToChat("How do I improve attendance in ${c.name}?") }
                            )
                        }
                    }
                }

                StudentSectionTab.COURSES_ASSIGNMENTS -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 1040.dp)
                            .padding(horizontal = horizPadding, vertical = 12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
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
                                    Text(
                                        text = "Active Course Assignments (${currentProfile.department})",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "Download question papers, complete assignment solutions, and upload written PDFs before deadlines.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                    )
                                }
                            }
                        }

                        items(assignments) { assignment ->
                            val mySubmission = submissions.firstOrNull {
                                it.assignmentId == assignment.id && it.rollNumber == currentProfile.rollNumber
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDarkMode) ObsidianSurface else LightSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    val isSubmissionsClosed = assignment.isClosed || assignment.isExpired
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = assignment.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.5.sp,
                                                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                            )
                                            Text(
                                                text = "${assignment.subjectName} (${assignment.subjectCode}) • ${assignment.facultyName}",
                                                fontSize = 11.5.sp,
                                                color = TextSecondary
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSubmissionsClosed) Color(0xFFDC2626) else Color(0xFF059669)
                                        ) {
                                            Text(
                                                text = if (isSubmissionsClosed) "Submissions Closed" else "Submissions Open",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = assignment.description,
                                        fontSize = 12.sp,
                                        color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155)
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Schedule, contentDescription = null, tint = LuminousAmber, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Deadline: ${assignment.deadlineFormatted}",
                                                fontSize = 11.sp,
                                                color = LuminousAmber,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        Text(
                                            text = "Max Marks: ${assignment.maxMarks}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CyberEmerald
                                        )
                                    }

                                    // Submission Status Card
                                    if (mySubmission != null) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (mySubmission.isGraded) CyberEmerald.copy(alpha = 0.12f) else ElectricIndigo.copy(alpha = 0.12f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (mySubmission.isGraded) CyberEmerald else ElectricIndigo),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = if (mySubmission.isGraded) "Graded: ${mySubmission.marksAwarded} / ${assignment.maxMarks}" else "Submitted: ${mySubmission.submittedPdfName}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = if (mySubmission.isGraded) CyberEmerald else Color.White
                                                    )
                                                    Text(
                                                        text = if (mySubmission.isGraded) "Feedback: ${mySubmission.feedback}" else "Uploaded on ${mySubmission.submissionDateFormatted} • Pending evaluation",
                                                        fontSize = 10.5.sp,
                                                        color = TextSecondary
                                                    )
                                                }
                                                Icon(
                                                    imageVector = if (mySubmission.isGraded) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                                                    contentDescription = null,
                                                    tint = if (mySubmission.isGraded) CyberEmerald else LuminousAmber,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                viewingPdfDoc = DriveDocument(
                                                    id = "asgn_${assignment.id}",
                                                    title = assignment.questionPdfName,
                                                    fileType = "PDF",
                                                    assetFileName = "VII SEM GS Assignment-IV -RR.pdf",
                                                    sizeFormatted = "320 KB",
                                                    uploadedDate = assignment.deadlineFormatted,
                                                    ownerId = "system"
                                                )
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp), tint = CyberEmerald)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Question PDF", fontSize = 11.sp)
                                        }

                                        IconButton(
                                            onClick = {
                                                PdfHelper.downloadPdfToDevice(
                                                    context = context,
                                                    assetFileName = "VII SEM GS Assignment-IV -RR.pdf",
                                                    destFileName = assignment.questionPdfName
                                                ) { _, msg ->
                                                    actionBannerMessage = msg
                                                }
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = "Download PDF", tint = CyberEmerald, modifier = Modifier.size(20.dp))
                                        }

                                        val isSubmitted = mySubmission != null
                                        Button(
                                            onClick = {
                                                if (isSubmitted) {
                                                    actionBannerMessage = "Assignment already submitted and locked. One-time submission rule is strictly enforced."
                                                } else {
                                                    submittingAssignment = assignment
                                                }
                                            },
                                            enabled = !isSubmissionsClosed && !isSubmitted,
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSubmitted) Color(0xFF047857) else CyberEmeraldDark,
                                                disabledContainerColor = if (isSubmitted) Color(0xFF064E3B) else Color(0xFF334155)
                                            ),
                                            modifier = Modifier.weight(1.3f)
                                        ) {
                                            Icon(
                                                imageVector = if (isSubmitted) Icons.Default.Lock else Icons.Default.UploadFile,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = if (isSubmitted) Color(0xFF6EE7B7) else Color.White
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isSubmissionsClosed) "Submissions Closed" else if (isSubmitted) "🔒 Submitted & Locked" else "Upload Answer PDF",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSubmitted) Color(0xFF6EE7B7) else Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Department Course Materials & Resources
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Department Course Materials & Syllabus",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        items(resources) { res ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDarkMode) ObsidianSurface else LightSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (res.type) {
                                            "Syllabus" -> Icons.Default.MenuBook
                                            "Lab Manual" -> Icons.Default.Science
                                            "Question Bank" -> Icons.Default.Quiz
                                            else -> Icons.Default.Description
                                        },
                                        contentDescription = null,
                                        tint = CyberEmerald,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(res.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isDarkMode) Color.White else Color(0xFF0F172A))
                                        Text("${res.subjectName} • Uploaded by ${res.facultyName} • ${res.fileSizeFormatted}", fontSize = 11.sp, color = TextSecondary)
                                    }
                                    IconButton(
                                        onClick = {
                                            viewingPdfDoc = DriveDocument(
                                                id = "res_${res.id}",
                                                title = "${res.title}.pdf",
                                                fileType = "PDF",
                                                assetFileName = "BECG_CASE_STUDY.pdf",
                                                sizeFormatted = res.fileSizeFormatted,
                                                uploadedDate = "Latest",
                                                ownerId = "system"
                                            )
                                        }
                                    ) {
                                        Icon(Icons.Default.Visibility, contentDescription = "View PDF", tint = ElectricIndigo, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            PdfHelper.downloadPdfToDevice(
                                                context = context,
                                                assetFileName = "BECG_CASE_STUDY.pdf",
                                                destFileName = "${res.title.replace(" ", "_")}.pdf"
                                            ) { _, msg ->
                                                actionBannerMessage = msg
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = "Download to Phone", tint = CyberEmerald, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                StudentSectionTab.MY_MARKS -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 1040.dp)
                            .padding(horizontal = horizPadding, vertical = 12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Continuous Assessment & Semester Results",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                            Text(
                                                text = "Autonomous Mid 1 (30M) + Mid 2 (30M) + Assignments (10M) + Sem End (70M)",
                                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { showHallTicketDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Hall Ticket", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = { showCertificateDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = CyberEmeraldDark),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Certificates", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        items(examMarks) { mark ->
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
                                                text = mark.subjectName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.5.sp,
                                                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                            )
                                            Text(
                                                text = "${mark.subjectCode} • ${mark.totalCredits} Credits",
                                                fontSize = 11.5.sp,
                                                color = TextSecondary
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = CyberEmerald.copy(alpha = 0.2f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberEmerald)
                                        ) {
                                            Text(
                                                text = "Grade: ${mark.gradeLetter}",
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
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isDarkMode) ObsidianElevated else Color(0xFFF1F5F9),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(6.dp)) {
                                                Text("Mid-1 (30)", fontSize = 10.sp, color = TextSecondary)
                                                Text("${mark.mid1Marks}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isDarkMode) Color.White else Color(0xFF0F172A))
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isDarkMode) ObsidianElevated else Color(0xFFF1F5F9),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(6.dp)) {
                                                Text("Mid-2 (30)", fontSize = 10.sp, color = TextSecondary)
                                                Text("${mark.mid2Marks}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isDarkMode) Color.White else Color(0xFF0F172A))
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isDarkMode) ObsidianElevated else Color(0xFFF1F5F9),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(6.dp)) {
                                                Text("Asgn (10)", fontSize = 10.sp, color = CyberEmerald)
                                                Text("${mark.assignmentMarks}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CyberEmerald)
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isDarkMode) ObsidianElevated else Color(0xFFF1F5F9),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(6.dp)) {
                                                Text("Sem (70)", fontSize = 10.sp, color = ElectricIndigo)
                                                Text("${mark.semResultMarks}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ElectricIndigo)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                StudentSectionTab.STUDY_BUDDY_AI -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 1040.dp)
                            .padding(horizontal = horizPadding, vertical = 12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Psychology, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "ScholarBot • AI Campus Assistant & Coach",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Trained to guide students in coursework, explain difficult concepts, analyze uploaded notes, and prepare for placement rounds.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                    )
                                }
                            }
                        }

                        // Upload Subject Notes for Document QA
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = ElectricIndigo.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = ElectricIndigo, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Document QA • Upload Notes / Materials for AI Analysis",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Upload any lecture PDF or textbook chapter. The AI parses theorems, diagrams, and formulas so you can ask instant doubts.",
                                        fontSize = 11.5.sp,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { studyPdfPicker.launch("application/pdf") },
                                            colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Upload PDF from Phone", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                        }

                                        OutlinedButton(
                                            onClick = { showDocAiDialog = true },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(14.dp), tint = CyberEmerald)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("AI Analysis", fontSize = 11.5.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isDarkMode) ObsidianElevated else LightSurface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = ElectricIndigo, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = uploadedStudyDocName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.5.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                                )
                                                Text(
                                                    text = "Ready for AI Doubts & Inquiries",
                                                    fontSize = 10.sp,
                                                    color = CyberEmerald
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    viewingPdfDoc = DriveDocument(
                                                        id = "study_doc",
                                                        title = uploadedStudyDocName,
                                                        fileType = "PDF",
                                                        assetFileName = "BECG_CASE_STUDY.pdf",
                                                        sizeFormatted = "1.2 MB",
                                                        uploadedDate = "Today",
                                                        ownerId = currentProfile.id
                                                    )
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Visibility, contentDescription = "View", tint = ElectricIndigo, modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = {
                                                    PdfHelper.downloadPdfToDevice(
                                                        context = context,
                                                        assetFileName = "BECG_CASE_STUDY.pdf",
                                                        destFileName = uploadedStudyDocName
                                                    ) { _, msg ->
                                                        actionBannerMessage = msg
                                                    }
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Download, contentDescription = "Download", tint = CyberEmerald, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Training Modules Cards
                        item {
                            Text(
                                text = "Career & Exam Preparation Modules",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        item {
                            val modules = listOf(
                                Triple("Technical Interview Prep", "Practice Data Structures, Algorithms, SQL queries, and System Design with live feedback.", "Launch Technical Mock Interview"),
                                Triple("Aptitude & Reasoning Round", "Time & Work, Permutations, Syllogisms, and Logical Puzzles for campus drives.", "Start 10-Question Aptitude Quiz"),
                                Triple("Communication & Soft Skills", "Improve corporate English, verbal fluency, and group discussion argumentation.", "Practice HR & Behavioral Questions")
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                modules.forEach { (title, desc, action) ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isDarkMode) ObsidianSurface else LightSurface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = if (isDarkMode) Color.White else Color(0xFF0F172A))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(desc, fontSize = 11.5.sp, color = TextSecondary)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier
                                                    .clickable { onNavigateToChat(action) }
                                                    .padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(action, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyberEmerald)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                StudentSectionTab.TASKS_EXAMS -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 1040.dp)
                            .padding(horizontal = horizPadding, vertical = 12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Daily Study Tasks & Exam To-Do List",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        IconButton(onClick = { showAddTaskDialog = true }) {
                                            Icon(Icons.Default.AddCircle, contentDescription = "Add Task", tint = CyberEmerald)
                                        }
                                    }
                                    Text(
                                        text = "Stay organized before examinations. Tap checkbox to mark study tasks as completed.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                    )
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("ALL", "PENDING", "COMPLETED").forEach { filter ->
                                    val count = when (filter) {
                                        "PENDING" -> studentTasks.count { !it.isCompleted }
                                        "COMPLETED" -> studentTasks.count { it.isCompleted }
                                        else -> studentTasks.size
                                    }
                                    FilterChip(
                                        selected = taskFilter == filter,
                                        onClick = { taskFilter = filter },
                                        label = { Text("$filter ($count)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = CyberEmeraldDark,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }

                        item {
                            val completedCount = studentTasks.count { it.isCompleted }
                            val totalCount = studentTasks.size
                            val progressRatio = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
                            Surface(
                                shape = RoundedCornerShape(10.dp),
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
                                        Text("Overall Study Tasks Completed", fontSize = 11.5.sp, color = TextSecondary)
                                        Text(
                                            "$completedCount of $totalCount Tasks Completed (${(progressRatio * 100).toInt()}%)",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (progressRatio >= 0.8f) Color(0xFF00E676) else LuminousAmber
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = progressRatio,
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                        color = if (progressRatio >= 0.8f) Color(0xFF00E676) else CyberEmerald,
                                        trackColor = if (isDarkMode) ObsidianBorder else LightElevated
                                    )
                                }
                            }
                        }

                        val filteredTasks = studentTasks.filter { task ->
                            when (taskFilter) {
                                "PENDING" -> !task.isCompleted
                                "COMPLETED" -> task.isCompleted
                                else -> true
                            }
                        }

                        items(filteredTasks) { task ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDarkMode) ObsidianSurface else LightSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (task.isCompleted) Color(0xFF059669) else if (isDarkMode) ObsidianBorder else LightElevated
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = task.isCompleted,
                                        onCheckedChange = { AcademicDataProvider.toggleStudentTask(task.id) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = Color(0xFF00E676),
                                            uncheckedColor = TextSecondary
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = task.title,
                                            fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (task.isCompleted) Color(0xFF94A3B8) else if (isDarkMode) Color.White else Color(0xFF0F172A),
                                            style = if (task.isCompleted) androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else androidx.compose.ui.text.TextStyle()
                                        )
                                        Text("Due: ${task.dueDate} • ${task.category}", fontSize = 10.5.sp, color = TextSecondary)
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (task.isCompleted) Color(0xFF065F46) else Color(0xFF78350F)
                                    ) {
                                        Text(
                                            text = if (task.isCompleted) "✓ COMPLETED" else "⏳ IN PROGRESS",
                                            color = if (task.isCompleted) Color(0xFF00E676) else LuminousAmber,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            AcademicDataProvider.deleteStudentTask(task.id)
                                            actionBannerMessage = "Task removed: '${task.title}'"
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Task", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Weekly Class Timetable Schedule",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            viewingPdfDoc = DriveDocument(
                                                id = "tt_doc",
                                                title = "Autonomous Weekly Timetable VII-SEM.pdf",
                                                fileType = "PDF",
                                                assetFileName = "VIISEM 26-27.pdf",
                                                sizeFormatted = "410 KB",
                                                uploadedDate = "Official",
                                                ownerId = "academic_cell"
                                            )
                                        },
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(15.dp), tint = ElectricIndigo)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("View PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = {
                                            PdfHelper.downloadPdfToDevice(
                                                context = context,
                                                assetFileName = "VIISEM 26-27.pdf",
                                                destFileName = "Official_Weekly_Timetable_VIISEM.pdf"
                                            ) { _, msg ->
                                                actionBannerMessage = msg
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberEmeraldDark),
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Download PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        items(AcademicDataProvider.getWeeklyTimetable()) { slot ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDarkMode) ObsidianSurface else LightSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${slot.day} • Period ${slot.period}", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = CyberEmerald)
                                        Text("${slot.subjectName} (${slot.subjectCode})", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = if (isDarkMode) Color.White else Color(0xFF0F172A))
                                        Text("${slot.facultyName} • ${slot.roomNumber}", fontSize = 10.5.sp, color = TextSecondary)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = ElectricIndigo.copy(alpha = 0.15f)
                                    ) {
                                        Text(slot.timeSlot, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = ElectricIndigo, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                StudentSectionTab.STUDENT_SETTINGS -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 1040.dp)
                            .padding(horizontal = horizPadding, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isDarkMode) ObsidianElevated else LightSurface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Student Profile & Preferences",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "Configure biometric alerts, attendance limits, and theme preferences",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                    )
                                }
                            }
                        }

                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDarkMode) ObsidianSurface else LightSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    SettingSwitchRow(
                                        title = "Alert when Attendance falls <75%",
                                        subtitle = "Sends high-priority notification to review 1-month recovery plan",
                                        checked = notify75,
                                        onCheckedChange = {
                                            notify75 = it
                                            settings.notifyBelow75 = it
                                            AcademicDataProvider.saveDataToDisk()
                                        },
                                        isDarkMode = isDarkMode
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = if (isDarkMode) ObsidianBorder else LightElevated)

                                    SettingSwitchRow(
                                        title = "Biometric Face Self Check-In",
                                        subtitle = "Enables camera scan verification during lecture check-ins",
                                        checked = selfCheckIn,
                                        onCheckedChange = {
                                            selfCheckIn = it
                                            settings.biometricSelfCheckIn = it
                                            AcademicDataProvider.saveDataToDisk()
                                        },
                                        isDarkMode = isDarkMode
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = if (isDarkMode) ObsidianBorder else LightElevated)

                                    SettingSwitchRow(
                                        title = "Daily Morning Class Timetable",
                                        subtitle = "Notifies period schedule at 8:30 AM every instructional day",
                                        checked = studyReminders,
                                        onCheckedChange = {
                                            studyReminders = it
                                            settings.dailyStudyReminders = it
                                            AcademicDataProvider.saveDataToDisk()
                                        },
                                        isDarkMode = isDarkMode
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = if (isDarkMode) ObsidianBorder else LightElevated)

                                    SettingSwitchRow(
                                        title = "Dark / Night Theme Mode",
                                        subtitle = "Switch between Obsidian Cyber and Clean Light interface",
                                        checked = isDarkMode,
                                        onCheckedChange = { onToggleTheme() },
                                        isDarkMode = isDarkMode
                                    )
                                }
                            }
                        }

                        // Editable Profile Card
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDarkMode) ObsidianSurface else LightSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "Edit Student Profile",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                    )
                                    OutlinedTextField(
                                        value = editStudentName,
                                        onValueChange = { editStudentName = it },
                                        label = { Text("Full Name") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = editRollNumber,
                                        onValueChange = { editRollNumber = it },
                                        label = { Text("Roll Number / Student ID") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = editDepartment,
                                            onValueChange = { editDepartment = it },
                                            label = { Text("Department") },
                                            modifier = Modifier.weight(1.2f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = editSection,
                                            onValueChange = { editSection = it },
                                            label = { Text("Section") },
                                            modifier = Modifier.weight(0.8f),
                                            singleLine = true
                                        )
                                    }
                                    OutlinedTextField(
                                        value = editEmail,
                                        onValueChange = { editEmail = it },
                                        label = { Text("Email Address") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    Button(
                                        onClick = {
                                            val updated = currentProfile.copy(
                                                name = editStudentName.trim(),
                                                rollNumber = editRollNumber.trim(),
                                                department = editDepartment.trim(),
                                                section = editSection.trim(),
                                                email = editEmail.trim()
                                            )
                                            SessionManager.saveProfile(updated)
                                            currentProfile = updated
                                            settings.notifyBelow75 = notify75
                                            settings.biometricSelfCheckIn = selfCheckIn
                                            settings.dailyStudyReminders = studyReminders
                                            AcademicDataProvider.saveDataToDisk()
                                            actionBannerMessage = "Profile and Settings updated and persisted successfully!"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberEmeraldDark),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Save Settings Changes", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
            }
        }
    }
    }

    // Assignment Submission Dialog
    submittingAssignment?.let { asgn ->
        AlertDialog(
            onDismissRequest = { submittingAssignment = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, tint = CyberEmerald)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit Assignment Solution", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(asgn.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CyberEmerald)
                    Text("Enter your student details and upload your written/scanned PDF file:", fontSize = 11.5.sp, color = TextSecondary)

                    OutlinedTextField(
                        value = submitStudentName,
                        onValueChange = { submitStudentName = it },
                        label = { Text("Student Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = submitRollNo,
                        onValueChange = { submitRollNo = it },
                        label = { Text("Roll Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = submitSectionTag,
                        onValueChange = { submitSectionTag = it },
                        label = { Text("Department & Section") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = submitPdfFileName,
                        onValueChange = { submitPdfFileName = it },
                        label = { Text("Selected PDF Document") },
                        trailingIcon = { Icon(Icons.Default.AttachFile, contentDescription = null, tint = CyberEmerald) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { assignmentPdfPicker.launch("application/pdf") },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Choose PDF", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                PdfHelper.openPdf(context, "VII SEM GS Assignment-IV -RR.pdf", submitPdfFileName)
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberEmerald),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberEmerald),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("View PDF", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val submission = AssignmentSubmission(
                            id = "sub_" + System.currentTimeMillis(),
                            assignmentId = asgn.id,
                            studentId = currentProfile.id,
                            studentName = submitStudentName,
                            rollNumber = submitRollNo,
                            department = currentProfile.department,
                            year = asgn.year,
                            section = submitSectionTag,
                            submittedPdfName = submitPdfFileName,
                            submissionTimestamp = System.currentTimeMillis(),
                            submissionDateFormatted = "Today at ${java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US).format(java.util.Date())}",
                            marksAwarded = null,
                            feedback = "Awaiting Faculty Review",
                            isGraded = false
                        )
                        AcademicDataProvider.submitAssignment(submission)
                        submittingAssignment = null
                        actionBannerMessage = "Assignment '${submitPdfFileName}' submitted for ${asgn.title}! Logged to faculty grading ledger."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF042F2E), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Confirm & Submit PDF", fontWeight = FontWeight.Bold, color = Color(0xFF042F2E), fontSize = 12.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { submittingAssignment = null },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Close", color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155), fontSize = 12.sp)
                }
            }
        )
    }

    // Document QA Dialog
    if (showDocAiDialog) {
        AlertDialog(
            onDismissRequest = {
                showDocAiDialog = false
                docAiSummaryResult = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoStories, contentDescription = null, tint = ElectricIndigo)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Study Document Analysis", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Analyzing '$uploadedStudyDocName' with ScholarBot AI Engine...",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDarkMode) ObsidianElevated else Color(0xFFF1F5F9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Summary & Exam Highlights:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = CyberEmerald
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• Chapter 1: Artificial Neural Networks & Perceptron limitations.\n" +
                                        "• Chapter 2: Multi-Layer Perceptrons & Vanishing Gradients.\n" +
                                        "• Key Theorem: Universal Approximation Theorem.\n" +
                                        "• Numerical Formula: Chain Rule gradient calculation for Weight W(l).\n" +
                                        "• High-Frequency Exam Question: ResNet skip connections vs Plain CNNs.",
                                fontSize = 11.sp,
                                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDocAiDialog = false
                        onNavigateToChat("Explain the backpropagation chain rule equations from my uploaded Deep Learning Unit 1 notes step by step with a numerical example.")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                ) {
                    Text("Ask Doubts in AI Chat", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDocAiDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Upload Private Vault Document Dialog
    if (showUploadDriveDialog) {
        AlertDialog(
            onDismissRequest = { showUploadDriveDialog = false },
            title = { Text("Upload to Private Vault", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This file will be completely private and viewable only by you.", fontSize = 11.5.sp, color = TextSecondary)
                    OutlinedTextField(
                        value = uploadDocTitle,
                        onValueChange = { uploadDocTitle = it },
                        label = { Text("Document Title (e.g. Identity_Card.pdf)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (uploadDocTitle.isNotBlank()) {
                            AcademicDataProvider.uploadPrivateDriveDoc(
                                ownerId = currentProfile.id,
                                title = uploadDocTitle,
                                fileType = "PDF"
                            )
                            showUploadDriveDialog = false
                            uploadDocTitle = ""
                            actionBannerMessage = "File uploaded to your isolated private vault!"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberEmeraldDark)
                ) {
                    Text("Upload", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUploadDriveDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Hall Ticket Dialog
    if (showHallTicketDialog) {
        AlertDialog(
            onDismissRequest = { showHallTicketDialog = false },
            title = { Text("Autonomous End-Semester Hall Ticket", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Text("Candidate: ${currentProfile.name} (${currentProfile.rollNumber})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("College: KSRMCE Autonomous • Dept: ${currentProfile.department}", fontSize = 11.5.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDarkMode) ObsidianElevated else Color(0xFFF1F5F9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Verified Status: ELIGIBLE (Attendance > 75%)", color = CyberEmerald, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                            Text("Center: Main Exam Block, Room 402", fontSize = 11.sp, color = TextSecondary)
                            Text("Timings: 10:00 AM - 01:00 PM", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            showHallTicketDialog = false
                            viewingPdfDoc = DriveDocument(
                                id = "ht_preview",
                                title = "Autonomous End-Semester Hall Ticket.pdf",
                                fileType = "PDF",
                                assetFileName = "VIISEM 26-27.pdf",
                                sizeFormatted = "450 KB",
                                uploadedDate = "Semester VII",
                                ownerId = currentProfile.id
                            )
                        }
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp), tint = ElectricIndigo)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View PDF", fontSize = 11.5.sp)
                    }

                    Button(
                        onClick = {
                            showHallTicketDialog = false
                            PdfHelper.downloadPdfToDevice(
                                context = context,
                                assetFileName = "VIISEM 26-27.pdf",
                                destFileName = "${currentProfile.rollNumber}_Autonomous_HallTicket.pdf"
                            ) { _, msg ->
                                actionBannerMessage = msg
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberEmeraldDark)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download to Phone", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showHallTicketDialog = false }) { Text("Close") }
            }
        )
    }

    // Digital Certificates Dialog
    if (showCertificateDialog) {
        AlertDialog(
            onDismissRequest = { showCertificateDialog = false },
            title = { Text("Verified Digital Certificates", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val certs = listOf(
                        "Autonomous Grade Card (Sem VI) - CGPA 8.85",
                        "NPTEL Elite Certificate in Deep Learning",
                        "Smart India Hackathon College Winner Certificate"
                    )
                    certs.forEach { cert ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDarkMode) ObsidianElevated else Color(0xFFF1F5F9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(cert, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = {
                                        showCertificateDialog = false
                                        viewingPdfDoc = DriveDocument(
                                            id = "cert_${cert.hashCode()}",
                                            title = "$cert.pdf",
                                            fileType = "PDF",
                                            assetFileName = "BECG_CASE_STUDY.pdf",
                                            sizeFormatted = "580 KB",
                                            uploadedDate = "Verified",
                                            ownerId = currentProfile.id
                                        )
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = "View", tint = ElectricIndigo, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = {
                                        PdfHelper.downloadPdfToDevice(
                                            context = context,
                                            assetFileName = "BECG_CASE_STUDY.pdf",
                                            destFileName = "${cert.take(20).replace(" ", "_")}_Certificate.pdf"
                                        ) { _, msg ->
                                            actionBannerMessage = msg
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = "Download", tint = CyberEmerald, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCertificateDialog = false
                        PdfHelper.downloadPdfToDevice(
                            context = context,
                            assetFileName = "BECG_CASE_STUDY.pdf",
                            destFileName = "${currentProfile.rollNumber}_All_Verified_Certificates.pdf"
                        ) { _, msg ->
                            actionBannerMessage = msg
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberEmeraldDark)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Download All to Phone", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCertificateDialog = false }) { Text("Close") }
            }
        )
    }

    // Add Task Dialog
    if (showAddTaskDialog) {
        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = { Text("Add Study Task / Goal", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        label = { Text("Task Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTaskDueDate,
                        onValueChange = { newTaskDueDate = it },
                        label = { Text("Due Date & Time") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTaskTitle.isNotBlank()) {
                            AcademicDataProvider.addStudentTask(newTaskTitle, newTaskDueDate)
                            showAddTaskDialog = false
                            newTaskTitle = ""
                            actionBannerMessage = "Study task added to checklist!"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberEmeraldDark)
                ) {
                    Text("Add Task", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Detailed Subject Component Breakdown Dialog
    selectedCourseForDetail?.let { course ->
        AlertDialog(
            onDismissRequest = { selectedCourseForDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FactCheck, contentDescription = null, tint = CyberEmerald)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(course.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("${course.code} • Assessment Breakdown", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Autonomous Continuous Assessment Components:",
                        fontSize = 11.5.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )

                    val componentList = listOf(
                        Pair("Quiz 1 Assessment", "${"%.1f".format(course.x1)} / 10.0"),
                        Pair("Mid-Term 1 Exam", "${"%.1f".format(course.x2)} / 10.0"),
                        Pair("Lab Practical & Viva", "${"%.1f".format(course.x3)} / 10.0"),
                        Pair("Assignment & Case Study", "${"%.1f".format(course.x4)} / 10.0"),
                        Pair("Mid-Term 2 Exam", "${"%.1f".format(course.x5)} / 10.0"),
                        Pair("Seminar & Tech Talk", "${"%.1f".format(course.x6)} / 10.0")
                    )

                    componentList.forEach { (name, mark) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDarkMode) ObsidianElevated else Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(name, fontSize = 12.sp, color = if (isDarkMode) Color.White else Color(0xFF0F172A))
                                Text(mark, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyberEmerald)
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = ObsidianBorder)

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ElectricIndigo.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricIndigo),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Internal Continuous Total", fontSize = 11.5.sp, color = TextSecondary)
                                Text("${"%.1f".format(course.internalTotal)} / 60.0", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Autonomous End-Term Exam", fontSize = 11.5.sp, color = TextSecondary)
                                Text("${"%.1f".format(course.x7)} / 40.0", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ElectricIndigo)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Final Grand Assessment", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyberEmerald)
                                Text("${"%.1f".format(course.grandTotal)} / 100.0 (A+)", fontWeight = FontWeight.Black, fontSize = 13.sp, color = CyberEmerald)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val c = course
                        selectedCourseForDetail = null
                        PdfHelper.downloadPdfToDevice(
                            context = context,
                            assetFileName = "VIISEM 26-27.pdf",
                            destFileName = "${c.code}_Official_Grade_Memo.pdf"
                        ) { _, msg ->
                            actionBannerMessage = msg
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = ObsidianBg, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Download Grade Memo PDF", color = ObsidianBg, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedCourseForDetail = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Full PDF Viewer Dialog with page rendering, external app launcher and download
    viewingPdfDoc?.let { doc ->
        PdfViewerDialog(
            document = doc,
            onDismiss = { viewingPdfDoc = null },
            isDarkMode = isDarkMode,
            onDownloadCompleted = { path ->
                actionBannerMessage = "Downloaded to phone: $path"
            }
        )
    }
}
}

@Composable
fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isDarkMode: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CyberEmerald,
                checkedTrackColor = CyberEmeraldDark
            )
        )
    }
}
