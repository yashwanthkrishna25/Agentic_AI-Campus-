package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agent.AcademicRiskAgent
import com.example.model.SubjectCourse
import com.example.ui.theme.*

@Composable
fun OverallAttendanceCard(
    percentage: Double = 80.15,
    attendedSessions: Int = 214,
    totalSessions: Int = 267,
    isDarkMode: Boolean = true,
    onViewCatchupPlan: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("overall_attendance_card"),
        shape = RoundedCornerShape(20.dp),
        color = if (isDarkMode) ObsidianElevated else LightSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
        shadowElevation = if (isDarkMode) 6.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Overall Attendance Metric",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isDarkMode) TextSecondary else Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${"%.2f".format(percentage)}%",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = if (percentage < 75.0) NeonCoral else (if (isDarkMode) CyberEmerald else Color(0xFF0D9488)),
                        fontWeight = FontWeight.Black,
                        fontSize = 34.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$attendedSessions of $totalSessions sessions attended",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isDarkMode) TextSecondary else Color(0xFF64748B),
                        fontSize = 13.sp
                    )
                )

                if (percentage < 75.0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onViewCatchupPlan,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCoral),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("1-Month Recovery Plan", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // Circular Progress Gauge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(82.dp)
            ) {
                val animatedProgress by animateFloatAsState(
                    targetValue = (percentage / 100.0).toFloat().coerceIn(0f, 1f),
                    animationSpec = tween(durationMillis = 1000),
                    label = "overall_attendance_anim"
                )

                Canvas(modifier = Modifier.size(80.dp)) {
                    val strokeWidth = 9.dp.toPx()
                    // Track background
                    drawArc(
                        color = if (isDarkMode) ObsidianBorder else Color(0xFFE2E8F0),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth)
                    )
                    // Neon progress arc
                    drawArc(
                        color = if (percentage < 75.0) Color(0xFFF43F5E) else (if (isDarkMode) Color(0xFF00F5D4) else Color(0xFF0D9488)),
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Icon(
                    imageVector = if (percentage >= 75.0) Icons.Default.Check else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (percentage >= 75.0) (if (isDarkMode) CyberEmerald else Color(0xFF0D9488)) else LuminousAmber,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun SubjectAttendanceCard(
    course: SubjectCourse,
    isDarkMode: Boolean = true,
    onTakeAttendance: () -> Unit,
    onOpenChatPlan: (SubjectCourse) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag("subject_card_${course.code}"),
        shape = RoundedCornerShape(16.dp),
        color = if (isDarkMode) ObsidianSurface else LightSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated),
        shadowElevation = if (isDarkMode) 2.dp else 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = course.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = course.sectionTag,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isDarkMode) TextSecondary else Color(0xFF475569),
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = "Instructor: ${course.facultyName} • [${course.courseType}]",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isDarkMode) TextMuted else Color(0xFF64748B),
                            fontSize = 10.sp
                        )
                    )
                }

                // Percentage Badge
                Surface(
                    color = if (isDarkMode) ObsidianElevated else Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (course.attendancePercentage < 75.0) NeonCoral else (if (isDarkMode) CyberEmerald else Color(0xFF0D9488))
                    )
                ) {
                    Text(
                        text = if (course.attendancePercentage % 1.0 == 0.0)
                            "${course.attendancePercentage.toInt()}%"
                        else
                            "${"%.2f".format(course.attendancePercentage)}%",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = if (course.attendancePercentage < 75.0) NeonCoral else (if (isDarkMode) CyberEmerald else Color(0xFF0D9488)),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Track
            val animatedFraction by animateFloatAsState(
                targetValue = (course.attendancePercentage / 100.0).toFloat().coerceIn(0f, 1f),
                animationSpec = tween(durationMillis = 800),
                label = "subject_progress_anim"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(if (isDarkMode) ObsidianBorder else Color(0xFFE2E8F0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedFraction)
                        .clip(CircleShape)
                        .background(
                            if (course.attendancePercentage < 75.0) NeonCoral else (if (isDarkMode) CyberEmerald else Color(0xFF0D9488))
                        )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${course.attendedSessions} of ${course.totalSessions} sessions",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isDarkMode) TextSecondary else Color(0xFF64748B),
                        fontSize = 11.5.sp
                    ),
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false).padding(end = 6.dp)
                )

                Text(
                    text = if (course.isAttendanceAtRisk) "Warning: <75%" else "Good Standing",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (course.isAttendanceAtRisk) NeonCoral else (if (isDarkMode) CyberEmerald else Color(0xFF0D9488)),
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1
                )
            }

            // Expandable section with research paper component marks
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    HorizontalDivider(color = if (isDarkMode) ObsidianBorder else LightElevated)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Subject Syllabus & Lecture Breakdown:",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF1E293B),
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Clean Attendance & Syllabus Progress Grid
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDarkMode) ObsidianElevated else Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) ObsidianBorder else LightElevated)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Lectures Conducted",
                                    fontSize = 11.5.sp,
                                    color = if (isDarkMode) TextSecondary else Color(0xFF475569)
                                )
                                Text(
                                    text = "${course.attendedSessions} / ${course.totalSessions} Hours",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (course.attendancePercentage >= 75.0) CyberEmerald else NeonCoral
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Syllabus Units Completed",
                                    fontSize = 11.5.sp,
                                    color = if (isDarkMode) TextSecondary else Color(0xFF475569),
                                    modifier = Modifier.weight(0.5f)
                                )
                                Text(
                                    text = "Unit 4 of 5 (80% Covered)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.5.sp,
                                    color = ElectricIndigo,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                    modifier = Modifier.weight(0.5f)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "75% Cutoff Status",
                                    fontSize = 11.5.sp,
                                    color = if (isDarkMode) TextSecondary else Color(0xFF475569),
                                    modifier = Modifier.weight(0.45f)
                                )
                                Text(
                                    text = if (course.attendancePercentage >= 75.0) "Safe Zone (No condonation)" else "Action Needed (Attend next 6)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = if (course.attendancePercentage >= 75.0) CyberEmerald else NeonCoral,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                    modifier = Modifier.weight(0.55f)
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
                            onClick = onTakeAttendance,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isDarkMode) CyberEmerald else Color(0xFF0D9488)
                            )
                        ) {
                            Text("Face Attendance", fontSize = 11.sp, maxLines = 1)
                        }

                        Button(
                            onClick = { onOpenChatPlan(course) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricIndigo
                            )
                        ) {
                            Text("1-Month AI Plan", fontSize = 11.sp, maxLines = 1, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComponentMarkBadge(
    label: String,
    value: Double,
    max: Int,
    isEndTerm: Boolean = false,
    isDarkMode: Boolean = true
) {
    Surface(
        color = if (isEndTerm) {
            if (isDarkMode) ElectricIndigo.copy(alpha = 0.25f) else Color(0xFFE0E7FF)
        } else {
            if (isDarkMode) ObsidianElevated else Color(0xFFF1F5F9)
        },
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isEndTerm) ElectricIndigo else (if (isDarkMode) ObsidianBorder else LightElevated)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isEndTerm) {
                        if (isDarkMode) Color(0xFFA5B4FC) else ElectricIndigo
                    } else {
                        if (isDarkMode) TextSecondary else Color(0xFF64748B)
                    },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "${"%.1f".format(value)}",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
