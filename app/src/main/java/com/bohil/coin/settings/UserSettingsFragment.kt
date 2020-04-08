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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_user_settings, container, false)
        //getUserInfo()
        return binding.root
    }



}