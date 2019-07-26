package com.bohil.coin

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.bohil.coin.databinding.FragmentTitleBinding

class TitleFragment : Fragment() {


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentTitleBinding>(inflater, R.layout.fragment_title, container, false)

        // Setting the background videos
        setBackgroundVideo(binding)

        return binding.root
    }


    // Sets the background video for the main screen
    private fun setBackgroundVideo(binding: FragmentTitleBinding) {
        binding.backgroundVideo.setVideoURI(Uri.parse("android.resource://" + activity?.packageName + "/" + R.raw.b1))
        binding.backgroundVideo.start()
    }


}
