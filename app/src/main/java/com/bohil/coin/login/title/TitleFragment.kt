package com.bohil.coin.login.title

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.results.SignInResult
import com.amazonaws.mobile.client.results.SignInState
import com.bohil.coin.R
import com.bohil.coin.databinding.FragmentTitleBinding

//import sun.jvm.hotspot.utilities.IntArray


class TitleFragment : Fragment() {

    private var email = ""
    private var password = ""
    private lateinit var binding: FragmentTitleBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_title, container, false)

        binding.signupButton.setOnClickListener {
            findNavController().navigate(TitleFragmentDirections.actionTitleFragmentToDisclosureFragment())
        }

        binding.loginButton.setOnClickListener {
            binding.TxtErrors.text = ""
            email = binding.emailField.text.toString()
            password = binding.passwordField.text.toString()
            validateLogin()
        }

        // Setting the background videos
        setBackgroundVideo(binding)

        binding.lifecycleOwner = this
        return binding.root
    }


    // Sets the background video for the main screen
    private fun setBackgroundVideo(binding: FragmentTitleBinding) {
        //Joseph: commented out for now, missing videos on Github so won't run on my machine
        //binding.backgroundVideo.setVideoURI(Uri.parse("android.resource://" + activity?.packageName + "/" + R.raw.b1))
        //binding.backgroundVideo.start()
    }

    private fun validateLogin() {
        AWSMobileClient.getInstance()
            .signIn(email, password, null, object : Callback<SignInResult> {
                override fun onResult(signInResult: SignInResult) {
                    activity!!.runOnUiThread {
                        when (signInResult.signInState) {
                            /*SignInState.DONE -> makeToast(activity,"Sign-in done.")
                            SignInState.SMS_MFA -> makeToast("Please confirm sign-in with SMS.")
                            SignInState.NEW_PASSWORD_REQUIRED -> makeToast("Please confirm sign-in with new password.")
                            else -> makeToast("Unsupported sign-in confirmation: " + signInResult.signInState)
                            */
                            SignInState.DONE -> navigateToMainActivity()
                            SignInState.SMS_MFA -> Log.e("SMS_MFA", "")
                            SignInState.NEW_PASSWORD_REQUIRED -> Log.e("NEW_PASS_REQ", "")
                            else -> Log.e("ERR", "Unsupported sign-in confirmation: " + signInResult.signInState)
                        }
                    }
                }

                override fun onError(e: Exception?) {
                    Log.e("ERR", "Sign-in error", e)

                    this@TitleFragment.activity!!.runOnUiThread {
                        var errMessage = e?.message.toString()
                        with (errMessage) {
                            when {
                                contains("UserNotFoundException") -> binding.TxtErrors.text = "The e-mail provided does not exist."
                                contains("InvalidParameterException") -> binding.TxtErrors.text = "Please provide both your e-mail and password."
                                contains("UserNotConfirmedException") -> binding.TxtErrors.text = "Your e-mail must be confirmed before signing in."
                                contains("NotAuthorizedException") -> binding.TxtErrors.text = "Incorrect e-mail or password."
                                contains("HTTP") -> binding.TxtErrors.text = "An active internet connection is required."
                                else -> binding.TxtErrors.text = e?.message.toString()
                            }
                        }
                    }
                }
            })
    }


    private fun navigateToMainActivity(){
        findNavController().navigate(TitleFragmentDirections.actionTitleFragmentToCoinActivity())

        /*val intent = Intent(context, SimpleNavActivity::class.java)
        startActivity(intent);*/
    }

}
