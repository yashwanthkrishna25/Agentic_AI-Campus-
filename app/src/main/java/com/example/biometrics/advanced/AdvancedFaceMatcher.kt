package com.example.biometrics.advanced

/**
 * Advanced Face Matching with Combined Distance Metrics.
 * Uses both Cosine Similarity and Euclidean Distance with
 * adaptive thresholds for higher attendance accuracy.
 */
object AdvancedFaceMatcher {

    private const val TAG = "AdvancedFaceMatcher"

    // Strict thresholds for attendance (higher security = fewer false accepts)
    const val COSINE_MATCH_THRESHOLD = 0.72f       // >= 0.72 = confident match
    const val COSINE_AMBIGUOUS_THRESHOLD = 0.60f   // 0.60-0.72 = needs confirmation
    const val EUCLIDEAN_MATCH_THRESHOLD = 0.85f    // Normalized euclidean distance score >= 0.85
    const val EUCLIDEAN_AMBIGUOUS_THRESHOLD = 0.75f

    // Combined decision: both metrics must agree for confident match
    const val REQUIRE_BOTH_METRICS = true

    data class MatchResult(
        val studentId: String,
        val studentName: String,
        val confidence: Float,        // 0-1 combined confidence
        val cosineSimilarity: Float,
        val euclideanScore: Float,
        val status: String,           // MATCHED, AMBIGUOUS, UNMATCHED
        val needsConfirmation: Boolean,
        val matchMetrics: MatchMetrics
    )

    data class MatchMetrics(
        val isCosineMatch: Boolean,
        val isEuclideanMatch: Boolean,
        val combinedScore: Float,
        val templateAgeDays: Long? = null
    )

    /**
     * Match a single face embedding against enrolled templates.
     * Returns the best match with combined confidence scoring.
     */
    fun match(
        probeEmbedding: FloatArray,
        templates: List<EnrolledTemplate>,
        requireBothMetrics: Boolean = REQUIRE_BOTH_METRICS
    ): MatchResult? {
        if (templates.isEmpty()) {
            return null
        }

        val probeNorm = l2Normalize(probeEmbedding)

        var bestTemplate: EnrolledTemplate? = null
        var bestCosine = -1f
        var bestEuclidean = -1f

        for (tpl in templates) {
            if (!tpl.isActive) continue
            val storedNorm = l2Normalize(tpl.embedding)

            val cosine = cosineSimilarity(probeNorm, storedNorm)
            val euclideanScore = euclideanSimilarityScore(probeNorm, storedNorm)

            // Combined score weights both metrics
            val combined = (cosine * 0.6f) + (euclideanScore * 0.4f)

            if (combined > bestCosine) {
                bestCosine = combined
                bestEuclidean = euclideanScore
                bestTemplate = tpl
            }
        }

        if (bestTemplate == null) {
            return null
        }

        val cosineMatch = bestCosine >= COSINE_MATCH_THRESHOLD
        val euclideanMatch = bestEuclidean >= EUCLIDEAN_MATCH_THRESHOLD

        val bothPass = cosineMatch && euclideanMatch
        val eitherPass = cosineMatch || euclideanMatch

        val status = when {
            requireBothMetrics && bothPass -> "MATCHED"
            !requireBothMetrics && eitherPass -> "MATCHED"
            (requireBothMetrics && !bothPass && (cosineMatch || euclideanMatch)) ||
                (!requireBothMetrics && !eitherPass && (bestCosine >= COSINE_AMBIGUOUS_THRESHOLD ||
                    bestEuclidean >= EUCLIDEAN_AMBIGUOUS_THRESHOLD)) -> "AMBIGUOUS"
            else -> "UNMATCHED"
        }

        val needsConfirmation = status == "AMBIGUOUS" || status == "UNMATCHED"

        return MatchResult(
            studentId = bestTemplate.studentId,
            studentName = bestTemplate.studentName,
            confidence = bestCosine,
            cosineSimilarity = bestCosine,
            euclideanScore = bestEuclidean,
            status = status,
            needsConfirmation = needsConfirmation,
            matchMetrics = MatchMetrics(
                isCosineMatch = cosineMatch,
                isEuclideanMatch = euclideanMatch,
                combinedScore = bestCosine,
                templateAgeDays = bestTemplate.enrolledAt?.let {
                    (System.currentTimeMillis() - it) / (1000 * 60 * 60 * 24)
                }
            )
        )
    }

    /**
     * Multi-frame matching: average embeddings across detected faces in a sequence
     * before comparing, reducing single-frame noise.
     */
    fun matchMultiFrame(
        frameEmbeddings: List<FloatArray>,
        templates: List<EnrolledTemplate>
    ): MatchResult? {
        if (frameEmbeddings.isEmpty()) return null

        // Average the embeddings across frames
        val dim = frameEmbeddings.first().size
        val sum = FloatArray(dim)
        for (emb in frameEmbeddings) {
            for (i in emb.indices) {
                sum[i] += emb[i]
            }
        }
        val count = frameEmbeddings.size.toFloat()
        val averaged = FloatArray(dim)
        for (i in sum.indices) {
            averaged[i] = sum[i] / count
        }

        return match(averaged, templates)
    }

    private fun l2Normalize(vector: FloatArray): FloatArray {
        val result = FloatArray(vector.size)
        var normSq = 0f
        for (v in vector) normSq += v * v
        val norm = kotlin.math.sqrt(normSq)
        if (norm == 0f) return result
        for (i in vector.indices) result[i] = vector[i] / norm
        return result
    }

    private fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        if (v1.size != v2.size || v1.isEmpty()) return 0f
        var dot = 0f; var n1 = 0f; var n2 = 0f
        for (i in v1.indices) {
            dot += v1[i] * v2[i]
            n1 += v1[i] * v1[i]
            n2 += v2[i] * v2[i]
        }
        val norm1 = kotlin.math.sqrt(n1).toFloat()
        val norm2 = kotlin.math.sqrt(n2).toFloat()
        return if (norm1 == 0f || norm2 == 0f) 0f else dot / (norm1 * norm2)
    }

    private fun euclideanSimilarityScore(v1: FloatArray, v2: FloatArray): Float {
        if (v1.size != v2.size) return 0f
        var distSq = 0f
        for (i in v1.indices) {
            val d = v1[i] - v2[i]
            distSq += d * d
        }
        val dist = kotlin.math.sqrt(distSq).toFloat()
        // Convert distance to similarity score: 1 / (1 + dist)
        return 1f / (1f + dist)
    }

    data class EnrolledTemplate(
        val studentId: String,
        val studentName: String,
        val embedding: FloatArray,
        val department: String,
        val section: String,
        val collegeId: String,
        val isActive: Boolean = true,
        val enrolledAt: Long? = null,
        val modelVersion: String = "arcface_v2",
        val frameCount: Int = 1
    )
}
