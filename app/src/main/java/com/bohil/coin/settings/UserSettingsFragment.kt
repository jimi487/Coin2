package com.bohil.coin.settings

import android.os.Bundle
import android.util.Log
import com.bohil.coin.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.bohil.coin.DBUtility
import com.bohil.coin.databinding.FragmentUserSettingsBinding


class UserSettingsFragment : Fragment() {

    private lateinit var binding: FragmentUserSettingsBinding
    private val _userData = DBUtility.getUserInfo(context)

    private val _fName = _userData?.first
    private val _lName = _userData?.last
    private val _birthday = _userData?.birthdate
    private val _country = _userData?.country
    private val _igHandle = _userData?.igHandle
    private val _snapHandle = _userData?.snapchatHandle
    private val _twitterHandle = _userData?.twitterHandle
    private val _sex = _userData?.sex
    private val _language = _userData?.language


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_user_settings, container, false)
        return binding.root
    }



}