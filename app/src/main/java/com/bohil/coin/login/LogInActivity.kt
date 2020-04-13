package com.bohil.coin.login

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.mobile.client.*
import com.bohil.coin.R
import com.bohil.coin.DBUtility
import com.bohil.coin.settings.UserManager


/**
 * This Activity handles the Sign Up and Log In of users
 */
class LogInActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        DBUtility.initAWS(applicationContext)
    }


    override fun onDestroy() {
        super.onDestroy()
        // Signs the user out
        DBUtility.signOutAWS()
        cacheDir.deleteRecursively()
    }
}
