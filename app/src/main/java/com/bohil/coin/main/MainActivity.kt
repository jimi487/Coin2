package com.bohil.coin.main

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bohil.coin.DBUtility
import com.bohil.coin.R


/**
 * The Activity the users are directed to once logged in
 */
class MainActivity : AppCompatActivity(), Coin.OnFragmentInteractionListener{


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.i("LOGGED IN SUCCESS", "WELCOME " + DBUtility.AWSInstance.username)
    }


    override fun onFragmentInteraction(uri: Uri) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
