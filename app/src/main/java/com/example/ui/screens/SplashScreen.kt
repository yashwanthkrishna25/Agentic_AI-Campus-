package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Netflix & Google-inspired Cinematic Opening Logo Animation for AGENTRA AI.
 * Features 4 orbiting neural color-beacons (Indigo, Emerald, Amber, Coral),
 * dynamic light-streak ribbon sweep tracing the iconic 'A' crest,
 * expanding sonic pulse bloom, and dramatic brand reveal.
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    // Phase control: 0 = Initial dots orbit, 1 = Ribbon sweep & A-crest formation, 2 = Brand burst & Title glow
    var animationPhase by remember { mutableStateOf(0) }

    // Infinite breathing / particle pulse
    val infiniteTransition = rememberInfiniteTransition(label = "splash_infinite")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_pass"
    )

    // Continuous subtle beacon rotation
    val beaconRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beacon_orbit"
    )

    // Cinematic zoom & scale transitions
    val logoScale = remember { Animatable(0.2f) }
    val ribbonProgress = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val flareScale = remember { Animatable(0f) }
    val statusText = remember { mutableStateOf("Initializing Multi-Agent Kernel...") }

    LaunchedEffect(Unit) {
        // Step 1: Scale up logo
        logoScale.animateTo(
            targetValue = 0.9f,
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        )
        animationPhase = 1
        statusText.value = "Calibrating Neural Vision & Biometrics..."

        // Step 2: Glow and flare
        ribbonProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(500, easing = FastOutSlowInEasing)
        )
        animationPhase = 2
        statusText.value = "Connecting Multi-Agent Campus Intelligence..."

        // Step 3: Title reveal & complete
        flareScale.animateTo(
            targetValue = 1.1f,
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        )
        flareScale.animateTo(1f, tween(200))
        logoScale.animateTo(1.0f, tween(200))
        titleAlpha.animateTo(1f, tween(300))
        statusText.value = "Welcome to AGENTRA AI CAMPUS"

        delay(400)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F1E3D),
                        Color(0xFF070B18),
                        Color(0xFF02040A)
                    )
                )
            )
            .clickable { onSplashFinished() }
            .testTag("splash_screen_container"),
        contentAlignment = Alignment.Center
    ) {
        // Fast Skip button at top end
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp, end = 20.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Surface(
                onClick = { onSplashFinished() },
                shape = RoundedCornerShape(20.dp),
                color = Color(0x33FFFFFF),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFFFFF))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SKIP >>",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // Cinematic background light flares & stars
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2.4f

            val currentFlare = flareScale.value
            if (currentFlare > 0.05f) {
                val glowRadius = (280.dp.toPx() * currentFlare).coerceAtLeast(5f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF4F46E5).copy(alpha = (0.25f * currentFlare).coerceIn(0f, 1f)),
                            Color(0xFF00F5D4).copy(alpha = (0.15f * currentFlare).coerceIn(0f, 1f)),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = glowRadius
                    ),
                    center = Offset(cx, cy),
                    radius = glowRadius
                )
            }

            val radius = 110.dp.toPx()
            val colors = listOf(
                Color(0xFF4F46E5),
                Color(0xFF00F5D4),
                Color(0xFFF59E0B),
                Color(0xFFF43F5E)
            )

            for (i in 0 until 4) {
                val angleRad = Math.toRadians((beaconRotation + (i * 90.0)).toDouble())
                val dotX = cx + (radius * cos(angleRad)).toFloat()
                val dotY = cy + (radius * sin(angleRad)).toFloat()

                drawCircle(
                    color = colors[i].copy(alpha = 0.35f),
                    center = Offset(dotX, dotY),
                    radius = 16.dp.toPx()
                )
                drawCircle(
                    color = colors[i],
                    center = Offset(dotX, dotY),
                    radius = 6.dp.toPx()
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Institutional Campus Header Badge
            Surface(
                color = Color(0xFF0F1E3D).copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, LuminousAmber.copy(alpha = 0.4f)),
                modifier = Modifier
                    .scale(logoScale.value.coerceAtLeast(0.4f))
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = "KSRMCE Campus",
                        tint = LuminousAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "KSRM COLLEGE OF ENGINEERING (AUTONOMOUS)",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            // Main Official AGENTRA AI CAMPUS Emblem with Shimmer Sweep
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(200.dp)
                    .scale(logoScale.value)
            ) {
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = Color(0xFF0F1A30),
                    border = androidx.compose.foundation.BorderStroke(
                        2.5.dp,
                        Brush.sweepGradient(
                            listOf(
                                CyberEmerald,
                                ElectricIndigo,
                                ElectricViolet,
                                LuminousAmber,
                                CyberEmerald
                            )
                        )
                    ),
                    shadowElevation = 24.dp,
                    modifier = Modifier.size(190.dp)
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.agentra_logo),
                        contentDescription = "AGENTRA AI CAMPUS Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp)
                            .clip(RoundedCornerShape(26.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                }

                // Shimmer light-sweep overlay
                Canvas(modifier = Modifier.size(190.dp)) {
                    val w = size.width
                    val h = size.height
                    val shimmerBrush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.45f),
                            Color.Transparent
                        ),
                        start = Offset(shimmerOffset, 0f),
                        end = Offset(shimmerOffset + 140f, h)
                    )
                    drawRoundRect(
                        brush = shimmerBrush,
                        topLeft = Offset(0f, 0f),
                        size = size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(26.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // AGENTRA AI CAMPUS Title with Cinematic Multi-Agent System Glow
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.scale(titleAlpha.value.coerceAtLeast(0.1f))
            ) {
                Text(
                    text = "AGENTRA AI CAMPUS",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp,
                        color = Color.White,
                        fontSize = 28.sp
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    color = CyberEmerald.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "AGENTIC AI ATTENDANCE & ACADEMIC PERFORMANCE ASSISTANT",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CyberEmerald,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "A Multi-Agent System for Face-Recognition Attendance & Academic Risk Analysis",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "KSRM College of Engineering (Autonomous) • Kadapa, AP",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFF1F5F9),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(modifier = Modifier.height(34.dp))

            // Linear Progress Bar & Status Text
            LinearProgressIndicator(
                modifier = Modifier
                    .width(180.dp)
                    .height(3.5.dp)
                    .clip(CircleShape),
                color = CyberEmerald,
                trackColor = Color(0xFF1E293B)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = statusText.value,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tap screen to continue",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextMuted,
                    fontSize = 10.sp
                )
            )
        }
    }
}
