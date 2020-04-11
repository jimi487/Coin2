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

    var CurrentUser : User? = null
    var UserID : String = ""

    const val TAG  = "UserManager"

    fun setUserId(context : Context?) : String {
        Log.i(TAG, "Setting user id...")
        runBlocking {
            val getAttrJob = GlobalScope.launch {
                UserID =
                    DBUtility.AWSInstance.userAttributes[context!!.getString(R.string.cognito_firestore)].toString()
                Log.i(TAG, "User id set to $UserID")
            }

            getAttrJob.join()

            getAndUpdateUserInfo(UserID)
        }
            return UserID
        }



    private fun getAndUpdateUserInfo(id : String) {

            Log.e(TAG, "*/*/*/*/*/*GETTING DOC....*/*/*/*/*/*")
            DBUtility.FirebaseInstance.collection("CoinBank").document(id)
                .addSnapshotListener(object : EventListener<DocumentSnapshot?> {
                    override fun onEvent(@Nullable snapshot: DocumentSnapshot?, @Nullable e: FirebaseFirestoreException?) {
                        if (e != null) {
                            Log.w(TAG, "Listen failed.", e)
                            return
                        }
                        if (snapshot != null && snapshot.exists()) {
                            Log.e(TAG, "*/*/*/*/*/*GOT DOC....Updating data....*/*/*/*/*/*")
                            Log.d(TAG, "Current data: " + snapshot.data)

                            snapshot.data?.let { updateUserInfo(it) }
                            Log.e(TAG, "*/*/*/*/*/*Update data complete...*/*/*/*/*/*")

                        } else {
                            Log.d(TAG, "Current data: null")
                        }
                    }
                })
        }


        fun updateUserInfo(data: MutableMap<String, Any?>) {
            CurrentUser = User(
                data["birthdate"].toString(),
                data["country"].toString(),
                data["first"].toString(),
                data["last"].toString(),
                data["igHandle"].toString(),
                data["twitterHandle"].toString(),
                data["snapchatHandle"].toString(),
                data["sex"].toString(),
                data["language"].toString()
            )
        }
    }

