package com.bohil.coin.login.registration


import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.bohil.coin.DBUtility

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
