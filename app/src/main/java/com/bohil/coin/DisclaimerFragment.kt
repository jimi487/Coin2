package com.bohil.coin

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bohil.coin.databinding.DisclaimerFragmentBinding


class DisclaimerFragment: Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding =
            DataBindingUtil.inflate<DisclaimerFragmentBinding>(inflater, R.layout.disclaimer_fragment, container, false)

        // Loading the Disclaimer text information into the TextView
        loadingDisclaimer(binding, context!!)

        // On click to navigate to the Register Fragment
        binding.confirmButton.setOnClickListener {
            this.findNavController().navigate(DisclaimerFragmentDirections.actionDisclosureFragmentToRegisterFragment())
        }

        // Adding logic to the scrollview to only enable
        agree(binding)

        return binding.root
    }


    /**
     * Only enables the Confirm button to be visible once the user has scrolled down to the bottom of the
     * ScrollView
     */
    // TODO Add this method to the ViewModel instead
    private fun agree(binding: DisclaimerFragmentBinding){
        // Checking whether the user is at the bottom of the scroll view
        binding.disclaimerScrollView.viewTreeObserver.addOnScrollChangedListener {
            if(binding.disclaimerScrollView.getChildAt(0).bottom <= (binding.disclaimerScrollView.height +
                        binding.disclaimerScrollView.scrollY)){
                binding.confirmButton.visibility = View.VISIBLE
            }
        }
        // Else the ScrollView is somewhere in the middle
    }

    /*
    Loads the text information from the Disclaimer file into the TextView
     */
    // TODO Add this method into the ViewModel instead
    private fun loadingDisclaimer(binding: DisclaimerFragmentBinding, context: Context){
        binding.disclaimerText.text = context.assets.open("disclaimer.txt").bufferedReader().use{
            it.readText()
        }
    }


}