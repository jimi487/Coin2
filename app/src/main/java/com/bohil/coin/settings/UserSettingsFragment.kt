package com.bohil.coin.settings

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.room.util.DBUtil
import com.amazonaws.services.rekognition.model.Image
import com.amazonaws.services.rekognition.model.S3Object
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.AmazonS3URI
import com.amazonaws.services.s3.S3ClientOptions
import com.amazonaws.util.IOUtils
import com.amplifyframework.core.Amplify
import com.bohil.coin.DBUtility
import com.bohil.coin.R
import com.bohil.coin.databinding.FragmentUserSettingsBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class UserSettingsFragment : Fragment() {

    private lateinit var binding: FragmentUserSettingsBinding
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

        binding.BtnSave.setOnClickListener {
            saveInfo()
        }

        binding.BtnBack.setOnClickListener{
            findNavController().navigate(UserSettingsFragmentDirections.actionUserSettingsFragmentToCoinFragment())
        }
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
    }

    companion object {
        const val TAG = "SETTINGS"
    }
}