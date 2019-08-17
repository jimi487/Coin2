// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.bohil.coin.login.facedetection

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Camera
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.bohil.coin.R
import com.bohil.coin.databinding.ActivityPreviewBinding
import com.bohil.coin.login.common.CameraSource
import com.google.firebase.ml.common.FirebaseMLException
import kotlinx.android.synthetic.main.activity_preview.*
import timber.log.Timber
import java.io.IOException

/**
 * Activity that handles creating the View for the Camera and User Detection
 */
class PreviewActivity : AppCompatActivity(), OnRequestPermissionsResultCallback,
    CompoundButton.OnCheckedChangeListener {

    //TODO replace this with Camera2 api
    private var cameraSource: CameraSource? = null
    private var selectedModel = FACE_CONTOUR

    // Variable that handles saving faces
    private lateinit var faceContourDetectorProcessor : FaceContourDetectorProcessor
    private lateinit var binding : ActivityPreviewBinding


    private val requiredPermissions: Array<String?>
        get() {
            return try {
                val info = this.packageManager
                    .getPackageInfo(this.packageName, PackageManager.GET_PERMISSIONS)
                val ps = info.requestedPermissions
                if (ps != null && ps.isNotEmpty()) {
                    ps
                } else {
                    arrayOfNulls(0)
                }
            } catch (e: Exception) {
                arrayOfNulls(0)
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding  = DataBindingUtil.setContentView(this, R.layout.activity_preview)
        binding.lifecycleOwner =  this
        Timber.d("onCreate")

        // Creating the User Contour View Model
        faceContourDetectorProcessor = FaceContourDetectorProcessor()
        binding.faceContourDetectorProcessor = faceContourDetectorProcessor
        binding.facesHelpText.bringToFront()


        if (firePreview == null) {
            Timber.d("Preview is null")
        }

        if (fireFaceOverlay == null) {
            Timber.d("graphicOverlay is null")
        }

        val facingSwitch = facingSwitch
        facingSwitch.setOnCheckedChangeListener(this)

        // Hide the toggle button if there is only 1 camera
        if (Camera.getNumberOfCameras() == 1) {
            facingSwitch.visibility = View.GONE
        }

        if (allPermissionsGranted()) {
            createCameraSource()
            numOfFaces()
        } else {
            getRuntimePermissions()
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        Timber.d("Set facing")

        cameraSource?.let {
            if (isChecked) {
                it.setFacing(CameraSource.CAMERA_FACING_BACK)
            } else {
                it.setFacing(CameraSource.CAMERA_FACING_FRONT)
            }
        }
        firePreview?.stop()
        startCameraSource()
    }

    private fun createCameraSource() {
        // If there's no existing cameraSource, create one
        if (cameraSource == null) {
            cameraSource = CameraSource(this, fireFaceOverlay)
        }

        try {
            Timber.i("Using face detector Processor")
            cameraSource?.setMachineLearningFrameProcessor(faceContourDetectorProcessor)
        } catch (e: FirebaseMLException) {
            Timber.e("can not create camera source: ")
        }
    }

    private fun startCameraSource() {
        cameraSource?.let {
            try {
                if (firePreview == null) {
                    Timber.d("resume: Preview is null")
                }
                if (fireFaceOverlay == null) {
                    Timber.d("resume: graphOverlay is null")
                }
                firePreview?.start(cameraSource, fireFaceOverlay)
            } catch (e: IOException) {
                Timber.e("Unable to start camera source")
                cameraSource?.release()
                cameraSource = null
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume")
        startCameraSource()
    }

    override fun onPause() {
        super.onPause()
        firePreview?.stop()
    }

    public override fun onDestroy() {
        super.onDestroy()
        cameraSource?.release()
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in requiredPermissions) {
            if (!isPermissionGranted(this, permission!!)) {
                return false
            }
        }
        return true
    }

    private fun getRuntimePermissions() {
        val allNeededPermissions = arrayListOf<String>()
        for (permission in requiredPermissions) {
            if (!isPermissionGranted(this, permission!!)) {
                allNeededPermissions.add(permission)
            }
        }

        if (allNeededPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this, allNeededPermissions.toTypedArray(),
                PERMISSION_REQUESTS
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        Timber.i("Permission granted!")
        if (allPermissionsGranted()) {
            createCameraSource()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    /**
     * Handles how many faces are present in the screen
     * Only allows the user to take a picture if there is ony only 1 face detected on the screen
     */
    private fun numOfFaces(){
        faceContourDetectorProcessor.numFaces.observe(this, Observer {facesDetected ->

            when(facesDetected){

                0 ->{
                    binding.facesHelpText.text = resources.getString(R.string.no_face_detected)
                    binding.captureButton.isClickable = false
                }
                1 -> {
                    binding.facesHelpText.text = resources.getString(R.string.one_face_detected)
                    binding.captureButton.isClickable = true
                }
                else -> {
                    binding.facesHelpText.text = resources.getString(R.string.multiple_faces_detected)
                    binding.captureButton.isClickable = false
                }
            }
        })
    }


    companion object {
        private const val FACE_CONTOUR = "User Contour"
        private const val PERMISSION_REQUESTS = 1

        private fun isPermissionGranted(context: Context, permission: String): Boolean {
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            ) {
                Timber.i("Permission granted: $permission")
                return true
            }
            Timber.i("Permission NOT granted: $permission")
            return false
        }
    }
}
