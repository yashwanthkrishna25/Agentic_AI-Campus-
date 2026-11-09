package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserProfile
import com.example.ui.theme.*
import kotlinx.coroutines.delay

enum class QrDialogMode {
    SCAN_CHECKIN,
    GENERATE_TOKEN
}

@Composable
fun QrAttendanceDialog(
    userProfile: UserProfile,
    isDarkMode: Boolean,
    onDismiss: () -> Unit,
    onAttendanceMarked: (String) -> Unit
) {
    var mode by remember { mutableStateOf(QrDialogMode.SCAN_CHECKIN) }
    var selectedSubject by remember { mutableStateOf("Deep Learning [2339609]") }
    var isScanned by remember { mutableStateOf(false) }
    var isDecoding by remember { mutableStateOf(false) }

    // Dynamic QR Token refresh countdown (30s)
    var countdownSeconds by remember { mutableStateOf(28) }
    var tokenNonce by remember { mutableStateOf(94821) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            if (countdownSeconds > 1) {
                countdownSeconds -= 1
            } else {
                countdownSeconds = 30
                tokenNonce = (10000..99999).random()
            }
        }
    }

    // Display-only QR payload. It is deliberately not an attendance URL or signed session token.
    val dynamicQrPayload = remember(selectedSubject, tokenNonce) {
        "AGENTRA_PREVIEW_ONLY:${android.net.Uri.encode(selectedSubject)}:$tokenNonce"
    }

    // Laser scan animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser_transition")
    val laserYRatio by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_scan_y"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = CyberEmerald,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "QR Attendance Suite",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Mode Switcher Tabs
                Surface(
                    color = if (isDarkMode) ObsidianElevated else LightSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        Button(
                            onClick = { mode = QrDialogMode.SCAN_CHECKIN },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (mode == QrDialogMode.SCAN_CHECKIN) CyberEmerald else Color.Transparent,
                                contentColor = if (mode == QrDialogMode.SCAN_CHECKIN) Color(0xFF042F2E) else TextSecondary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Self Check-In", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { mode = QrDialogMode.GENERATE_TOKEN },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (mode == QrDialogMode.GENERATE_TOKEN) ElectricIndigo else Color.Transparent,
                                contentColor = if (mode == QrDialogMode.GENERATE_TOKEN) Color.White else TextSecondary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate QR", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (mode) {
                    QrDialogMode.SCAN_CHECKIN -> {
                        // Scanner Viewport with animated laser beam
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black)
                                .border(2.dp, CyberEmerald.copy(alpha = 0.8f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            // Target Finder graphics & corner crosshairs
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                val stroke = 4.dp.toPx()
                                val corner = 24.dp.toPx()

                                // Top-Left
                                drawLine(CyberEmerald, Offset(16f, 16f), Offset(16f + corner, 16f), stroke)
                                drawLine(CyberEmerald, Offset(16f, 16f), Offset(16f, 16f + corner), stroke)

                                // Top-Right
                                drawLine(CyberEmerald, Offset(w - 16f, 16f), Offset(w - 16f - corner, 16f), stroke)
                                drawLine(CyberEmerald, Offset(w - 16f, 16f), Offset(w - 16f, 16f + corner), stroke)

                                // Bottom-Left
                                drawLine(CyberEmerald, Offset(16f, h - 16f), Offset(16f + corner, h - 16f), stroke)
                                drawLine(CyberEmerald, Offset(16f, h - 16f), Offset(16f, h - 16f - corner), stroke)

                                // Bottom-Right
                                drawLine(CyberEmerald, Offset(w - 16f, h - 16f), Offset(w - 16f - corner, h - 16f), stroke)
                                drawLine(CyberEmerald, Offset(w - 16f, h - 16f), Offset(w - 16f, h - 16f - corner), stroke)

                                // Animated glowing Laser Scanline
                                val laserY = h * laserYRatio
                                drawLine(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color.Transparent, CyberEmerald, Color(0xFF00F5D4), CyberEmerald, Color.Transparent)
                                    ),
                                    start = Offset(20f, laserY),
                                    end = Offset(w - 20f, laserY),
                                    strokeWidth = 3.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }

                            // Center QR watermark / target hint
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = CyberEmerald.copy(alpha = 0.4f),
                                    modifier = Modifier.size(80.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isDecoding) "DECODING QR MATRIX..." else "ALIGN QR MATRIX INSIDE FRAME",
                                    color = if (isDecoding) CyberEmerald else Color.White.copy(alpha = 0.7f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (!isScanned) {
                            Text(
                                text = "Camera-based QR decoding and server-side attendance validation are not configured. Use this screen as a preview only; no attendance can be recorded here.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.5.sp,
                                    textAlign = TextAlign.Center
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    isDecoding = false
                                    isScanned = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("scan_classroom_qr_button")
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color(0xFF042F2E))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Check QR Validation Availability", color = Color(0xFF042F2E), fontWeight = FontWeight.Bold)
                            }

                        } else {
                            // FULL DECODED QR INFORMATION CARD (As requested by user!)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDarkMode) ObsidianElevated else LightSurface,
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, CyberEmerald),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Verified Header badge
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Surface(
                                            color = Color(0xFF042F2E),
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberEmerald)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Verified, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("PREVIEW ONLY • NOT VERIFIED", color = CyberEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Text(
                                            text = "Session details unavailable",
                                            fontSize = 11.sp,
                                            color = TextSecondary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    HorizontalDivider(color = if (isDarkMode) ObsidianBorder else LightElevated)

                                    // Decoded Session Breakdown
                                    QrDetailItem("Course Title", selectedSubject)
                                    QrDetailItem("Classroom / Hall", "Not verified")
                                    QrDetailItem("Faculty In-Charge", "Not verified")
                                    QrDetailItem("Signed student identity", "Not verified")
                                    QrDetailItem("Geo-Fence Lock", "Not configured")
                                    QrDetailItem("Local preview nonce", "#$tokenNonce • Not signed")
                                    QrDetailItem("Server validation", "Not configured — attendance was not recorded")

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "Signed QR sessions, expiry checks, duplicate prevention, and blockchain audit are not configured. No attendance was recorded.",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = { isScanned = false },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Scan Another QR Code")
                            }
                        }
                    }

                    QrDialogMode.GENERATE_TOKEN -> {
                        // Real QR Code Generator view
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "KSRM College of Engineering (Autonomous)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberEmerald
                            )
                            Text(
                                text = "QR Preview Generator (Not Valid for Attendance)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // REAL QR CODE VIEW (Mathematical 2D Matrix!)
                            RealQRCodeView(
                                dataString = dynamicQrPayload,
                                size = 210.dp,
                                backgroundColor = Color.White,
                                qrColor = Color.Black
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // 30s Dynamic countdown
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = { countdownSeconds / 30f },
                                    modifier = Modifier.size(18.dp),
                                    color = CyberEmerald,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Dynamic Token rotates in ${countdownSeconds}s (Anti-Proxy)",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Payload display
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDarkMode) ObsidianElevated else LightSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "PREVIEW PAYLOAD — NOT SERVER-SIGNED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextMuted
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = dynamicQrPayload,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = CyberEmerald,
                                        lineHeight = 14.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    mode = QrDialogMode.SCAN_CHECKIN
                                    isScanned = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF042F2E))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Inspect Preview Only", color = Color(0xFF042F2E), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = CyberEmerald, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = if (isDarkMode) ObsidianSurface else LightBg
    )

}

@Composable
private fun QrDetailItem(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color.White,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}
