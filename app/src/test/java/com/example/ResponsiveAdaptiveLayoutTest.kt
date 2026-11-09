package com.example

import androidx.compose.ui.unit.dp
import com.example.data.AcademicDataProvider
import com.example.ui.responsive.DevicePosture
import com.example.ui.responsive.WindowAdaptiveInfo
import com.example.ui.responsive.WindowHeightClass
import com.example.ui.responsive.WindowWidthClass
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying the AGENTRA AI Campus Responsive Adaptive Layout System
 * across all screen sizes, device postures, orientations, and role profiles.
 */
class ResponsiveAdaptiveLayoutTest {

    @Test
    fun testSmallPhoneBreakpoints() {
        // 320dp small phone portrait (e.g., iPhone SE 1st gen, small Android Go)
        val info320 = WindowAdaptiveInfo(
            widthClass = WindowWidthClass.COMPACT,
            heightClass = WindowHeightClass.MEDIUM,
            posture = DevicePosture.SMALL_PHONE,
            screenWidthDp = 320.dp,
            screenHeightDp = 640.dp,
            isLandscape = false,
            isSmallScreen = true
        )

        assertTrue(info320.isSmallScreen)
        assertFalse(info320.isMediumOrExpanded)
        assertFalse(info320.useNavigationRail)
        assertEquals(1, info320.dashboardColumns)
        assertEquals(10.dp, info320.contentHorizontalPadding)

        // 360dp phone portrait (standard small/compact Android device)
        val info360 = WindowAdaptiveInfo(
            widthClass = WindowWidthClass.COMPACT,
            heightClass = WindowHeightClass.MEDIUM,
            posture = DevicePosture.SMALL_PHONE,
            screenWidthDp = 360.dp,
            screenHeightDp = 800.dp,
            isLandscape = false,
            isSmallScreen = true
        )

        assertTrue(info360.isSmallScreen)
        assertFalse(info360.isMediumOrExpanded)
        assertFalse(info360.useNavigationRail)
        assertEquals(1, info360.dashboardColumns)
    }

    @Test
    fun testNormalPhoneBreakpoints() {
        // 393dp standard smartphone portrait (e.g. Pixel 7 / Pixel 8, iPhone 14/15)
        val info393 = WindowAdaptiveInfo(
            widthClass = WindowWidthClass.COMPACT,
            heightClass = WindowHeightClass.MEDIUM,
            posture = DevicePosture.NORMAL_PHONE,
            screenWidthDp = 393.dp,
            screenHeightDp = 852.dp,
            isLandscape = false,
            isSmallScreen = false
        )

        assertFalse(info393.isSmallScreen)
        assertFalse(info393.isMediumOrExpanded)
        assertFalse(info393.useNavigationRail)
        assertEquals(1, info393.dashboardColumns)
        assertEquals(16.dp, info393.contentHorizontalPadding)
    }

    @Test
    fun testLargePhoneBreakpoints() {
        // 430dp large smartphone / phablet portrait (e.g. Pixel 8 Pro, Galaxy S24 Ultra)
        val info430 = WindowAdaptiveInfo(
            widthClass = WindowWidthClass.COMPACT,
            heightClass = WindowHeightClass.EXPANDED,
            posture = DevicePosture.LARGE_PHONE,
            screenWidthDp = 430.dp,
            screenHeightDp = 932.dp,
            isLandscape = false,
            isSmallScreen = false
        )

        assertFalse(info430.isSmallScreen)
        assertFalse(info430.isMediumOrExpanded)
        assertFalse(info430.useNavigationRail)
        assertEquals(2, info430.dashboardColumns)
        assertEquals(16.dp, info430.contentHorizontalPadding)
    }

    @Test
    fun testLandscapePhoneBreakpoints() {
        // Phone in landscape mode (e.g., 852dp width x 393dp height)
        val infoLandscape = WindowAdaptiveInfo(
            widthClass = WindowWidthClass.EXPANDED,
            heightClass = WindowHeightClass.COMPACT,
            posture = DevicePosture.FOLDABLE_OR_TABLET,
            screenWidthDp = 852.dp,
            screenHeightDp = 393.dp,
            isLandscape = true,
            isSmallScreen = false
        )

        assertTrue(infoLandscape.isLandscape)
        assertTrue(infoLandscape.isMediumOrExpanded)
        assertTrue(infoLandscape.useNavigationRail) // Side rail avoids bottom bar blocking content in landscape
        assertEquals(4, infoLandscape.dashboardColumns)
    }

    @Test
    fun testMediumTabletBreakpoints() {
        // 700dp foldable / small tablet (e.g. iPad Mini, Galaxy Fold unfolded)
        val info700 = WindowAdaptiveInfo(
            widthClass = WindowWidthClass.MEDIUM,
            heightClass = WindowHeightClass.MEDIUM,
            posture = DevicePosture.FOLDABLE_OR_TABLET,
            screenWidthDp = 700.dp,
            screenHeightDp = 900.dp,
            isLandscape = false,
            isSmallScreen = false
        )

        assertTrue(info700.isMediumOrExpanded)
        assertTrue(info700.useNavigationRail)
        assertEquals(3, info700.dashboardColumns)
        assertEquals(24.dp, info700.contentHorizontalPadding)
    }

    @Test
    fun testExpandedLargeTabletBreakpoints() {
        // 1024dp large tablet / desktop
        val info1024 = WindowAdaptiveInfo(
            widthClass = WindowWidthClass.EXPANDED,
            heightClass = WindowHeightClass.EXPANDED,
            posture = DevicePosture.FOLDABLE_OR_TABLET,
            screenWidthDp = 1024.dp,
            screenHeightDp = 1366.dp,
            isLandscape = false,
            isSmallScreen = false
        )

        assertTrue(info1024.isMediumOrExpanded)
        assertTrue(info1024.useNavigationRail)
        assertEquals(4, info1024.dashboardColumns)
        assertEquals(32.dp, info1024.contentHorizontalPadding)
        assertEquals(1140.dp, info1024.maxContentWidth)
    }

    @Test
    fun testStudentIdentityVerification() {
        // Verify student identity is preserved as Sattu Penchala Prasad with roll number 239Y1A0501
        val defaultStudent = AcademicDataProvider.defaultStudentProfile
        assertEquals("Sattu Penchala Prasad", defaultStudent.name)
        assertEquals("239Y1A0501", defaultStudent.rollNumber)

        val roster = AcademicDataProvider.getFullClassRoster()
        val found = roster.find { it.rollNumber == "239Y1A0501" }
        assertNotNull(found)
        assertEquals("Sattu Penchala Prasad", found?.name)
    }
}
