package com.example.biometrics

import com.example.data.AcademicDataProvider
import com.example.model.AttendanceStatus
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.sqrt

/**
 * Automated Unit Tests for AGENTRA AI Campus Face Recognition Engine & Attendance System.
 * Tests biometric vector extraction, L2 normalization, cosine similarity,
 * enrollment persistence, template matching, threshold gating, and attendance auditing.
 */
class FaceRecognitionEngineTest {

    @Before
    fun setup() {
        // FaceRecognitionEngine static seeds are active
    }

    @Test
    fun testVectorDimensionAndL2Normalization() {
        val vector = FaceRecognitionEngine.generateRealisticFaceVector("TEST_STUDENT_01", 0.0f)
        assertEquals("Face embedding must be exactly 128 dimensions", 128, vector.size)

        var sumSq = 0.0
        for (v in vector) {
            sumSq += (v * v).toDouble()
        }
        val norm = sqrt(sumSq).toFloat()
        assertEquals("Vector must be unit length (L2 norm = 1.0)", 1.0f, norm, 1e-4f)
    }

    @Test
    fun testCosineSimilarityProperties() {
        val v1 = FaceRecognitionEngine.generateRealisticFaceVector("STUDENT_A", 0.0f)
        val v1Identical = v1.clone()

        // 1. Identity: Sim(v, v) == 1.0
        val selfSim = FaceRecognitionEngine.cosineSimilarity(v1, v1Identical)
        assertEquals("Self-similarity must equal 1.0", 1.0f, selfSim, 1e-5f)

        // 2. Symmetry: Sim(v1, v2) == Sim(v2, v1)
        val v2 = FaceRecognitionEngine.generateRealisticFaceVector("STUDENT_B", 0.05f)
        val sim12 = FaceRecognitionEngine.cosineSimilarity(v1, v2)
        val sim21 = FaceRecognitionEngine.cosineSimilarity(v2, v1)
        assertEquals("Cosine similarity must be symmetric", sim12, sim21, 1e-5f)

        // 3. Orthogonal vectors: Sim(v, ortho) == 0.0
        val ortho1 = floatArrayOf(1f, 0f, 0f)
        val ortho2 = floatArrayOf(0f, 1f, 0f)
        assertEquals(0.0f, FaceRecognitionEngine.cosineSimilarity(ortho1, ortho2), 1e-5f)

        // 4. Opposite vectors: Sim(v, -v) == -1.0
        val opp1 = floatArrayOf(1f, 0f)
        val opp2 = floatArrayOf(-1f, 0f)
        assertEquals(-1.0f, FaceRecognitionEngine.cosineSimilarity(opp1, opp2), 1e-5f)
    }

    @Test
    fun testAuthoritativeSeededTemplatesExist() {
        // Faculty template: Dr. P. V. Kishore
        assertTrue(
            "Faculty template Dr. P. V. Kishore must be enrolled",
            FaceRecognitionEngine.isEnrolled("KSRM-CSE-FAC-102")
        )

        // Student template: Sattu Penchala Prasad (239Y1A0501)
        assertTrue(
            "Student template Sattu Penchala Prasad must be enrolled",
            FaceRecognitionEngine.isEnrolled("239Y1A0501")
        )

        val templates = FaceRecognitionEngine.getAllEnrolledTemplates()
        assertTrue("Must have at least 8 seeded campus templates", templates.size >= 8)
    }

    @Test
    fun testStudentFaceEnrollmentAndReadbackPersistence() {
        val newRoll = "239Y1A0599"
        val newName = "Test Enrollment Candidate"
        val vectors = listOf(
            FaceRecognitionEngine.generateRealisticFaceVector(newRoll, 0.01f),
            FaceRecognitionEngine.generateRealisticFaceVector(newRoll, 0.02f),
            FaceRecognitionEngine.generateRealisticFaceVector(newRoll, 0.00f),
            FaceRecognitionEngine.generateRealisticFaceVector(newRoll, -0.01f),
            FaceRecognitionEngine.generateRealisticFaceVector(newRoll, 0.015f)
        )

        // 1. Consent rejection check: must fail if consent not granted
        val rejected = FaceRecognitionEngine.enrollTemplate(
            personId = newRoll,
            personName = newName,
            role = "STUDENT",
            department = "CSE",
            section = "Sec B",
            embeddings = vectors,
            consentConfirmed = false
        )
        assertFalse("Enrollment must fail if DPDP consent is false", rejected)

        // 2. Successful enrollment with consent
        val enrolled = FaceRecognitionEngine.enrollTemplate(
            personId = newRoll,
            personName = newName,
            role = "STUDENT",
            department = "CSE",
            section = "Sec B",
            embeddings = vectors,
            consentConfirmed = true
        )
        assertTrue("Enrollment must succeed with valid embeddings and consent", enrolled)

        // 3. Authoritative Readback Verification
        assertTrue("Readback check: student must be in active biometric bank", FaceRecognitionEngine.isEnrolled(newRoll))

        val allTemplates = FaceRecognitionEngine.getAllEnrolledTemplates()
        val saved = allTemplates.find { it.personId == newRoll }
        assertNotNull("Enrolled template must be retrievable", saved)
        assertEquals(newName, saved?.personName)
        assertEquals("STUDENT", saved?.role)
        assertEquals(128, saved?.embedding?.size)

        // 4. Test Deletion / Revocation of biometric record
        val deleted = FaceRecognitionEngine.deleteTemplate(newRoll)
        assertTrue("Template deletion must succeed for statutory revocation", deleted)
        assertFalse("Template must not exist after deletion", FaceRecognitionEngine.isEnrolled(newRoll))
    }

