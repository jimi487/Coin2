package com.bohil.coin.login

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.mobile.client.*
import com.amplifyframework.core.Amplify
import com.amplifyframework.storage.s3.AWSS3StoragePlugin
import com.bohil.coin.R


/**
 * This Activity handles the Sign Up and Log In of users
 */
class LogInActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initializing the AWS Amplify instance
        AWSMobileClient.getInstance()
            .initialize(applicationContext, object : Callback<UserStateDetails> {
                override fun onResult(userStateDetails: UserStateDetails) {
                    try {
                        Amplify.addPlugin(AWSS3StoragePlugin())
                        Amplify.configure(applicationContext)
                        when(userStateDetails.userState){
                            UserState.SIGNED_IN -> AWSMobileClient.getInstance().signOut()
                            UserState.SIGNED_OUT -> AWSMobileClient.getInstance().tokens
                        }

                        // IMPORTANT! Refreshes the access tokens stored in the cache
                        //AWSMobileClient.getInstance().tokens
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

    override fun onDestroy() {
        super.onDestroy()
        // Signs the user out
        AWSMobileClient.getInstance().signOut(
            SignOutOptions.builder().signOutGlobally(true).build(),
            object : Callback<Void?> {
                override fun onResult(result: Void?) {
                    Log.d("LogInActivity", "signed-out")
                }

                override fun onError(e: java.lang.Exception) {
                    Log.e("LogInActivity", "sign-out error", e)
                }
            })
        cacheDir.deleteRecursively()
    }
}
