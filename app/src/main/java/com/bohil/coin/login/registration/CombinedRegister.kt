package com.bohil.coin.login.registration

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.results.SignInResult
import com.amazonaws.mobile.client.results.SignInState
import com.amazonaws.mobile.client.results.SignUpResult
import com.bohil.coin.R
import com.bohil.coin.databinding.CombinedRegisterBinding
import java.util.HashMap

class CombinedRegister : Fragment() {

    private lateinit var binding: CombinedRegisterBinding
    private lateinit var viewModel:RegisterViewModel
    private lateinit var firstName : String
    private lateinit var lastName : String
    private lateinit var dob : String
    private lateinit var sex : String
    private lateinit var lang : String
    private lateinit var country : String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        viewModel = ViewModelProviders.of(this).get(RegisterViewModel::class.java)

        binding = DataBindingUtil.inflate(inflater, R.layout.combined_register, container, false)
        binding.lifecycleOwner = this

        binding.registerButton.setOnClickListener { validateForm() }
        binding.confirmationCheckbox.setOnClickListener { toggleSignup() }

        // Adding navigation to the camera icon
        /*binding.faceImageButton.setOnClickListener {
            it.findNavController().navigate(RegisterFragmentDirections.actionRegisterFragmentToPreviewActivity())
        }*/




        return binding.root
    }

    private fun toggleSignup() {
        binding.registerButton.isEnabled = binding.confirmationCheckbox.isChecked
    }

    private fun validateForm() {
        var valid = true

        //AWS Fields
        val email = binding.username.text.toString()
        val password = binding.password.text.toString()

        //Firebase Fields
        firstName = binding.firstNameEditText.text.toString()
        lastName = binding.lastNameEditText.text.toString()
        dob = binding.dobText.text.toString()
        lang = binding.languageSpinner.selectedItem.toString()
        sex = binding.sexSpinner.selectedItem.toString()
        country = binding.countrySpinner.selectedItem.toString()
        val listOfStrings = listOf(firstName, lastName, dob)

        if (TextUtils.isEmpty(email)) {
            binding.username.error = "Required"
            valid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.username.error = "Please input a valid email address"
            valid = false
        } else {
            binding.username.error = null
        }

        if (TextUtils.isEmpty(password)) {
            binding.password.error = "Required"
            valid = false
        } else {
            binding.password.error = null
        }

        when {
            listOfStrings.contains("") -> if (listOfStrings.filter { it == "" }.isNotEmpty()) {
                Toast.makeText(context, "All fields must be filled", Toast.LENGTH_SHORT).show()
                valid = false
            }
        }

        if (valid) {
            //Add user to AWS
            beginSignUpProcess(binding.username.text.toString(), binding.password.text.toString())
        }
    }

    /**
     * Function used to sign-up a user to the Cognito user pool by sending a verification code
     * to the e-mail address provided. Will display error messages if the e-mail is already in use,
     * or if the password is too short.
     *
     * @param user: The username (e-mail) provided by the user
     * @param pass: The password provided by the user
     *
     * TODO: Implement redirect to login (?) or another fragment after verification e-mail sent
     */
    private fun beginSignUpProcess (user: String, pass: String) {
        binding.loading.visibility = View.VISIBLE
        binding.TxtProgress.text = getString(R.string.sign_up_in_progress)


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

                        if (!signUpResult.confirmationState) {
                            binding.TxtProgress.text = getString(R.string.check_inbox)
                            binding.loading.visibility = View.INVISIBLE
                            binding.confirmCode.visibility = View.VISIBLE
                            binding.registerButton.text = getString(R.string.confirm_button)

                            // Changing the button on click to confirm the verification code
                            binding.registerButton.setOnClickListener { verifySignUp(user) }



                        } else {
                            binding.TxtProgress.text = getString(R.string.signup_success)
                        }
                    }

                    //Create user in Firebase
                    viewModel.addUser(firstName,lastName, lang, country,sex, dob, getString(R.string.firestore_table), getString(R.string.cognito_firestore))
                }

                override fun onError(e: Exception) {
                    Log.e("ERR IN SIGN UP", e.message)
                    this@CombinedRegister.activity?.runOnUiThread {
                        val errMessage = e.message.toString()//.toLowerCase()

                        //Not an ideal way of checking for the type of exception.. might want to find another way
                        with(errMessage) {
                            when {
                                contains("exists") -> makeToast(getString(R.string.email_in_use))
                                contains("password") -> makeToast(getString(R.string.pass_too_short))
                                contains("http") -> makeToast("An active internet connection is required")
                            }
                        }
                        binding.TxtProgress.text = ""
                        binding.loading.visibility = View.GONE

                    }

                }
            })
    }

    /**
     * Confirms the user using the code sent to their email
     */
    private fun verifySignUp(userName: String) {
        val code = binding.confirmCode.text.toString()

        AWSMobileClient.getInstance().confirmSignUp(
            userName,
            code,
            object :
                Callback<SignUpResult> {
                override fun onResult(signUpResult: SignUpResult) {
                    ThreadUtils.runOnUiThread(Runnable {
                        Log.d(TAG, "Sign-up callback state: " + signUpResult.confirmationState)
                        if (!signUpResult.confirmationState) {
                            val details = signUpResult.userCodeDeliveryDetails
                            makeToast("Confirm sign-up with: " + details.destination)

                        } else {
                            //Sign up completed
                            makeToast("Sign up Complete")
                            signIn()
                        }
                    })
                }

                override fun onError(e: java.lang.Exception) {
                    Log.e(TAG, "Confirm sign-up error", e)

                    this@CombinedRegister.activity?.runOnUiThread {
                        makeToast("Invalid code")
                    }
                }
            })

    }


    /**
     * Signs the user in using provided credentials
     */
    private fun signIn() {

        val username = binding.username.text.toString()
        val password = binding.password.text.toString()

        AWSMobileClient.getInstance().signIn(
            username,
            password,
            null,
            object : Callback<SignInResult> {
                override fun onResult(signInResult: SignInResult) {
                    ThreadUtils.runOnUiThread {
                        Log.d(TAG, "Sign-in callback state: " + signInResult.signInState)
                        when (signInResult.signInState) {
                            SignInState.DONE -> {
                                makeToast("Sign-in Complete")
                                findNavController().navigate(CombinedRegisterDirections.combinedFragmentToPreviewActivity())
                            }

                            SignInState.SMS_MFA -> makeToast("Please confirm sign-in with SMS.")
                            SignInState.NEW_PASSWORD_REQUIRED -> makeToast("Please confirm sign-in with new password.")
                            else -> makeToast("Unsupported sign-in confirmation: " + signInResult.signInState)
                        }
                    }
                }


                override fun onError(e: java.lang.Exception) {
                    Log.e(TAG, "Sign-in error", e)
                    binding.TxtProgress.text = e.toString()
                }
            })
    }

    /**
     * Utility function to help make toast
     */
    private fun makeToast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    /**
     * Handles when the user is redirected back to the app
     */
    override fun onResume() {
        super.onResume()
        val activityIntent = Intent()
        if (activityIntent.data != null && "myapp" == activityIntent.data?.scheme) {
            AWSMobileClient.getInstance().handleAuthResponse(activityIntent)
        }
    }

    companion object {
        private const val TAG = "SignUpFragment"
    }
}
