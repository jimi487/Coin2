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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.hardware.Camera
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.ResultListener
import com.amplifyframework.storage.result.StorageUploadFileResult
import com.bohil.coin.R
import com.bohil.coin.databinding.ActivityPreviewBinding
import com.bohil.coin.login.common.CameraSource
import com.google.firebase.ml.common.FirebaseMLException
import kotlinx.android.synthetic.main.activity_preview.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


/**
 * Old Activity that handled capturing users images
 * // TODO Clean Folder
 */
class PreviewActivity : AppCompatActivity(), OnRequestPermissionsResultCallback,
    CompoundButton.OnCheckedChangeListener {


    private var cameraSource: CameraSource? = null
    private lateinit var faceContourDetectorProcessor : FaceContourDetectorProcessor
    private lateinit var binding : ActivityPreviewBinding

    // Variable that handles saving faces
    private lateinit var capturedImages: MutableList<Pair<File, Uri>>
    private var capturedImagesIndex = 0
    private lateinit var currentPhotoPath:String
    private lateinit var photoURI: Uri

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
        capturedImages = mutableListOf()
        binding.lifecycleOwner =  this

        // Creating the User Contour View Model
        faceContourDetectorProcessor = FaceContourDetectorProcessor()
        binding.faceContourDetectorProcessor = faceContourDetectorProcessor

        // Launches the intent to capture a picture
        binding.captureButton.setOnClickListener {
            captureImage()
        }

        // Uploading the Picture to the Amazon s3 collection and navigating to the main activity
        binding.cptCompleteBtn.setOnClickListener {
            //val intent = Intent(this, MainActivity::class.java)
            uploadFile()
            startActivity(intent)
        }

        if (firePreview == null) {
            Log.d(TAG, "Preview is null")
        }

        if (fireFaceOverlay == null) {
            Log.d(TAG, "graphicOverlay is null")
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
        Log.d(TAG, "Set facing")

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
            Log.i(TAG, "Using face detector Processor")
            cameraSource?.setMachineLearningFrameProcessor(faceContourDetectorProcessor)
        } catch (e: FirebaseMLException) {
            Log.e(TAG, "can not create camera source: ")
        }
    }

    private fun startCameraSource() {
        cameraSource?.let {
            try {
                if (firePreview == null) {
                    Log.d(TAG, "resume: Preview is null")
                }
                if (fireFaceOverlay == null) {
                    Log.d(TAG, "resume: graphOverlay is null")
                }
                firePreview?.start(cameraSource, fireFaceOverlay)
            } catch (e: IOException) {
                Log.e(TAG, "Unable to start camera source")
                cameraSource?.release()
                cameraSource = null
            }
        }
    }

    /**
     * Creates an image file for the image
     */
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    /**
     * Captures the image
     */
    private fun captureImage(){

/*        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureIntent.resolveActivity(packageManager)?.let{
            val values = ContentValues()
            values.put(MediaStore.Images.Media.TITLE, "New Picture")
            values.put(MediaStore.Images.Media.DESCRIPTION, "User Picture")
            val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
        }*/
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    Log.d(TAG, "Error creating the file")
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    photoURI= FileProvider.getUriForFile(
                        this,
                        "com.bohil.coin.fileprovider",
                        it
                    )
                    // Adding the file and uri to the captured images list
                    capturedImages.add(Pair(photoFile, photoURI))
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
                }
            }
        }
    }

    /**
     * Handles when the user is brought back to the app from taking a picture
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK){
            Log.d(TAG, "Creating bitmap")
            val imageBitmap = BitmapFactory.decodeFile(capturedImages[capturedImagesIndex].first.path)
            Log.d(TAG, "Created bitmap, creating thumbnail")
            changeThumbnail(imageBitmap)
        }
    }

    /**
     * Changes the thumbnails in the preview screen
     */
    private fun changeThumbnail(thumb:Bitmap){
        when (capturedImagesIndex){
            0 -> {binding.userImage00.setImageBitmap(thumb);}
            1 -> {binding.userImage01.setImageBitmap(thumb);}
            2 -> {binding.userImage02.setImageBitmap(thumb);}
        }
        capturedImagesIndex++

        if(capturedImages.size > 2) binding.cptCompleteBtn.visibility = View.VISIBLE
    }
/*
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        startCameraSource()
    }*/

    override fun onPause() {
        super.onPause()
        firePreview?.stop()
    }

    /**
     * Uploads the file to the Amazon s3 collection
     */
    private fun uploadFile(){
        Amplify.Storage.uploadFile(
                capturedImages[0].first.name,
            capturedImages[0].first.absolutePath,
            object: ResultListener<StorageUploadFileResult>{
                override fun onResult(result: StorageUploadFileResult?) {
                    Log.d(TAG, "File added successfully")
                }

                override fun onError(error: Throwable?) {
                    Log.d(TAG, "File not added successfully")
                }
            }
            )
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
        Log.i(TAG, "Permission granted!")
        if (allPermissionsGranted()) {
            createCameraSource()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    /**
     * Handles how many faces are present in the screen
     * Only allows the user to take a picture if there is 1 face detected on the screen
     */
    private fun numOfFaces(){
        faceContourDetectorProcessor.numFaces.observe(this, Observer {facesDetected ->
            when(facesDetected){
                1 -> {
                    binding.facesHelpText.setTextColor(Color.BLUE)
                    binding.facesHelpText.text = resources.getString(R.string.one_face_detected)
                    binding.captureButton.isClickable = true
                }
                else ->{
                    binding.facesHelpText.setTextColor(Color.WHITE)
                    binding.facesHelpText.text = resources.getString(R.string.no_face_detected)
                    binding.captureButton.isClickable = false
                }
            }
        })
    }

    companion object {
        private const val PERMISSION_REQUESTS = 1
        private const val REQUEST_TAKE_PHOTO = 1001
        private const val TAG = "PreviewActivity"


        private fun isPermissionGranted(context: Context, permission: String): Boolean {
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            ) {
                Log.i(TAG, "Permission granted: $permission")
                return true
            }
            Log.i(TAG, "Permission NOT granted: $permission")
            return false
        }
    }
}
