package com.bohil.coin.login.registration


import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amazonaws.mobile.client.AWSMobileClient
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.ResultListener
import com.amplifyframework.storage.result.StorageUploadFileResult
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "RegisterViewModel"
private lateinit var user: HashMap<String, String>

class RegisterViewModel: ViewModel() {

    /**
     * Main function to implement the add user sequence
     */
    fun addUser(fName:String, lName:String, language:String, country:String,
                sex:String, dob:String, collectionName:String, cognitoAttribute:String,
                userPicture: Pair<File, Uri>){
        viewModelScope.launch{
            createUser(fName, lName, language, country, sex, dob, collectionName, cognitoAttribute, userPicture)
        }
    }

    /**
     * Creates the user
     * Uploads the FireStore collection to include the information provided by the user in the
     * Register Fragment.
     * Then updates the user in AWS Cognito so their FirestoreID is saved
     */
    private suspend fun createUser(fName:String, lName:String, language:String, country:String,
                                   sex:String, dob:String, collectionName:String, cognitoAttribute: String,
                                   userPicture: Pair<File, Uri>)
            = withContext(Dispatchers.IO){

        val db = FirebaseFirestore.getInstance()
        user = hashMapOf(
            "first" to fName,
            "last" to lName,
            "language" to language,
            "country" to country,
            "sex" to sex,
            "birthdate" to dob // Switch this to Timestamp(Date())
        )

        // Adding the user to the FireStore DB
        db.collection(collectionName)
            .add(user)
            .addOnSuccessListener {
                //Update user's FireStore ID in Cognito
                try{
                    Log.d(TAG, "Updating the user Firestore Key")
                    viewModelScope.launch {
                        updateCognito(cognitoAttribute, it.id)
                        uploadFile(userPicture, it.id)
                    }


                }catch(e: Exception){
                    Log.d(TAG, e.toString())
                }

            }
            .addOnFailureListener{
                Log.w(TAG, "Error adding document", it)
            }

    }

    /**
     * Updates the user's Firestore ID in Cognito
     */
    private suspend fun updateCognito(attribute:String, id:String) = withContext(Dispatchers.IO){
        try{
            AWSMobileClient.getInstance().updateUserAttributes(hashMapOf(attribute to id))
            // Adding the users image to the S3 collection
        }catch(e:Exception){
            Log.e(TAG, "Unable to add key", e)
        }
        Log.d(TAG, "User key finished adding")

    }

    /**
     * Uploads the file to the Amazon s3 collection
     */
    private suspend fun uploadFile(userFile: Pair<File, Uri>, firebaseID:String){
        Amplify.Storage.uploadFile(
            firebaseID,
            userFile.first.absolutePath,
            object: ResultListener<StorageUploadFileResult> {
                override fun onResult(result: StorageUploadFileResult?) {
                    Log.d(TAG, "File added successfully")
                }

                override fun onError(error: Throwable?) {
                    Log.d(TAG, "File not added successfully")
                }
            }
        )
    }

    /**
     * Uses Firebase to verify if a face was detected in a picture
     */
    fun verifyPicture(userPicture: Pair<File, Uri>): Int{
        var facesFound = 0
        val options = FirebaseVisionFaceDetectorOptions.Builder()
            .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
            .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
            .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
            .build()

        val imageBitmap = BitmapFactory.decodeFile(userPicture.first.path)
        val image = FirebaseVisionImage.fromBitmap(imageBitmap)
        val detector = FirebaseVision.getInstance().getVisionFaceDetector(options)
        val result = detector.detectInImage(image)
            .addOnSuccessListener { faces ->
                facesFound = faces.size
            }
            .addOnFailureListener { e ->
                Log.d(TAG, e.message)
            }
        return facesFound
    }


}
