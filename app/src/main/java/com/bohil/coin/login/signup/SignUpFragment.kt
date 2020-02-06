package com.bohil.coin.login.signup


import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.results.SignUpResult
import com.bohil.coin.R
import com.bohil.coin.databinding.FragmentSignUpBinding
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import java.util.*


class SignUpFragment : Fragment() {

    private lateinit var viewModel: SignUpViewModel
    private lateinit var binding: FragmentSignUpBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_sign_up, container, false)

        binding.registerButton.setOnClickListener { validateForm() }
        // Long click the submit button to bypass registration
        /*binding.registerButton.setOnLongClickListener{
            findNavController().navigate(SignUpFragmentDirections.actionSignUpFragmentToRegisterFragment())
            true
        }*/

        auth = FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(SignUpViewModel::class.java)
    }

    private fun validateForm() {
        var valid = true
        val email = binding.username.text.toString()
        val password = binding.password.text.toString()

        if(TextUtils.isEmpty(email)) {
            binding.username.error = "Required"
            valid = false
        } else if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            binding.username.error = "Please input a valid email address"
            valid = false
        }
        else {
            binding.username.error = null
        }

        if(TextUtils.isEmpty(password)){
            binding.password.error = "Required"
            valid = false
        } else{
            binding.password.error = null
        }
        if(valid){

            Test(binding.username.text.toString(), binding.password.text.toString())
            /*createAccount(email, password)
            when(FirebaseAuth.getInstance().currentUser){
                null -> Toast.makeText(activity, "The user was not successfully created", Toast.LENGTH_LONG).show()
                else -> {
                    findNavController().navigate(SignUpFragmentDirections.actionSignUpFragmentToRegisterFragment())

                }
            }*/
        }
    }

    private fun createAccount(email: String, password: String): Boolean{

        var success = false
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener (activity!!){task ->
                if(task.isSuccessful){
                    success = true
                    val user = auth.currentUser
                }else{
                    success = false
                    Toast.makeText(activity, "Something was wrong with the registration", Toast.LENGTH_LONG).show()
                }
            }
        return success
    }

    //TODO Implement a proper email verification sequence
    private fun sendEmailVerificationWithContinueUrl(){
        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        val url = "http://www.example.com/verify?uid=" + user?.uid
        val actionCodeSettings = ActionCodeSettings.newBuilder()
            .setUrl(url)
            .setIOSBundleId("com.example.ios")
            .setAndroidPackageName("com.bohil.coin", false, null)
            .build()

        user?.sendEmailVerification(actionCodeSettings)?.addOnCompleteListener { task ->
            // What to do if the email link is successfully sent
            if(task.isSuccessful){

            }
        }
    }

    fun Test(user: String, pass: String) {

        binding.loading.visibility = View.VISIBLE
        binding.TxtProgress.text = "Signing you up..."
        val attributes: MutableMap<String, String> =
            HashMap()
        attributes["email"] = user
        AWSMobileClient.getInstance().signUp(
            user,
            pass,
            attributes,
            null,
            object :
                Callback<SignUpResult> {
                override fun onResult(signUpResult: SignUpResult) {
                    activity!!.runOnUiThread {

                        Log.d(
                            "TESTTEST",
                            "Sign-up callback state: " + signUpResult.confirmationState
                        )
                        if (!signUpResult.confirmationState) {
                            val details =
                                signUpResult.userCodeDeliveryDetails
                            Log.i(
                                "CONFIRM",
                                "Confirm sign-up with: " + details.destination
                            )
                            binding.TxtProgress.text = "Confirm sign-up with " + details.destination

                        } else {
                            Log.i("COMPLETE", "Sign-up done.")
                            binding.TxtProgress.text = "Sign-up completed successfully"
                        }
                    }
                }

                override fun onError(e: Exception) {
                    binding.TxtProgress.text = e.toString()
                    binding.loading.visibility = View.GONE
                }
            })
    }

    companion object{private const val TAG = "EmailPassword"}

}
