package com.example.biometrics.advanced

import kotlin.math.abs

/**
 * Advanced Face Quality Assessment for Attendance Accuracy.
 * Evaluates blur, head pose, lighting, eye aspect ratio, and face size
 * to reject low-quality captures before enrollment or recognition.
 */
object FaceQualityAssessor {

    private const val TAG = "FaceQualityAssessor"

    // Thresholds tuned for attendance accuracy
    const val MIN_FACE_SIZE_RATIO = 0.12f          // Face must be >= 12% of frame width
    const val MAX_BLUR_SCORE = 120.0f              // Laplacian variance threshold (lower = blurrier)
    const val MAX_HEAD_YAW_DEG = 20.0f             // Max left/right head turn
    const val MAX_HEAD_PITCH_DEG = 15.0f           // Max up/down head tilt
    const val MAX_HEAD_ROLL_DEG = 15.0f            // Max head roll
    const val MIN_EYE_ASPECT_RATIO = 0.20f         // Eyes must be open (not blinking/closed)
    const val MIN_LIGHTING_SCORE = 0.30f           // Min average brightness (0-1)
    const val MAX_LIGHTING_SCORE = 0.85f           // Max average brightness (avoid overexposure)

    data class QualityResult(
        val isPass: Boolean,
        val score: Float,                 // 0-100 overall quality score
        val faceSizeRatio: Float,
        val blurScore: Float,
        val yawDeg: Float,
        val pitchDeg: Float,
        val rollDeg: Float,
        val eyeAspectRatio: Float,
        val lightingScore: Float,
        val rejectionReason: String? = null
    )

    /**
     * Comprehensive quality check for a detected face.
     * Returns pass/fail with detailed metrics.
     */
    fun assessQuality(
        faceWidthPx: Float,
        frameWidthPx: Float,
        blurVariance: Float,
        yawDeg: Float,
        pitchDeg: Float,
        rollDeg: Float,
        leftEyeOpenProb: Float,
        rightEyeOpenProb: Float,
        avgBrightness: Float  // 0-1 normalized
    ): QualityResult {
        val faceSizeRatio = faceWidthPx / frameWidthPx
        val eyeAspectRatio = (leftEyeOpenProb + rightEyeOpenProb) / 2f
        val lightingScore = avgBrightness.coerceIn(0f, 1f)

        val reasons = mutableListOf<String>()
        var score = 100f

        // 1. Face size check
        if (faceSizeRatio < MIN_FACE_SIZE_RATIO) {
            reasons.add("Face too small (move closer to camera)")
            score -= 25f
        }

        // 2. Blur check
        if (blurVariance < MAX_BLUR_SCORE) {
            reasons.add("Image too blurry (hold steady)")
            score -= 25f
        }

        // 3. Head pose checks
        if (abs(yawDeg) > MAX_HEAD_YAW_DEG) {
            reasons.add("Head turned too far to side")
            score -= 15f
        }
        if (abs(pitchDeg) > MAX_HEAD_PITCH_DEG) {
            reasons.add("Head tilted up/down too much")
            score -= 10f
        }
        if (abs(rollDeg) > MAX_HEAD_ROLL_DEG) {
            reasons.add("Head rolled too much")
            score -= 10f
        }

        // 4. Eye openness (liveness indicator - no blink)
        if (eyeAspectRatio < MIN_EYE_ASPECT_RATIO) {
            reasons.add("Eyes not fully open (possible blink or spoof)")
            score -= 20f
        }

        // 5. Lighting check
        if (lightingScore < MIN_LIGHTING_SCORE) {
            reasons.add("Lighting too dim")
            score -= 15f
        }
        if (lightingScore > MAX_LIGHTING_SCORE) {
            reasons.add("Lighting too bright / overexposed")
            score -= 10f
        }

        val isPass = reasons.isEmpty()
        return QualityResult(
            isPass = isPass,
            score = coerce(score, 0f, 100f),
            faceSizeRatio = faceSizeRatio,
            blurScore = blurVariance,
            yawDeg = yawDeg,
            pitchDeg = pitchDeg,
            rollDeg = rollDeg,
            eyeAspectRatio = eyeAspectRatio,
            lightingScore = lightingScore,
            rejectionReason = if (isPass) null else reasons.joinToString(". ")
        )
    }

    private fun coerce(value: Float, min: Float, max: Float): Float =
        value.coerceIn(min, max)
}
