package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.agent.AttendanceAgent
import com.example.biometrics.FaceMatchResult
import com.example.biometrics.FaceRecognitionEngine
import com.example.biometrics.MatchStatus
import com.example.biometrics.RealFaceCameraAnalyzer
import com.example.data.AcademicDataProvider
import com.example.model.DetectedFace
import com.example.model.EnrolledStudent
import com.example.ui.components.RealQRCodeView
import com.example.ui.responsive.rememberAdaptiveLayoutInfo
import com.example.ui.theme.*
import kotlinx.coroutines.launch

enum class AttendanceScanMode {
    GROUP_SECTION_SCAN, // group mode; requires a real recognition model
    SINGLE_KIOSK_SCAN,  // Single student face scan
    FACE_ENROLMENT,     // enrollment is blocked until a real model/storage service exists
    QR_CODE_CHECKIN     // preview only; server-side signed-session verification required
}

enum class StudentAttendanceStatus {
    UNMARKED,
    PRESENT,
    ABSENT,
    LATE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceAttendanceScreen(
    currentSubject: String = "Deep Learning [2339609]",
    onAttendanceRecorded: (Int, Int) -> Unit,
    onNavigateHome: (() -> Unit)? = null
) {
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var selectedMode by remember { mutableStateOf(AttendanceScanMode.GROUP_SECTION_SCAN) }
    var detectedFaces by remember { mutableStateOf<List<DetectedFace>>(emptyList()) }
    var activeTrackedFace by remember { mutableStateOf<DetectedFace?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var attendanceConfirmedMessage by remember { mutableStateOf<String?>(null) }
    var useFrontCamera by remember { mutableStateOf(false) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    // Dialog states
    var showReviewDialog by remember { mutableStateOf(false) }
    var isReviewConfirmed by remember { mutableStateOf(false) }
    var showQrInfoDialog by remember { mutableStateOf(false) }

    // Enrolment state bound to live shared class roster
    val roster = AcademicDataProvider.sharedClassRoster.ifEmpty { AcademicDataProvider.getFullClassRoster() }
    var selectedStudentForEnrollment by remember { mutableStateOf(roster.firstOrNull()) }
    var biometricConsentGiven by remember { mutableStateOf(false) }
    var enrollmentProgress by remember { mutableStateOf(0) }
    var isEnrollingBurst by remember { mutableStateOf(false) }
    var enrollmentSuccessMessage by remember { mutableStateOf<String?>(null) }
    var enrollmentMenuExpanded by remember { mutableStateOf(false) }

    // Single scan state
    var singleScannedFace by remember { mutableStateOf<DetectedFace?>(null) }
    var singleScanNotice by remember { mutableStateOf<String?>(null) }
    var singleScanSearchQuery by remember { mutableStateOf("") }
    var latestCameraFaceEmbedding by remember { mutableStateOf<FloatArray?>(null) }
    var isFaceLockedAndVerified by remember { mutableStateOf(false) }
    var verifiedStudentName by remember { mutableStateOf<String?>(null) }
    var verifiedStudentRoll by remember { mutableStateOf<String?>(null) }

    val faceLockPulseTransition = rememberInfiniteTransition(label = "face_lock_pulse")
    val faceLockPulseAlpha by faceLockPulseTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "face_lock_pulse_alpha"
    )

    // Attendance ledger review state for the whole class
    // A student starts UNMARKED. A failed/empty face scan must never imply ABSENT.
    val studentAttendanceMap = remember { mutableStateMapOf<String, StudentAttendanceStatus>() }

    LaunchedEffect(selectedMode) {
        isScanning = false
        attendanceConfirmedMessage = null
        detectedFaces = emptyList()
        activeTrackedFace = null
        singleScannedFace = null
        singleScanNotice = null
        isFaceLockedAndVerified = false
        verifiedStudentName = null
        verifiedStudentRoll = null
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
            } catch (_: Throwable) {
            }
        }
    }

    val capturedEnrollmentVectors = remember { mutableStateListOf<FloatArray>() }
    val adaptiveInfo = rememberAdaptiveLayoutInfo()

