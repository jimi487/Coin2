package com.bohil.coin.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.amazonaws.services.rekognition.model.Image
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.util.IOUtils
import com.bohil.coin.DBUtility
import com.bohil.coin.R
import com.bohil.coin.databinding.FragmentUserSettingsBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class UserSettingsFragment : Fragment() {

    private lateinit var binding: FragmentUserSettingsBinding
    private lateinit var photoURI: Uri
    private lateinit var capturedImage: Pair<File, Uri>
    private var _userData : UserManager.User? = UserManager.UserDocs[UserManager.UserID]
    private var imgLoc : String = ""
    private val _fName = _userData?.first
    private val _lName = _userData?.last
    private val _birthday = _userData?.birthdate
    private val _country = _userData?.country
    private val _igHandle = _userData?.igHandle
    private val _snapHandle = _userData?.snapchatHandle
    private val _twitterHandle = _userData?.twitterHandle
    private val _sex = _userData?.sex
    private val _language = _userData?.language

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_user_settings, container, false)
        binding.BtnSave.setOnClickListener { saveInfo() }
        binding.BtnBack.setOnClickListener{ findNavController().navigate(UserSettingsFragmentDirections.actionUserSettingsFragmentToCoinFragment()) }
        binding.faceImage.setOnClickListener { captureImage() }

        setTextboxes()
        retrieveImg()
        return binding.root
    }

    private fun retrieveImg() {
        Log.i(TAG, "Retrieving img from s3 and setting to image view....")
        val imageObj = DBUtility.retrieveImageFromS3(UserManager.UserID)
        val imageContent = AmazonS3Client(DBUtility.AWSInstance.credentials).getObject(imageObj.s3Object.bucket, "public/${UserManager.UserID}.jpg").objectContent
        createImageFile()
        IOUtils.copy(imageContent, FileOutputStream(imgLoc))
        val imageBitmap = BitmapFactory.decodeFile(File(imgLoc).absolutePath)
        binding.faceImage.setImageBitmap(imageBitmap)
        Log.i(TAG, "Retrieved img from s3 and set successfully")
    }


    private fun setTextboxes() {
        Log.e(TAG, "Updating textboxes...")
        binding.TxtFName.setText(_fName)
        binding.TxtLName.setText(_lName)
        binding.TxtDOB.setText(_birthday)
        binding.countrySpinner.setSelection(resources.getStringArray(R.array.country_array).indexOf(_country))
        binding.TxtIgHandle.setText(_igHandle)
        binding.TxtSnapHandle.setText(_snapHandle)
        binding.TxtTwitterHandle.setText(_twitterHandle)
        binding.sexSpinner.setSelection(resources.getStringArray(R.array.sex_array).indexOf(_sex))
        binding.languageSpinner.setSelection(resources.getStringArray(R.array.language_array).indexOf(_language))
        Log.i(TAG, "Set textboxes successfully")
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = activity!!.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            imgLoc = absolutePath
        }
    }

    private fun saveInfo() {
        val doc = DBUtility.FirebaseInstance.collection(context!!.getString(R.string.firestore_table)).document(UserManager.UserID)

        val userInfo = UserManager.User(
            binding.TxtDOB.text.toString(),
            binding.countrySpinner.selectedItem.toString(),
            binding.TxtFName.text.toString(),
            binding.TxtLName.text.toString(),
            binding.TxtIgHandle.text.toString(),
            binding.TxtTwitterHandle.text.toString(),
            binding.TxtSnapHandle.text.toString(),
            binding.sexSpinner.selectedItem.toString(),
            binding.languageSpinner.selectedItem.toString()
        )

        //Change the data of current user to updated data
        UserManager.UserDocs[UserManager.UserID] = userInfo

        val updateJob = doc.set(userInfo)

        updateJob.addOnSuccessListener {
            Toast.makeText(context, "Save successful!", Toast.LENGTH_SHORT).show()
        }

        updateJob.addOnFailureListener{
            Toast.makeText(context, "Error with save", Toast.LENGTH_SHORT).show()
        }

        if(oneFace) DBUtility.uploadFile(capturedImage, UserManager.UserID, context!!)


    }

    /**
     * Captures the image
     */
    private fun captureImage(){

        val packageManager = context!!.packageManager
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    Log.d(TAG, "Error creating the file")
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    photoURI= FileProvider.getUriForFile(
                        context!!,
                        "com.bohil.coin.fileprovider",
                        it
                    )

                    capturedImage = Pair(photoFile, photoURI)
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
            val imageBitmap = BitmapFactory.decodeFile(capturedImage.first.path)
            val screenImage = Bitmap.createScaledBitmap(imageBitmap, 100, 100, false)

            val faces = verifyPicture(context!!, screenImage)

            if (faces == 1){
                oneFace = true
                changeThumbnail(imageBitmap)
            }else{
                oneFace = false
                makeToast("Unable to replace picture since no faces were detected!")
            }

        }
    }


    /**
     * Changes the thumbnails in the preview screen
     */
    private fun changeThumbnail(thumb: Bitmap){
        binding.faceImage.setImageBitmap(thumb)
    }

    /**
     * Uses Firebase to verify if a face was detected in a picture
     */
    fun verifyPicture(appContext: Context, screenImage: Bitmap): Int{
        var facesFound = 0
        val image = Image().withBytes(DBUtility.convertToByteBuffer(appContext, screenImage))

        runBlocking {
            val detectFacesJob = GlobalScope.launch {
                val results = DBUtility.detectFaces(image)
                facesFound = results?.faceDetails!!.size
            }
            detectFacesJob.join()
        }

        return facesFound
    }

    private fun makeToast(msg: String, length: Int = 0) {
        if (length == 0)
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() else
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

    }

    companion object {
        const val TAG = "SETTINGS"
        private var oneFace = false
        private const val REQUEST_TAKE_PHOTO = 1001

    }
}