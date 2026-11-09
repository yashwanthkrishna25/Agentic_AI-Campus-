package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserRole
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String = "AGENTRA AI",
    activeRole: UserRole = UserRole.STUDENT,
    notificationCount: Int = 5,
    isDarkMode: Boolean = true,
    onToggleTheme: () -> Unit,
    onMenuClick: () -> Unit,
    onRoleSwitcherClick: () -> Unit,
    onSearchClick: () -> Unit,
    onQrClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onEmailCenterClick: () -> Unit = {},
    onHomeClick: () -> Unit = {}
) {
    val roleAccent = when (activeRole) {
        UserRole.STUDENT -> CyberEmerald
        UserRole.FACULTY -> ElectricIndigo
        UserRole.HOD -> Color(0xFF8B5CF6)
        UserRole.PRINCIPAL, UserRole.PRINCIPAL_ADMIN -> LuminousAmber
        UserRole.ADMIN -> Color(0xFFEF4444)
        UserRole.SUPER_ADMIN -> Color(0xFF7C3AED)
        UserRole.PARENT -> Color(0xFF38BDF8)
    }

    val adaptiveInfo = com.example.ui.responsive.LocalWindowAdaptiveInfo.current
    val isSmall = adaptiveInfo.isSmallScreen

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(onClick = onRoleSwitcherClick)
                    .padding(vertical = 2.dp)
                    .testTag("topbar_role_switcher")
            ) {
                Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.agentra_official_icon),
                    contentDescription = "AGENTRA AI CAMPUS Logo",
                    modifier = Modifier
                        .size(if (isSmall) 32.dp else 36.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, roleAccent, CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(if (isSmall) 6.dp else 8.dp))

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isSmall) "AGENTRA" else "AGENTRA AI",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.3.sp,
                                color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                fontSize = if (isSmall) 13.5.sp else 15.sp
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                        Surface(
                            color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                if (isDarkMode) LuminousAmber.copy(alpha = 0.5f) else Color(0xFFD97706)
                            )
                        ) {
                            Text(
                                text = "KSRMCE",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarkMode) LuminousAmber else Color(0xFFB45309),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    Text(
                        text = if (isSmall) activeRole.displayName else "KSRMCE (Autonomous) • ${activeRole.displayName}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = when (activeRole) {
                                UserRole.STUDENT -> CyberEmerald
                                UserRole.FACULTY -> if (isDarkMode) Color(0xFFA5B4FC) else ElectricIndigo
                                UserRole.HOD -> Color(0xFFC084FC)
                                UserRole.PRINCIPAL, UserRole.PRINCIPAL_ADMIN -> LuminousAmber
                                UserRole.ADMIN -> Color(0xFFFCA5A5)
                                UserRole.SUPER_ADMIN -> Color(0xFFC4B5FD)
                                UserRole.PARENT -> Color(0xFF38BDF8)
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isSmall) 9.sp else 10.sp
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .size(if (isSmall) 38.dp else 44.dp)
                    .testTag("menu_drawer_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open Navigation Menu",
                    tint = CyberEmerald
                )
            }
        },
        actions = {
            // One-Tap Direct Home Page Button
            IconButton(
                onClick = onHomeClick,
                modifier = Modifier
                    .size(if (isSmall) 36.dp else 42.dp)
                    .testTag("topbar_one_tap_home_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "One-Tap Open Home Page",
                    tint = roleAccent,
                    modifier = Modifier.size(if (isSmall) 20.dp else 22.dp)
                )
            }

            // Day / Night Theme Toggle
            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier
                    .size(if (isSmall) 36.dp else 42.dp)
                    .testTag("theme_toggle_button")
            ) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle Light/Dark Theme",
                    tint = if (isDarkMode) LuminousAmber else Color(0xFF475569),
                    modifier = Modifier.size(if (isSmall) 18.dp else 20.dp)
                )
            }

            // KSRMCE Email Alert Center Button
            IconButton(
                onClick = onEmailCenterClick,
                modifier = Modifier
                    .size(if (isSmall) 36.dp else 42.dp)
                    .testTag("top_email_center_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Mail,
                    contentDescription = "KSRMCE Email Alert Center",
                    tint = NeonAmber,
                    modifier = Modifier.size(if (isSmall) 18.dp else 20.dp)
                )
            }

            // Interactive Notifications Bell with Dynamic Badge
            Box(
                contentAlignment = Alignment.TopEnd,
                modifier = Modifier.padding(end = if (isSmall) 4.dp else 8.dp)
            ) {
                IconButton(
                    onClick = onNotificationsClick,
                    modifier = Modifier
                        .size(if (isSmall) 36.dp else 42.dp)
                        .testTag("top_notification_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = if (isDarkMode) Color.White else Color(0xFF0F172A),
                        modifier = Modifier.size(if (isSmall) 18.dp else 20.dp)
                    )
                }
                if (notificationCount > 0) {
                    Box(
                        modifier = Modifier
                            .offset(x = (-2).dp, y = 4.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(NeonCoral),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = notificationCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = if (isDarkMode) ObsidianSurface else LightSurface
        )
    )
}
