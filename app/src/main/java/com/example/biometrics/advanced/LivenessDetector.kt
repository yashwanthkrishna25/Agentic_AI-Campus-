package com.example.biometrics.advanced

/**
 * Liveness Detection Framework (Anti-Spoofing).
 * Detects presentation attacks: printed photos, replayed videos, masks.
 * Uses texture analysis, motion consistency, and eye-blink detection.
 */
object LivenessDetector {

    private const val TAG = "LivenessDetector"

    // Liveness thresholds
    const val MIN_MOTION_VARIANCE = 0.005f       // Real faces have micro-movements
    const val MIN_TEXTURE_VARIANCE = 0.15f       // Real skin has natural texture variation
    const val MIN_BLINK_DETECTION_RATE = 0.3f    // At least 30% of frames should show blink potential
    const val MIN_REFLECTION_VARIANCE = 0.08f    // Real eyes have catchlights that move

    data class LivenessResult(
        val isLive: Boolean,
        val confidence: Float,        // 0-1
        val motionScore: Float,
        val textureScore: Float,
        val blinkScore: Float,
        val reflectionScore: Float,
        val attackType: String? = null, // "PRINTED_PHOTO", "REPLAYED_VIDEO", "MASK", null
        val reasons: List<String> = emptyList()
    )

    /**
     * Run liveness detection on a sequence of face frames.
     * Returns whether the face appears to be a live person.
     */
    fun detectLiveness(
        frameTextures: List<FloatArray>,      // Local binary pattern or texture descriptors per frame
        framePositions: List<FloatArray>,     // Face landmark positions per frame (for motion)
        eyeAspectRatios: List<Float>,         // Eye aspect ratio per frame (for blink)
        frameBrightness: List<Float>          // Average brightness per frame
    ): LivenessResult {
        val reasons = mutableListOf<String>()
        var score = 100f

        // 1. Motion analysis: real faces move slightly between frames
        val motionScore = computeMotionScore(framePositions)
        if (motionScore < MIN_MOTION_VARIANCE) {
            reasons.add("No natural micro-movement detected (possible photo)")
            score -= 30f
        }

        // 2. Texture analysis: real skin has high-frequency variation
        val textureScore = computeTextureScore(frameTextures)
        if (textureScore < MIN_TEXTURE_VARIANCE) {
            reasons.add("Texture too uniform (possible printed photo or screen)")
            score -= 25f
        }

        // 3. Blink analysis: real humans blink periodically
        val blinkScore = computeBlinkScore(eyeAspectRatios)
        if (blinkScore < MIN_BLINK_DETECTION_RATE) {
            reasons.add("Insufficient blink activity (possible video replay)")
            score -= 25f
        }

        // 4. Reflection/catchlight analysis
        val reflectionScore = computeReflectionScore(frameBrightness)
        if (reflectionScore < MIN_REFLECTION_VARIANCE) {
            reasons.add("No eye catchlight variation (possible flat surface)")
            score -= 20f
        }

        val isLive = reasons.isEmpty()
        val attackType = if (!isLive) determineAttackType(motionScore, textureScore, blinkScore, reflectionScore) else null

        return LivenessResult(
            isLive = isLive,
            confidence = coerce(score, 0f, 100f) / 100f,
            motionScore = motionScore,
            textureScore = textureScore,
            blinkScore = blinkScore,
            reflectionScore = reflectionScore,
            attackType = attackType,
            reasons = reasons
        )
    }

    private fun computeMotionScore(positions: List<FloatArray>): Float {
        if (positions.size < 3) return 0f
        var totalMovement = 0f
        var pairs = 0
        for (i in 1 until positions.size) {
            val prev = positions[i - 1]
            val curr = positions[i]
            if (prev.size == curr.size) {
                var distSq = 0f
                for (j in prev.indices) {
                    val d = curr[j] - prev[j]
                    distSq += d * d
                }
                totalMovement += kotlin.math.sqrt(distSq).toFloat()
                pairs++
            }
        }
        return if (pairs > 0) totalMovement / pairs else 0f
    }

    private fun computeTextureScore(textures: List<FloatArray>): Float {
        if (textures.isEmpty()) return 0f
        var totalVariance = 0f
        for (tex in textures) {
            if (tex.isEmpty()) continue
            val mean = if (tex.isEmpty()) 0f else tex.average().toFloat()
            var varSum = 0f
            for (v in tex) {
                varSum += (v - mean) * (v - mean)
            }
            totalVariance += varSum / tex.size
        }
        return totalVariance / textures.size
    }

    private fun computeBlinkScore(earValues: List<Float>): Float {
        if (earValues.isEmpty()) return 0f
        // Count frames where EAR drops below threshold (blink candidate)
        val blinkThreshold = 0.25f
        var blinkCount = 0
        for (ear in earValues) {
            if (ear < blinkThreshold) blinkCount++
        }
        return blinkCount / earValues.size.toFloat()
    }

    private fun computeReflectionScore(brightness: List<Float>): Float {
        if (brightness.size < 2) return 0f
        // Catchlights cause brightness variation across frames
        var max = brightness.maxOrNull() ?: 0f
        var min = brightness.minOrNull() ?: 0f
        return (max - min).coerceIn(0f, 1f)
    }

    private fun determineAttackType(
        motion: Float, texture: Float, blink: Float, reflection: Float
    ): String {
        return when {
            motion < MIN_MOTION_VARIANCE && texture < MIN_TEXTURE_VARIANCE -> "PRINTED_PHOTO"
            motion >= MIN_MOTION_VARIANCE && blink < MIN_BLINK_DETECTION_RATE -> "REPLAYED_VIDEO"
            texture < MIN_TEXTURE_VARIANCE && reflection < MIN_REFLECTION_VARIANCE -> "MASK"
            else -> "UNKNOWN_SPOOF"
        }
    }

    private fun coerce(value: Float, min: Float, max: Float): Float =
        value.coerceIn(min, max)
}
