package com.bohil.coin.login

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

/*
 * Uses Firebase Auth to Sign the user up
 */
class SignUp2Activity : AppCompatActivity() {


    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //binding = DataBindingUtil.setContentView(this, R.layout.fragment_sign_up)

        //On click listeners for the buttons

        //Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

    }

    public override fun onStart(){
        super.onStart()
        // Check if the user is signed in (non-null) and update UI accordingly
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    private fun createAccount(email: String, password: String){
        if(!validateForm()){
            return
        }

        //showProgressDialog()

        //Start creating user with email

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this){task ->
                if(task.isSuccessful){
                    //Sign in success, update UI with the signed-in user's information
                    val user = auth.currentUser
                    updateUI(user)
                } else{
                    // If sign in fails, display a message to the user
                    Toast.makeText(baseContext, "Authentication failed", Toast.LENGTH_LONG).show()
                    updateUI(null)
                }

                // Start exclude
                //hideProgressDialog()
            }

    }

    private fun signIn(email: String, password: String) {
        if (!validateForm()) {
            return
        }
        //showProgressDialog()

        // Start sign in with email
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed in user's information
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user
                    Toast.makeText(baseContext, "Authentication failed", Toast.LENGTH_LONG).show()
                    updateUI(null)
                }

                //Start exclude
                if (!task.isSuccessful) {
                    // Change TextView if task unsuccessful
                }

                //hideProgressDialog()
            }
    }

    private fun signOut(){
        auth.signOut()
        updateUI(null)
    }

    private fun sendEmailVerification(){
        //Disable button

        // Send verification email
        val user = auth.currentUser
        user?.sendEmailVerification()?.addOnCompleteListener (this){task ->
            //Re enable button
            //verifyEmailButton.isEnabled = true

            if(task.isSuccessful){
                Toast.makeText(baseContext, "Verification email sent to ${user.email}", Toast.LENGTH_LONG).show()
            } else{
                Toast.makeText(baseContext, "Failed to send verification email", Toast.LENGTH_LONG).show()
            }
        }
    }

    /*
    Checks to see whether the form has been filled out correctly
     */
    private fun validateForm(): Boolean{
        var valid = true

        // Email textview
        /*val email = fieldEmail.text.toString()
        if(TextUtils.isEmpty(email)){
            fieldEmail.error = "Required"
            valid = false
            } else{
            fieldEmail.error = null
            }
            val password = fieldPassword.text.toString()
            if(TextUtils.isEmpty(password)){
            fieldPassword.error = "Required"
            valid = false
            } else{
            fieldPassword.error = null
            }

            */
            return valid
    }

    /*
    Checks to see if the user is currently logged in and updates UI accordingly
     */
    private fun updateUI(user: FirebaseUser?){
        //hideProgressDialog()
        if(user != null){
            /*
            status.text = getString(R.string.emailpassword_status_fmt, user.email, user.isEmailVerified)
            detail.text = getString(R.string.firebase_status_fmt, user.uid)

            emailPasswordButtons.visiblity = View.GONE
            emailPasswordFields.visibility = View.GONE
            signedInButtons.visibility = View.VISIBLE

            verifyEmailButton.isEnabled = !user.isEmailVerified*/
        }else{
            /*
            status.setText(R.string.signed_out)
            detail.text = null
            emailpasswordButtons.visiblity = View.VISIBLE
            emailPasswordFields.visibility = View.VISIBLE
            signedInButtons.visibility = View.GONE
             */
        }
    }



    companion object{
        private const val TAG = "EmailPassword"
    }
}
