package com.bohil.coin

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.amazonaws.mobile.client.*
import com.amazonaws.mobile.client.results.SignInResult
import com.amazonaws.mobile.client.results.SignInState
import com.amplifyframework.core.Amplify
import com.amplifyframework.storage.s3.AWSS3StoragePlugin
import com.bohil.coin.databinding.FragmentTitleBinding
import com.bohil.coin.login.title.TitleFragment
import com.bohil.coin.main.SimpleNavActivity

/**
 * Class containing common DB methods
 */
object DBUtility {

    fun initAWS(applicationContext : Context) {
        // Initializing the AWS Amplify instance
        getAWSInstance()
        .initialize(applicationContext, object : Callback<UserStateDetails> {
            override fun onResult(userStateDetails: UserStateDetails) {
                try {
                    Amplify.addPlugin(AWSS3StoragePlugin())
                    Amplify.configure(applicationContext)
                    when(userStateDetails.userState){
                        UserState.SIGNED_IN -> getAWSInstance().signOut()
                        UserState.SIGNED_OUT -> getAWSInstance().tokens
                    }

                    // IMPORTANT! Refreshes the access tokens stored in the cache
                    //DBUtility.getAWSInstance().tokens
                } catch (e: java.lang.Exception) {
                    Log.e("ApiQuickstart", e.message)
                }
            }

            override fun onError(e: Exception?) {
                Log.e("INIT", "Initialization error.", e)
            }
        }
        )
    }

    fun signOutAWS() {
        getAWSInstance().signOut(
            SignOutOptions.builder().signOutGlobally(true).build(),
            object : Callback<Void?> {
                override fun onResult(result: Void?) {
                    Log.d("LogInActivity", "signed-out")
                }

                override fun onError(e: java.lang.Exception) {
                    Log.e("LogInActivity", "sign-out error", e)
                }
            })
    }

    fun getAWSInstance() : AWSMobileClient {
        return AWSMobileClient.getInstance()
    }

}


