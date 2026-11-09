package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.EmailAlertDispatcher
import com.example.model.EmailCategory
import com.example.model.EmailNotificationLog
import com.example.ui.theme.*

/**
 * Institutional Email Alert Center & Dispatch Modal.
 * Allows viewing dispatched official email notices to Parents, Mentors, and Students,
 * and manually triggering 75% Shortage Alerts, Andhra Exam Schedules, and Grade Sheets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailAlertCenterDialog(
    onDismiss: () -> Unit,
    onEmailDispatched: (String) -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf<EmailCategory?>(null) }
    var emailToInspect by remember { mutableStateOf<EmailNotificationLog?>(null) }
    val emailList = EmailAlertDispatcher.sentEmails

    val filteredEmails = remember(selectedCategory, emailList.size) {
        if (selectedCategory == null) emailList else emailList.filter { it.category == selectedCategory }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = ObsidianSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header with KSRM College of Engineering details
                Surface(
                    color = Color(0xFF0F1E3D),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(NeonAmber.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MarkEmailRead,
                                    contentDescription = null,
                                    tint = NeonAmber,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "KSRMCE EMAIL DISPATCH GATEWAY",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Automated Parent & Faculty Advisory System",
                                    color = CyberEmerald,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Fast dispatch trigger actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val sent = EmailAlertDispatcher.sendAttendanceShortageAlert()
                                    onEmailDispatched("75% Shortage Red Alert sent to Parent & Mentor!")
                                    emailToInspect = sent
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Dispatch <75% Alert", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Button(
                                onClick = {
                                    val sent = EmailAlertDispatcher.sendAndhraExamScheduleAlert()
                                    onEmailDispatched("Andhra Exam Schedule emailed to Parent & Student!")
                                    emailToInspect = sent
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Email Timetable", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                // Category Filter Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CategoryPill(
                        title = "All Logs (${emailList.size})",
                        isSelected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        color = CyberEmerald
                    )
                    CategoryPill(
                        title = "< 75% Alerts",
                        isSelected = selectedCategory == EmailCategory.ATTENDANCE_SHORTAGE_ALERT,
                        onClick = { selectedCategory = EmailCategory.ATTENDANCE_SHORTAGE_ALERT },
                        color = Color(0xFFEF4444)
                    )
                    CategoryPill(
                        title = "Exams",
                        isSelected = selectedCategory == EmailCategory.EXAM_SCHEDULE_NOTIFICATION,
                        onClick = { selectedCategory = EmailCategory.EXAM_SCHEDULE_NOTIFICATION },
                        color = ElectricIndigo
                    )
                    CategoryPill(
                        title = "OD Credits",
                        isSelected = selectedCategory == EmailCategory.ACTIVITY_OD_APPROVAL,
                        onClick = { selectedCategory = EmailCategory.ACTIVITY_OD_APPROVAL },
                        color = NeonAmber
                    )
                }

                Divider(color = ObsidianBorder, thickness = 1.dp)

                // List of Sent Emails
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredEmails, key = { it.id }) { email ->
                        EmailItemCard(
                            email = email,
                            onClick = { emailToInspect = email }
                        )
                    }
                }
            }
        }
    }

    // Detail Email Inspection Modal
    if (emailToInspect != null) {
        val email = emailToInspect!!
        AlertDialog(
            onDismissRequest = { emailToInspect = null },
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.85f)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ObsidianSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberEmerald)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (email.category) {
                                EmailCategory.ATTENDANCE_SHORTAGE_ALERT -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                EmailCategory.EXAM_SCHEDULE_NOTIFICATION -> ElectricIndigo.copy(alpha = 0.2f)
                                EmailCategory.EXAM_RESULTS_GRADE_CARD -> CyberEmerald.copy(alpha = 0.2f)
                                EmailCategory.ACTIVITY_OD_APPROVAL -> NeonAmber.copy(alpha = 0.2f)
                                EmailCategory.CAMPUS_ANNOUNCEMENT -> Color(0xFF38BDF8).copy(alpha = 0.2f)
                            }
                        ) {
                            Text(
                                text = email.category.badge,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (email.category) {
                                    EmailCategory.ATTENDANCE_SHORTAGE_ALERT -> Color(0xFFFCA5A5)
                                    EmailCategory.EXAM_SCHEDULE_NOTIFICATION -> Color(0xFFA5B4FC)
                                    EmailCategory.EXAM_RESULTS_GRADE_CARD -> CyberEmerald
                                    EmailCategory.ACTIVITY_OD_APPROVAL -> NeonAmber
                                    EmailCategory.CAMPUS_ANNOUNCEMENT -> Color(0xFF7DD3FC)
                                }
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(text = email.sentTimestamp, fontSize = 11.sp, color = TextMuted)
                        IconButton(onClick = { emailToInspect = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = email.subject,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.5.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = Color(0xFF090D1A),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "FROM: noreply-alerts@ksrmce.ac.in (KSRMCE Server)", fontSize = 11.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "TO: ${email.recipients.joinToString(", ")}", fontSize = 11.sp, color = CyberEmerald, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "STATUS: ${email.deliveryStatus}", fontSize = 10.5.sp, color = Color(0xFF6EE7B7))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Formatted body
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = ObsidianElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(14.dp)
                        ) {
                            Text(
                                text = email.contentBody,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { emailToInspect = null },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Done / Close Preview", color = ObsidianBg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryPill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) color else Color(0xFF334155)),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) color else TextSecondary
        )
    }
}

@Composable
private fun EmailItemCard(
    email: EmailNotificationLog,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = ObsidianElevated,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (email.category == EmailCategory.ATTENDANCE_SHORTAGE_ALERT) Color(0xFFEF4444).copy(alpha = 0.4f) else ObsidianBorder
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (email.category) {
                        EmailCategory.ATTENDANCE_SHORTAGE_ALERT -> Color(0xFFEF4444).copy(alpha = 0.2f)
                        EmailCategory.EXAM_SCHEDULE_NOTIFICATION -> ElectricIndigo.copy(alpha = 0.2f)
                        EmailCategory.EXAM_RESULTS_GRADE_CARD -> CyberEmerald.copy(alpha = 0.2f)
                        EmailCategory.ACTIVITY_OD_APPROVAL -> NeonAmber.copy(alpha = 0.2f)
                        EmailCategory.CAMPUS_ANNOUNCEMENT -> Color(0xFF38BDF8).copy(alpha = 0.2f)
                    }
                ) {
                    Text(
                        text = email.category.badge,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (email.category) {
                            EmailCategory.ATTENDANCE_SHORTAGE_ALERT -> Color(0xFFFCA5A5)
                            EmailCategory.EXAM_SCHEDULE_NOTIFICATION -> Color(0xFFA5B4FC)
                            EmailCategory.EXAM_RESULTS_GRADE_CARD -> CyberEmerald
                            EmailCategory.ACTIVITY_OD_APPROVAL -> NeonAmber
                            EmailCategory.CAMPUS_ANNOUNCEMENT -> Color(0xFF7DD3FC)
                        }
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(text = email.sentTimestamp, fontSize = 10.sp, color = TextMuted)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = email.subject,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 13.sp
                ),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Recipients: ${email.recipientRoles}",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSecondary,
                    fontSize = 10.5.sp
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = CyberEmerald,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = email.deliveryStatus,
                    color = CyberEmerald,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "View Message ➔",
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
