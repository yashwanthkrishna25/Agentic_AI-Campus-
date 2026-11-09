package com.example.navigation

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserRole
import com.example.ui.theme.*

/**
 * Direction indicator for screen slide transition animations.
 */
enum class NavTransitionDirection {
    FORWARD,
    BACKWARD,
    REPLACE
}

/**
 * Unified Navigation Controller using HashRouter-style logic.
 * Encapsulates URL hash-style routing (#/student/campus, #/faculty/group_scan, etc.),
 * backstack state management, parameter parsing, and directional transition handling.
 */
@Stable
class HashRouter(initialHash: String = "#/student/campus") {
    var currentHash by mutableStateOf(initialHash)
        private set

    var transitionDirection by mutableStateOf(NavTransitionDirection.FORWARD)
        private set

    private val backStack = mutableStateListOf<String>().apply {
        add(initialHash)
    }

    val canGoBack: Boolean
        get() = backStack.size > 1

    val history: List<String>
        get() = backStack.toList()

    /**
     * Extracts the base path without query parameters or hash prefix.
     * E.g. "#/faculty/group_scan?subject=Deep%20Learning" -> "/faculty/group_scan"
     */
    val currentPath: String
        get() {
            val raw = currentHash.trim().removePrefix("#").substringBefore("?").trim()
            val cleaned = raw.trimStart('/')
            return "/$cleaned"
        }

    /**
     * Extracts parsed query parameters map from the active hash URL.
     */
    val queryParams: Map<String, String>
        get() {
            val querySection = currentHash.substringAfter("?", "")
            if (querySection.isBlank()) return emptyMap()

            val map = mutableMapOf<String, String>()
            querySection.split("&").forEach { pair ->
                val tokens = pair.split("=")
                if (tokens.size == 2) {
                    try {
                        map[Uri.decode(tokens[0])] = Uri.decode(tokens[1])
                    } catch (e: Exception) {
                        map[tokens[0]] = tokens[1]
                    }
                }
            }
            return map
        }

    fun getQueryParam(key: String): String? = queryParams[key]

    /**
     * Navigates to a new Hash route, recording into history for back-stack traversal.
     */
    fun navigate(hashUrl: String) {
        val trimmed = hashUrl.trim()
        val sanitized = if (trimmed.startsWith("#/")) {
            trimmed
        } else if (trimmed.startsWith("#")) {
            "#/" + trimmed.removePrefix("#").trimStart('/')
        } else if (trimmed.startsWith("/")) {
            "#$trimmed"
        } else {
            "#/$trimmed"
        }
        if (sanitized == currentHash) return

        transitionDirection = NavTransitionDirection.FORWARD
        backStack.add(sanitized)
        currentHash = sanitized
    }

    /**
     * Replaces the current Hash route without adding a new history entry.
     */
    fun replace(hashUrl: String) {
        val trimmed = hashUrl.trim()
        val sanitized = if (trimmed.startsWith("#/")) {
            trimmed
        } else if (trimmed.startsWith("#")) {
            "#/" + trimmed.removePrefix("#").trimStart('/')
        } else if (trimmed.startsWith("/")) {
            "#$trimmed"
        } else {
            "#/$trimmed"
        }
        if (sanitized == currentHash) return

        transitionDirection = NavTransitionDirection.REPLACE
        if (backStack.isNotEmpty()) {
            backStack[backStack.size - 1] = sanitized
        } else {
            backStack.add(sanitized)
        }
        currentHash = sanitized
    }

    /**
     * Pops the top route from history, moving to previous route with backward transition.
     */
    fun popBackStack(): Boolean {
        if (backStack.size > 1) {
            transitionDirection = NavTransitionDirection.BACKWARD
            backStack.removeAt(backStack.size - 1)
            currentHash = backStack.last()
            return true
        }
        return false
    }

