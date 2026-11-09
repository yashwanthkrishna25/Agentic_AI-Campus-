package com.example.biometrics

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage

/**
 * CameraX ImageAnalysis Analyzer for real-time ML Kit face detection & recognition.
 */
class RealFaceCameraAnalyzer(
    private val isFrontCamera: Boolean = true,
    private val targetRole: String? = null,
    private val specificPersonId: String? = null,
    private val onFacesDetected: (List<FaceMatchResult>, frameWidth: Int, frameHeight: Int) -> Unit
) : ImageAnalysis.Analyzer {

    private var isProcessing = false

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null || isProcessing) {
            imageProxy.close()
            return
        }

        isProcessing = true
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        val frameWidth = if (rotationDegrees == 90 || rotationDegrees == 270) imageProxy.height else imageProxy.width
        val frameHeight = if (rotationDegrees == 90 || rotationDegrees == 270) imageProxy.width else imageProxy.height

        FaceRecognitionEngine.detector.process(inputImage)
            .addOnSuccessListener { faces ->
                val results = mutableListOf<FaceMatchResult>()
                for (face in faces) {
                    val quality = FaceRecognitionEngine.validateFaceQuality(face, frameWidth, frameHeight)
                    val match = FaceRecognitionEngine.matchFace(
                        face = face,
                        frameWidth = frameWidth,
                        frameHeight = frameHeight,
                        targetRole = targetRole,
                        specificPersonId = specificPersonId
                    )
                    results.add(match)
                }
                onFacesDetected(results, frameWidth, frameHeight)
            }
            .addOnFailureListener {
                onFacesDetected(emptyList(), frameWidth, frameHeight)
            }
            .addOnCompleteListener {
                isProcessing = false
                imageProxy.close()
            }
    }
}