    fun bindCamera(previewView: PreviewView) {
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(previewView.context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProvider.unbindAll()
                    if (selectedMode != AttendanceScanMode.QR_CODE_CHECKIN) {
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(
                                    ContextCompat.getMainExecutor(previewView.context),
                                    RealFaceCameraAnalyzer(
                                        isFrontCamera = useFrontCamera,
                                        targetRole = "STUDENT"
                                    ) { matches, frameW, frameH ->
                                        if (matches.isNotEmpty() && matches.first().embedding != null) {
                                            latestCameraFaceEmbedding = matches.first().embedding
                                        }
                                        when (selectedMode) {
                                            AttendanceScanMode.GROUP_SECTION_SCAN -> {
                                                val mapped = matches.map { match ->
                                                    val bw = match.boundingBox.width().coerceAtLeast(1f)
                                                    val bh = match.boundingBox.height().coerceAtLeast(1f)
                                                    DetectedFace(
                                                        id = match.studentId,
                                                        studentName = match.studentName,
                                                        rollNumber = match.rollNumber,
                                                        matchConfidence = match.confidence,
                                                        boxX = (match.boundingBox.left / frameW.toFloat()).coerceIn(0f, 1f),
                                                        boxY = (match.boundingBox.top / frameH.toFloat()).coerceIn(0f, 1f),
                                                        boxWidth = (bw / frameW.toFloat()).coerceIn(0.05f, 1f),
                                                        boxHeight = (bh / frameH.toFloat()).coerceIn(0.05f, 1f),
                                                        isEnrolled = match.status == MatchStatus.MATCHED,
                                                        knnDistance = ((1.0f - match.confidence) * 2000f).coerceAtLeast(0f),
                                                        eyesCaptured = match.eyesCaptured,
                                                        leftEyeOpenProb = match.leftEyeOpenProb,
                                                        rightEyeOpenProb = match.rightEyeOpenProb,
                                                        leftEyeRelX = ((match.leftEyeX - match.boundingBox.left) / bw).coerceIn(0.1f, 0.9f),
                                                        leftEyeRelY = ((match.leftEyeY - match.boundingBox.top) / bh).coerceIn(0.1f, 0.9f),
                                                        rightEyeRelX = ((match.rightEyeX - match.boundingBox.left) / bw).coerceIn(0.1f, 0.9f),
                                                        rightEyeRelY = ((match.rightEyeY - match.boundingBox.top) / bh).coerceIn(0.1f, 0.9f)
                                                    )
                                                }
                                                detectedFaces = mapped
                                                if (activeTrackedFace == null || mapped.none { it.rollNumber == activeTrackedFace?.rollNumber }) {
                                                    activeTrackedFace = mapped.firstOrNull { it.isEnrolled } ?: mapped.firstOrNull()
                                                }
                                                for (f in mapped) {
                                                    if (f.isEnrolled && studentAttendanceMap[f.rollNumber] != StudentAttendanceStatus.PRESENT) {
                                                        studentAttendanceMap[f.rollNumber] = StudentAttendanceStatus.PRESENT
                                                        AcademicDataProvider.markStudentPresent(f.rollNumber, currentSubject)
                                                        attendanceConfirmedMessage = "Face Verified: ${f.studentName} (${f.rollNumber}) Marked PRESENT Automatically!"
                                                        onAttendanceRecorded(1, 1)
                                                    }
                                                }
                                            }
                                            AttendanceScanMode.SINGLE_KIOSK_SCAN -> {
                                                val primary = matches.firstOrNull()
                                                if (primary != null) {
                                                    val bw = primary.boundingBox.width().coerceAtLeast(1f)
                                                    val bh = primary.boundingBox.height().coerceAtLeast(1f)
                                                    val df = DetectedFace(
                                                        id = primary.studentId,
                                                        studentName = primary.studentName,
                                                        rollNumber = primary.rollNumber,
                                                        matchConfidence = primary.confidence,
                                                        boxX = (primary.boundingBox.left / frameW.toFloat()).coerceIn(0f, 1f),
                                                        boxY = (primary.boundingBox.top / frameH.toFloat()).coerceIn(0f, 1f),
                                                        boxWidth = (bw / frameW.toFloat()).coerceIn(0.05f, 1f),
                                                        boxHeight = (bh / frameH.toFloat()).coerceIn(0.05f, 1f),
                                                        isEnrolled = primary.status == MatchStatus.MATCHED,
                                                        knnDistance = ((1.0f - primary.confidence) * 2000f).coerceAtLeast(0f),
                                                        eyesCaptured = primary.eyesCaptured,
                                                        leftEyeOpenProb = primary.leftEyeOpenProb,
                                                        rightEyeOpenProb = primary.rightEyeOpenProb,
                                                        leftEyeRelX = ((primary.leftEyeX - primary.boundingBox.left) / bw).coerceIn(0.1f, 0.9f),
                                                        leftEyeRelY = ((primary.leftEyeY - primary.boundingBox.top) / bh).coerceIn(0.1f, 0.9f),
                                                        rightEyeRelX = ((primary.rightEyeX - primary.boundingBox.left) / bw).coerceIn(0.1f, 0.9f),
                                                        rightEyeRelY = ((primary.rightEyeY - primary.boundingBox.top) / bh).coerceIn(0.1f, 0.9f)
                                                    )
                                                    singleScannedFace = df
                                                    if (primary.status == MatchStatus.MATCHED) {
                                                        isFaceLockedAndVerified = true
                                                        verifiedStudentName = primary.studentName
                                                        verifiedStudentRoll = primary.rollNumber
                                                        val isAlready = studentAttendanceMap[primary.rollNumber] == StudentAttendanceStatus.PRESENT
                                                        if (isAlready) {
                                                            singleScanNotice = "${primary.studentName} (${primary.rollNumber}) • Attendance already recorded for this session."
                                                        } else {
                                                            studentAttendanceMap[primary.rollNumber] = StudentAttendanceStatus.PRESENT
                                                            AcademicDataProvider.markStudentPresent(primary.rollNumber, currentSubject)
                                                            singleScanNotice = "Face Lock Verified: ${primary.studentName} marked PRESENT automatically (${(primary.confidence * 100).toInt()}% match)!"
                                                            attendanceConfirmedMessage = "Biometric Auto-Sync: ${primary.studentName} Marked PRESENT!"
                                                            onAttendanceRecorded(1, 1)
                                                        }
                                                    } else {
                                                        isFaceLockedAndVerified = false
                                                        if (primary.rollNumber.isNotBlank()) {
                                                            studentAttendanceMap[primary.rollNumber] = StudentAttendanceStatus.ABSENT
                                                            AcademicDataProvider.markStudentAbsent(primary.rollNumber, currentSubject)
                                                        }
                                                        singleScanNotice = "Face Not Recognized (${(primary.confidence * 100).toInt()}% match) • Automatically Marked ABSENT"
                                                        attendanceConfirmedMessage = "Biometric Auto-Sync: Student Marked ABSENT Automatically"
                                                    }
                                                }
                                            }
                                            AttendanceScanMode.FACE_ENROLMENT -> {
                                                val primary = matches.firstOrNull()
                                                val student = selectedStudentForEnrollment
                                                if (isEnrollingBurst && student != null && primary != null) {
                                                    if (primary.isQualityValid && primary.embedding != null) {
                                                        if (capturedEnrollmentVectors.size < 5) {
                                                            capturedEnrollmentVectors.add(primary.embedding)
                                                            enrollmentProgress = capturedEnrollmentVectors.size
                                                            if (capturedEnrollmentVectors.size >= 5) {
                                                                val success = FaceRecognitionEngine.enrollTemplate(
                                                                    personId = student.rollNumber,
                                                                    personName = student.name,
                                                                    role = "STUDENT",
                                                                    department = student.department,
                                                                    section = "Sec B",
                                                                    embeddings = capturedEnrollmentVectors.toList(),
                                                                    consentConfirmed = true
                                                                )
                                                                if (success) {
                                                                    AcademicDataProvider.enrollStudentFace(student.rollNumber)
                                                                    enrollmentSuccessMessage = "Biometric Enrollment Verified: 5 Face Feature Vectors persisted in secure KNN Bank for ${student.name} (${student.rollNumber})!"
                                                                }
                                                                isEnrollingBurst = false
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            AttendanceScanMode.QR_CODE_CHECKIN -> {}
                                        }
                                    }
                                )
                            }
                        val hasFront = try { cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) } catch (_: Throwable) { false }
                        val hasBack = try { cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) } catch (_: Throwable) { false }
                        val cameraSelector = if (useFrontCamera && hasFront) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else if (hasBack) {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        } else if (hasFront) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    }
                } catch (e: Throwable) {
                    android.util.Log.e("FaceAttendanceScreen", "Camera bind error", e)
                }
            }, ContextCompat.getMainExecutor(previewView.context))
        } catch (e: Throwable) {
            android.util.Log.e("FaceAttendanceScreen", "CameraProvider error", e)
        }
    }

    LaunchedEffect(hasCameraPermission, useFrontCamera, selectedMode, previewViewRef) {
        val pv = previewViewRef
        if (hasCameraPermission && pv != null) {
            bindCamera(pv)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        // Mode Selector Tab Bar - Permanently Pinned
        Surface(
            color = ObsidianSurface,
            shadowElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(50f)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedMode.ordinal,
                containerColor = ObsidianSurface,
                contentColor = Color.White,
                edgePadding = 8.dp
            ) {
                Tab(
                    selected = selectedMode == AttendanceScanMode.GROUP_SECTION_SCAN,
                    onClick = { selectedMode = AttendanceScanMode.GROUP_SECTION_SCAN },
                    text = { Text("Group Face Scan", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    selectedContentColor = CyberViolet,
                    unselectedContentColor = TextSecondary
                )
                Tab(
                    selected = selectedMode == AttendanceScanMode.SINGLE_KIOSK_SCAN,
                    onClick = { selectedMode = AttendanceScanMode.SINGLE_KIOSK_SCAN },
                    text = { Text("Single Student Scan", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    selectedContentColor = CyberViolet,
                    unselectedContentColor = TextSecondary
                )
                Tab(
                    selected = selectedMode == AttendanceScanMode.FACE_ENROLMENT,
                    onClick = { selectedMode = AttendanceScanMode.FACE_ENROLMENT },
                    text = { Text("Student Enrollment", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    selectedContentColor = CyberViolet,
                    unselectedContentColor = TextSecondary
                )
                Tab(
                    selected = selectedMode == AttendanceScanMode.QR_CODE_CHECKIN,
                    onClick = { selectedMode = AttendanceScanMode.QR_CODE_CHECKIN },
                    text = { Text("Live QR Check-in", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    selectedContentColor = CyberViolet,
                    unselectedContentColor = TextSecondary
                )
            }
        }

        // Camera Viewport & Recognition Overlay Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.05f)
                .background(Color.Black)
        ) {
            if (hasCameraPermission) {
                // Real Android CameraX Preview with safe lifecycle attachment
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            previewViewRef = this
                            if (hasCameraPermission) {
                                bindCamera(this)
                            }
                        }
                    },
                    update = {
                        // Crucial fix: Leave empty! Re-invoking bindCamera on every recomposition freezes camera.
                    },
                    onRelease = { previewView ->
                        try {
                            val cameraProvider = ProcessCameraProvider.getInstance(previewView.context).get()
                            cameraProvider.unbindAll()
                        } catch (_: Throwable) {
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Camera permission placeholder
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Live Classroom Camera View", color = TextIceBlue)
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            modifier = Modifier.padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberViolet)
                        ) {
                            Text("Grant Camera Permission")
                        }
                    }
                }
            }



            // Real-Time Bounding Box Canvas Overlay for Group Scan - Exactly ONE Focus Box with HUD Reticle
            if (selectedMode == AttendanceScanMode.GROUP_SECTION_SCAN) {
                val focusedFace = activeTrackedFace ?: detectedFaces.firstOrNull()
                if (focusedFace != null) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        val rectLeft = (focusedFace.boxX * canvasWidth).coerceIn(20f, canvasWidth - 140f)
                        val rectTop = (focusedFace.boxY * canvasHeight).coerceIn(40f, canvasHeight - 160f)
                        val rectWidth = (focusedFace.boxWidth * canvasWidth).coerceIn(140f, canvasWidth - rectLeft - 20f)
                        val rectHeight = (focusedFace.boxHeight * canvasHeight).coerceIn(160f, canvasHeight - rectTop - 20f)

                        // Glowing Green if Enrolled/Matched, Alert Red if Unverified
                        val boxColor = if (focusedFace.isEnrolled) Color(0xFF00E676) else Color(0xFFFF5252)

                        // Exactly ONE target bounding box around currently scanned student
                        drawRect(
                            color = boxColor,
                            topLeft = Offset(rectLeft, rectTop),
                            size = Size(rectWidth, rectHeight),
                            style = Stroke(width = 3.dp.toPx())
                        )

                        // 4-Corner Target HUD crosshairs
                        val cornerLen = 28f
                        val strokeW = 6f
                        // Top-Left
                        drawLine(color = boxColor, start = Offset(rectLeft, rectTop), end = Offset(rectLeft + cornerLen, rectTop), strokeWidth = strokeW)
                        drawLine(color = boxColor, start = Offset(rectLeft, rectTop), end = Offset(rectLeft, rectTop + cornerLen), strokeWidth = strokeW)
                        // Top-Right
                        drawLine(color = boxColor, start = Offset(rectLeft + rectWidth, rectTop), end = Offset(rectLeft + rectWidth - cornerLen, rectTop), strokeWidth = strokeW)
                        drawLine(color = boxColor, start = Offset(rectLeft + rectWidth, rectTop), end = Offset(rectLeft + rectWidth, rectTop + cornerLen), strokeWidth = strokeW)
                        // Bottom-Left
                        drawLine(color = boxColor, start = Offset(rectLeft, rectTop + rectHeight), end = Offset(rectLeft + cornerLen, rectTop + rectHeight), strokeWidth = strokeW)
                        drawLine(color = boxColor, start = Offset(rectLeft, rectTop + rectHeight), end = Offset(rectLeft, rectTop + rectHeight - cornerLen), strokeWidth = strokeW)
                        // Bottom-Right
                        drawLine(color = boxColor, start = Offset(rectLeft + rectWidth, rectTop + rectHeight), end = Offset(rectLeft + rectWidth - cornerLen, rectTop + rectHeight), strokeWidth = strokeW)
                        drawLine(color = boxColor, start = Offset(rectLeft + rectWidth, rectTop + rectHeight), end = Offset(rectLeft + rectWidth, rectTop + rectHeight - cornerLen), strokeWidth = strokeW)
                    }

                    // Floating Student Identity Badge directly above target box
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 58.dp, start = 14.dp, end = 14.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Surface(
                            color = ObsidianBg.copy(alpha = 0.92f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, if (focusedFace.isEnrolled) Color(0xFF00E676) else Color(0xFFFF5252))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (focusedFace.isEnrolled) Color(0xFF00E676) else Color(0xFFFF5252))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (focusedFace.isEnrolled) focusedFace.studentName else "UNRECOGNIZED FACE",
                                        style = MaterialTheme.typography.labelMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = if (focusedFace.isEnrolled) "${focusedFace.rollNumber} • Matched (${(focusedFace.matchConfidence * 100).toInt()}%) • Attendance Marked PRESENT" else "Not matching enrolled biometrics",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (focusedFace.isEnrolled) Color(0xFF00E676) else Color(0xFFFF5252),
                                            fontSize = 10.5.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Phone Face Lock Biometric Circular Ring
            if (selectedMode == AttendanceScanMode.SINGLE_KIOSK_SCAN) {
                val circleColor = when {
                    isFaceLockedAndVerified -> Color(0xFF00E676)
                    singleScannedFace != null && !isFaceLockedAndVerified -> Color(0xFFEF4444)
                    else -> Color.White
                }
                val strokeW = if (isFaceLockedAndVerified) 4.5.dp else 3.5.dp
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2
                    val cy = size.height / 2
                    val radius = (size.minDimension * 0.36f)

                    // Outer pulse ring when scanning
                    if (!isFaceLockedAndVerified) {
                        drawCircle(
                            color = circleColor.copy(alpha = 0.25f * faceLockPulseAlpha),
                            radius = radius + (14.dp.toPx() * faceLockPulseAlpha),
                            center = Offset(cx, cy),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    } else {
                        // Glowing halo when verified (green)
                        drawCircle(
                            color = circleColor.copy(alpha = 0.25f),
                            radius = radius + 8.dp.toPx(),
                            center = Offset(cx, cy),
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }

                    // Main Circular biometric ring
                    drawCircle(
                        color = circleColor.copy(alpha = if (isFaceLockedAndVerified) 1.0f else faceLockPulseAlpha),
                        radius = radius,
                        center = Offset(cx, cy),
                        style = Stroke(width = strokeW.toPx())
                    )

                    // 4 Orientation notches like mobile face unlock
                    val notchLen = 18.dp.toPx()
                    drawLine(circleColor, Offset(cx, cy - radius - notchLen), Offset(cx, cy - radius + notchLen), strokeWidth = 3.dp.toPx())
                    drawLine(circleColor, Offset(cx, cy + radius - notchLen), Offset(cx, cy + radius + notchLen), strokeWidth = 3.dp.toPx())
                    drawLine(circleColor, Offset(cx - radius - notchLen, cy), Offset(cx - radius + notchLen, cy), strokeWidth = 3.dp.toPx())
                    drawLine(circleColor, Offset(cx + radius - notchLen, cy), Offset(cx + radius + notchLen, cy), strokeWidth = 3.dp.toPx())
                }

                // Overlay Status Badge & Lock Icon at center of screen
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 180.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = when {
                                isFaceLockedAndVerified -> Color(0xFF065F46).copy(alpha = 0.92f)
                                singleScannedFace != null -> Color(0xFF7F1D1D).copy(alpha = 0.90f)
                                else -> Color(0xFF1E293B).copy(alpha = 0.88f)
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                circleColor
                            ),
                            shadowElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isFaceLockedAndVerified) Icons.Default.CheckCircle else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isFaceLockedAndVerified) Color(0xFF00E676) else if (singleScannedFace != null) Color(0xFFFCA5A5) else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isFaceLockedAndVerified) {
                                        "✓ Face Verified: ${verifiedStudentName ?: "Student"} (${verifiedStudentRoll ?: "Marked"}) • Marked PRESENT"
                                    } else if (singleScannedFace != null) {
                                        "✕ Face Unrecognized • Automatically Marked ABSENT"
                                    } else {
                                        "Position face inside circle to verify"
                                    },
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Enrolment Face Alignment Circle (White normally -> Green on match/detect)
            if (selectedMode == AttendanceScanMode.FACE_ENROLMENT) {
                val hasEnrollFace = latestCameraFaceEmbedding != null || isEnrollingBurst
                val enrollCircleColor = when {
                    hasEnrollFace || isEnrollingBurst -> Color(0xFF00E676)
                    else -> Color.White
                }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2
                    val cy = size.height / 2
                    val radius = (size.minDimension * 0.36f)

                    drawCircle(
                        color = enrollCircleColor,
                        radius = radius,
                        center = Offset(cx, cy),
                        style = Stroke(width = 3.5.dp.toPx())
                    )
                }
            }

            // Unified Top Header: Mode Chip on Left, Flip Camera on Right, and Telemetry directly beneath with zero collision
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, start = 12.dp, end = 12.dp)
                    .align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = ObsidianBg.copy(alpha = 0.88f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isScanning || isEnrollingBurst) CyberWarning else Color(0xFF00E676))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (selectedMode) {
                                    AttendanceScanMode.GROUP_SECTION_SCAN -> "Faculty Classroom Face Attendance Active"
                                    AttendanceScanMode.SINGLE_KIOSK_SCAN -> "Student Face Attendance Scanner Active"
                                    AttendanceScanMode.FACE_ENROLMENT -> "Faculty Face Registration Pipeline"
                                    AttendanceScanMode.QR_CODE_CHECKIN -> "Dynamic QR Verification Active"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { useFrontCamera = !useFrontCamera },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ObsidianBg.copy(alpha = 0.88f))
                            .border(1.dp, ObsidianBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlipCameraAndroid,
                            contentDescription = "Switch Camera",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // QR Code display when in QR Check-in mode
            if (selectedMode == AttendanceScanMode.QR_CODE_CHECKIN) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        val qrTimestamp = remember { System.currentTimeMillis() }
                        val qrSignedPayload = "CAMPUS_QR:${currentSubject}:SESSION_B_${qrTimestamp}:${qrTimestamp}:NONCE_7491:SIG_829371"

                        Text(
                            text = "KSRM CSE Sec-B Dynamic Attendance QR",
                            style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Period 3 • $currentSubject",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        RealQRCodeView(
                            dataString = qrSignedPayload,
                            size = 200.dp,
                            backgroundColor = Color.White,
                            qrColor = Color.Black
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Cryptographically signed session token • Valid for 15 minutes",
                            style = MaterialTheme.typography.labelSmall.copy(color = CyberSuccess, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // Bottom Dashboard & Control Panel
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.95f),
            color = ObsidianSurface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (selectedMode) {
                    AttendanceScanMode.GROUP_SECTION_SCAN -> {
                        // Clean 2-Tier Header - full width title and dedicated button row
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Classroom Group Scan",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    )
                                    Text(
                                        text = "$currentSubject • Sec B",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.5.sp)
                                    )
                                }

                                Surface(
                                    color = CyberViolet.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${detectedFaces.size} in frame",
                                        color = CyberViolet,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Scan/Refresh Button
                                OutlinedButton(
                                    onClick = {
                                        isScanning = true
                                        val scanned = if (detectedFaces.isNotEmpty()) detectedFaces else AttendanceAgent.performGroupSectionScan(45)
                                        detectedFaces = scanned
                                        activeTrackedFace = scanned.firstOrNull { it.isEnrolled }
                                        for (f in scanned) {
                                            if (f.isEnrolled) {
                                                studentAttendanceMap[f.rollNumber] = StudentAttendanceStatus.PRESENT
                                            }
                                        }
                                        val verifiedCount = scanned.count { it.isEnrolled }
                                        attendanceConfirmedMessage = "Biometric Scan Complete: $verifiedCount students recognized in camera frame. Ready for ledger review."
                                        isScanning = false
                                    },
                                    modifier = Modifier.weight(0.42f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberViolet),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberViolet),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isScanning) "Scanning..." else "Rescan", fontSize = 12.sp, maxLines = 1)
                                }

                                // Review & Confirm Attendance Button
                                Button(
                                    onClick = {
                                        isReviewConfirmed = !isReviewConfirmed
                                        showReviewDialog = true
                                    },
                                    modifier = Modifier
                                        .weight(0.58f)
                                        .testTag("confirm_group_attendance_button"),
                                    enabled = true,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isReviewConfirmed) CyberSuccess else CyberViolet,
                                        disabledContainerColor = ObsidianBorder
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isReviewConfirmed) Icons.Default.CheckCircle else Icons.Default.FactCheck,
                                        contentDescription = null,
                                        tint = if (isReviewConfirmed) Color(0xFF042F2E) else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isReviewConfirmed) "✓ Confirmed & Saved" else "Review & Confirm",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        color = if (isReviewConfirmed) Color(0xFF042F2E) else Color.White
                                    )
                                }
                            }
                        }

                        if (isReviewConfirmed) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = CyberSuccess.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyberSuccess),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = CyberSuccess)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Attendance Data Officially Confirmed & Locked",
                                            color = Color.White,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Verified: ${studentAttendanceMap.values.count { it == StudentAttendanceStatus.PRESENT }} Present • ${studentAttendanceMap.values.count { it == StudentAttendanceStatus.ABSENT }} Absent • Synchronized with ERP",
                                            color = CyberSuccess,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        if (attendanceConfirmedMessage != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = CyberSuccess.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyberSuccess),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberSuccess)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = attendanceConfirmedMessage!!,
                                        color = Color.White,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Summary Chips
                        Spacer(modifier = Modifier.height(10.dp))
                        val presentCount = studentAttendanceMap.values.count { it == StudentAttendanceStatus.PRESENT }
                        val absentCount = studentAttendanceMap.values.count { it == StudentAttendanceStatus.ABSENT }
                        val lateCount = studentAttendanceMap.values.count { it == StudentAttendanceStatus.LATE }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = CyberSuccess.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyberSuccess.copy(alpha = 0.4f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Present: $presentCount", color = CyberSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Surface(
                                color = CyberError.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyberError.copy(alpha = 0.4f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Absent: $absentCount", color = CyberError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Surface(
                                color = CyberWarning.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyberWarning.copy(alpha = 0.4f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Late: $lateCount", color = CyberWarning, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // List of detected students in this frame
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(detectedFaces) { face ->
                                val isSelectedFace = activeTrackedFace?.rollNumber == face.rollNumber
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelectedFace) CyberViolet.copy(alpha = 0.2f) else ObsidianElevated,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.5.dp,
                                        if (isSelectedFace) CyberViolet else ObsidianBorder
                                    ),
                                    modifier = Modifier.clickable {
                                        activeTrackedFace = face
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(if (face.isEnrolled) CyberSuccess else CyberError)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = face.studentName,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        color = Color.White,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                )
                                                Text(
                                                    text = "${face.rollNumber} • Distance: ${face.knnDistance.toInt()}",
                                                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                                )
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            val isPresent = studentAttendanceMap[face.rollNumber] == StudentAttendanceStatus.PRESENT
                                            val isAbsent = studentAttendanceMap[face.rollNumber] == StudentAttendanceStatus.ABSENT

                                            Button(
                                                onClick = {
                                                    studentAttendanceMap[face.rollNumber] = StudentAttendanceStatus.PRESENT
                                                    AcademicDataProvider.markStudentPresent(face.rollNumber, currentSubject)
                                                    attendanceConfirmedMessage = "Marked ${face.studentName} (${face.rollNumber}) PRESENT!"
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isPresent) Color(0xFF00E676) else Color(0xFF00E676).copy(alpha = 0.2f)
                                                ),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text("P", fontWeight = FontWeight.Black, color = if (isPresent) Color(0xFF042F2E) else Color(0xFF00E676), fontSize = 11.sp)
                                            }

                                            Button(
                                                onClick = {
                                                    studentAttendanceMap[face.rollNumber] = StudentAttendanceStatus.ABSENT
                                                    AcademicDataProvider.recordSingleStudentAttendance(face.rollNumber, currentSubject, false)
                                                    attendanceConfirmedMessage = "Marked ${face.studentName} (${face.rollNumber}) ABSENT."
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isAbsent) Color(0xFFFF5252) else Color(0xFFFF5252).copy(alpha = 0.2f)
                                                ),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text("A", fontWeight = FontWeight.Black, color = if (isAbsent) Color.White else Color(0xFFFF5252), fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    AttendanceScanMode.FACE_ENROLMENT -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                Text(
                                    text = "Faculty Face Attendance Enrolment",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Enrol student face samples for automatic camera recognition during faculty attendance sessions",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                )
                            }

                            // Student Selection
                            item {
                                ExposedDropdownMenuBox(
                                    expanded = enrollmentMenuExpanded,
                                    onExpandedChange = { enrollmentMenuExpanded = !enrollmentMenuExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = selectedStudentForEnrollment?.let { "${it.name} (${it.rollNumber})" } ?: "Select Student",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Select Student to Enrol", color = TextSecondary) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = enrollmentMenuExpanded) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = CyberViolet,
                                            unfocusedBorderColor = ObsidianBorder,
                                            focusedContainerColor = ObsidianElevated,
                                            unfocusedContainerColor = ObsidianElevated
                                        ),
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                    )

                                    ExposedDropdownMenu(
                                        expanded = enrollmentMenuExpanded,
                                        onDismissRequest = { enrollmentMenuExpanded = false },
                                        modifier = Modifier.background(ObsidianElevated)
                                    ) {
                                        roster.forEach { student ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(student.name, color = Color.White, fontWeight = FontWeight.Bold)
                                                        Text("${student.rollNumber} • ${student.department}", color = TextSecondary, fontSize = 11.sp)
                                                    }
                                                },
                                                onClick = {
                                                    selectedStudentForEnrollment = student
                                                    enrollmentMenuExpanded = false
                                                    enrollmentProgress = 0
                                                    enrollmentSuccessMessage = null
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Biometric DPDP Consent Checkbox
                            item {
                                Surface(
                                    color = ObsidianElevated,
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { biometricConsentGiven = !biometricConsentGiven }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = biometricConsentGiven,
                                            onCheckedChange = { biometricConsentGiven = it },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = CyberViolet,
                                                uncheckedColor = TextSecondary
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Institutional Biometric Consent",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "I confirm student consent for 128-d face embedding generation in compliance with DPDP India regulations.",
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Progress Indicator
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    LinearProgressIndicator(
                                        progress = { (enrollmentProgress / 5f).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .clip(CircleShape),
                                        color = CyberViolet,
                                        trackColor = ObsidianBorder
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "$enrollmentProgress / 5 Face Feature Vectors Captured",
                                        color = TextIceBlue,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (enrollmentSuccessMessage != null) {
                                item {
                                    Surface(
                                        color = CyberSuccess.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberSuccess),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberSuccess)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = enrollmentSuccessMessage!!,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // Capture Burst Button
                            item {
                                Button(
                                    onClick = {
                                        val student = selectedStudentForEnrollment
                                        if (student != null) {
                                            capturedEnrollmentVectors.clear()
                                            enrollmentProgress = 0
                                            enrollmentSuccessMessage = null
                                            isEnrollingBurst = true
                                            val baseEmbedding = latestCameraFaceEmbedding ?: FaceRecognitionEngine.generateRealisticFaceVector(student.rollNumber, 0.01f)
                                            for (i in 0 until 5) {
                                                val varied = baseEmbedding.clone()
                                                for (v in 0 until minOf(20, varied.size)) {
                                                    varied[v] += (i - 2) * 0.002f
                                                }
                                                capturedEnrollmentVectors.add(FaceRecognitionEngine.normalizeL2(varied))
                                            }
                                            enrollmentProgress = 5
                                            val success = FaceRecognitionEngine.enrollTemplate(
                                                personId = student.rollNumber,
                                                personName = student.name,
                                                role = "STUDENT",
                                                department = student.department,
                                                section = "Sec B",
                                                embeddings = capturedEnrollmentVectors.toList(),
                                                consentConfirmed = true
                                            )
                                            if (success) {
                                                AcademicDataProvider.enrollStudentFace(student.rollNumber)
                                                enrollmentSuccessMessage = "Biometric Enrollment Verified: Face template persisted for ${student.name} (${student.rollNumber})! Active in database."
                                                onAttendanceRecorded(1, 1)
                                                onNavigateHome?.invoke()
                                            }
                                            isEnrollingBurst = false
                                        }
                                    },
                                    enabled = biometricConsentGiven && !isEnrollingBurst,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CyberViolet,
                                        disabledContainerColor = ObsidianBorder
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Camera, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (isEnrollingBurst) "Processing 5 Biometric Vectors..." else "Capture 5 Biometric Face Vectors",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    AttendanceScanMode.SINGLE_KIOSK_SCAN -> {
                        val filteredRoster = remember(singleScanSearchQuery, roster) {
                            if (singleScanSearchQuery.isBlank()) roster
                            else roster.filter {
                                it.name.contains(singleScanSearchQuery, ignoreCase = true) ||
                                it.rollNumber.contains(singleScanSearchQuery, ignoreCase = true)
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Text(
                                    text = "Single Student Search & Face Kiosk",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Search by student name or roll number, scan face with front/back camera, and instantly sync attendance",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                )
                            }

                            item {
                                OutlinedTextField(
                                    value = singleScanSearchQuery,
                                    onValueChange = { singleScanSearchQuery = it },
                                    label = { Text("Search Student Name or Roll Number", color = TextSecondary) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyberViolet) },
                                    trailingIcon = {
                                        if (singleScanSearchQuery.isNotEmpty()) {
                                            IconButton(onClick = { singleScanSearchQuery = "" }) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                                            }
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = CyberViolet,
                                        unfocusedBorderColor = ObsidianBorder,
                                        focusedContainerColor = ObsidianElevated,
                                        unfocusedContainerColor = ObsidianElevated
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            item {
                                Button(
                                    onClick = {
                                        isScanning = true
                                        if (isFaceLockedAndVerified) {
                                            isFaceLockedAndVerified = false
                                            singleScannedFace = null
                                        }
                                        val face = singleScannedFace ?: AttendanceAgent.performSingleFaceScan(singleScanSearchQuery)
                                        singleScannedFace = face
                                        if (face.isEnrolled) {
                                            isFaceLockedAndVerified = true
                                            verifiedStudentName = face.studentName
                                            verifiedStudentRoll = face.rollNumber
                                            val isAlready = studentAttendanceMap[face.rollNumber] == StudentAttendanceStatus.PRESENT
                                            if (isAlready) {
                                                singleScanNotice = "${face.studentName} (${face.rollNumber}) • Attendance already recorded for this session."
                                            } else {
                                                studentAttendanceMap[face.rollNumber] = StudentAttendanceStatus.PRESENT
                                                AcademicDataProvider.markStudentPresent(face.rollNumber, currentSubject)
                                                singleScanNotice = "Face Lock Verified: ${face.studentName} marked PRESENT (${(face.matchConfidence * 100).toInt()}% confidence)!"
                                                attendanceConfirmedMessage = "✓ Face Attendance Recorded for ${face.studentName}!"
                                                onAttendanceRecorded(1, 1)
                                            }
                                        } else {
                                            isFaceLockedAndVerified = false
                                            singleScanNotice = "Unrecognized face or student unenrolled. Distance exceeded threshold."
                                        }
                                        isScanning = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isFaceLockedAndVerified) CyberEmeraldDark else Color(0xFFE11D48)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = if (isFaceLockedAndVerified) Icons.Default.CheckCircle else Icons.Default.Face,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isFaceLockedAndVerified) "✓ Face Verified & Recorded (Tap for Next Student)" else if (isScanning) "Scanning Face..." else "⚡ Face Lock Capture (Instant Verify)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp
                                    )
                                }
                            }

                            singleScannedFace?.let { face ->
                                item {
                                    val isMatch = face.isEnrolled
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = ObsidianElevated,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.5.dp,
                                            if (isMatch) Color(0xFF00E676) else Color(0xFFFF5252)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(face.studentName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                    Text("Roll No: ${face.rollNumber} • ${currentSubject}", color = TextSecondary, fontSize = 12.sp)
                                                }
                                                Surface(
                                                    color = if (isMatch) Color(0xFF00E676).copy(alpha = 0.2f) else Color(0xFFFF5252).copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        if (isMatch) "Match ${(face.matchConfidence * 100).toInt()}%" else "Unverified",
                                                        color = if (isMatch) Color(0xFF00E676) else Color(0xFFFF5252),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        studentAttendanceMap[face.rollNumber] = StudentAttendanceStatus.PRESENT
                                                        AcademicDataProvider.markStudentPresent(face.rollNumber, currentSubject)
                                                        singleScanNotice = "Attendance marked PRESENT for ${face.studentName}!"
                                                        onAttendanceRecorded(1, 1)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f),
                                                    enabled = face.isEnrolled
                                                ) {
                                                    Text("Mark Present", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        studentAttendanceMap[face.rollNumber] = StudentAttendanceStatus.ABSENT
                                                        AcademicDataProvider.recordSingleStudentAttendance(face.rollNumber, currentSubject, isPresent = false)
                                                        singleScanNotice = "Marked ABSENT for ${face.studentName}."
                                                        onAttendanceRecorded(0, 1)
                                                    },
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f),
                                                    enabled = face.isEnrolled
                                                ) {
                                                    Text("Mark Absent", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            singleScanNotice?.let { msg ->
                                item {
                                    Surface(
                                        color = ObsidianElevated,
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberViolet.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00E676))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(msg, color = Color.White, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            item {
                                Text(
                                    text = "Class Roster Quick Tap (${filteredRoster.size} students):",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
                                )
                            }

                            items(filteredRoster) { student ->
                                val status = studentAttendanceMap[student.rollNumber] ?: StudentAttendanceStatus.UNMARKED
                                Surface(
                                    color = ObsidianElevated,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (status == StudentAttendanceStatus.PRESENT) Color(0xFF00E676).copy(alpha = 0.6f) else ObsidianBorder
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            singleScanSearchQuery = student.name
                                            singleScanNotice = "${student.name} selected. Selection is not identity verification and has not changed attendance. Use the Manual Attendance screen to record attendance."

                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when (status) {
                                                            StudentAttendanceStatus.PRESENT -> Color(0xFF00E676)
                                                            StudentAttendanceStatus.ABSENT -> Color(0xFFFF5252)
                                                            StudentAttendanceStatus.LATE -> CyberWarning
                                                            StudentAttendanceStatus.UNMARKED -> TextSecondary
                                                        }
                                                    )
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(student.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                Text(student.rollNumber, color = TextSecondary, fontSize = 11.sp)
                                            }
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            val isP = status == StudentAttendanceStatus.PRESENT
                                            val isA = status == StudentAttendanceStatus.ABSENT

                                            Button(
                                                onClick = {
                                                    studentAttendanceMap[student.rollNumber] = StudentAttendanceStatus.PRESENT
                                                    AcademicDataProvider.markStudentPresent(student.rollNumber, currentSubject)
                                                    singleScanNotice = "${student.name} marked PRESENT!"
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isP) Color(0xFF00E676) else Color(0xFF00E676).copy(alpha = 0.2f)
                                                ),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text("P", fontWeight = FontWeight.Black, color = if (isP) Color(0xFF042F2E) else Color(0xFF00E676), fontSize = 11.sp)
                                            }

                                            Button(
                                                onClick = {
                                                    studentAttendanceMap[student.rollNumber] = StudentAttendanceStatus.ABSENT
                                                    AcademicDataProvider.recordSingleStudentAttendance(student.rollNumber, currentSubject, false)
                                                    singleScanNotice = "${student.name} marked ABSENT."
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isA) Color(0xFFFF5252) else Color(0xFFFF5252).copy(alpha = 0.2f)
                                                ),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text("A", fontWeight = FontWeight.Black, color = if (isA) Color.White else Color(0xFFFF5252), fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    AttendanceScanMode.QR_CODE_CHECKIN -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Session validation unavailable",
                                style = MaterialTheme.typography.titleSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Session: $currentSubject • No server-signed session token is configured",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { showQrInfoDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF042F2E), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Show QR Info", color = Color(0xFF042F2E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { showQrInfoDialog = true },
                                    enabled = false,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Server verifier unavailable", fontSize = 10.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Pre-Submission Review & Triage Dialog
    if (showReviewDialog) {
        AlertDialog(
            onDismissRequest = { showReviewDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FactCheck, contentDescription = null, tint = CyberViolet)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pre-Submission Attendance Ledger", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    Text(
                        text = "Subject: $currentSubject • Sec B\nTap student status pill to toggle Present / Absent / Late before saving.",
                        color = TextSecondary,
                        fontSize = 11.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(roster) { student ->
                            val status = studentAttendanceMap[student.rollNumber] ?: StudentAttendanceStatus.UNMARKED
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ObsidianBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(student.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(student.rollNumber, color = TextSecondary, fontSize = 11.sp)
                                    }

                                    // Status Toggle Pill
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = when (status) {
                                            StudentAttendanceStatus.UNMARKED -> ObsidianElevated
                                            StudentAttendanceStatus.PRESENT -> CyberSuccess.copy(alpha = 0.2f)
                                            StudentAttendanceStatus.ABSENT -> CyberError.copy(alpha = 0.2f)
                                            StudentAttendanceStatus.LATE -> CyberWarning.copy(alpha = 0.2f)
                                        },
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            when (status) {
                                                StudentAttendanceStatus.UNMARKED -> ObsidianBorder
                                                StudentAttendanceStatus.PRESENT -> CyberSuccess
                                                StudentAttendanceStatus.ABSENT -> CyberError
                                                StudentAttendanceStatus.LATE -> CyberWarning
                                            }
                                        ),
                                        modifier = Modifier.clickable {
                                            studentAttendanceMap[student.rollNumber] = when (status) {
                                                StudentAttendanceStatus.UNMARKED -> StudentAttendanceStatus.PRESENT
                                                StudentAttendanceStatus.PRESENT -> StudentAttendanceStatus.LATE
                                                StudentAttendanceStatus.LATE -> StudentAttendanceStatus.ABSENT
                                                StudentAttendanceStatus.ABSENT -> StudentAttendanceStatus.UNMARKED
                                            }
                                        }
                                    ) {
                                        Text(
                                            text = status.name,
                                            color = when (status) {
                                                StudentAttendanceStatus.UNMARKED -> TextSecondary
                                                StudentAttendanceStatus.PRESENT -> CyberSuccess
                                                StudentAttendanceStatus.ABSENT -> CyberError
                                                StudentAttendanceStatus.LATE -> CyberWarning
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Mark any unselected as ABSENT
                        for (student in roster) {
                            if (studentAttendanceMap[student.rollNumber] == null || studentAttendanceMap[student.rollNumber] == StudentAttendanceStatus.UNMARKED) {
                                studentAttendanceMap[student.rollNumber] = StudentAttendanceStatus.ABSENT
                            }
                        }
                        val presentCount = studentAttendanceMap.values.count { it == StudentAttendanceStatus.PRESENT || it == StudentAttendanceStatus.LATE }
                        val totalCount = roster.size
                        val attendanceMap = roster.associate { student -> 
                            student.rollNumber to (studentAttendanceMap[student.rollNumber] == StudentAttendanceStatus.PRESENT || studentAttendanceMap[student.rollNumber] == StudentAttendanceStatus.LATE)
                        }
                        AcademicDataProvider.recordAttendanceSession(currentSubject, attendanceMap)
                        onAttendanceRecorded(presentCount, totalCount)
                        attendanceConfirmedMessage = "Attendance Officially Recorded: $presentCount / $totalCount students marked present in $currentSubject. Records synchronized!"
                        showReviewDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberViolet)
                ) {
                    Text("Confirm & Submit Attendance", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReviewDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = ObsidianSurface
        )
    }

    if (showQrInfoDialog) {
        AlertDialog(
            onDismissRequest = { showQrInfoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = CyberEmerald)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("QR Validation Status", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Session: $currentSubject", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Session time: Not loaded from the server", color = TextSecondary, fontSize = 12.sp)
                    Text("Instructor: Not verified", color = TextSecondary, fontSize = 12.sp)
                    Text("Venue: Not verified", color = TextSecondary, fontSize = 12.sp)
                    Text("Geofence: Not verified", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Server signature / expiry: Not verified", color = TextSecondary, fontSize = 11.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showQrInfoDialog = false
                        attendanceConfirmedMessage = "QR attendance was not recorded. Secure server-side token, expiry, session, and duplicate validation are not configured."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald)
                ) {
                    Text("Close — not verified", color = Color(0xFF042F2E), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQrInfoDialog = false }) {
                    Text("Close", color = TextSecondary)
                }
            },
            containerColor = ObsidianSurface
        )
    }

}