    /**
     * Resets the router to the role default root dashboard.
     */
    fun navigateToRoleRoot(role: UserRole) {
        val rootHash = when (role) {
            UserRole.SUPER_ADMIN -> Routes.SUPER_ADMIN_HUB
            UserRole.STUDENT -> Routes.STUDENT_CAMPUS
            UserRole.FACULTY -> Routes.FACULTY_COMMAND
            UserRole.HOD -> Routes.HOD_DESK
            UserRole.PRINCIPAL, UserRole.PRINCIPAL_ADMIN -> Routes.PRINCIPAL_EXECUTIVE
            UserRole.PARENT -> Routes.PARENT_HUB
            UserRole.ADMIN -> Routes.ADMIN_HUB
        }
        transitionDirection = NavTransitionDirection.REPLACE
        backStack.clear()
        backStack.add(rootHash)
        currentHash = rootHash
    }

    companion object Routes {
        // Super Admin Routes
        const val SUPER_ADMIN_HUB = "#/super_admin/hub"
        const val SUPER_ADMIN_AI = "#/super_admin/chat"

        // Student Routes
        const val STUDENT_CAMPUS = "#/student/campus"
        const val STUDENT_ATTENDANCE = "#/student/attendance"
        const val STUDENT_RISK = "#/student/risk"
        const val STUDENT_AI = "#/student/chat"

        // Faculty Routes
        const val FACULTY_COMMAND = "#/faculty/command"
        const val FACULTY_SUMMARY = "#/faculty/summary"
        const val FACULTY_GROUP_SCAN = "#/faculty/group_scan"
        const val FACULTY_MANUAL_LEDGER = "#/faculty/manual_ledger"
        const val FACULTY_ADVISOR = "#/faculty/chat"
        const val FACULTY_CAMPUS = "#/faculty/campus"

        // HOD Routes
        const val HOD_DESK = "#/hod/desk"
        const val HOD_BELOW_75 = "#/hod/below_75"
        const val HOD_AI = "#/hod/chat"
        const val HOD_CAMPUS = "#/hod/campus"

        // Principal / Dean Routes
        const val PRINCIPAL_EXECUTIVE = "#/principal/executive"
        const val PRINCIPAL_FACULTY = "#/principal/faculty"
        const val PRINCIPAL_BIOMETRICS = "#/principal/faculty"
        const val PRINCIPAL_RESEARCH = "#/principal/research"
        const val PRINCIPAL_GOVERNANCE = "#/principal/governance"
        const val PRINCIPAL_CAMPUS = "#/principal/campus"
        const val PRINCIPAL_AI = "#/principal/chat"

        // Parent Routes
        const val PARENT_HUB = "#/parent/hub"
        const val PARENT_RISK = "#/parent/risk"
        const val PARENT_CONNECT = "#/parent/connect"
        const val PARENT_CAMPUS = "#/parent/campus"

        // Admin Routes
        const val ADMIN_HUB = "#/admin/hub"
        const val ADMIN_ISSUES = "#/admin/issues"
        const val ADMIN_AI = "#/admin/chat"
        const val ADMIN_CAMPUS = "#/admin/campus"

        // Shared Routes
        const val CHAT = "#/chat"
        const val EXAMS = "#/shared/exams"
        const val POMODORO = "#/pomodoro"
        const val AUTH_LOGIN = "#/auth/login"

        fun buildChatRoute(role: UserRole, prompt: String? = null): String {
            val encodedPrompt = prompt?.let { Uri.encode(it) } ?: ""
            val roleRoute = when (role) {
                UserRole.SUPER_ADMIN -> "/super_admin/chat"
                UserRole.ADMIN -> "/admin/chat"
                UserRole.PRINCIPAL, UserRole.PRINCIPAL_ADMIN -> "/principal/chat"
                UserRole.HOD -> "/hod/chat"
                UserRole.FACULTY -> "/faculty/chat"
                UserRole.STUDENT -> "/student/chat"
                UserRole.PARENT -> "/chat"
            }
            return if (encodedPrompt.isNotEmpty()) {
                "#$roleRoute?role=${role.name}&prompt=$encodedPrompt"
            } else {
                "#$roleRoute?role=${role.name}"
            }
        }

        fun buildGroupScanRoute(subject: String): String {
            return "#/faculty/group_scan?subject=${Uri.encode(subject)}"
        }
    }
}

