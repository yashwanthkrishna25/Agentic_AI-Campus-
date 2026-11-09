package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AcademicDataProvider
import com.example.model.SubjectCourse
import com.example.ui.components.OverallAttendanceCard
import com.example.ui.components.SubjectAttendanceCard
import com.example.ui.theme.*

enum class AcademicSubTab {
    CURRENT_TERM,
    EXAM_RESULTS
}

@Composable
fun AcademicsScreen(
    onNavigateToCamera: (String) -> Unit,
    onNavigateToManual: () -> Unit,
    onNavigateToChat: (String) -> Unit
) {
    var selectedSubTab by remember { mutableStateOf(AcademicSubTab.CURRENT_TERM) }
    var selectedTerm by remember { mutableStateOf("ODD Semester (3/5/7) 2026-27") }

    val courses = AcademicDataProvider.sharedCourses

    val totalAttended = courses.sumOf { it.attendedSessions }
    val totalHeld = courses.sumOf { it.totalSessions }
    val overallPercentage = if (totalHeld > 0) (totalAttended.toDouble() / totalHeld.toDouble()) * 100.0 else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        // Top Segmented Bar for "Current Term" vs "Exam Results"
        Surface(
            color = ObsidianSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { selectedSubTab = AcademicSubTab.CURRENT_TERM }
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Current Term",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = if (selectedSubTab == AcademicSubTab.CURRENT_TERM) CyberEmerald else TextSecondary,
                            fontWeight = if (selectedSubTab == AcademicSubTab.CURRENT_TERM) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                    if (selectedSubTab == AcademicSubTab.CURRENT_TERM) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(3.dp)
                                .background(CyberEmerald)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { selectedSubTab = AcademicSubTab.EXAM_RESULTS }
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Exam Results",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = if (selectedSubTab == AcademicSubTab.EXAM_RESULTS) CyberEmerald else TextSecondary,
                            fontWeight = if (selectedSubTab == AcademicSubTab.EXAM_RESULTS) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                    if (selectedSubTab == AcademicSubTab.EXAM_RESULTS) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(3.dp)
                                .background(CyberEmerald)
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Select Term Dropdown Box
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ObsidianSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ACADEMIC CYCLE",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp, letterSpacing = 1.sp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = selectedTerm,
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = CyberEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (selectedSubTab == AcademicSubTab.CURRENT_TERM) {
                // Overall Attendance Summary Gauge Card
                item {
                    OverallAttendanceCard(
                        percentage = overallPercentage,
                        attendedSessions = totalAttended,
                        totalSessions = totalHeld,
                        onViewCatchupPlan = {
                            onNavigateToChat("Generate a 1-month plan to reach > 75% attendance from current $overallPercentage%")
                        }
                    )
                }

                // Quick Action Bar for Taking Face Attendance & Manual Edit
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onNavigateToCamera("Deep Learning [2339609]") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_face_attendance_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberEmeraldDark)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Face Camera", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = onNavigateToManual,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_manual_attendance_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                        ) {
                            Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Manual Edit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Subject Wise Attendance Cards
                items(courses) { course ->
                    SubjectAttendanceCard(
                        course = course,
                        onTakeAttendance = { onNavigateToCamera(course.name) },
                        onOpenChatPlan = {
                            onNavigateToChat("My attendance for ${course.name} is ${course.attendancePercentage}%. I need to reach > 75% in 1 month, what should I do?")
                        }
                    )
                }
            } else {
                // Exam Results & Marks Tab
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ObsidianSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Assessment Marks & Components",
                                style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Internal Assessment Components + End-Semester Exam",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }
                }

                items(courses) { course ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ObsidianSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = course.name,
                                        style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "${course.code} • ${course.courseType}",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                    )
                                }

                                Surface(
                                    color = Color(0xFF042F2E),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberEmerald)
                                ) {
                                    Text(
                                        text = "Total: ${"%.1f".format(course.grandTotal)}/100",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelMedium.copy(color = CyberEmerald, fontWeight = FontWeight.Bold)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Internals: ${"%.1f".format(course.internalTotal)}/60", color = TextSecondary, fontSize = 12.sp)
                                Text("End-Term: ${"%.1f".format(course.x7)}/40", color = Color(0xFFA5B4FC), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (course.grandTotal >= 70) "Band 5 (A+)" else if (course.grandTotal >= 60) "Band 4 (A)" else "Band 3 (B)",
                                    color = CyberEmerald,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