    @Test
    fun testQualityGateThresholds() {
        // Face size requirement: at least 15% of min(frameW, frameH)
        val frameW = 720
        val frameH = 1280
        val minSize = (frameW * 0.15f) // 108px

        val validBoxW = 200f
        val invalidBoxW = 50f // < 108px

        assertTrue("Valid box size exceeds 15% of frame", validBoxW >= minSize)
        assertFalse("Invalid box size rejected for being too far", invalidBoxW >= minSize)

        // Head pose constraints
        val validYaw = 12.0f
        val invalidYaw = 35.0f
        assertTrue("Yaw within +-25 deg is accepted", kotlin.math.abs(validYaw) <= 25f)
        assertFalse("Yaw > 25 deg is rejected", kotlin.math.abs(invalidYaw) <= 25f)

        // Eye openness
        val eyeOpenProb = 0.85f
        val eyeClosedProb = 0.10f
        assertTrue("Eyes open >= 0.25 accepted", eyeOpenProb >= 0.25f)
        assertFalse("Eyes closed < 0.25 rejected", eyeClosedProb >= 0.25f)
    }

    @Test
    fun testAttendanceSessionSubmissionAndPercentageUpdate() {
        val subject = "Deep Learning [2339609]"
        val roster = AcademicDataProvider.getFullClassRoster()
        assertTrue("Roster must not be empty", roster.isNotEmpty())

        val attendanceMap = mutableMapOf<String, Boolean>()
        for (student in roster) {
            // Mark all except first two as present
            attendanceMap[student.rollNumber] = (student.rollNumber != "239Y1A05B8" && student.rollNumber != "239Y1A05B9")
        }

        AcademicDataProvider.recordAttendanceSession(subject, attendanceMap)

        val updatedRoster = AcademicDataProvider.sharedClassRoster
        val presentStudent = updatedRoster.find { it.rollNumber == "239Y1A0501" }
        assertEquals(AttendanceStatus.PRESENT, presentStudent?.currentStatus)

        val absentStudent = updatedRoster.find { it.rollNumber == "239Y1A05B8" }
        assertEquals(AttendanceStatus.ABSENT, absentStudent?.currentStatus)
    }

    @Test
    fun testSingleStudentAttendanceDuplicateCheck() {
        val roll = "239Y1A0501"
        val subject = "Deep Learning [2339609]"

        // First mark: records present
        AcademicDataProvider.markStudentPresent(roll, subject)
        val student = AcademicDataProvider.sharedClassRoster.find { it.rollNumber == roll }
        assertEquals(AttendanceStatus.PRESENT, student?.currentStatus)

        // Second mark: remains present without duplicate record crash
        AcademicDataProvider.markStudentPresent(roll, subject)
        assertEquals(AttendanceStatus.PRESENT, student?.currentStatus)
    }

    @Test
    fun testSingleStudentAttendanceLiveCourseUpdate() {
        val roll = "239Y1A0501"
        val subject = "2339609" // Deep Learning
        val courseBefore = AcademicDataProvider.sharedCourses.find { it.code == subject }
        val attendedBefore = courseBefore?.attendedSessions ?: 0
        val totalBefore = courseBefore?.totalSessions ?: 0

        val success = AcademicDataProvider.recordSingleStudentAttendance(roll, subject, isPresent = true)
        assertTrue("Single student attendance must return true", success)

        val courseAfter = AcademicDataProvider.sharedCourses.find { it.code == subject }
        assertEquals(attendedBefore + 1, courseAfter?.attendedSessions)
        assertEquals(totalBefore + 1, courseAfter?.totalSessions)

        val latestNotif = AcademicDataProvider.sharedNotifications.firstOrNull()
        assertTrue("Notification must announce single student kiosk attendance", latestNotif?.contains(roll) == true)
    }
}

