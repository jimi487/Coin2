package com.bohil.coin.login.facedetection

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bohil.coin.login.common.CameraImageGraphic
import com.bohil.coin.login.common.FrameMetadata
import com.bohil.coin.login.common.GraphicOverlay
import com.google.android.gms.tasks.Task
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import timber.log.Timber
import java.io.IOException

/**
 * ViewModel for the Preview Activity
 */

// TODO Change to Landmark detector possibly
class FaceContourDetectorProcessor : VisionProcessorBase<List<FirebaseVisionFace>>() {

    private val detector: FirebaseVisionFaceDetector

    // LiveData to hold the number of Faces in the preview
    private var _numFaces = MutableLiveData<Int?>()
    val numFaces : LiveData<Int?>
    get() = _numFaces

    init {
        val options = FirebaseVisionFaceDetectorOptions.Builder()
            .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
            .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
            .build()

        detector = FirebaseVision.getInstance().getVisionFaceDetector(options)
    }

    override fun stop() {
        try {
            detector.close()
        } catch (e: IOException) {
            Timber.e("Exception thrown while trying to close User Contour Detector: $e")
        }
    }

    override fun detectInImage(image: FirebaseVisionImage): Task<List<FirebaseVisionFace>> {
        return detector.detectInImage(image)
    }

    override fun onSuccess(
        originalCameraImage: Bitmap?,
        results: List<FirebaseVisionFace>,
        frameMetadata: FrameMetadata,
        graphicOverlay: GraphicOverlay
    ) {
        graphicOverlay.clear()

        originalCameraImage?.let {
            val imageGraphic = CameraImageGraphic(graphicOverlay, it)
            graphicOverlay.add(imageGraphic)
        }

        // The number of faces
        _numFaces.value = results.size

        // Faces located
        results.forEach {
            val faceGraphic = FaceContourGraphic(graphicOverlay, it)
            graphicOverlay.add(faceGraphic)
        }
        graphicOverlay.postInvalidate()
    }

    override fun onFailure(e: Exception) {
        Log.e(TAG, "User detection failed $e")
    }

    companion object {
        private const val TAG = "FaceContourDetectorProc"
    }

}