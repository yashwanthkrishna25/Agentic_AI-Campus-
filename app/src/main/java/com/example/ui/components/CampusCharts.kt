package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Reusable Interactive Donut Chart for Attendance & Risk Distributions.
 */
@Composable
fun AttendanceDonutChart(
    presentPct: Float,
    absentPct: Float,
    latePct: Float = 0f,
    excusedPct: Float = 0f,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = true
) {
    var selectedSlice by remember { mutableStateOf<String?>(null) }
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(presentPct, absentPct, selectedSlice) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
    }

    val total = (presentPct + absentPct + latePct + excusedPct).coerceAtLeast(1f)
    val presentAngle = (presentPct / total) * 360f
    val absentAngle = (absentPct / total) * 360f
    val lateAngle = (latePct / total) * 360f
    val excusedAngle = (excusedPct / total) * 360f

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isDarkMode) CyberElevated else LightSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) CyberBorder else LightBorder)
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
                    text = "Attendance Distribution",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isDarkMode) Color.White else LightTextPrimary
                )
                if (selectedSlice != null) {
                    Text(
                        text = "Reset Filter",
                        color = CyberViolet,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { selectedSlice = null }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                Canvas(modifier = Modifier.size(140.dp)) {
                    val stroke = 22.dp.toPx()
                    val diameter = size.minDimension - stroke
                    val topLeft = Offset(stroke / 2, stroke / 2)
                    val arcSize = Size(diameter, diameter)

                    var startAngle = -90f
                    val sweepProgress = animationProgress.value

                    when (selectedSlice) {
                        "Present" -> {
                            drawArc(
                                color = CyberSuccess,
                                startAngle = -90f,
                                sweepAngle = 360f * sweepProgress,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(stroke, cap = StrokeCap.Round)
                            )
                        }
                        "Absent" -> {
                            drawArc(
                                color = CyberError,
                                startAngle = -90f,
                                sweepAngle = 360f * sweepProgress,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(stroke, cap = StrokeCap.Round)
                            )
                        }
                        "Late" -> {
                            drawArc(
                                color = CyberWarning,
                                startAngle = -90f,
                                sweepAngle = 360f * sweepProgress,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(stroke, cap = StrokeCap.Round)
                            )
                        }
                        "Excused" -> {
                            drawArc(
                                color = CyberInfo,
                                startAngle = -90f,
                                sweepAngle = 360f * sweepProgress,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(stroke, cap = StrokeCap.Round)
                            )
                        }
                        else -> {
                            // Present Arc
                            drawArc(
                                color = CyberSuccess,
                                startAngle = startAngle,
                                sweepAngle = presentAngle * sweepProgress,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(stroke, cap = StrokeCap.Round)
                            )
                            startAngle += presentAngle

                            // Absent Arc
                            drawArc(
                                color = CyberError,
                                startAngle = startAngle,
                                sweepAngle = absentAngle * sweepProgress,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(stroke, cap = StrokeCap.Round)
                            )
                            startAngle += absentAngle

                            // Late Arc
                            if (latePct > 0f) {
                                drawArc(
                                    color = CyberWarning,
                                    startAngle = startAngle,
                                    sweepAngle = lateAngle * sweepProgress,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(stroke, cap = StrokeCap.Round)
                                )
                                startAngle += lateAngle
                            }

                            // Excused Arc
                            if (excusedPct > 0f) {
                                drawArc(
                                    color = CyberInfo,
                                    startAngle = startAngle,
                                    sweepAngle = excusedAngle * sweepProgress,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(stroke, cap = StrokeCap.Round)
                                )
                            }
                        }
                    }
                }

                // Center readout
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val (rateText, labelText) = when (selectedSlice) {
                        "Present" -> "${String.format("%.1f", presentPct)}%" to "Present Only"
                        "Absent" -> "${String.format("%.1f", absentPct)}%" to "Absent Only"
                        "Late" -> "${String.format("%.1f", latePct)}%" to "Late Only"
                        "Excused" -> "${String.format("%.1f", excusedPct)}%" to "Excused Only"
                        else -> "${String.format("%.1f", presentPct)}%" to "Present Rate"
                    }
                    Text(
                        text = rateText,
                        color = if (isDarkMode) Color.White else LightTextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = labelText,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Clickable Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ChartLegendItem("Present", CyberSuccess, "${String.format("%.1f", presentPct)}%", selectedSlice == "Present") {
                    selectedSlice = if (selectedSlice == "Present") null else "Present"
                }
                ChartLegendItem("Absent", CyberError, "${String.format("%.1f", absentPct)}%", selectedSlice == "Absent") {
                    selectedSlice = if (selectedSlice == "Absent") null else "Absent"
                }
                if (latePct > 0f) {
                    ChartLegendItem("Late", CyberWarning, "${String.format("%.1f", latePct)}%", selectedSlice == "Late") {
                        selectedSlice = if (selectedSlice == "Late") null else "Late"
                    }
                }
                if (excusedPct > 0f) {
                    ChartLegendItem("Excused", CyberInfo, "${String.format("%.1f", excusedPct)}%", selectedSlice == "Excused") {
                        selectedSlice = if (selectedSlice == "Excused") null else "Excused"
                    }
                }
            }
        }
    }
}

/**
 * Reusable Monthly Attendance Trend Line Chart with Interactive Month Selection.
 */
