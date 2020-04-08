package com.bohil.coin

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.amazonaws.mobile.client.*
import com.amazonaws.mobileconnectors.s3.transferutility.TransferService
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.ResultListener
import com.amplifyframework.storage.result.StorageUploadFileResult
import com.amplifyframework.storage.s3.AWSS3StoragePlugin
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.Executors

/**
 * Class containing common DB methods
 */
object DBUtility {

    private const val TAG = "DBUtility"
    private var UserID : String = ""
    private var UserData : Users? = null
    val AWSInstance: AWSMobileClient = AWSMobileClient.getInstance()
    val FirebaseInstance = FirebaseFirestore.getInstance()


    fun initAWS(applicationContext : Context) {
        // Initializing the AWS Amplify instance
        AWSInstance.initialize(applicationContext, object : Callback<UserStateDetails> {
                override fun onResult(userStateDetails: UserStateDetails) {
                    try {
                        Amplify.addPlugin(AWSS3StoragePlugin())
                        applicationContext.startService(
                            Intent(
                                applicationContext,
                                TransferService::class.java
                            )
                        )
                        Amplify.configure(applicationContext)
                        when(userStateDetails.userState){
                            UserState.SIGNED_IN -> AWSMobileClient.getInstance().signOut()
                            UserState.SIGNED_OUT -> AWSMobileClient.getInstance().tokens
                        }

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


    fun getUserInfo(context:Context?) : Users? {
        runBlocking {

            val getUserAttributeJob = GlobalScope.launch {
                UserID = AWSInstance.userAttributes[context!!.getString(R.string.cognito_firestore)].toString()
            }

            //Wait for job to complete
            getUserAttributeJob.join()

           //Retrieve firestore document for logged in user
            val doc = FirebaseInstance.collection(context!!.getString(R.string.firestore_table)).document(UserID)

            doc.get()
                .addOnSuccessListener { document ->
                    if (document != null) {

                        //Convert document to object of type Users and assign it to UserData
                       UserData = document.toObject<Users>()
                    } else {
                        Log.d("SETTINGS", "No such document")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d("SETTINGS", "get failed with ", exception)
                }
        }
        return UserData

    }

    fun getUserData() : Users? {
        return UserData
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

    data class Users(
        val birthdate: String? = null,
        val country: String? = null,
        val first: String? = null,
        val last: String? = null,
        val igHandle: String? = null,
        val twitterHandle: String? = null,
        val snapchatHandle: String? = null,
        val sex: String? = null,
        val language: String? = null
    )

}






