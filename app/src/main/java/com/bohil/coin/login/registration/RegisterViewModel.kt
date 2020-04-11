package com.bohil.coin.login.registration


import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.amazonaws.services.rekognition.model.Image
import com.bohil.coin.DBUtility
import kotlinx.coroutines.*
import java.io.File
import java.nio.ByteBuffer

private const val TAG = "RegisterViewModel"
private lateinit var sourceImageBytes:ByteBuffer
private lateinit var user: HashMap<String, String>

class RegisterViewModel: ViewModel() {

    /**
     * Main function to implement the add user sequence
     */
    fun addUser(fName:String, lName:String, language:String, country:String,
                sex:String, dob:String, instagramHandle:String, twitterHandle:String, snapchatHandle:String,
                collectionName:String, cognitoFirestore:String, userPicture: Pair<File, Uri>, appContext: Context){
        GlobalScope.launch{
            createUser(fName, lName, language, country, sex, dob, instagramHandle, twitterHandle, snapchatHandle,
                collectionName, cognitoFirestore, userPicture, appContext)
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
                                   collectionName:String, cognitoAttribute: String, userPicture: Pair<File, Uri>, appContext: Context) = withContext(Dispatchers.IO){

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

        DBUtility.addFirebaseUser(user, collectionName, cognitoAttribute, userPicture, appContext)

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

}