@Composable
fun MonthlyTrendLineChart(
    trends: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = true
) {
    var selectedMonthIndex by remember { mutableStateOf<Int?>(null) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(trends) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isDarkMode) CyberElevated else LightSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) CyberBorder else LightBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Attendance Trend (Semester)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isDarkMode) Color.White else LightTextPrimary
                )
                if (selectedMonthIndex != null && selectedMonthIndex!! in trends.indices) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CyberViolet.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberViolet),
                        modifier = Modifier.clickable { selectedMonthIndex = null }
                    ) {
                        Text(
                            text = "${trends[selectedMonthIndex!!].first}: ${String.format("%.1f", trends[selectedMonthIndex!!].second)}% ✕",
                            color = Color(0xFFDDD6FE),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CyberSuccess.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberSuccess.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "+2.4% vs Prev",
                            color = CyberSuccess,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                if (trends.isEmpty()) return@Canvas
                val w = size.width
                val h = size.height
                val paddingBottom = 20.dp.toPx()
                val effectiveHeight = h - paddingBottom

                val minVal = 60f
                val maxVal = 100f
                val range = maxVal - minVal

                val stepX = w / (trends.size - 1).coerceAtLeast(1)

                val points = trends.mapIndexed { idx, pair ->
                    val x = idx * stepX
                    val normalizedY = ((pair.second - minVal) / range).coerceIn(0f, 1f)
                    val y = effectiveHeight - (normalizedY * effectiveHeight * progress.value)
                    Offset(x, y)
                }

                // Threshold 75% dashed line
                val thresholdY = effectiveHeight - (((75f - minVal) / range) * effectiveHeight)
                drawLine(
                    color = CyberWarning.copy(alpha = 0.5f),
                    start = Offset(0f, thresholdY),
                    end = Offset(w, thresholdY),
                    strokeWidth = 1.5.dp.toPx()
                )

                // Fill under curve
                val fillPath = Path().apply {
                    moveTo(0f, effectiveHeight)
                    points.forEach { lineTo(it.x, it.y) }
                    lineTo(w, effectiveHeight)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            CyberViolet.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )

                // Main stroke curve
                val strokePath = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }
                drawPath(
                    path = strokePath,
                    color = CyberViolet,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Highlight selected month vertical guideline & halo
                if (selectedMonthIndex != null && selectedMonthIndex!! in points.indices) {
                    val selPt = points[selectedMonthIndex!!]
                    drawLine(
                        color = Color(0xFFDDD6FE).copy(alpha = 0.7f),
                        start = Offset(selPt.x, 0f),
                        end = Offset(selPt.x, effectiveHeight),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawCircle(color = CyberViolet.copy(alpha = 0.4f), radius = 10.dp.toPx(), center = selPt)
                }

                // Point dots
                points.forEachIndexed { idx, pt ->
                    val isSelected = selectedMonthIndex == idx
                    drawCircle(color = if (isSelected) Color(0xFF00E676) else CyberVioletPressed, radius = if (isSelected) 6.dp.toPx() else 5.dp.toPx(), center = pt)
                    drawCircle(color = Color.White, radius = if (isSelected) 3.5.dp.toPx() else 2.5.dp.toPx(), center = pt)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Interactive Month labels row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                trends.forEachIndexed { idx, pair ->
                    val isSelected = selectedMonthIndex == idx
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) CyberViolet.copy(alpha = 0.25f) else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) CyberViolet else Color.Transparent),
                        modifier = Modifier.clickable {
                            selectedMonthIndex = if (selectedMonthIndex == idx) null else idx
                        }
                    ) {
                        Text(
                            text = pair.first,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Reusable Subject Attendance Bar Chart with 75% threshold indicator.
 */
@Composable
fun SubjectAttendanceBarChart(
    subjects: List<Triple<String, Float, String>>, // Name, Pct, Code
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = true
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isDarkMode) CyberElevated else LightSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) CyberBorder else LightBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Subject-Wise Compliance",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isDarkMode) Color.White else LightTextPrimary
                )
                Text(
                    text = "Threshold: 75%",
                    color = CyberWarning,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            subjects.forEach { item ->
                val name = item.first
                val pct = item.second
                val isBelowThreshold = pct < 75f
                val barColor = if (isBelowThreshold) CyberError else CyberViolet

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = name,
                            color = if (isDarkMode) TextIceBlue else LightTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )
                        Text(
                            text = "${String.format("%.1f", pct)}%",
                            color = if (isBelowThreshold) CyberError else CyberSuccess,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(if (isDarkMode) CyberSurface else LightElevated)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = (pct / 100f).coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(barColor)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Reusable Department Multi-Comparison Chart (Attendance & Academic CGPA).
 */
@Composable
fun DepartmentComparisonChart(
    depts: List<Triple<String, Float, Double>>, // Dept code, Attendance %, CGPA
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = true
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isDarkMode) CyberElevated else LightSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) CyberBorder else LightBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Department Attendance & CGPA Comparison",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isDarkMode) Color.White else LightTextPrimary
            )

            depts.forEach { dept ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(55.dp)
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = dept.first,
                            fontWeight = FontWeight.Bold,
                            color = CyberViolet,
                            fontSize = 12.sp
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { (dept.second / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = if (dept.second >= 75f) CyberSuccess else CyberError,
                            trackColor = if (isDarkMode) CyberSurface else LightElevated
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${dept.second}% | ${dept.third}",
                        color = if (isDarkMode) TextIceBlue else LightTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartLegendItem(
    label: String,
    color: Color,
    value: String,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) color else Color.Transparent
        ),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "$label: $value",
                color = if (isSelected) Color.White else TextSecondary,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
