package com.bohil.coin.login.signup

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread
import com.bohil.coin.DBUtility
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.results.SignInResult
import com.amazonaws.mobile.client.results.SignInState
import com.amazonaws.mobile.client.results.SignUpResult
import com.bohil.coin.R
import com.bohil.coin.databinding.FragmentSignUpBinding
import java.util.*


class SignUpFragment : Fragment() {

    private lateinit var binding: FragmentSignUpBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_sign_up, container, false)

        binding.signupButton.setOnClickListener { validateForm() }
        // Long click the submit button to bypass registration
        binding.signupButton.setOnLongClickListener{
            findNavController().navigate(SignUpFragmentDirections.actionSignUpFragmentToCombinedFragment())
            true
        }
        return binding.root
    }

    private fun validateForm() {
        var valid = true
        binding.TxtErrors.text = ""
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
    private fun beginSignUpProcess(user: String, pass: String) {
        binding.loading.visibility = View.VISIBLE
        binding.TxtProgress.text = getString(R.string.sign_up_in_progress)

        val attributes: MutableMap<String, String> =
            HashMap()
        attributes["email"] = user

            DBUtility.AWSInstance.signUp(
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
                                binding.signupButton.text = getString(R.string.confirm_button)

                                // Changing the button on click to confirm the verification code
                                binding.signupButton.setOnClickListener {verifySignUp(user)}

                            } else {
                                binding.TxtProgress.text = getString(R.string.signup_success)
                            }
                        }
                    }

                    override fun onError(e: Exception) {
                        Log.e("ERR IN SIGN UP", e.message)
                        this@SignUpFragment.activity?.runOnUiThread {
                            val errMessage = e.message.toString()

                            with(errMessage) {
                                when {
                                    contains("UsernameExistsException") -> binding.TxtErrors.text = getString(R.string.email_in_use)
                                    contains("InvalidPasswordException") or contains("InvalidParameterException") -> binding.TxtErrors.text = getString(R.string.pass_too_short)
                                    contains("HTTP") -> binding.TxtErrors.text = "An active internet connection is required"
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
    private fun verifySignUp(userName: String){
        val code = binding.confirmCode.text.toString()

        DBUtility.AWSInstance.confirmSignUp(
            userName,
            code,
            object :
                Callback<SignUpResult> {
                override fun onResult(signUpResult: SignUpResult) {
                    runOnUiThread(Runnable {
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
                    runOnUiThread(Runnable {
                        Log.e(TAG, "Confirm sign-up error", e)
                        with(e.message.toString()) {
                            when {
                                contains("CodeMismatchException") or contains("InvalidParameterException") -> makeToast(
                                    "Please input the correct code"
                                )
                            }
                        }
                    })
                }

            })

    }

    /**
     * Signs the user in using provided credentials
     */
    private fun signIn(){

        val username = binding.username.text.toString()
        val password = binding.password.text.toString()

        DBUtility.AWSInstance.signIn(
            username,
            password,
            null,
            object : Callback<SignInResult> {
                override fun onResult(signInResult: SignInResult) {
                    runOnUiThread {
                        Log.d(TAG, "Sign-in callback state: " + signInResult.signInState)
                        when (signInResult.signInState) {
                            SignInState.DONE -> {
                                makeToast("Sign-in Complete")
                                findNavController().navigate(SignUpFragmentDirections.actionSignUpFragmentToCombinedFragment()) }
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

    /*
    Utility function to help make toast
     */
    private fun makeToast(msg:String){
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    //TODO Handle when the user is redirected back to the app
    /**
     * Handles when the user is redirected back to the app
     */
    override fun onResume() {
        super.onResume()
        val activityIntent = Intent()
        if (activityIntent.data != null && "myapp" == activityIntent.data?.scheme) {
            DBUtility.AWSInstance.handleAuthResponse(activityIntent)
        }
    }

    companion object{ private const val TAG = "SignUpFragment"

}}
