package com.bohil.coin.login.registration

import android.app.Activity
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
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.bohil.coin.R
import com.bohil.coin.databinding.FragmentCombinedBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import com.bohil.coin.DBUtility

class CombinedRegister : Fragment() {

    private lateinit var binding: FragmentCombinedBinding
    private lateinit var viewModel:RegisterViewModel
    private lateinit var capturedImage: Pair<File, Uri>
    private lateinit var currentPhotoPath:String
    private lateinit var photoURI: Uri

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProviders.of(this).get(RegisterViewModel::class.java)
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_combined, container, false)
        binding.lifecycleOwner = this

        try{
            binding.emailText.setText(DBUtility.AWSInstance.username.toString())
        }catch(e:Exception){

        }

        binding.finishButton2.setOnClickListener { validateForm() }

        binding.confirmationCheckbox.setOnCheckedChangeListener { _, isChecked ->
           if(isChecked) {
               binding.finishButton2.isEnabled = true
               binding.finishButton2.isClickable = true
           } else {
               binding.finishButton2.isEnabled = false
               binding.finishButton2.isClickable = false
           }
        }

        // Capturing the users image
        binding.faceImage.setOnClickListener {
            captureImage()
        }

        return binding.root
    }

    private fun validateForm() {
        var valid = true

        //Firebase Fields
        val firstName = binding.firstNameEditText.text.toString()
        val lastName = binding.lastNameEditText.text.toString()
        val dob = binding.dobText.text.toString()
        val lang = binding.languageSpinner.selectedItem.toString()
        val sex = binding.sexSpinner.selectedItem.toString()
        val country = binding.countrySpinner.selectedItem.toString()
        val instagramHandle = binding.igHandle.text.toString()
        val twitterHandle = binding.twitterHandle.text.toString()
        val snapchatHandle = binding.snapchatHandle.text.toString()
        val listOfStrings = listOf(firstName, lastName, dob)


        when {
            listOfStrings.contains("") -> if (listOfStrings.filter { it == "" }.isNotEmpty()) {
                makeToast("All fields must be filled")
                valid = false
            }
            binding.faceImage.drawable == null -> {
                makeToast("Please take a picture")
                valid = false
            }
            !oneFace -> {
                makeToast("Unable to find your face. Please upload picture again")
            }
        }

        if (valid) {
            val userFile = capturedImage
            //Add user to AWS
            viewModel.addUser(firstName,lastName, lang, country,sex, dob, instagramHandle, twitterHandle, snapchatHandle,
                getString(R.string.firestore_table), getString(R.string.cognito_firestore), userFile)
            findNavController().navigate(CombinedRegisterDirections.actionCombinedFragmentToCoinActivity())
        }
    }

    /**
     * Creates an image file for the image
     */
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

        val packageManager = context!!.packageManager

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
                        context!!,
                        "com.bohil.coin.fileprovider",
                        it
                    )
                    // Adding the file and uri to the captured images list
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
            val facesFound = viewModel.verifyPicture(capturedImage)
            makeToast("Faces Found: $facesFound")
            /*if (facesFound != 1){
                makeToast("Please only have one face in your picture")
            }
            else{*/
                oneFace = true
                Log.d(TAG, "Creating bitmap")
                val imageBitmap = BitmapFactory.decodeFile(capturedImage.first.path)
                Log.d(TAG, "Created bitmap, creating thumbnail")
                changeThumbnail(imageBitmap)
            //}

        }
    }


    /**
     * Changes the thumbnails in the preview screen
     */
    private fun changeThumbnail(thumb: Bitmap){
        binding.faceImage.setImageBitmap(thumb)
    }

    /**
     * Utility function to help make toast
     */
    private fun makeToast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    /**
     * Handles when the user is redirected back to the app
     */
    override fun onResume() {
        super.onResume()
        val activityIntent = Intent()
        if (activityIntent.data != null && "myapp" == activityIntent.data?.scheme) {
            DBUtility.AWSInstance.handleAuthResponse(activityIntent)
        }
    }

    companion object {
        private const val TAG = "CombinedFragment"
        private const val REQUEST_TAKE_PHOTO = 1001
        private var oneFace = false


    }
}
