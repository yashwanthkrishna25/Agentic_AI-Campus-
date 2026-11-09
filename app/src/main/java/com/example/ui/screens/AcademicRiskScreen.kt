package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agent.AcademicRiskAgent
import com.example.agent.AdvisorAgent
import com.example.data.AcademicDataProvider
import com.example.model.AcademicSurveyData
import com.example.model.RiskLevel
import com.example.model.UserRole
import com.example.ui.responsive.LocalWindowAdaptiveInfo
import com.example.ui.theme.*

@Composable
fun AcademicRiskScreen(
    userRole: UserRole = UserRole.STUDENT,
    onNavigateToChat: (String) -> Unit
) {
    val adaptiveInfo = LocalWindowAdaptiveInfo.current
    val isTwoPane = adaptiveInfo.isLandscape && adaptiveInfo.screenWidthDp >= 600.dp
    val horizPadding = if (adaptiveInfo.isSmallScreen) 10.dp else 16.dp

    val currentProfile = com.example.data.SessionManager.getCurrentProfile()
    val liveCourses = AcademicDataProvider.sharedCourses
    val liveAttended = liveCourses.sumOf { it.attendedSessions }
    val liveTotal = liveCourses.sumOf { it.totalSessions }
    val livePercentage = if (liveTotal > 0) (liveAttended.toDouble() / liveTotal.toDouble()) * 100.0 else 83.76

    val liveAttendanceBand = when {
        livePercentage >= 90.0 -> 5.0f
        livePercentage >= 80.0 -> 4.0f
        livePercentage >= 75.0 -> 3.0f
        livePercentage >= 65.0 -> 2.0f
        else -> 1.0f
    }

    // Automatically locked and derived from official attendance and CGPA
    val attendanceBand = liveAttendanceBand
    val cgpaBand = ((currentProfile.cgpa / 10.0f) * 5.0f).coerceIn(1.0, 5.0).toFloat()
    val stressLevel = when {
        livePercentage >= 85.0 -> 1.8f
        livePercentage >= 75.0 -> 2.6f
        livePercentage >= 65.0 -> 3.8f
        else -> 4.6f
    }
    val studyConsistency = when {
        livePercentage >= 85.0 -> 4.5f
        livePercentage >= 75.0 -> 3.6f
        else -> 2.2f
    }

    val survey = AcademicSurveyData(
        stressLevel = stressLevel.toInt().coerceIn(1, 5),
        cgpaBand = cgpaBand.toInt().coerceIn(1, 5),
        studyConsistency = studyConsistency.toInt().coerceIn(1, 5),
        attendanceBand = attendanceBand.toInt().coerceIn(1, 5)
    )

    val riskResult = remember(stressLevel, cgpaBand, studyConsistency, attendanceBand) {
        AcademicRiskAgent.evaluateAcademicRisk(survey)
    }

    val catchupPlan = remember(attendanceBand, liveAttended, liveTotal) {
        val attended = if (liveTotal > 0) liveAttended else 84
        val total = if (liveTotal > 0) liveTotal else 100
        AdvisorAgent.calculateAttendanceCatchupPlan(attended, total)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1100.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizPadding, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = "Academic Risk Agent",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "AI Predictive Academic Early Warning & Catch-Up Radar",
                    style = MaterialTheme.typography.bodySmall.copy(color = CyberEmerald, fontSize = 11.5.sp)
                )
            }
        }

        // Live Risk Status Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = when (riskResult.riskLevel) {
                RiskLevel.LOW -> Color(0xFF064E3B)
                RiskLevel.MODERATE -> Color(0xFF78350F)
                RiskLevel.HIGH -> Color(0xFF7F1D1D)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PREDICTED STATUS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.8f),
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = riskResult.riskLevel.label,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (riskResult.riskLevel) {
                                RiskLevel.LOW -> Icons.Default.Verified
                                RiskLevel.MODERATE -> Icons.Default.Warning
                                RiskLevel.HIGH -> Icons.Default.CrisisAlert
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Probabilities distribution row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ProbabilityPill(label = "Low Risk", prob = riskResult.probabilityLow, color = LowRiskGreen)
                    ProbabilityPill(label = "Moderate", prob = riskResult.probabilityModerate, color = ModerateRiskAmber)
                    ProbabilityPill(label = "High Risk", prob = riskResult.probabilityHigh, color = HighRiskRed)
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Advisor Agent Recommendation:",
                    style = MaterialTheme.typography.labelMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = riskResult.advisorAction,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE2E8F0)),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Interactive Sliders for the 4 Dominant Indicators (Section V-C of paper)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E293B),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Adjust Academic Indicators (Table VII Model):",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Permutation importance proved these 4 items drive 98.4% model accuracy.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8), fontSize = 11.sp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Locked Status Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF065F46).copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberEmerald),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "🔒 Synchronized with Official Student Attendance",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Live Attendance: ${"%.2f".format(livePercentage)}% ($liveAttended of $liveTotal sessions). Risk & predictions update automatically.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 1. Stress Level
                IndicatorSlider(
                    label = "Stress Level",
                    subtext = "Derived: Based on attendance deficit & current academic load",
                    value = stressLevel,
                    range = 1f..5f,
                    unit = "/ 5",
                    enabled = false,
                    onValueChange = {}
                )

                // 2. CGPA Band
                IndicatorSlider(
                    label = "CGPA Band",
                    subtext = "Derived: Official Academic CGPA (${"%.2f".format(currentProfile.cgpa)})",
                    value = cgpaBand,
                    range = 1f..5f,
                    unit = "/ 5",
                    enabled = false,
                    onValueChange = {}
                )

                // 3. Study Consistency
                IndicatorSlider(
                    label = "Study Consistency",
                    subtext = "Derived: Daily session engagement & regular attendance",
                    value = studyConsistency,
                    range = 1f..5f,
                    unit = "/ 5",
                    enabled = false,
                    onValueChange = {}
                )

                // 4. Attendance Band
                IndicatorSlider(
                    label = "Attendance Band",
                    subtext = "Derived: ${"%.2f".format(livePercentage)}% Attendance from Biometric Attendance Ledger",
                    value = attendanceBand,
                    range = 1f..5f,
                    unit = "/ 5",
                    enabled = false,
                    onValueChange = {}
                )
            }
        }

        // 1-Month Attendance Recovery Plan Card (Handles user request)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E293B),
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
                            text = "1-Month Attendance Recovery Plan",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Calculated strategy to cross mandatory 75% cutoff",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.EventAvailable,
                        contentDescription = null,
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                catchupPlan.roadmapMilestones.forEach { step ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowRight,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFE2E8F0),
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        onNavigateToChat("My attendance is below 75%, I have 1 month to get above 75%, generate my daily schedule.")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ask ScholarBot AI for Personalized Timetable", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    }
}

@Composable
fun ProbabilityPill(label: String, prob: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
        )
        Text(
            text = "${(prob * 100).toInt()}%",
            style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
fun IndicatorSlider(
    label: String,
    subtext: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                text = "${value.toInt()} $unit",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (enabled) CrimsonPrimary else Color(0xFF94A3B8),
                    fontWeight = FontWeight.Bold
                )
            )
        }
        Text(
            text = subtext,
            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontSize = 10.sp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            enabled = enabled,
            steps = 3,
            colors = SliderDefaults.colors(
                thumbColor = if (enabled) CrimsonPrimary else Color(0xFF64748B),
                activeTrackColor = if (enabled) CrimsonPrimary else Color(0xFF475569),
                inactiveTrackColor = Color(0xFF334155),
                disabledThumbColor = Color(0xFF64748B),
                disabledActiveTrackColor = Color(0xFF475569),
                disabledInactiveTrackColor = Color(0xFF1E293B)
            )
        )
    }
}
