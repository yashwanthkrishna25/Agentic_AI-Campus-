package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AcademicDataProvider
import android.content.Intent
import com.example.data.FirestoreRepository
import com.example.model.FeedComment
import com.example.model.FeedPost
import com.example.model.GroupChatMessage
import com.example.model.StudyDocument
import com.example.model.DriveDocument
import com.example.model.DriveFolder
import com.example.ui.components.PdfViewerDialog
import com.example.ui.theme.*
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.R
import com.example.util.PdfHelper
import com.example.util.PomodoroSoundHelper
import com.example.util.PomodoroTone
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CampusLifeTab {
    FEED,
    DRIVE_MATERIALS,
    POMODORO_TIMER
}

@Composable
fun CampusFeedScreen(
    initialTab: CampusLifeTab = CampusLifeTab.FEED,
    isStudent: Boolean = false,
    userRole: com.example.model.UserRole = com.example.model.UserRole.STUDENT,
    isDarkMode: Boolean = true,
    onNavigateToAttendance: (() -> Unit)? = null,
    onNavigateToChat: ((String) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    val feedPosts = AcademicDataProvider.sharedFeedPosts

    val driveFolders = AcademicDataProvider.sharedDriveFolders
    val driveDocs = AcademicDataProvider.sharedDriveDocs
    var selectedFolderFilter by remember { mutableStateOf<String?>("All") }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showUploadDocDialog by remember { mutableStateOf(false) }
    var previewPdfDocument by remember { mutableStateOf<DriveDocument?>(null) }
    var newFolderName by remember { mutableStateOf("") }
    var newDocTitle by remember { mutableStateOf("") }
    var newDocFolder by remember { mutableStateOf("Class Notes & Lectures") }
    var newDocType by remember { mutableStateOf("PDF") }
    var driveBannerMessage by remember { mutableStateOf<String?>(null) }

    var showCreatePostDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    // Pomodoro Timer State
    var pomodoroTimeSeconds by remember { mutableStateOf(25 * 60) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var sessionsCompleted by remember { mutableStateOf(3) }
    var stopwatchSoundActive by remember { mutableStateOf(true) }

    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning && pomodoroTimeSeconds > 0) {
            delay(1000)
            pomodoroTimeSeconds--
            if (stopwatchSoundActive && pomodoroTimeSeconds > 0) {
                PomodoroSoundHelper.playStopwatchTick(context)
            }
            if (pomodoroTimeSeconds == 0) {
                isTimerRunning = false
                sessionsCompleted++
                PomodoroSoundHelper.playTone(context, PomodoroTone.COMPLETED)
                pomodoroTimeSeconds = 25 * 60
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkMode) ObsidianBg else LightBg)
    ) {
        // Tab Bar
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = if (isDarkMode) ObsidianSurface else LightSurface,
            contentColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
        ) {
            Tab(
                selected = selectedTab == CampusLifeTab.FEED,
                onClick = { selectedTab = CampusLifeTab.FEED },
                text = { Text("Feed", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.DynamicFeed, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("campus_tab_feed")
            )
            val tab2Title = when (userRole) {
                com.example.model.UserRole.HOD -> "Syllabus Audit"
                com.example.model.UserRole.PRINCIPAL, com.example.model.UserRole.PRINCIPAL_ADMIN -> "NIRF Governance"
                com.example.model.UserRole.ADMIN -> "Facility Ops"
                com.example.model.UserRole.SUPER_ADMIN -> "Cluster Ops"
                else -> "Academic Studio"
            }
            val tab2Icon = when (userRole) {
                com.example.model.UserRole.HOD -> Icons.Default.MenuBook
                com.example.model.UserRole.PRINCIPAL, com.example.model.UserRole.PRINCIPAL_ADMIN -> Icons.Default.AccountBalance
                com.example.model.UserRole.ADMIN -> Icons.Default.Apartment
                com.example.model.UserRole.SUPER_ADMIN -> Icons.Default.Dns
                else -> Icons.Default.School
            }
            Tab(
                selected = selectedTab == CampusLifeTab.DRIVE_MATERIALS,
                onClick = { selectedTab = CampusLifeTab.DRIVE_MATERIALS },
                text = { Text(tab2Title, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                icon = { Icon(tab2Icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("campus_tab_drive")
            )
            Tab(
                selected = selectedTab == CampusLifeTab.POMODORO_TIMER,
                onClick = { selectedTab = CampusLifeTab.POMODORO_TIMER },
                text = { Text("Pomodoro", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("campus_tab_pomodoro")
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                CampusLifeTab.FEED -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (isStudent) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF1E293B),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigateToAttendance?.invoke() }
                                        .testTag("student_attendance_banner_card")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.School,
                                                    contentDescription = null,
                                                    tint = Color(0xFF10B981),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "Welcome to Campus Hub!",
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                )
                                                Text(
                                                    text = "Overall Attendance: 84.6% • Safe Status",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color(0xFF10B981),
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = { onNavigateToAttendance?.invoke() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                "My Attendance",
                                                color = Color(0xFF042F2E),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                Icons.Default.ArrowForward,
                                                contentDescription = null,
                                                tint = Color(0xFF042F2E),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        items(feedPosts) { post ->
                            FeedPostCard(
                                post = post,
                                isDarkMode = isDarkMode,
                                onUpvote = {
                                    post.isUpvoted = !post.isUpvoted
                                    if (post.isUpvoted) post.upvotes++ else post.upvotes--
                                    val idx = feedPosts.indexOfFirst { it.id == post.id }
                                    if (idx != -1) feedPosts[idx] = post.copy()
                                }
                            )
                        }
                    }

                    // Floating Action Button to Add Post (Only for faculty/staff/admins, hidden for students)
                    if (!isStudent) {
                        FloatingActionButton(
                            onClick = { showCreatePostDialog = true },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(20.dp)
                                .testTag("create_post_fab"),
                            containerColor = CrimsonPrimary,
                            contentColor = Color.White
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Create Announcement Post")
                        }
                    }
                }

                CampusLifeTab.DRIVE_MATERIALS -> {
                    when (userRole) {
                        com.example.model.UserRole.HOD -> HodSyllabusAuditProcessView(onNavigateToChat = { onNavigateToChat?.invoke(it) })
                        com.example.model.UserRole.PRINCIPAL, com.example.model.UserRole.PRINCIPAL_ADMIN -> PrincipalNirfGovernanceProcessView(onNavigateToChat = { onNavigateToChat?.invoke(it) })
                        com.example.model.UserRole.ADMIN -> AdminFacilityOpsProcessView(onNavigateToChat = { onNavigateToChat?.invoke(it) })
                        com.example.model.UserRole.SUPER_ADMIN -> SuperAdminClusterOpsProcessView(onNavigateToChat = { onNavigateToChat?.invoke(it) })
                        else -> StudentAcademicLabProcessView(
                            driveDocs = driveDocs,
                            onOpenDoc = { doc -> previewPdfDocument = doc },
                            onDownloadDoc = { doc ->
                                val res = PdfHelper.downloadPdfToDevice(
                                    context = context,
                                    fileNameOrAsset = doc.assetFileName ?: "study_material_becg.pdf",
                                    displayTitle = doc.title
                                )
                                driveBannerMessage = res
                            },
                            onNavigateToChat = onNavigateToChat
                        )
                    }
                }

                CampusLifeTab.POMODORO_TIMER -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Academic Productivity Pomodoro",
                            style = MaterialTheme.typography.titleLarge.copy(color = Color.White, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Based on research paper stress mitigation guidelines",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        val minutes = pomodoroTimeSeconds / 60
                        val seconds = pomodoroTimeSeconds % 60
                        val progressFraction = (pomodoroTimeSeconds.toFloat() / (25f * 60f)).coerceIn(0f, 1f)

                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawArc(
                                    color = Color(0xFF334155),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 12.dp.toPx())
                                )
                                drawArc(
                                    color = CrimsonPrimary,
                                    startAngle = -90f,
                                    sweepAngle = progressFraction * 360f,
                                    useCenter = false,
                                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "%02d:%02d".format(minutes, seconds),
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 44.sp
                                    )
                                )
                                Text(
                                    text = if (isTimerRunning) "FOCUS SESSION" else "PAUSED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isTimerRunning) Color(0xFF34D399) else Color(0xFF94A3B8),
                                        letterSpacing = 1.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    if (!isTimerRunning && stopwatchSoundActive) {
                                        PomodoroSoundHelper.playStopwatchTick(context)
                                    } else {
                                        PomodoroSoundHelper.playTone(context, PomodoroTone.PAUSE)
                                    }
                                    isTimerRunning = !isTimerRunning
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("pomodoro_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isTimerRunning) "Pause" else "Start Focus", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    PomodoroSoundHelper.playTone(context, PomodoroTone.RESET)
                                    isTimerRunning = false
                                    pomodoroTimeSeconds = 25 * 60
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reset")
                            }

                            // Compact Speaker Toggle Button (Clean 48.dp size, no vertical stretching)
                            IconButton(
                                onClick = {
                                    stopwatchSoundActive = !stopwatchSoundActive
                                    if (stopwatchSoundActive) {
                                        PomodoroSoundHelper.playStopwatchTick(context)
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = if (stopwatchSoundActive) CyberViolet.copy(alpha = 0.25f) else Color(0xFF1E293B),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (stopwatchSoundActive) CyberViolet else Color(0xFF475569),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .testTag("pomodoro_sound_toggle_btn")
                            ) {
                                Icon(
                                    imageVector = if (stopwatchSoundActive) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                    contentDescription = "Stopwatch Ticking Toggle",
                                    tint = if (stopwatchSoundActive) Color(0xFFA78BFA) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Sessions Completed Today: $sessionsCompleted (75 mins focused)",
                            color = Color(0xFFE2E8F0),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    if (showCreatePostDialog) {
        var postTitle by remember { mutableStateOf("") }
        var postBody by remember { mutableStateOf("") }
        var selectedImageUri by remember { mutableStateOf<String?>(null) }
        var selectedTag by remember { mutableStateOf("Campus Event") }

        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                selectedImageUri = uri.toString()
            }
        }

        AlertDialog(
            onDismissRequest = { showCreatePostDialog = false },
            title = { Text("Create Campus Post & Announcement", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = postTitle,
                        onValueChange = { postTitle = it },
                        label = { Text("Title") },
                        placeholder = { Text("e.g. AI Hackathon Registration Open") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = postBody,
                        onValueChange = { postBody = it },
                        label = { Text("Content / Details") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    // Image attachment area
                    Text("Attach Post Image / Banner:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    if (!selectedImageUri.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F172A))
                        ) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Attached Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { selectedImageUri = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.7f))
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color(0xFF38BDF8))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pick Image from Gallery", color = Color(0xFF38BDF8), fontSize = 12.sp)
                        }

                        // Instant Campus Preset Images
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "Hackathon Poster" to "https://images.unsplash.com/photo-1531482615713-2afd69097998?w=800",
                                "AI Workshop" to "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=800",
                                "Campus Fest" to "https://images.unsplash.com/photo-1523240795612-9a054b0db644?w=800"
                            ).forEach { (label, url) ->
                                SuggestionChip(
                                    onClick = { selectedImageUri = url },
                                    label = { Text(label, fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (postTitle.isNotBlank()) {
                            feedPosts.add(
                                0,
                                FeedPost(
                                    id = System.currentTimeMillis().toString(),
                                    authorName = "SATTU PENCHALA PRASAD",
                                    authorRole = if (isStudent) "Student Representative" else "Faculty Contributor",
                                    date = "Today",
                                    title = postTitle,
                                    content = postBody,
                                    upvotes = 1,
                                    commentsCount = 0,
                                    isUpvoted = true,
                                    tag = selectedTag,
                                    imageUrl = selectedImageUri
                                )
                            )
                            AcademicDataProvider.saveDataToDisk()
                            showCreatePostDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) {
                    Text("Publish Post with Image")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePostDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // === DRIVE: UPLOAD NEW FILE / DOCUMENT DIALOG ===
    if (showUploadDocDialog) {
        var docTitleInput by remember { mutableStateOf(newDocTitle) }
        var selectedFolderInput by remember {
            mutableStateOf(
                if (selectedFolderFilter != null && selectedFolderFilter != "All") selectedFolderFilter!!
                else driveFolders.firstOrNull()?.name ?: "Class Notes & Lectures"
            )
        }
        var selectedTypeInput by remember { mutableStateOf(newDocType) }
        val selectedSizeKb by remember { mutableStateOf("${(1..5).random()}.${(1..9).random()} MB") }
        val customAuthor by remember { mutableStateOf(if (isStudent) "SATTU PENCHALA PRASAD (CSE)" else "Faculty Contributor") }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var selectedFileUri by remember { mutableStateOf<String?>(null) }
        var selectedFileName by remember { mutableStateOf<String?>(null) }
        var selectedFileSizeText by remember { mutableStateOf<String?>(null) }
        var selectedFileSizeBytes by remember { mutableLongStateOf(2850000L) }

        val docPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}

                selectedFileUri = uri.toString()
                var displayName: String? = null
                var fileSize: Long = 0L
                try {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (cursor.moveToFirst()) {
                            if (nameIndex != -1) displayName = cursor.getString(nameIndex)
                            if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                        }
                    }
                } catch (_: Exception) {}

                val finalName = displayName ?: uri.lastPathSegment?.substringAfterLast('/') ?: "document.pdf"
                selectedFileName = finalName
                docTitleInput = finalName
                if (fileSize > 0) {
                    selectedFileSizeBytes = fileSize
                    val mb = fileSize.toDouble() / (1024.0 * 1024.0)
                    selectedFileSizeText = "%.2f MB".format(mb)
                }
                if (finalName.endsWith(".pdf", ignoreCase = true)) {
                    selectedTypeInput = "PDF"
                } else if (finalName.endsWith(".docx", ignoreCase = true) || finalName.endsWith(".doc", ignoreCase = true)) {
                    selectedTypeInput = "DOCX"
                } else if (finalName.endsWith(".pptx", ignoreCase = true)) {
                    selectedTypeInput = "PPTX"
                }
            }
        }

        val availableTypes = listOf("PDF", "DOCX", "PPTX", "XLSX", "TXT")
        val sampleTemplates = listOf(
            "Unit 4 Deep Learning & CNN Architecture Notes",
            "Mid-Term 2 Solved Model Question Paper",
            "Lab Experiment 6 Java Socket Programming Manual",
            "Operating Systems Process Scheduling Cheat Sheet",
            "Design & Analysis of Algorithms Previous Papers"
        )

        AlertDialog(
            onDismissRequest = { showUploadDocDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Upload Document to Drive",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
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
                    Text(
                        "Choose a file from phone storage or enter academic title to add to Drive.",
                        fontSize = 11.5.sp,
                        color = Color(0xFF94A3B8)
                    )

                    // Phone Storage File Picker Action Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF10B981)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                docPickerLauncher.launch(
                                    arrayOf(
                                        "application/pdf",
                                        "application/msword",
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                        "application/vnd.ms-powerpoint",
                                        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                        "text/plain",
                                        "*/*"
                                    )
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981).copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (selectedFileName != null) "Selected: $selectedFileName" else "Upload Phone Document / PDF",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (selectedFileSizeText != null) "File size: $selectedFileSizeText • Tap to change" else "Tap here to select PDF from your phone storage",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFFA7F3D0)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = docTitleInput,
                        onValueChange = {
                            docTitleInput = it
                            errorMessage = null
                        },
                        label = { Text("Document Title *") },
                        placeholder = { Text("e.g. Unit 3 Neural Networks.pdf") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upload_doc_title_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A)
                        )
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = Color(0xFFEF4444),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        "Target Folder",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(driveFolders) { folder ->
                            val isFolderSelected = (selectedFolderInput == folder.name)
                            FilterChip(
                                selected = isFolderSelected,
                                onClick = { selectedFolderInput = folder.name },
                                label = { Text(folder.name, fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Folder,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF10B981),
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    Text(
                        "File Type",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        availableTypes.forEach { type ->
                            val isTypeSelected = (selectedTypeInput == type)
                            FilterChip(
                                selected = isTypeSelected,
                                onClick = { selectedTypeInput = type },
                                label = { Text(type, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (type == "PDF") Color(0xFFEF4444) else Color(0xFF3B82F6),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Text(
                        "Quick Auto-Fill Suggestions",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        sampleTemplates.take(3).forEach { template ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1E293B),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { docTitleInput = template }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(template, color = Color(0xFFCBD5E1), fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalTitle = docTitleInput.trim()
                        if (finalTitle.isBlank()) {
                            errorMessage = "Please enter or pick a document title"
                            return@Button
                        }

                        val targetFolderObj = driveFolders.find { it.name == selectedFolderInput }
                            ?: driveFolders.firstOrNull()
                        val folderName = targetFolderObj?.name ?: "Class Notes & Lectures"
                        val folderId = targetFolderObj?.id ?: "fld_notes"

                        val newDoc = DriveDocument(
                            id = "doc_${System.currentTimeMillis()}",
                            folderId = folderId,
                            folderName = folderName,
                            title = if (finalTitle.endsWith(".$selectedTypeInput", ignoreCase = true)) finalTitle else "$finalTitle.${selectedTypeInput.lowercase()}",
                            fileType = selectedTypeInput,
                            sizeBytes = selectedFileSizeBytes,
                            sizeFormatted = selectedFileSizeText ?: selectedSizeKb,
                            uploadedBy = customAuthor,
                            uploadedDate = "Just now",
                            downloadUrl = "https://campus.ksrmce.ac.in/drive/files/${System.currentTimeMillis()}",
                            localUri = selectedFileUri
                        )

                        driveDocs.add(0, newDoc)
                        targetFolderObj?.let { it.fileCount++ }
                        selectedFolderFilter = folderName
                        driveBannerMessage = "Successfully uploaded '${newDoc.title}' to $folderName!"
                        showUploadDocDialog = false
                        newDocTitle = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    modifier = Modifier.testTag("confirm_upload_doc_button")
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload to Drive", fontWeight = FontWeight.Bold, color = Color(0xFF042F2E))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showUploadDocDialog = false },
                    modifier = Modifier.testTag("cancel_upload_doc_button")
                ) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B),
            textContentColor = Color.White
        )
    }

    // === DRIVE: CREATE NEW FOLDER DIALOG ===
    if (showCreateFolderDialog) {
        var folderNameInput by remember { mutableStateOf(newFolderName) }
        var selectedColorHex by remember { mutableStateOf(0xFF10B981) }
        var folderError by remember { mutableStateOf<String?>(null) }

        val colorOptions = listOf(
            0xFF10B981 to "Emerald",
            0xFF6366F1 to "Indigo",
            0xFFF59E0B to "Amber",
            0xFFEC4899 to "Pink",
            0xFF06B6D4 to "Cyan",
            0xFF8B5CF6 to "Violet"
        )

        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = null,
                        tint = Color(selectedColorHex),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Create New Drive Folder",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Enter folder name to categorize syllabus, notes, lab code, or assignments.",
                        fontSize = 11.5.sp,
                        color = Color(0xFF94A3B8)
                    )

                    OutlinedTextField(
                        value = folderNameInput,
                        onValueChange = {
                            folderNameInput = it
                            folderError = null
                        },
                        label = { Text("Folder Name *") },
                        placeholder = { Text("e.g. Mini Projects 2026") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_folder_name_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(selectedColorHex),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A)
                        )
                    )

                    if (folderError != null) {
                        Text(
                            text = folderError!!,
                            color = Color(0xFFEF4444),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text("Folder Color Tag", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        colorOptions.forEach { (cHex, name) ->
                            val isColorSelected = (selectedColorHex == cHex)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(cHex))
                                    .border(
                                        width = if (isColorSelected) 2.dp else 0.dp,
                                        color = if (isColorSelected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = cHex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isColorSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = name,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedName = folderNameInput.trim()
                        if (trimmedName.isBlank()) {
                            folderError = "Folder name cannot be empty"
                            return@Button
                        }
                        if (driveFolders.any { it.name.equals(trimmedName, ignoreCase = true) }) {
                            folderError = "Folder '$trimmedName' already exists"
                            return@Button
                        }

                        val newFld = DriveFolder(
                            id = "fld_${System.currentTimeMillis()}",
                            name = trimmedName,
                            createdDate = "Today",
                            colorHex = selectedColorHex,
                            fileCount = 0
                        )
                        driveFolders.add(newFld)
                        selectedFolderFilter = trimmedName
                        driveBannerMessage = "Created folder '$trimmedName' in Drive!"
                        showCreateFolderDialog = false
                        newFolderName = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(selectedColorHex)),
                    modifier = Modifier.testTag("confirm_create_folder_button")
                ) {
                    Text("Create Folder", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreateFolderDialog = false },
                    modifier = Modifier.testTag("cancel_create_folder_button")
                ) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B),
            textContentColor = Color.White
        )
    }

    // PDF Preview Modal
    previewPdfDocument?.let { doc ->
        PdfViewerDialog(
            document = doc,
            isDarkMode = isDarkMode,
            onDismiss = { previewPdfDocument = null },
            onDownloadCompleted = { driveBannerMessage = it }
        )
    }

    // === CREATE CAMPUS ANNOUNCEMENT / IMAGE POST DIALOG ===
    if (showCreatePostDialog) {
        var postTitleInput by remember { mutableStateOf("") }
        var postContentInput by remember { mutableStateOf("") }
        var selectedTagInput by remember { mutableStateOf("Announcement") }
        var attachedImageUri by remember { mutableStateOf<String?>(null) }
        var postError by remember { mutableStateOf<String?>(null) }

        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                attachedImageUri = uri.toString()
            }
        }

        val availableTags = listOf(
            "Announcement",
            "Academic",
            "Event",
            "Hackathon",
            "Placement",
            "Sports",
            "Workshop"
        )

        val presetBanners = listOf(
            "Hackathon 2026" to "https://images.unsplash.com/photo-1504384308090-c894fdcc538d?w=800&q=80",
            "Auditorium Fest" to "https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=800&q=80",
            "Robotics & AI" to "https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=800&q=80",
            "Sports Ground" to "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=800&q=80",
            "Library Study" to "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?w=800&q=80"
        )

        AlertDialog(
            onDismissRequest = { showCreatePostDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Create Campus Post with Image",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
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
                    Text(
                        "Share an official announcement, club event, technical hackathon, or circular with the campus community.",
                        fontSize = 11.5.sp,
                        color = Color(0xFF94A3B8)
                    )

                    OutlinedTextField(
                        value = postTitleInput,
                        onValueChange = {
                            postTitleInput = it
                            postError = null
                        },
                        label = { Text("Post Title / Headline *") },
                        placeholder = { Text("e.g. AGENTRA AI Hackathon Registration Open") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_post_title_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CrimsonPrimary,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A)
                        )
                    )

                    Text("Category Tag", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(availableTags) { tag ->
                            val isSelected = (selectedTagInput == tag)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTagInput = tag },
                                label = { Text(tag, fontSize = 10.5.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CrimsonPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF0F172A),
                                    labelColor = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = postContentInput,
                        onValueChange = {
                            postContentInput = it
                            postError = null
                        },
                        label = { Text("Announcement Details *") },
                        placeholder = { Text("Describe event date, venue, eligibility, or guidelines...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("create_post_content_input"),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CrimsonPrimary,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A)
                        )
                    )

                    // Image Attachment Section
                    Text("Attach Event Image / Banner", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))

                    // Buttons: Pick from Gallery OR pick preset
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_gallery_image_btn")
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick from Device", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        if (attachedImageUri != null) {
                            OutlinedButton(
                                onClick = { attachedImageUri = null },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Remove", fontSize = 10.5.sp)
                            }
                        }
                    }

                    // Preset Quick Campus Banners
                    Text("Or Choose a Campus Event Banner:", fontSize = 10.5.sp, color = Color(0xFF94A3B8))
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presetBanners) { (title, url) ->
                            val isChosen = (attachedImageUri == url)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isChosen) CrimsonPrimary.copy(alpha = 0.3f) else Color(0xFF0F172A),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isChosen) CrimsonPrimary else Color(0xFF334155)
                                ),
                                modifier = Modifier.clickable { attachedImageUri = url }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Image,
                                        contentDescription = null,
                                        tint = if (isChosen) CrimsonPrimary else Color(0xFF38BDF8),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        title,
                                        fontSize = 10.sp,
                                        color = if (isChosen) Color.White else Color(0xFFCBD5E1),
                                        fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // Live Attached Image Preview
                    if (!attachedImageUri.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0F172A),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "Image Attached Ready to Post",
                                            color = Color(0xFF10B981),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text("Preview", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black)
                                ) {
                                    AsyncImage(
                                        model = attachedImageUri,
                                        contentDescription = "Post Image Preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                        error = painterResource(id = R.drawable.agentra_logo)
                                    )
                                }
                            }
                        }
                    }

                    if (postError != null) {
                        Text(
                            text = postError!!,
                            color = Color(0xFFEF4444),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedTitle = postTitleInput.trim()
                        val trimmedContent = postContentInput.trim()
                        if (trimmedTitle.isBlank()) {
                            postError = "Please enter a post title"
                            return@Button
                        }
                        if (trimmedContent.isBlank()) {
                            postError = "Please enter post details"
                            return@Button
                        }

                        val nowStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
                        val newPost = FeedPost(
                            id = "post_${System.currentTimeMillis()}",
                            authorName = "Sattu Penchala Prasad",
                            authorRole = if (isStudent) "Student (4th Year CSE Sec B)" else "Campus Contributor",
                            date = nowStr,
                            title = trimmedTitle,
                            content = trimmedContent,
                            upvotes = 1,
                            commentsCount = 0,
                            isUpvoted = false,
                            tag = selectedTagInput,
                            imageUrl = attachedImageUri
                        )

                        feedPosts.add(0, newPost)
                        AcademicDataProvider.saveDataToDisk()
                        driveBannerMessage = "Announcement '${newPost.title}' with image posted to Campus Feed!"
                        showCreatePostDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    modifier = Modifier.testTag("confirm_create_post_button")
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Publish Post", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreatePostDialog = false },
                    modifier = Modifier.testTag("cancel_create_post_button")
                ) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B),
            textContentColor = Color.White
        )
    }
}

@Composable
fun FeedPostCard(
    post: FeedPost,
    isDarkMode: Boolean = true,
    onUpvote: () -> Unit
) {
    val context = LocalContext.current
    var isLiked by remember(post.id, post.isUpvoted) { mutableStateOf(post.isUpvoted) }
    var likeCount by remember(post.id, post.upvotes) { mutableStateOf(post.upvotes) }
    var showCommentsDialog by remember { mutableStateOf(false) }
    var newCommentText by remember { mutableStateOf("") }
    val comments = remember { mutableStateListOf<FeedComment>().apply { addAll(post.comments) } }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDarkMode) Color(0xFF1E293B) else LightSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) Color(0xFF334155) else LightElevated),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE11D48)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "KSRMCE",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = post.date,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        )
                    }
                }

                Surface(
                    color = if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = post.tag,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF334155),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155),
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp
                )
            )

            // Attached Image Display
            if (!post.imageUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                ) {
                    AsyncImage(
                        model = post.imageUrl,
                        contentDescription = "Post Image Attachment",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = painterResource(id = R.drawable.agentra_logo)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Posted by ${post.authorName}",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isDarkMode) Color(0xFF38BDF8) else ElectricIndigo,
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0))
            Spacer(modifier = Modifier.height(8.dp))

            // Like, Comment, Share Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like Button (Changes color dynamically!)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val newLiked = !isLiked
                            isLiked = newLiked
                            likeCount = if (newLiked) likeCount + 1 else (likeCount - 1).coerceAtLeast(0)
                            onUpvote()
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) Color(0xFFE11D48) else (if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)),
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$likeCount Likes",
                        color = if (isLiked) Color(0xFFE11D48) else (if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)),
                        fontSize = 12.sp,
                        fontWeight = if (isLiked) FontWeight.Bold else FontWeight.Medium
                    )
                }

                // Comment Button (Accessible to anyone to view & post comments)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showCommentsDialog = true }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Comments",
                        tint = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.commentsCount} Comments",
                        color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                        fontSize = 12.sp
                    )
                }

                // Share Button (Accessible to anyone to share via Android share intent)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TITLE, post.title)
                                putExtra(Intent.EXTRA_SUBJECT, post.title)
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "📢 *${post.title}*\n\n${post.content}\n\nPosted by ${post.authorName} (${post.authorRole})\nAGENTRA AI Campus"
                                )
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Share Post")
                            context.startActivity(shareIntent)
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Share",
                        color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    // Anyone can view & post comments dialog
    if (showCommentsDialog) {
        AlertDialog(
            onDismissRequest = { showCommentsDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Comments (${comments.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                    IconButton(onClick = { showCommentsDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    // Post reference
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F172A),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = post.title,
                            color = Color(0xFF38BDF8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(8.dp),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }

                    // Comments list
                    if (comments.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No comments yet. Be the first to comment!", color = Color(0xFF64748B), fontSize = 12.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(comments) { comment ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF0F172A),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = comment.authorName,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.5.sp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "• ${comment.authorRole}",
                                                    color = Color(0xFF38BDF8),
                                                    fontSize = 10.sp
                                                )
                                            }
                                            Text(
                                                text = comment.time,
                                                color = Color(0xFF94A3B8),
                                                fontSize = 10.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = comment.text,
                                            color = Color(0xFFE2E8F0),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Comment Input Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newCommentText,
                            onValueChange = { newCommentText = it },
                            placeholder = { Text("Write a comment...", color = Color(0xFF64748B), fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (newCommentText.isNotBlank()) {
                                    val newComment = FeedComment(
                                        authorName = "Campus User",
                                        authorRole = "Verified",
                                        text = newCommentText.trim(),
                                        time = "Just now"
                                    )
                                    comments.add(newComment)
                                    post.comments.add(newComment)
                                    post.commentsCount++
                                    newCommentText = ""
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Post Comment", tint = Color(0xFF042F2E), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = Color(0xFF1E293B)
        )
    }
}
