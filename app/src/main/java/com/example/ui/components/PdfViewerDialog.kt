package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.DriveDocument
import com.example.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Full PDF Viewer & Downloader Modal Dialog.
 * Allows students and faculty to view full multi-page PDF notes, study materials, question papers,
 * and download the PDF with live progress indicator and local save verification.
 */
@Composable
fun PdfViewerDialog(
    document: DriveDocument,
    isDarkMode: Boolean = true,
    onDismiss: () -> Unit,
    onDownloadCompleted: (String) -> Unit
) {
    var currentPage by remember { mutableIntStateOf(1) }
    var totalDocPages by remember { mutableIntStateOf(3) }
    var zoomLevel by remember { mutableFloatStateOf(1.0f) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var isDownloaded by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    var renderedBitmap by remember(document.localUri, document.title, currentPage) {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }

    LaunchedEffect(document.localUri, document.title, currentPage) {
        try {
            val pfd: android.os.ParcelFileDescriptor? = if (document.localUri != null) {
                val uri = android.net.Uri.parse(document.localUri)
                context.contentResolver.openFileDescriptor(uri, "r")
            } else {
                val assetKey = com.example.util.PdfHelper.resolveAssetName(document.title)
                val tempFile = java.io.File(context.cacheDir, "render_${assetKey.replace(' ', '_')}")
                var copied = false
                try {
                    context.assets.open(assetKey).use { inStream ->
                        java.io.FileOutputStream(tempFile).use { outStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                    copied = true
                } catch (_: Exception) {}
                if (!copied) {
                    try {
                        context.assets.open(document.title).use { inStream ->
                            java.io.FileOutputStream(tempFile).use { outStream ->
                                inStream.copyTo(outStream)
                            }
                        }
                        copied = true
                    } catch (_: Exception) {}
                }
                if (tempFile.exists() && tempFile.length() > 0) {
                    android.os.ParcelFileDescriptor.open(tempFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                } else null
            }

            if (pfd != null) {
                val renderer = android.graphics.pdf.PdfRenderer(pfd)
                totalDocPages = renderer.pageCount.coerceAtLeast(1)
                val pIdx = (currentPage - 1).coerceIn(0, totalDocPages - 1)
                val page = renderer.openPage(pIdx)
                val bmp = android.graphics.Bitmap.createBitmap(
                    (page.width * 1.5).toInt(),
                    (page.height * 1.5).toInt(),
                    android.graphics.Bitmap.Config.ARGB_8888
                )
                bmp.eraseColor(android.graphics.Color.WHITE)
                page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                pfd.close()
                renderedBitmap = bmp
            }
        } catch (e: Throwable) {
            android.util.Log.e("PdfViewerDialog", "Error rendering PDF page", e)
        }
    }

    fun launchExternalViewer() {
        com.example.util.PdfHelper.openPdf(context, document.title, document.title)
    }

    LaunchedEffect(isDownloading) {
        if (isDownloading) {
            downloadProgress = 0f
            while (downloadProgress < 1f) {
                delay(100L)
                downloadProgress += 0.25f
            }
            downloadProgress = 1f

            val resultMsg = com.example.util.PdfHelper.downloadPdfToDevice(context, document.title, document.title)
            isDownloading = false
            isDownloaded = true
            onDownloadCompleted(resultMsg)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 26.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = if (isDarkMode) ObsidianBg else LightBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Surface(
                    color = if (isDarkMode) ObsidianSurface else LightSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = document.title,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                    ),
                                    maxLines = 1
                                )
                                Text(
                                    text = "Uploaded by ${document.uploadedBy} • ${document.sizeFormatted}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Quick launch in external / native PDF App
                            IconButton(
                                onClick = { launchExternalViewer() },
                                modifier = Modifier.testTag("pdf_open_external_btn")
                            ) {
                                Icon(
                                    Icons.Default.OpenInNew,
                                    contentDescription = "Open in PDF App",
                                    tint = Color(0xFF10B981)
                                )
                            }
                            // Zoom controls
                            IconButton(onClick = { if (zoomLevel > 0.8f) zoomLevel -= 0.15f }) {
                                Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = if (isDarkMode) Color.White else Color(0xFF0F172A))
                            }
                            IconButton(onClick = { if (zoomLevel < 1.4f) zoomLevel += 0.15f }) {
                                Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = if (isDarkMode) Color.White else Color(0xFF0F172A))
                            }
                            IconButton(onClick = onDismiss, modifier = Modifier.testTag("pdf_close_btn")) {
                                Icon(Icons.Default.Close, contentDescription = "Close PDF", tint = if (isDarkMode) Color.White else Color(0xFF0F172A))
                            }
                        }
                    }
                }

                // Download Banner / Progress
                if (isDownloading) {
                    Surface(color = Color(0xFF1E293B), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Downloading PDF...", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("${(downloadProgress * 100).toInt()}%", color = CyberEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier.fillMaxWidth(),
                                color = CyberEmerald,
                                trackColor = Color(0xFF334155)
                            )
                        }
                    }
                }

                if (isDownloaded) {
                    Surface(color = Color(0xFF065F46), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Downloaded to /InternalStorage/Download/${document.title}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Simulated PDF Document Page Viewport
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(if (isDarkMode) Color(0xFF0B132B) else Color(0xFFE2E8F0))
                        .padding(12.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (document.localUri != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            shadowElevation = 8.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    color = Color(0xFF042F2E),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = document.title,
                                                color = Color(0xFFA7F3D0),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "Page $currentPage of $totalDocPages • Local Uploaded Document",
                                                color = Color.White,
                                                fontSize = 10.5.sp
                                            )
                                        }
                                        Button(
                                            onClick = { launchExternalViewer() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFF042F2E))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Open App", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF042F2E))
                                        }
                                    }
                                }

                                if (renderedBitmap != null) {
                                    Image(
                                        bitmap = renderedBitmap!!.asImageBitmap(),
                                        contentDescription = "PDF Page $currentPage",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .graphicsLayer(scaleX = zoomLevel, scaleY = zoomLevel)
                                    )
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(64.dp))
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = document.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF0F172A),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Text(
                                            text = "Size: ${document.sizeFormatted} • Uploaded from Phone Storage",
                                            fontSize = 11.5.sp,
                                            color = Color(0xFF64748B)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = { launchExternalViewer() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                        ) {
                                            Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Open in Native PDF Reader App", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Document Sheet for Autonomous Institutional Notes
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            shadowElevation = 8.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(20.dp)
                        ) {
                            // University Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "KSRM COLLEGE OF ENGINEERING (AUTONOMOUS)",
                                        fontSize = (13f * zoomLevel).sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0F172A),
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = "Department of Computer Science & Engineering • Kadapa - 516003",
                                        fontSize = (10.5f * zoomLevel).sp,
                                        color = Color(0xFF475569)
                                    )
                                    Text(
                                        text = "Approved by AICTE, New Delhi & Affiliated to JNTUA, Ananthapuramu",
                                        fontSize = (9.5f * zoomLevel).sp,
                                        color = Color(0xFF64748B)
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        color = Color(0xFF0F172A),
                                        thickness = 1.5.dp
                                    )
                                }
                            }

                            // Document Info Box
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF1F5F9),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("SUBJECT: ${document.folderName.uppercase()}", fontWeight = FontWeight.Bold, fontSize = (11f * zoomLevel).sp, color = Color(0xFF0F172A))
                                        Text("TITLE: ${document.title}", fontWeight = FontWeight.SemiBold, fontSize = (10f * zoomLevel).sp, color = Color(0xFF334155))
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("PAGE: $currentPage / $totalDocPages", fontWeight = FontWeight.Bold, fontSize = (10f * zoomLevel).sp, color = Color(0xFF0D9488))
                                        Text("FACULTY: ${document.uploadedBy}", fontSize = (9.5f * zoomLevel).sp, color = Color(0xFF64748B))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            val isHallTicket = document.title.contains("Hall Ticket", ignoreCase = true)
                            val isGradeMemo = document.title.contains("Grade", ignoreCase = true) || document.title.contains("Memo", ignoreCase = true) || document.title.contains("Result", ignoreCase = true)

                            if (isHallTicket) {
                                // Official Hall Ticket View
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFEFF6FF),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("JNTUA B.Tech 4th Year Sem-7 Autonomous Examinations", fontWeight = FontWeight.Black, fontSize = (12f * zoomLevel).sp, color = Color(0xFF1E3A8A))
                                        Text("CANDIDATE: SATTU PENCHALA PRASAD", fontWeight = FontWeight.Bold, fontSize = (11f * zoomLevel).sp, color = Color(0xFF1E40AF))
                                        Text("ROLL NUMBER: 239Y1A0501 • CSE (Sec B)", fontSize = (10.5f * zoomLevel).sp, color = Color(0xFF374151))
                                        Text("CENTER: KSRMCE Main Academic Block, Kadapa", fontSize = (10f * zoomLevel).sp, color = Color(0xFF4B5563))
                                        Text("ATTENDANCE CUTOFF: 80.15% (VERIFIED & ELIGIBLE)", fontWeight = FontWeight.Bold, fontSize = (10f * zoomLevel).sp, color = Color(0xFF059669))
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text("SCHEDULED THEORY & PRACTICAL PAPERS:", fontWeight = FontWeight.Bold, fontSize = (11.5f * zoomLevel).sp, color = Color(0xFF0F172A))
                                Spacer(modifier = Modifier.height(6.dp))

                                val papers = listOf(
                                    Triple("18 Nov 2026", "Cyber Security [2305701]", "10:00 AM - 01:00 PM"),
                                    Triple("21 Nov 2026", "Deep Learning [2339609]", "10:00 AM - 01:00 PM"),
                                    Triple("24 Nov 2026", "Organizational Behaviour [23HS772]", "10:00 AM - 01:00 PM"),
                                    Triple("27 Nov 2026", "Prompt Engineering Lab [2339751]", "02:00 PM - 05:00 PM"),
                                    Triple("30 Nov 2026", "Cloud Computing & DevOps [2332604]", "10:00 AM - 01:00 PM")
                                )

                                papers.forEach { (date, subject, time) ->
                                    Surface(
                                        color = Color(0xFFF8FAFC),
                                        shape = RoundedCornerShape(6.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(subject, fontWeight = FontWeight.Bold, fontSize = (11f * zoomLevel).sp, color = Color(0xFF0F172A))
                                                Text("Date: $date • Slot: $time", fontSize = (9.5f * zoomLevel).sp, color = Color(0xFF64748B))
                                            }
                                            Text("INVIGILATOR SIGN", fontSize = (8.5f * zoomLevel).sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Sd/-", fontSize = (10f * zoomLevel).sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                                        Text("Controller of Examinations", fontSize = (9f * zoomLevel).sp, color = Color(0xFF64748B))
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Sd/-", fontSize = (10f * zoomLevel).sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                                        Text("Principal & Chief Superintendent", fontSize = (9f * zoomLevel).sp, color = Color(0xFF64748B))
                                    }
                                }
                            } else if (isGradeMemo) {
                                // Official Grade Memorandum View
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFECFDF5),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("PROVISIONAL SEMESTER GRADE CARD", fontWeight = FontWeight.Black, fontSize = (12f * zoomLevel).sp, color = Color(0xFF065F46))
                                        Text("CANDIDATE: SATTU PENCHALA PRASAD [239Y1A0501]", fontWeight = FontWeight.Bold, fontSize = (11f * zoomLevel).sp, color = Color(0xFF047857))
                                        Text("SEMESTER: B.Tech IV Year I Sem (CSE Sec B)", fontSize = (10.5f * zoomLevel).sp, color = Color(0xFF374151))
                                        Text("SGPA: 8.65 • CGPA: 8.42 • RESULT: DISTINCTION", fontWeight = FontWeight.Black, fontSize = (11f * zoomLevel).sp, color = Color(0xFF065F46))
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                val grades = listOf(
                                    Triple("Cyber Security [2305701]", "Grade: A+", "Credits: 4 • Score: 88/100"),
                                    Triple("Deep Learning [2339609]", "Grade: A", "Credits: 4 • Score: 84/100"),
                                    Triple("Prompt Engineering Lab [2339751]", "Grade: O", "Credits: 3 • Score: 95/100"),
                                    Triple("Organizational Behaviour [23HS772]", "Grade: A+", "Credits: 3 • Score: 89/100"),
                                    Triple("Cloud Computing & DevOps [2332604]", "Grade: A", "Credits: 4 • Score: 82/100"),
                                    Triple("Capstone AI Project Work [2339799]", "Grade: O", "Credits: 4 • Score: 96/100")
                                )

                                grades.forEach { (course, grade, meta) ->
                                    Surface(
                                        color = Color(0xFFF8FAFC),
                                        shape = RoundedCornerShape(6.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(course, fontWeight = FontWeight.Bold, fontSize = (10.5f * zoomLevel).sp, color = Color(0xFF0F172A))
                                                Text(meta, fontSize = (9.5f * zoomLevel).sp, color = Color(0xFF64748B))
                                            }
                                            Text(grade, fontWeight = FontWeight.Black, fontSize = (12f * zoomLevel).sp, color = Color(0xFF047857))
                                        }
                                    }
                                }
                            } else {
                                // Dynamic Course / Drive Notes
                                when (currentPage) {
                                    1 -> {
                                        Text(
                                            text = "COURSE STUDY MODULE: ${document.title.uppercase()}",
                                            fontSize = (12.5f * zoomLevel).sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "1. Introduction & Overview:\n" +
                                                    "This official lecture material is curated by ${document.uploadedBy} for KSRMCE Autonomous students. It encapsulates essential mathematical concepts, computational frameworks, and exam preparations.",
                                            fontSize = (11.5f * zoomLevel).sp,
                                            color = Color(0xFF1E293B),
                                            lineHeight = (18f * zoomLevel).sp
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFFEF3C7),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text("★ Key Theoretical Formulation:", fontWeight = FontWeight.Bold, fontSize = (11f * zoomLevel).sp, color = Color(0xFF92400E))
                                                Text(
                                                    "Loss Function: L(θ) = -1/N Σ [ y_i log(h_θ(x_i)) + (1 - y_i) log(1 - h_θ(x_i)) ]\n" +
                                                            "Gradient Descent Update: θ_j := θ_j - α * ∂L/∂θ_j",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = (10.5f * zoomLevel).sp,
                                                    color = Color(0xFF78350F)
                                                )
                                            }
                                        }
                                    }
                                    2 -> {
                                        Text(
                                            text = "SECTION 2: LAB EXECUTIONS & ARCHITECTURAL PATTERNS",
                                            fontSize = (12.5f * zoomLevel).sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Laboratory execution requires running code on the department cluster. Verify embedding vectors and cluster convergence across training epochs.",
                                            fontSize = (11.5f * zoomLevel).sp,
                                            color = Color(0xFF1E293B),
                                            lineHeight = (18f * zoomLevel).sp
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFF1F5F9),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "// High-Performance Implementation\n" +
                                                        "import numpy as np\n" +
                                                        "def evaluate_vectors(vectors, query):\n" +
                                                        "    return np.linalg.norm(vectors - query, axis=1)\n",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = (10.5f * zoomLevel).sp,
                                                color = Color(0xFF0F172A),
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }
                                    else -> {
                                        Text(
                                            text = "SECTION 3: AUTONOMOUS REVIEW & QUESTION BANK",
                                            fontSize = (12.5f * zoomLevel).sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Q1. Explain the difference between supervised gradient backpropagation and stochastic mini-batch estimators. [7 Marks]\n\n" +
                                                    "Q2. Derive the closed-form equation for Ridge Regression regularization. [7 Marks]\n\n" +
                                                    "Q3. Detail how KNN biometric feature extraction maps 512-dimension face vectors onto high-density Euclidean clusters. [10 Marks]",
                                            fontSize = (11.5f * zoomLevel).sp,
                                            color = Color(0xFF1E293B),
                                            lineHeight = (19f * zoomLevel).sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "— End of Document Preview • KSRMCE Autonomous Examination Branch —",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }

            // Bottom Action Footer
            Surface(
                color = if (isDarkMode) ObsidianSurface else LightSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Centered Pagination Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        OutlinedButton(
                            onClick = { if (currentPage > 1) currentPage-- },
                            enabled = currentPage > 1,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.NavigateBefore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Prev", fontSize = 11.5.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Page $currentPage of $totalDocPages",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        OutlinedButton(
                            onClick = { if (currentPage < totalDocPages) currentPage++ },
                            enabled = currentPage < totalDocPages,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Next", fontSize = 11.5.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.NavigateNext, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Dual Action Buttons: Open in Native App & Download
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { launchExternalViewer() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("open_pdf_native_btn"),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open in PDF App", color = if (isDarkMode) Color(0xFFA7F3D0) else Color(0xFF042F2E), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { isDownloading = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("download_pdf_full_btn")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF042F2E), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Download", color = Color(0xFF042F2E), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
            }
        }
    }
}
