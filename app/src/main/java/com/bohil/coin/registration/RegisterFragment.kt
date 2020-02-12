package com.bohil.coin.registration

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import com.bohil.coin.R
import com.bohil.coin.databinding.RegisterFragmentBinding

class RegisterFragment : Fragment() {

    companion object {
        fun newInstance() = RegisterFragment()
    }

    private lateinit var viewModel: RegisterViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val binding =
            DataBindingUtil.inflate<RegisterFragmentBinding>(inflater, R.layout.register_fragment, container, false)

        // Enable the complete registration button only if the user agrees the information provided is accurate
        binding.confirmationCheckbox.setOnClickListener{view ->
            //TODO Fix code so button changes availability not depending on when the checkbox is clicked
            // in other words, make it so when all fields are properly filled, the button becomes available
            // Display toasts for which field is not properly completed
            with(view as CheckBox){
                if(view.isChecked){
                    // If the checkbox is checked, makes sure the other fields aren't empty
                    if(binding.firstNameEditText.text.toString() != "" && binding.lastNameEditText.text.toString()
                        != "" && binding.ageTextField.text.toString() != "") {
                        binding.finishButton.setBackgroundColor(Color.GRAY)
                        binding.finishButton.isClickable = true
                    }
                }
                else{
                    binding.finishButton.setBackgroundColor(Color.WHITE)
                    binding.finishButton.isClickable = false
                }
            }
        }

        // Adding navigation to the camera icon
        binding.faceImageButton.setOnClickListener {
            it.findNavController().navigate(RegisterFragmentDirections.actionRegisterFragmentToPreviewActivity())
        }

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // TODO Create a Factory provider to create the ViewModel
        viewModel = ViewModelProviders.of(this).get(RegisterViewModel::class.java)
        // TODO: Use the ViewModel
    }


}
