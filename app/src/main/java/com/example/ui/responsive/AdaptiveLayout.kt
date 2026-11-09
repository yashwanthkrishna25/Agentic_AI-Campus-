package com.example.ui.responsive

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standard Material 3 Window Width Size Classes.
 * - COMPACT: Width < 600.dp (typical portrait phones)
 * - MEDIUM: 600.dp <= Width < 840.dp (small tablets, foldables, landscape phones)
 * - EXPANDED: Width >= 840.dp (large tablets, desktop/docked environments)
 */
enum class WindowWidthClass {
    COMPACT,
    MEDIUM,
    EXPANDED
}

/**
 * Standard Material 3 Window Height Size Classes.
 * - COMPACT: Height < 480.dp (landscape phones)
 * - MEDIUM: 480.dp <= Height < 900.dp (typical portrait phones, small tablets)
 * - EXPANDED: Height >= 900.dp (tall phones, large tablets in portrait)
 */
enum class WindowHeightClass {
    COMPACT,
    MEDIUM,
    EXPANDED
}

/**
 * Detailed device classification for adaptive phone, foldable, and tablet UI decisions.
 */
enum class DevicePosture {
    SMALL_PHONE,       // < 375dp width (e.g. 320dp - 360dp narrow screens)
    NORMAL_PHONE,      // 375dp - 430dp width (standard modern smartphones)
    LARGE_PHONE,       // 430dp - 600dp width (large flagships, phablets)
    FOLDABLE_OR_TABLET // >= 600dp width
}

/**
 * Comprehensive snapshot of the current window configuration and adaptive properties.
 */
@Immutable
data class WindowAdaptiveInfo(
    val widthClass: WindowWidthClass,
    val heightClass: WindowHeightClass,
    val posture: DevicePosture,
    val screenWidthDp: Dp,
    val screenHeightDp: Dp,
    val isLandscape: Boolean,
    val isSmallScreen: Boolean
) {
    /**
     * Whether the screen is medium or expanded width (tablets, desktops, landscape foldables).
     */
    val isMediumOrExpanded: Boolean
        get() = widthClass != WindowWidthClass.COMPACT

    /**
     * Recommended outer horizontal margin for screens:
     * - Small phone: 12.dp
     * - Normal/Large phone: 16.dp
     * - Medium/Tablet: 24.dp
     * - Expanded: 32.dp
     */
    val contentHorizontalPadding: Dp
        get() = when (widthClass) {
            WindowWidthClass.COMPACT -> if (isSmallScreen) 10.dp else 16.dp
            WindowWidthClass.MEDIUM -> 24.dp
            WindowWidthClass.EXPANDED -> 32.dp
        }

    /**
     * Recommended dashboard grid column count:
     * - Narrow phone (< 440dp): 1 column (or 2 for compact stat chips)
     * - Wide phone / landscape phone: 2 columns
     * - Medium / Tablet (600 - 840dp): 2 to 3 columns
     * - Expanded tablet (> 840dp): 3 to 4 columns
     */
    val dashboardColumns: Int
        get() = when {
            widthClass == WindowWidthClass.EXPANDED -> 4
            widthClass == WindowWidthClass.MEDIUM -> 3
            isLandscape -> 2
            posture == DevicePosture.LARGE_PHONE -> 2
            else -> 1
        }

    /**
     * Whether navigation should switch to a NavigationRail (left side) rather than Bottom Navigation.
     * Triggered on tablets (width >= 600dp) or landscape orientation where vertical height is limited.
     */
    val useNavigationRail: Boolean
        get() = widthClass != WindowWidthClass.COMPACT || (isLandscape && heightClass == WindowHeightClass.COMPACT)

    /**
     * Maximum width constraint for readable content (prevents cards from stretching over 1200dp).
     */
    val maxContentWidth: Dp
        get() = when (widthClass) {
            WindowWidthClass.COMPACT -> Dp.Unspecified
            WindowWidthClass.MEDIUM -> 760.dp
            WindowWidthClass.EXPANDED -> 1140.dp
        }
}

/**
 * CompositionLocal providing access to the current [WindowAdaptiveInfo].
 */
val LocalWindowAdaptiveInfo = staticCompositionLocalOf {
    WindowAdaptiveInfo(
        widthClass = WindowWidthClass.COMPACT,
        heightClass = WindowHeightClass.MEDIUM,
        posture = DevicePosture.NORMAL_PHONE,
        screenWidthDp = 393.dp,
        screenHeightDp = 852.dp,
        isLandscape = false,
        isSmallScreen = false
    )
}

