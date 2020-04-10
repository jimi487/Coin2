package com.bohil.coin.login.registration


import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amazonaws.services.rekognition.model.DetectFacesResult
import com.amazonaws.services.rekognition.model.Image
import com.bohil.coin.DBUtility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "RegisterViewModel"
private lateinit var user: HashMap<String, String>

class RegisterViewModel: ViewModel() {

    /**
     * Main function to implement the add user sequence
     */
    fun addUser(fName:String, lName:String, language:String, country:String,
                sex:String, dob:String, instagramHandle:String, twitterHandle:String, snapchatHandle:String,
                collectionName:String, cognitoAttribute:String, userPicture: Pair<File, Uri>){
        viewModelScope.launch{
            createUser(fName, lName, language, country, sex, dob, instagramHandle, twitterHandle, snapchatHandle,
                collectionName, cognitoAttribute, userPicture)
        }
    }

    /**
     * Creates the user
     * Uploads the FireStore collection to include the information provided by the user in the
     * Register Fragment.
     * Then updates the user in AWS Cognito so their FirestoreID is saved
     */
    private suspend fun createUser(fName:String, lName:String, language:String, country:String,
                                   sex:String, dob:String, instagramHandle:String, twitterHandle:String, snapchatHandle:String,
                                   collectionName:String, cognitoAttribute: String, userPicture: Pair<File, Uri>) = withContext(Dispatchers.IO){

        user = hashMapOf(
            "first" to fName,
            "last" to lName,
            "language" to language,
            "country" to country,
            "sex" to sex,
            "birthdate" to dob, // Switch this to Timestamp(Date())
            "igHandle" to instagramHandle,
            "twitterHandle" to twitterHandle,
            "snapchatHandle" to snapchatHandle
        )

        DBUtility.addFirebaseUser(user, collectionName, cognitoAttribute, userPicture)

    }

    /**
     * Verifies whether there is an image in the picture before the user submits
     */
    fun detectFaces(image: Image): DetectFacesResult?{
        var results: DetectFacesResult? = null
        runBlocking {
            val getFacesDetectedJob = viewModelScope.launch {
                results =  DBUtility.detectFaces(image)
            }
            // Wait for job to complete
            getFacesDetectedJob.join()
        }
        return results
    }
    //TODO Switch to DBUtility verifyPicture
    /**
     * Uses Firebase to verify if a face was detected in a picture
     */
    fun verifyPicture(userPicture: Pair<File, Uri>): Int{
        var facesFound = 0

        viewModelScope.launch {
            //DBUtility.detectFaces(userPicture.first.absolutePath)
        }
        /*
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
                facesFound = 10000
                Log.d(TAG, e.message)
            }*/
        return facesFound
    }

}
