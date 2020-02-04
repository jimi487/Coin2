package com.bohil.coin.login.title

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bohil.coin.R
import com.bohil.coin.databinding.FragmentTitleBinding

class TitleFragment : Fragment() {


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentTitleBinding>(inflater,
            R.layout.fragment_title, container, false)

        binding.signupButton.setOnClickListener {
            findNavController().navigate(TitleFragmentDirections.actionTitleFragmentToDisclosureFragment())
        }

        binding.loginButton.setOnClickListener { navigateToMainActivity() }

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


    private fun navigateToMainActivity(){
        val directions = TitleFragmentDirections.actionTitleFragmentToMainActivity()
        findNavController().navigate(directions)
    }

}
