package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.agent.AcademicRiskAgent
import com.example.agent.AdvisorAgent
import com.example.agent.AttendanceAgent
import com.example.model.AcademicSurveyData
import com.example.model.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AGENTRA AI CAMPUS", appName)
    val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.agentra_logo)
    assertTrue("agentra_logo drawable should be loadable", drawable != null)
  }

  @Test
  fun `verify attendance agent multi face group scan`() {
    val faces = AttendanceAgent.performGroupSectionScan(45)
    assertTrue("Should detect at least 40 faces in section", faces.size >= 40)
    assertTrue("Most faces should be enrolled", faces.count { it.isEnrolled } >= 35)
  }

  @Test
  fun `verify advisor agent 1-month recovery plan calculation`() {
    // Current attendance 70 out of 100 is 70% (<75%)
    val plan = AdvisorAgent.calculateAttendanceCatchupPlan(70, 100)
    assertEquals(70.0, plan.currentPercentage, 0.01)
    // Formula: ceil((0.75*100 - 70) / 0.25) = (75 - 70) / 0.25 = 20
    assertEquals(20, plan.sessionsNeededWithoutAbsence)
    assertTrue("Should have milestones", plan.roadmapMilestones.isNotEmpty())
  }

  @Test
  fun `verify academic risk agent high risk classification`() {
    // High stress (5), low CGPA (1), low study (1), low attendance (1)
    val survey = AcademicSurveyData(stressLevel = 5, cgpaBand = 1, studyConsistency = 1, attendanceBand = 1)
    val result = AcademicRiskAgent.evaluateAcademicRisk(survey)
    assertEquals(RiskLevel.HIGH, result.riskLevel)
    assertTrue("High risk probability should dominate", result.probabilityHigh > 0.8)
  }

  @Test
  fun `verify parent role and session persistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.data.SessionManager.init(context)
    val parentProfile = com.example.data.AcademicDataProvider.defaultParentProfile
    com.example.data.SessionManager.saveProfile(parentProfile)
    assertTrue("Session should indicate logged in", com.example.data.SessionManager.isLoggedIn())
    val profile = com.example.data.SessionManager.getCurrentProfile()
    assertEquals(com.example.model.UserRole.PARENT, profile.role)
    assertEquals("PAR-239Y1A0501", profile.rollNumber)
  }
}
