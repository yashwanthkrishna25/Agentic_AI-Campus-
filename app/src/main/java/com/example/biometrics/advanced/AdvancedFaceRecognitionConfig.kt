package com.example.biometrics.advanced

import com.example.model.UserRole

/**
 * Advanced Face Recognition Engine Configuration.
 * Centralized configuration for production-grade face attendance.
 * All thresholds are tunable via backend or remote config.
 */
object AdvancedFaceRecognitionConfig {

    // Model selection
    const val FACE_DETECTION_MODEL = "mlkit_v2"          // Google ML Kit Face Detection
    const val FACE_EMBEDDING_MODEL = "arcface_resnet50"  // Advanced embedding model (configurable)
    const val FACE_DETECTOR_BACKEND = "opencv_dnn"       // OpenCV DNN backend

    // Enrollment requirements
    const val MIN_ENROLLMENT_FRAMES = 8
    const val MAX_ENROLLMENT_FRAMES = 15
    const val MIN_ENROLLMENT_CONFIDENCE = 0.75f
    const val REQUIRED_LIVENESS_FOR_ENROLLMENT = true

    // Recognition thresholds
    const val COSINE_MATCH_THRESHOLD = 0.72f
    const val COSINE_AMBIGUOUS_THRESHOLD = 0.60f
    const val EUCLIDEAN_MATCH_THRESHOLD = 0.85f
    const val EUCLIDEAN_AMBIGUOUS_THRESHOLD = 0.75f
    const val REQUIRE_BOTH_METRICS = true

    // Liveness requirements for attendance
    const val REQUIRED_LIVENESS_FOR_ATTENDANCE = true
    const val MIN_LIVENESS_CONFIDENCE = 0.80f

    // Quality gates
    const val MIN_FACE_QUALITY_SCORE = 70.0f   // 0-100, below this rejects capture
    const val MAX_RECOGNITION_DISTANCE_M = 2.0f // Max distance from camera (approx)

    // Role-based permissions
    val ROLE_CAN_ENROLL = setOf(UserRole.FACULTY, UserRole.ADMIN, UserRole.SUPER_ADMIN)
    val ROLE_CAN_RECOGNIZE = setOf(UserRole.FACULTY, UserRole.HOD, UserRole.ADMIN, UserRole.SUPER_ADMIN)
    val ROLE_CAN_DELETE_TEMPLATE = setOf(UserRole.ADMIN, UserRole.SUPER_ADMIN)

    // Template versioning
    const val TEMPLATE_MODEL_VERSION = "arcface_v2_multi_frame"
    const val TEMPLATE_DIMENSION = 128

    // Anti-spoofing
    const val LIVENESS_MODEL = "micro_motion_texture_blink"
    const val ANTI_SPOOFING_ENABLED = true

    /**
     * Returns the appropriate match threshold based on security level.
     */
    fun getCosineThreshold(strict: Boolean = true): Float =
        if (strict) COSINE_MATCH_THRESHOLD else COSINE_AMBIGUOUS_THRESHOLD

    /**
     * Returns whether a role is allowed to enroll face templates.
     */
    fun canEnroll(role: UserRole): Boolean = ROLE_CAN_ENROLL.contains(role)

    /**
     * Returns whether a role is allowed to run face recognition.
     */
    fun canRecognize(role: UserRole): Boolean = ROLE_CAN_RECOGNIZE.contains(role)

    /**
     * Returns whether a role is allowed to delete face templates.
     */
    fun canDeleteTemplate(role: UserRole): Boolean = ROLE_CAN_DELETE_TEMPLATE.contains(role)
}