/**
 * Remember helper for HashRouter.
 */
@Composable
fun rememberHashRouter(initialHash: String = "#/student/campus"): HashRouter {
    return remember { HashRouter(initialHash) }
}

/**
 * Animated Transition Host container for HashRouter routes.
 * Animates screen changes with smooth directional sliding and opacity fades.
 */
@Composable
fun HashRouterHost(
    router: HashRouter,
    modifier: Modifier = Modifier,
    content: @Composable (currentPath: String, params: Map<String, String>) -> Unit
) {
    AnimatedContent(
        targetState = router.currentHash,
        transitionSpec = {
            when (router.transitionDirection) {
                NavTransitionDirection.FORWARD -> {
                    (slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(240)))
                        .togetherWith(
                            slideOutHorizontally(
                                targetOffsetX = { -it / 4 },
                                animationSpec = tween(260, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(200))
                        )
                }
                NavTransitionDirection.BACKWARD -> {
                    (slideInHorizontally(
                        initialOffsetX = { -it / 4 },
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(240)))
                        .togetherWith(
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(260, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(200))
                        )
                }
                NavTransitionDirection.REPLACE -> {
                    fadeIn(animationSpec = tween(220))
                        .togetherWith(fadeOut(animationSpec = tween(180)))
                }
            }
        },
        label = "hash_router_transition",
        modifier = modifier
    ) { targetHash ->
        val rawPath = targetHash.trim().removePrefix("#").substringBefore("?").trim().trimStart('/')
        val cleanPath = "/$rawPath"
        val querySection = targetHash.substringAfter("?", "")
        val params = mutableMapOf<String, String>()
        if (querySection.isNotBlank()) {
            querySection.split("&").forEach { pair ->
                val tokens = pair.split("=")
                if (tokens.size == 2) {
                    try {
                        params[Uri.decode(tokens[0])] = Uri.decode(tokens[1])
                    } catch (_: Exception) {
                        params[tokens[0]] = tokens[1]
                    }
                }
            }
        }
        content(cleanPath, params)
    }
}

/**
 * HashBreadcrumbBar:
 * Sleek, interactive Material 3 breadcrumb pill showing current HashRouter URL route.
 * Includes back navigation action and visual hash route indicator.
 */
@Composable
fun HashBreadcrumbBar(
    router: HashRouter,
    activeRole: UserRole,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val roleColor = when (activeRole) {
        UserRole.STUDENT -> StudentAccent
        UserRole.FACULTY -> FacultyAccent
        UserRole.HOD -> HodAccent
        UserRole.PRINCIPAL, UserRole.PRINCIPAL_ADMIN -> PrincipalAccent
        UserRole.PARENT -> ParentAccent
        UserRole.ADMIN -> AdminAccent
        UserRole.SUPER_ADMIN -> AdminAccent
    }

    Surface(
        color = if (isDarkMode) ObsidianSurface.copy(alpha = 0.92f) else LightSurfaceHigh.copy(alpha = 0.95f),
        shape = RoundedCornerShape(Spacing.sm),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDarkMode) ObsidianBorder else Color(0xFFCBD5E1)
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.xxs)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (router.canGoBack) {
                    IconButton(
                        onClick = { router.popBackStack() },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("hashrouter_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = roleColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.xs))
                } else {
                    Icon(
                        imageVector = Icons.Default.Tag,
                        contentDescription = "Hash Route",
                        tint = roleColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                }

                Text(
                    text = router.currentHash,
                    color = if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                    fontSize = 11.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.testTag("hashrouter_current_path")
                )
            }

            // Role indicator chip
            Surface(
                color = roleColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(Spacing.sm),
                border = androidx.compose.foundation.BorderStroke(1.dp, roleColor.copy(alpha = 0.35f))
            ) {
                Text(
                    text = activeRole.displayName.take(14),
                    color = roleColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp)
                )
            }
        }
    }
}

