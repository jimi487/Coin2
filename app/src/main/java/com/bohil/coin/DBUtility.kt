package com.bohil.coin

import android.content.Context
import android.net.Uri
import android.util.Log
import com.amazonaws.mobile.client.*
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.ResultListener
import com.amplifyframework.storage.result.StorageUploadFileResult
import com.amplifyframework.storage.s3.AWSS3StoragePlugin
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import kotlinx.coroutines.*

/**
 * Class containing common DB methods
 */
object DBUtility {

    private const val TAG = "DBUtility"
    val AWSInstance: AWSMobileClient = AWSMobileClient.getInstance()
    val FirebaseInstance = FirebaseFirestore.getInstance()

    fun initAWS(applicationContext : Context) {
        // Initializing the AWS Amplify instance
        AWSInstance
        .initialize(applicationContext, object : Callback<UserStateDetails> {
            override fun onResult(userStateDetails: UserStateDetails) {
                try {
                    Amplify.addPlugin(AWSS3StoragePlugin())
                    Amplify.configure(applicationContext)
                    when(userStateDetails.userState){
                        UserState.SIGNED_IN -> AWSInstance.signOut()
                        UserState.SIGNED_OUT -> AWSInstance.tokens
                    }

                    // IMPORTANT! Refreshes the access tokens stored in the cache
                    //DBUtility.AWSInstance.tokens
                } catch (e: java.lang.Exception) {
                    Log.e("ApiQuickstart", e.message)
                }
            }

            override fun onError(e: Exception?) {
                Log.e("INIT", "Initialization error.", e)
            }
        }
        )
    }

    fun signOutAWS() {
        AWSInstance.signOut(
            SignOutOptions.builder().signOutGlobally(true).build(),
            object : Callback<Void?> {
                override fun onResult(result: Void?) {
                    Log.d("LogInActivity", "signed-out")
                }

                override fun onError(e: java.lang.Exception) {
                    Log.e("LogInActivity", "sign-out error", e)
                }
            })
    }

    suspend fun addFirebaseUser(user : HashMap<String, String>, collectionName:String, cognitoAttribute: String, userPicture: Pair<File, Uri>) {
        FirebaseInstance.collection(collectionName)
            .add(user)
            .addOnSuccessListener {
                //Update user's FireStore ID in Cognito
                try {
                    Log.d(TAG, "Updating the user Firestore Key")
                    GlobalScope.launch {
                        updateCognito(cognitoAttribute, it.id)
                        uploadFile(userPicture, it.id)
                    }
                } catch (e: Exception) {
                    Log.d(TAG, e.toString())
                }
            }

            .addOnFailureListener {
                Log.w(TAG, "Error adding document", it)
            }
    }


    /**
     * Updates the user's Firestore ID in Cognito
     */
    private suspend fun updateCognito(attribute:String, id:String) = withContext(Dispatchers.IO){
        try{
            AWSInstance.updateUserAttributes(hashMapOf(attribute to id))
            // Adding the users image to the S3 collection
        }catch(e:Exception){
            Log.e(TAG, "Unable to add key", e)
        }
        Log.d(TAG, "User key finished adding")

    }

    /**
     * Uploads the file to the Amazon s3 collection
     */
    private fun uploadFile(userFile: Pair<File, Uri>, firebaseID:String){
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
}




