package com.example.agent

import com.example.model.AcademicSurveyData
import com.example.model.RiskLevel
import com.example.model.RiskPredictionResult
import com.example.model.SubjectCourse
import kotlin.math.exp

/**
 * Academic Risk Agent as formulated in Section III-C and Table VII of the paper:
 * Uses the standardized logistic-regression model (Table VII) with four dominant predictors:
 * - Stress level (1 to 5)
 * - CGPA band (1 to 5: 1=<6.0, 2=6-7, 3=7-8, 4=8-9, 5=9-10)
 * - Study consistency (1 to 5)
 * - Attendance band (1 to 5: 1=<65%, 2=65-74%, 3=75-84%, 4=85-94%, 5=95-100%)
 *
 * Accuracy reported: 98.4% with logistic regression and 94.7% with Gradient Boosting on 1,200 survey responses.
 */
object AcademicRiskAgent {

    // Standardized coefficients from Table VII
    private const val BETA_HIGH_STRESS = 4.90
    private const val BETA_HIGH_CGPA = -4.13
    private const val BETA_HIGH_STUDY = -3.10
    private const val BETA_HIGH_ATTENDANCE = -2.73

    private const val BETA_LOW_STRESS = -4.66
    private const val BETA_LOW_CGPA = 3.95
    private const val BETA_LOW_STUDY = 2.96
    private const val BETA_LOW_ATTENDANCE = 2.71

    private const val BETA_MOD_STRESS = -0.24
    private const val BETA_MOD_CGPA = 0.19
    private const val BETA_MOD_STUDY = 0.15
    private const val BETA_MOD_ATTENDANCE = 0.02

    // Population mean & std estimates for standardization (Z-score = (X - mean) / std)
    private const val MEAN_STRESS = 3.0
    private const val STD_STRESS = 1.15

    private const val MEAN_CGPA = 3.2
    private const val STD_CGPA = 1.05

    private const val MEAN_STUDY = 3.1
    private const val STD_STUDY = 1.10

    private const val MEAN_ATTENDANCE = 3.3
    private const val STD_ATTENDANCE = 1.08

    fun evaluateAcademicRisk(survey: AcademicSurveyData): RiskPredictionResult {
        val zStress = (survey.stressLevel - MEAN_STRESS) / STD_STRESS
        val zCgpa = (survey.cgpaBand - MEAN_CGPA) / STD_CGPA
        val zStudy = (survey.studyConsistency - MEAN_STUDY) / STD_STUDY
        val zAttendance = (survey.attendanceBand - MEAN_ATTENDANCE) / STD_ATTENDANCE

        val logitHigh = (BETA_HIGH_STRESS * zStress) + (BETA_HIGH_CGPA * zCgpa) +
                (BETA_HIGH_STUDY * zStudy) + (BETA_HIGH_ATTENDANCE * zAttendance)

        val logitLow = (BETA_LOW_STRESS * zStress) + (BETA_LOW_CGPA * zCgpa) +
                (BETA_LOW_STUDY * zStudy) + (BETA_LOW_ATTENDANCE * zAttendance)

        val logitMod = (BETA_MOD_STRESS * zStress) + (BETA_MOD_CGPA * zCgpa) +
                (BETA_MOD_STUDY * zStudy) + (BETA_MOD_ATTENDANCE * zAttendance)

        // Softmax
        val maxLogit = maxOf(logitHigh, logitLow, logitMod)
        val expHigh = exp(logitHigh - maxLogit)
        val expLow = exp(logitLow - maxLogit)
        val expMod = exp(logitMod - maxLogit)
        val sumExp = expHigh + expLow + expMod

        val pHigh = expHigh / sumExp
        val pLow = expLow / sumExp
        val pMod = expMod / sumExp

        val riskLevel = when {
            pHigh >= 0.50 -> RiskLevel.HIGH
            pLow >= 0.50 -> RiskLevel.LOW
            else -> RiskLevel.MODERATE
        }

        val warnings = mutableListOf<String>()
        if (survey.stressLevel >= 4) {
            warnings.add("Elevated psychological stress index (Level ${survey.stressLevel}/5)")
        }
        if (survey.attendanceBand <= 2) {
            warnings.add("Attendance band is below institution threshold (<75%)")
        }
        if (survey.cgpaBand <= 2) {
            warnings.add("Historical GPA is below 7.0 band")
        }
        if (survey.studyConsistency <= 2) {
            warnings.add("Inconsistent daily revision habits detected")
        }

        val advisorAction = when (riskLevel) {
            RiskLevel.HIGH -> "URGENT: Faculty counselling meeting scheduled. 1-month recovery plan mandated."
            RiskLevel.MODERATE -> "ADVISORY: Monitor attendance consistency and schedule peer study group."
            RiskLevel.LOW -> "STABLE: Strong academic indicators. On track for honors credit."
        }

        return RiskPredictionResult(
            riskLevel = riskLevel,
            probabilityLow = pLow,
            probabilityModerate = pMod,
            probabilityHigh = pHigh,
            score = (pHigh * 100.0),
            warningFactors = warnings,
            advisorAction = advisorAction
        )
    }

    /**
     * Component marks analysis (Table VIII):
     * Evaluates internal components x1-x6 (out of 10) to assess probability of failing (< 40 total).
     */
    fun evaluateComponentMarksRisk(course: SubjectCourse): String {
        val internalSum = course.x1 + course.x2 + course.x3 + course.x4 + course.x5 + course.x6
        // Internal max is 60 points. Passing total is >= 40.
        // If internal is >= 35, passing is nearly guaranteed even with low end-term.
        return when {
            internalSum < 25.0 -> "HIGH RISK of Course Failure (Internal Score: ${"%.1f".format(internalSum)}/60). Early Intervention required."
            internalSum < 38.0 -> "MODERATE RISK: Needs at least 15+ marks in End-Term (x7) to ensure safe passing band."
            else -> "SAFE TRAJECTORY: Strong internal marks (${"%.1f".format(internalSum)}/60). Projected Band: 4 or 5."
        }
    }
}
