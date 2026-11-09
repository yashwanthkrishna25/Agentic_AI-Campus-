package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Cyber Weekly Analysis Line Chart Widget with interactive weekday selectors.
 * Clicking any day dynamically updates metrics, highlighted point, and trend breakdown.
 */
@Composable
fun CyberWeeklyAnalysisWidget(
    title: String = "Weekly Analysis",
    subtitle: String = "Daily biometric & operational trend analysis",
    unitPrefix: String = "",
    unitSuffix: String = "%",
    baseValues: List<Float> = listOf(78.5f, 82.0f, 86.4f, 81.2f, 89.5f, 75.0f, 83.2f),
    dayLabels: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
    accentColor: Color = NeonCoral,
    isDarkMode: Boolean = true,
    modifier: Modifier = Modifier
) {
    var selectedDayIndex by remember { mutableStateOf(2) } // default Wed
    val currentValue = baseValues.getOrElse(selectedDayIndex) { 80f }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isDarkMode) Color(0xFF13192B) else LightSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) Color(0xFF1E293B) else LightElevated)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.5.sp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "$unitPrefix${"%.1f".format(currentValue)}$unitSuffix (${dayLabels[selectedDayIndex]})",
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Day Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dayLabels.forEachIndexed { index, day ->
                    val isSelected = index == selectedDayIndex
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) accentColor else (if (isDarkMode) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                            )
                            .clickable { selectedDayIndex = index }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day,
                            fontSize = 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else (if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Canvas Chart
            val animatedValue by animateFloatAsState(
                targetValue = currentValue,
                animationSpec = tween(500),
                label = "weekly_chart_anim"
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val w = size.width
                val h = size.height
                val padY = 16f
                val chartH = h - (padY * 2)

                val minVal = 60f
                val maxVal = 100f

                // Horizontal grid lines
                for (i in 0..3) {
                    val y = padY + (chartH * (i / 3f))
                    drawLine(
                        color = Color.White.copy(alpha = 0.08f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                }

                // Points path
                val stepX = w / (baseValues.size - 1).coerceAtLeast(1)
                val points = baseValues.mapIndexed { i, v ->
                    val normY = ((v - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
                    Offset(i * stepX, padY + chartH * (1f - normY))
                }

                val path = Path()
                val fillPath = Path()

                points.forEachIndexed { i, pt ->
                    if (i == 0) {
                        path.moveTo(pt.x, pt.y)
                        fillPath.moveTo(pt.x, h)
                        fillPath.lineTo(pt.x, pt.y)
                    } else {
                        val prev = points[i - 1]
                        val cx = (prev.x + pt.x) / 2
                        path.cubicTo(cx, prev.y, cx, pt.y, pt.x, pt.y)
                        fillPath.cubicTo(cx, prev.y, cx, pt.y, pt.x, pt.y)
                    }
                }
                fillPath.lineTo(points.last().x, h)
                fillPath.close()

                // Draw Gradient Fill
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(accentColor.copy(alpha = 0.35f), Color.Transparent)
                    )
                )

                // Draw Line
                drawPath(
                    path = path,
                    color = accentColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Highlight Selected Point
                if (selectedDayIndex in points.indices) {
                    val selPt = points[selectedDayIndex]
                    // Outer glow
                    drawCircle(
                        color = accentColor.copy(alpha = 0.4f),
                        radius = 9.dp.toPx(),
                        center = selPt
                    )
                    // Inner white dot
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = selPt
                    )
                }
            }
        }
    }
}

/**
 * Cyber Speedometer / Semi-Circular Radial Gauge Widget (0% - 100%).
 * Interactive needle with gradient arc and dynamic compliance status rating.
 */
