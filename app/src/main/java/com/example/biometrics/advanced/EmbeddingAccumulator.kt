package com.example.biometrics.advanced

import com.example.biometrics.FaceRecognitionEngine

/**
 * Multi-Frame Embedding Accumulator for Robust Face Templates.
 * Collects embeddings across multiple frames, rejects outliers,
 * and produces a single stable 128-D L2-normalized vector.
 * This dramatically improves recognition accuracy vs single-frame.
 */
object EmbeddingAccumulator {

    private const val TAG = "EmbeddingAccumulator"
    const val MIN_FRAMES_FOR_ENROLLMENT = 8
    const val MAX_FRAMES_FOR_ENROLLMENT = 15
    const val COSINE_OUTLIER_THRESHOLD = 0.85f  // Embeddings must be >= 0.85 similar to cluster center

    data class AccumulatedEmbedding(
        val embedding: FloatArray,     // Final L2-normalized 128-D vector
        val frameCount: Int,
        val meanConfidence: Float,
        val stabilityScore: Float      // 0-1, higher = more consistent across frames
    )

    /**
     * Accumulates face embeddings from multiple frames.
     * Removes outliers using cosine similarity against running mean.
     * Returns null if insufficient quality frames collected.
     */
    fun accumulate(
        embeddings: List<FloatArray>,   // Each is a 128-D raw embedding from a frame
        confidences: List<Float>        // Detection confidence per frame (0-1)
    ): AccumulatedEmbedding? {
        if (embeddings.isEmpty() || embeddings.size < MIN_FRAMES_FOR_ENROLLMENT) {
            return null
        }

        // Filter out low-confidence frames
        val validIndices = embeddings.indices.filter { confidences.getOrNull(it) ?: 0f >= 0.70f }
        if (validIndices.size < MIN_FRAMES_FOR_ENROLLMENT) {
            return null
        }

        val validEmbeddings = validIndices.map { embeddings.getOrNull(it)!! }
        val validConfidences = validIndices.map { confidences.getOrNull(it)!! }

        // L2-normalize each embedding
        val normalized = validEmbeddings.map { l2Normalize(it) }

        // Compute running mean embedding
        val dim = normalized.first().size
        val sum = FloatArray(dim)
        for (emb in normalized) {
            for (i in emb.indices) {
                sum[i] += emb[i]
            }
        }
        val meanEmbedding = FloatArray(dim)
        val count = normalized.size.toFloat()
        for (i in sum.indices) {
            meanEmbedding[i] = sum[i] / count
        }
        val meanNormalized = l2Normalize(meanEmbedding)

        // Remove outliers: frames with cosine similarity < threshold to mean
        val inlierIndices = normalized.indices.filter { i ->
            cosineSimilarity(normalized[i], meanNormalized) >= COSINE_OUTLIER_THRESHOLD
        }

        if (inlierIndices.size < MIN_FRAMES_FOR_ENROLLMENT) {
            // Too many outliers, try with relaxed threshold
            val relaxedInliers = normalized.indices.filter { i ->
                cosineSimilarity(normalized[i], meanNormalized) >= 0.70f
            }
            if (relaxedInliers.size < MIN_FRAMES_FOR_ENROLLMENT) {
                return null
            }
            val finalEmbs = relaxedInliers.map { normalized[it] }
            val finalConfs = relaxedInliers.map { validConfidences[it] }
            return buildAccumulatedEmbedding(finalEmbs, finalConfs)
        }

        val finalEmbs = inlierIndices.map { normalized[it] }
        val finalConfs = inlierIndices.map { validConfidences[it] }
        return buildAccumulatedEmbedding(finalEmbs, finalConfs)
    }

    private fun buildAccumulatedEmbedding(
        embeddings: List<FloatArray>,
        confidences: List<Float>
    ): AccumulatedEmbedding {
        val dim = embeddings.first().size
        val sum = FloatArray(dim)
        for (emb in embeddings) {
            for (i in emb.indices) {
                sum[i] += emb[i]
            }
        }
        val count = embeddings.size.toFloat()
        val finalEmb = FloatArray(dim)
        for (i in sum.indices) {
            finalEmb[i] = sum[i] / count
        }
        val normalizedFinal = l2Normalize(finalEmb)

        // Stability: average pairwise cosine similarity among inliers
        var stabilitySum = 0f
        var pairs = 0
        for (i in embeddings.indices) {
            for (j in i + 1 until embeddings.size) {
                stabilitySum += cosineSimilarity(embeddings[i], embeddings[j])
                pairs++
            }
        }
        val stability = if (pairs > 0) stabilitySum / pairs else 1f

        val meanConfidence = if (confidences.isEmpty()) 0f else confidences.average().toFloat()

        return AccumulatedEmbedding(
            embedding = normalizedFinal,
            frameCount = embeddings.size,
            meanConfidence = meanConfidence,
            stabilityScore = stability
        )
    }

    private fun l2Normalize(vector: FloatArray): FloatArray {
        val result = FloatArray(vector.size)
        var normSq = 0f
        for (v in vector) {
            normSq += v * v
        }
        val norm = kotlin.math.sqrt(normSq)
        if (norm == 0f) return result
        for (i in vector.indices) {
            result[i] = vector[i] / norm
        }
        return result
    }

    private fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        if (v1.size != v2.size || v1.isEmpty()) return 0f
        var dot = 0f
        var n1 = 0f
        var n2 = 0f
        for (i in v1.indices) {
            dot += v1[i] * v2[i]
            n1 += v1[i] * v1[i]
            n2 += v2[i] * v2[i]
        }
        val norm1 = kotlin.math.sqrt(n1).toFloat()
        val norm2 = kotlin.math.sqrt(n2).toFloat()
        if (norm1 == 0f || norm2 == 0f) return 0f
        return dot / (norm1 * norm2)
    }
}
