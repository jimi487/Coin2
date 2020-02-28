package com.bohil.coin.login.registration


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amazonaws.mobile.client.AWSMobileClient
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "RegisterViewModel"
private lateinit var user: HashMap<String, String>
class RegisterViewModel: ViewModel() {


    /**
     * Main function to implement the add user sequence
     */
    fun addUser(fName:String, lName:String, language:String, country:String,
                sex:String, dob:String, collectionName:String, cognitoAttribute:String){
        viewModelScope.launch{
            createUser(fName, lName, language, country, sex, dob, collectionName, cognitoAttribute)
        }
    }

    /**
     * Creates the user
     * Uploads the FireStore collection to include the information provided by the user in the
     * Register Fragment.
     * Then updates the user in AWS Cognito so their FirestoreID is saved
     */
    private suspend fun createUser(fName:String, lName:String, language:String, country:String,
                                   sex:String, dob:String, collectionName:String, cognitoAttribute: String)
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
                    viewModelScope.launch { updateCognito(cognitoAttribute, it.id) }

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
        }catch(e:Exception){
            Log.e(TAG, "Unable to add key", e)
        }
        Log.d(TAG, "User key finished adding")

    }

}