@Composable
fun CyberSpeedometerWidget(
    title: String = "Total Performance Index",
    currentScore: Float = 84.5f,
    minVal: Float = 0f,
    maxVal: Float = 100f,
    unitSuffix: String = "%",
    isDarkMode: Boolean = true,
    onScoreChange: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var dynamicScore by remember { mutableStateOf(currentScore) }
    val animatedScore by animateFloatAsState(
        targetValue = dynamicScore,
        animationSpec = tween(700),
        label = "speedometer_anim"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isDarkMode) Color(0xFF13192B) else LightSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) Color(0xFF1E293B) else LightElevated)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold
                    )
                )
                val statusText = when {
                    dynamicScore >= 85f -> "Optimal ★"
                    dynamicScore >= 75f -> "Compliant ✓"
                    else -> "Risk Alert ⚠️"
                }
                val statusColor = when {
                    dynamicScore >= 85f -> CyberEmerald
                    dynamicScore >= 75f -> ElectricIndigo
                    else -> NeonCoral
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Gauge Canvas
            Box(
                modifier = Modifier
                    .size(width = 200.dp, height = 110.dp)
                    .clickable {
                        // Cycle through sample scenarios on click for interactive feedback
                        dynamicScore = when (dynamicScore) {
                            in 0f..74f -> 82.5f
                            in 75f..87f -> 94.2f
                            else -> 68.0f
                        }
                        onScoreChange?.invoke(dynamicScore)
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2
                    val cy = h - 10f
                    val radius = (w / 2) - 16f
                    val strokeW = 14.dp.toPx()

                    // Background Track
                    drawArc(
                        color = Color.White.copy(alpha = 0.1f),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round),
                        topLeft = Offset(cx - radius, cy - radius),
                        size = Size(radius * 2, radius * 2)
                    )

                    // Gradient Progress Arc
                    val sweep = ((animatedScore - minVal) / (maxVal - minVal)).coerceIn(0f, 1f) * 180f
                    drawArc(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFF10B981), Color(0xFF00F5D4))
                        ),
                        startAngle = 180f,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round),
                        topLeft = Offset(cx - radius, cy - radius),
                        size = Size(radius * 2, radius * 2)
                    )

                    // Needle
                    val angleRad = Math.toRadians((180f + sweep).toDouble())
                    val needleLen = radius - 8.dp.toPx()
                    val nx = cx + (needleLen * cos(angleRad)).toFloat()
                    val ny = cy + (needleLen * sin(angleRad)).toFloat()

                    drawLine(
                        color = Color.White,
                        start = Offset(cx, cy),
                        end = Offset(nx, ny),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Center Pivot Hub
                    drawCircle(color = Color(0xFF00F5D4), radius = 6.dp.toPx(), center = Offset(cx, cy))
                    drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(cx, cy))
                }

                // Digital Readout
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = "${"%.1f".format(animatedScore)}$unitSuffix",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 22.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap gauge to simulate real-time sensor & ledger fluctuations",
                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.5.sp)
            )
        }
    }
}

/**
 * Cyber Donut & Multi-Ring Breakdown Widget (e.g. 25%, 40%, 60% concentric rings).
 * Interactive clicking on tiers highlights detailed metrics.
 */
