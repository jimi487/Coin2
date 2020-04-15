package com.bohil.coin.settings

import android.content.Context
import android.util.Log
import androidx.annotation.Nullable
import com.amplifyframework.core.Amplify
import com.bohil.coin.DBUtility
import com.bohil.coin.R
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.model.Document
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


object UserManager {


    data class User(
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

    var UserDocs : MutableMap<String, User> = mutableMapOf()
    var UserID : String = ""

    const val TAG  = "UserManager"

    fun setUserId(context : Context?, id : String) {
        Log.i(TAG, "Setting user id...")
        /*runBlocking {
            val getAttrJob = GlobalScope.launch {
                UserID = id ?: DBUtility.AWSInstance.userAttributes["custom:FireStoreID"].toString()
                Log.i(TAG, "User id set to $UserID")
            }

            getAttrJob.join()
            //getAllFirestoreDocs()
        }*/

        UserID = id
        Log.i(TAG, "User id set to $UserID")
        getAllFirestoreDocs()

    }

    /**
     * Get all the firestore documents and store them in the UserDocs var
     */
    private fun getAllFirestoreDocs() {
        Log.i(TAG, "Getting all firestore docs....")
        DBUtility.FirebaseInstance.collection("CoinBank")
            .get()
            .addOnSuccessListener { documents ->
                Log.i(TAG, "Retrieved all firestore docs successfully")
                for(doc in documents) {
                    UserDocs[doc.id] = doc.toObject()
                }
            }

            .addOnFailureListener {e ->
                Log.e(TAG, "Unable to retrieve firestore docs")
                Log.e(TAG, e.message)
            }
    }
}