/**
 * Calculates and returns the current [WindowAdaptiveInfo] based on LocalConfiguration.
 */
@Composable
fun rememberWindowAdaptiveInfo(): WindowAdaptiveInfo {
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp.dp
    val heightDp = configuration.screenHeightDp.dp
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val widthClass = when {
        widthDp < 600.dp -> WindowWidthClass.COMPACT
        widthDp < 840.dp -> WindowWidthClass.MEDIUM
        else -> WindowWidthClass.EXPANDED
    }

    val heightClass = when {
        heightDp < 480.dp -> WindowHeightClass.COMPACT
        heightDp < 900.dp -> WindowHeightClass.MEDIUM
        else -> WindowHeightClass.EXPANDED
    }

    val posture = when {
        widthDp < 375.dp -> DevicePosture.SMALL_PHONE
        widthDp < 430.dp -> DevicePosture.NORMAL_PHONE
        widthDp < 600.dp -> DevicePosture.LARGE_PHONE
        else -> DevicePosture.FOLDABLE_OR_TABLET
    }

    val isSmallScreen = posture == DevicePosture.SMALL_PHONE

    return remember(widthDp, heightDp, isLandscape) {
        WindowAdaptiveInfo(
            widthClass = widthClass,
            heightClass = heightClass,
            posture = posture,
            screenWidthDp = widthDp,
            screenHeightDp = heightDp,
            isLandscape = isLandscape,
            isSmallScreen = isSmallScreen
        )
    }
}

/**
 * Convenient alias for [rememberWindowAdaptiveInfo].
 */
@Composable
fun rememberAdaptiveLayoutInfo(): WindowAdaptiveInfo = rememberWindowAdaptiveInfo()

/**
 * Wrapper that centers content and enforces max readable width constraints on tablets/foldables,
 * while expanding naturally to fill available width on phones.
 */
@Composable
fun ResponsiveContainer(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 1140.dp,
    horizontalPadding: Dp? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val adaptiveInfo = LocalWindowAdaptiveInfo.current
    val effectivePadding = horizontalPadding ?: adaptiveInfo.contentHorizontalPadding

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = effectivePadding),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (adaptiveInfo.widthClass != WindowWidthClass.COMPACT && maxWidth != Dp.Unspecified) {
                        Modifier.widthIn(max = maxWidth)
                    } else {
                        Modifier
                    }
                ),
            content = content
        )
    }
}

/**
 * Adaptive two-pane layout:
 * - On Compact portrait screens: displays [primaryPane] stacked above [secondaryPane].
 * - On Medium/Expanded or Landscape screens: displays [primaryPane] and [secondaryPane] side-by-side with proportional weights.
 */
@Composable
fun ResponsiveTwoPane(
    primaryPane: @Composable BoxScope.() -> Unit,
    secondaryPane: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    primaryWeight: Float = 1.15f,
    secondaryWeight: Float = 1f,
    spacing: Dp = 16.dp,
    forceSideBySide: Boolean = false
) {
    val adaptiveInfo = LocalWindowAdaptiveInfo.current
    val showSideBySide = forceSideBySide || adaptiveInfo.widthClass != WindowWidthClass.COMPACT || adaptiveInfo.isLandscape

    if (showSideBySide) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .weight(primaryWeight)
                    .fillMaxHeight(),
                content = primaryPane
            )
            Box(
                modifier = Modifier
                    .weight(secondaryWeight)
                    .fillMaxHeight(),
                content = secondaryPane
            )
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                content = primaryPane
            )
            Box(
                modifier = Modifier.fillMaxWidth(),
                content = secondaryPane
            )
        }
    }
}

/**
 * Adaptive 2-column or 1-column grid helper for dashboard metric cards.
 * Automatically displays items in 2 columns if space allows (width >= 370dp), or 1 column on tight screens.
 */
@Composable
fun ResponsiveStatRow(
    modifier: Modifier = Modifier,
    spacing: Dp = 10.dp,
    content: @Composable RowScope.() -> Unit
) {
    val adaptiveInfo = LocalWindowAdaptiveInfo.current
    if (adaptiveInfo.isSmallScreen) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            // Stacked on ultra-compact screens
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                content = content
            )
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            content = content
        )
    }
}