@Composable
fun CyberDonutAnalysisWidget(
    title: String = "Categorical Analysis",
    rings: List<Triple<String, Float, Color>> = listOf(
        Triple("Biometric Attendance", 85f, CyberEmerald),
        Triple("Continuous Assessments", 74f, ElectricIndigo),
        Triple("Syllabus Attainment", 92f, LuminousAmber)
    ),
    isDarkMode: Boolean = true,
    modifier: Modifier = Modifier
) {
    var selectedRingIndex by remember { mutableStateOf(0) }
    val activeRing = rings.getOrElse(selectedRingIndex) { rings.first() }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isDarkMode) Color(0xFF13192B) else LightSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) Color(0xFF1E293B) else LightElevated)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Concentric Rings Canvas
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clickable {
                            selectedRingIndex = (selectedRingIndex + 1) % rings.size
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2
                        val cy = size.height / 2

                        rings.forEachIndexed { i, ring ->
                            val r = 48.dp.toPx() - (i * 12.dp.toPx())
                            val isSel = i == selectedRingIndex
                            val strokeW = if (isSel) 7.dp.toPx() else 5.dp.toPx()

                            // Track
                            drawCircle(
                                color = Color.White.copy(alpha = 0.08f),
                                radius = r,
                                center = Offset(cx, cy),
                                style = Stroke(width = strokeW)
                            )

                            // Value Arc
                            val sweep = (ring.second / 100f).coerceIn(0f, 1f) * 360f
                            drawArc(
                                color = ring.third,
                                startAngle = -90f,
                                sweepAngle = sweep,
                                useCenter = false,
                                style = Stroke(width = strokeW, cap = StrokeCap.Round),
                                topLeft = Offset(cx - r, cy - r),
                                size = Size(r * 2, r * 2)
                            )
                        }
                    }

                    Text(
                        text = "${activeRing.second.toInt()}%",
                        fontWeight = FontWeight.Black,
                        color = activeRing.third,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Interactive Legend Rows
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rings.forEachIndexed { idx, ring ->
                        val isSelected = idx == selectedRingIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ring.third.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { selectedRingIndex = idx }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(ring.third)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = ring.first,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "${"%.1f".format(ring.second)}%",
                                color = ring.third,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Cyber Grouped Bar & Radar Matrix with Year/Semester Filter Selector Pills (2023, 2024, 2025, 2026).
 * Clicking any year pill updates all bars, values, and radar plots instantly.
 */
@Composable
fun CyberGroupedBarAndRadarWidget(
    title: String = "Annual Analytics & Multi-Axis Matrix",
    years: List<String> = listOf("2023", "2024", "2025", "2026"),
    isDarkMode: Boolean = true,
    modifier: Modifier = Modifier
) {
    var selectedYear by remember { mutableStateOf("2026") }

    // Dynamic data driven by selected year
    val barHeights = when (selectedYear) {
        "2023" -> listOf(45f, 60f, 50f, 75f, 65f, 80f, 55f, 70f, 60f, 85f, 70f, 78f)
        "2024" -> listOf(55f, 70f, 65f, 80f, 72f, 85f, 68f, 82f, 75f, 90f, 80f, 84f)
        "2025" -> listOf(65f, 78f, 74f, 88f, 80f, 92f, 76f, 89f, 84f, 95f, 88f, 91f)
        else -> listOf(75f, 88f, 82f, 94f, 89f, 96f, 85f, 94f, 90f, 98f, 93f, 97f) // 2026
    }

    val radarAxes = listOf("Syllabus", "Attendance", "Placements", "Research", "Labs")
    val radarScores = when (selectedYear) {
        "2023" -> listOf(0.65f, 0.70f, 0.60f, 0.55f, 0.72f)
        "2024" -> listOf(0.72f, 0.76f, 0.68f, 0.64f, 0.78f)
        "2025" -> listOf(0.82f, 0.84f, 0.78f, 0.74f, 0.86f)
        else -> listOf(0.92f, 0.94f, 0.88f, 0.82f, 0.95f) // 2026
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isDarkMode) Color(0xFF13192B) else LightSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) Color(0xFF1E293B) else LightElevated)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with interactive year chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    years.forEach { yr ->
                        val isSel = yr == selectedYear
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) ElectricIndigo else Color(0xFF1E293B))
                                .clickable { selectedYear = yr }
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = yr,
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color.White else TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Grouped Bar Chart
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .height(110.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val barCount = barHeights.size
                        val barW = (w / barCount) * 0.7f
                        val spacing = (w / barCount) * 0.3f

                        barHeights.forEachIndexed { i, bh ->
                            val x = i * (barW + spacing)
                            val normH = (bh / 100f).coerceIn(0f, 1f) * h
                            val color = when (i % 3) {
                                0 -> ElectricIndigo
                                1 -> CyberEmerald
                                else -> LuminousAmber
                            }

                            drawRoundRect(
                                color = color,
                                topLeft = Offset(x, h - normH),
                                size = Size(barW, normH),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Radar / Spider Chart Canvas
                Box(
                    modifier = Modifier
                        .size(105.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2
                        val cy = size.height / 2
                        val maxR = size.minDimension * 0.44f
                        val count = radarAxes.size

                        // Concentric grid webs
                        for (level in 1..3) {
                            val r = maxR * (level / 3f)
                            val gridPath = Path()
                            for (j in 0 until count) {
                                val angle = Math.toRadians((j * (360.0 / count) - 90.0))
                                val x = cx + (r * cos(angle)).toFloat()
                                val y = cy + (r * sin(angle)).toFloat()
                                if (j == 0) gridPath.moveTo(x, y) else gridPath.lineTo(x, y)
                            }
                            gridPath.close()
                            drawPath(gridPath, color = Color.White.copy(alpha = 0.08f), style = Stroke(1f))
                        }

                        // Data Polygon
                        val dataPath = Path()
                        radarScores.forEachIndexed { j, s ->
                            val r = maxR * s
                            val angle = Math.toRadians((j * (360.0 / count) - 90.0))
                            val x = cx + (r * cos(angle)).toFloat()
                            val y = cy + (r * sin(angle)).toFloat()
                            if (j == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
                        }
                        dataPath.close()

                        drawPath(
                            dataPath,
                            brush = Brush.radialGradient(
                                listOf(CyberEmerald.copy(alpha = 0.4f), Color.Transparent)
                            ),
                            style = Fill
                        )
                        drawPath(dataPath, color = CyberEmerald, style = Stroke(width = 2.dp.toPx()))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Monthly Class Batches (Jan - Dec $selectedYear)", color = TextMuted, fontSize = 9.5.sp)
                Text("Multi-Axis Radar Attainment", color = CyberEmerald, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Cyber Parameter Sliders Widget with toggle controls and interactive metric adjustments.
 */
@Composable
fun CyberParameterSlidersWidget(
    title: String = "Operational Parameters",
    params: List<Pair<String, Int>> = listOf(
        Pair("Biometric Face Engine", 94),
        Pair("Cloud Cluster Bandwidth", 78),
        Pair("Student Attendance Safe Bar", 82),
        Pair("Internal Exam Compliance", 88)
    ),
    isDarkMode: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isDarkMode) Color(0xFF13192B) else LightSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) Color(0xFF1E293B) else LightElevated)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold
                    )
                )
                Text("Real-Time Telemetry", color = CyberEmerald, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            }

            params.forEachIndexed { i, pair ->
                val barColor = when (i % 4) {
                    0 -> CyberEmerald
                    1 -> NeonBlue
                    2 -> LuminousAmber
                    else -> ElectricIndigo
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(pair.first, color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                        Text("${pair.second}%", color = barColor, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1E293B))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = (pair.second / 100f).coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(barColor)
                        )
                    }
                }
            }
        }
    }
}
