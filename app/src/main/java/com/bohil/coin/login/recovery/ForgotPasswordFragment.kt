package com.bohil.coin.login.recovery

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
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.results.ForgotPasswordResult
import com.amazonaws.mobile.client.results.ForgotPasswordState
import com.bohil.coin.DBUtility
import com.bohil.coin.R
import com.bohil.coin.databinding.FragmentForgotPassBinding


class ForgotPasswordFragment : Fragment() {

    private lateinit var binding : FragmentForgotPassBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_forgot_pass, container, false)

        binding.BtnReset.setOnClickListener{

            if (validate(1)) {
                sendConfirmationCode(binding.TxtEmail.text.toString())
            }
        }

        binding.BtnConfirmCode.setOnClickListener{
            if (validate(2)) {
                confirmCode(binding.TxtNewPassword.text.toString(), binding.TxtConfirmationCode.text.toString())
            }
        }

        binding.BtnLogin.setOnClickListener {
            findNavController().navigate(ForgotPasswordFragmentDirections.actionForgotPasswordFragmentToTitleFragment())
        }

        return binding.root
    }

    private fun validate(step : Int) : Boolean {
        var toReturn = true
         when(step) {
             1 -> {
                 if (TextUtils.isEmpty(binding.TxtEmail.text)) {
                     binding.TxtEmail.error = "Required"
                     toReturn = false
                 } else {
                     binding.TxtEmail.error = null
                 }
             }

             2 -> {
                 val areEmpty = listOf(binding.TxtConfirmationCode, binding.TxtNewPassword)
                 for (textBox in areEmpty) {
                     if(TextUtils.isEmpty(textBox.text.toString())) {
                         textBox.error = "Required"
                         toReturn = false
                     } else {
                         textBox.error = null
                     }
                 }
             }
         }

        return toReturn
    }


    private fun sendConfirmationCode(email : String) {
        DBUtility.AWSInstance
            .forgotPassword(email, object : Callback<ForgotPasswordResult?> {

                override fun onResult(result: ForgotPasswordResult?) {
                    runOnUiThread(Runnable {
                        Log.d(TAG, "forgot password state: " + result?.state)
                        when (result?.state) {
                            ForgotPasswordState.CONFIRMATION_CODE -> showStep2()
                            else -> Log.e(TAG, "un-supported forgot password state")
                        }
                    })
                }

                override fun onError(e: Exception?) {
                    Log.e(TAG, "forgot password error", e)
                    val error = e?.message.toString()
                    with(error) {
                        when {
                            contains("LimitExceededException") -> makeToast("Attempt limit exceeded, please try again after some time.", 1)
                        }
                    }
                }

            })
    }

    private fun confirmCode(newPass : String, confirmCode : String) {
        DBUtility.AWSInstance.confirmForgotPassword(newPass, confirmCode, object : Callback<ForgotPasswordResult> {

            override fun onResult(result: ForgotPasswordResult) {
                    runOnUiThread {
                        Log.d(TAG, "forgot password state: " + result.state)
                        when (result.state) {
                            ForgotPasswordState.DONE -> {
                                makeToast("Password changed successfully!")
                                binding.BtnConfirmCode.visibility = View.INVISIBLE
                                binding.BtnLogin.visibility = View.VISIBLE
                            }
                            else -> Log.e(
                                TAG,
                                "un-supported forgot password state"
                            )
                        }
                    }
                }

                override fun onError(e: java.lang.Exception) {
                    val error = e.message.toString()
                    with(error) {
                        when {
                            contains("CodeMismatchException") -> makeToast("Please input the correct code")
                            contains("InvalidParameterException") -> makeToast(getString(R.string.pass_too_short))
                            contains("LimitExceededException") -> makeToast("Attempt limit exceeded, please try again after some time.", 1)
                        }
                    }
                }
            })
    }

    private fun showStep2() {
        var toShow = listOf(binding.LblCodeSent, binding.TxtConfirmationCode, binding.TxtNewPassword, binding.BtnConfirmCode)
        var toHide = listOf(binding.LblInstructions, binding.TxtEmail, binding.BtnReset)

        toShow.forEach { t -> t.visibility = View.VISIBLE }
        toHide.forEach { t -> t.visibility = View.INVISIBLE }

    }


    private fun makeToast(msg: String, length: Int = 0) {
        if (length == 0)
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() else
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val TAG = "ForgotPassword"
    }

}