package com.bohil.coin.settings

import com.google.firebase.firestore.DocumentReference


data class User(    val birthdate: String? = null,
                    val country: String? = null,
                    val first: String? = null,
                    val last: String? = null,
                    val igHandle: String? = null,
                    val twitterHandle: String? = null,
                    val snapchatHandle: String? = null,
                    val sex: String? = null,
                    val language: String? = null) {


    /**
     * Update the UserData var after making changes to it in other classes
     */
    fun updateUserInfo(doc: DocumentReference) {
        doc.get()
            .addOnSuccessListener { document ->
                //DBUtility.UserData = document.toObject<Users>()
                //Log.i(DBUtility.TAG, "Updated doc ref success")
            }
            .addOnFailureListener{
                //Log.e(DBUtility.TAG, "Error updating doc ref ${it.message}")
            }
    }


}