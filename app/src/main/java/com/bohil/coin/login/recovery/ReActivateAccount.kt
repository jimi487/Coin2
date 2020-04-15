package com.bohil.coin.login.recovery

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils
import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.results.SignInResult
import com.amazonaws.mobile.client.results.SignInState
import com.amazonaws.mobile.client.results.SignUpResult
import com.bohil.coin.DBUtility
import com.bohil.coin.R
import com.bohil.coin.databinding.FragmentReactivateAcctBinding
import com.bohil.coin.login.signup.SignUpFragment
import com.bohil.coin.login.signup.SignUpFragmentDirections


class ReActivateAccount : Fragment() {

    private lateinit var binding : FragmentReactivateAcctBinding
    private lateinit var email : String
    private lateinit var password : String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_reactivate_acct, container, false)

        email = arguments?.get("email") as String
        password = arguments?.get("password") as String

        binding.BtnConfirmCode.setOnClickListener {
            confirmSignUp(binding.TxtConfirmationCode.text.toString())
        }

        return binding.root
    }

    private fun confirmSignUp(code : String) {
        DBUtility.AWSInstance.confirmSignUp(email, code, object : Callback<SignUpResult> {
                override fun onResult(signUpResult: SignUpResult) {
                    runOnUiThread{
                        Log.d(
                            TAG,
                            "Sign-up callback state: " + signUpResult.confirmationState
                        )
                        if (!signUpResult.confirmationState) {
                            val details = signUpResult.userCodeDeliveryDetails
                            makeToast("Confirm sign-up with: " + details.destination)

                        } else {
                            //Sign up completed
                            makeToast("Sign up Complete")
                            signIn()
                        }
                    }
                }

                override fun onError(e: java.lang.Exception) {
                    runOnUiThread {
                        Log.e(TAG, "Confirm sign-up error", e)
                        with(e.message.toString()) {
                            when {
                                contains("CodeMismatchException") or contains("InvalidParameterException") -> makeToast(
                                    "Please input the correct code"
                                )
                            }
                        }
                    }
                }

            })

    }

    private fun signIn() {
        DBUtility.AWSInstance.signIn(
            email,
            password,
            null,
            object : Callback<SignInResult> {
                override fun onResult(signInResult: SignInResult) {
                    runOnUiThread {
                        Log.d(
                            TAG,
                            "Sign-in callback state: " + signInResult.signInState
                        )
                        when (signInResult.signInState) {
                            SignInState.DONE -> {
                                makeToast("Sign-in Complete")
                                findNavController().navigate(ReActivateAccountDirections.actionReActivateAccountToCombinedFragment())
                            }
                            SignInState.SMS_MFA -> makeToast("Please confirm sign-in with SMS.")
                            SignInState.NEW_PASSWORD_REQUIRED -> makeToast("Please confirm sign-in with new password.")
                            else -> makeToast("Unsupported sign-in confirmation: " + signInResult.signInState)
                        }
                    }

                }

                override fun onError(e: java.lang.Exception) {
                    Log.e(TAG, "Sign-in error", e)
                }
            })
    }

    private fun makeToast(msg: String, length: Int = 0) {
        if (length == 0)
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() else
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val TAG = "ReActivateAccount"
    }

}