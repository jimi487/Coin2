package com.bohil.coin.login.registration

import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

//private val FIRESTORE_TABLE = getString(R.string.firestore_table)
private val test = RegisterFragment.newInstance().activity
class RegisterViewModel: ViewModel() {

    /**
     * Function to set up coroutine
     */
    fun addUser(fName:String, lName:String, language:String, country:String,
                sex:String, dob:String, collectionName:String){
        viewModelScope.launch{
            createUser(fName, lName, language, country, sex, dob, collectionName)
        }
    }

    private suspend fun createUser(fName:String, lName:String, language:String, country:String,
                                   sex:String, dob:String, collectionName:String)
            = withContext(Dispatchers.Default){
        val db = FirebaseFirestore.getInstance()

        val user = hashMapOf(
            "first" to fName,
            "last" to lName,
            "language" to language,
            "country" to country,
            "sex" to sex,
            "birthdate" to dob
        )

        //TODO Save the user ID and use it in naming the taken picture
        db.collection(collectionName)
            .add(user)
            .addOnSuccessListener {
                Log.d(TAG, "Added with ID: ${it.id}")
            }
            .addOnFailureListener{
                Log.w(TAG, "Error adding document", it)
            }

    }
}
