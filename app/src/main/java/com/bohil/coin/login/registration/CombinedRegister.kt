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
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.results.SignUpResult
import com.bohil.coin.R
import com.bohil.coin.databinding.CombinedRegisterBinding
import com.bohil.coin.databinding.FragmentTitleBinding
import java.util.HashMap

class CombinedRegister : Fragment() {

    private lateinit var binding: CombinedRegisterBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.combined_register, container, false)
        binding.lifecycleOwner = this

        binding.registerButton.setOnClickListener { validateForm() }
        binding.confirmationCheckbox.setOnClickListener{ toggleSignup() }

        return binding.root
    }

    private fun toggleSignup() {
        binding.registerButton.isEnabled = binding.confirmationCheckbox.isChecked
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

            beginSignUpProcess(binding.username.text.toString(), binding.password.text.toString())
            //findNavController().navigate(SignUpFragmentDirections.actionSignUpFragmentToRegisterFragment())
        }
    }

    /**
     * Function used to sign-up a user to the Cognito user pool by sending a verification link
     * to the e-mail address provided. Will display error messages if the e-mail is already in use,
     * or if the password is too short.
     *
     * @param user: The username (e-mail) provided by the user
     * @param pass: The password provided by the user
     *
     * TODO: Implement redirect to login (?) or another fragment after verification e-mail sent
     */
    private fun beginSignUpProcess(user: String, pass: String) {

        var success = false

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
                        } else {
                            binding.TxtProgress.text = getString(R.string.signup_success)
                        }
                    }

                    this@CombinedRegister.activity!!.runOnUiThread {
                        binding.loading.visibility = View.GONE
                        //binding.BtnNext.visibility = View.VISIBLE
                        binding.registerButton.visibility = View.GONE
                    }

                }

                override fun onError(e: Exception) {
                    Log.e("ERR IN SIGN UP", e.message)
                    this@CombinedRegister.activity!!.runOnUiThread {
                        val errMessage = e.message.toString().toLowerCase()

                        //Not an ideal way of checking for the type of exception.. might want to find another way
                        with(errMessage) {
                            when {
                                contains("exists") -> Toast.makeText(context, getString(R.string.email_in_use), Toast.LENGTH_LONG).show()
                                contains("password") -> Toast.makeText(context, getString(R.string.pass_too_short), Toast.LENGTH_LONG).show()
                                contains("http") -> Toast.makeText(context, "An active internet connection is required", Toast.LENGTH_LONG).show()
                            }
                        }
                        binding.TxtProgress.text = ""
                        binding.loading.visibility = View.GONE
                    }

                }
            })
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

    companion object{private const val TAG = "EmailPassword"}


}