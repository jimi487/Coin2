package com.bohil.coin.login

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.UserStateDetails
import com.amazonaws.cognito.*
import com.bohil.coin.R


/**
 * This Activity handles the Sign Up and Log In of users
 */
class LogInActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        AWSMobileClient.getInstance()
            .initialize(applicationContext, object : Callback<UserStateDetails> {
                override fun onResult(userStateDetails: UserStateDetails) {
                    android.util.Log.i("INIT", "onResult: " + userStateDetails.userState)
                }

                override fun onError(e: Exception?) {
                    android.util.Log.e("INIT", "Initialization error.", e)
                }
            }
            )
    }

}
