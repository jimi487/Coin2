package com.bohil.coin.login.registration

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.results.SignUpResult
import com.bohil.coin.databinding.FragmentRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import java.util.*

class RegisterFragment : Fragment() {

    companion object {
        fun newInstance() = RegisterFragment()
    }

    private lateinit var viewModel: RegisterViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val binding =
            DataBindingUtil.inflate<FragmentRegisterBinding>(inflater, com.bohil.coin.R.layout.fragment_register, container, false)

        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        Toast.makeText(activity, user.toString(), Toast.LENGTH_LONG).show()

        // Enable the complete registration button only if the user agrees the information provided is accurate
        binding.confirmationCheckbox.setOnClickListener{view ->
            //TODO Fix code so button changes availability not depending on when the checkbox is clicked
            // in other words, make it so when all fields are properly filled, the button becomes available
            // Display toasts for which field is not properly completed
            with(view as CheckBox) {
                if (view.isChecked) {
                    // If the checkbox is checked, makes sure the other fields aren't empty
                    binding.finishButton.setBackgroundColor(Color.WHITE)
                    binding.finishButton.isClickable = true

                } else {
                    binding.finishButton.setBackgroundColor(Color.GRAY)
                    binding.finishButton.isClickable = false
                }
            }
        }


        // Adding navigation to the camera icon
        binding.faceImageButton.setOnClickListener {
            it.findNavController().navigate(RegisterFragmentDirections.actionRegisterFragmentToPreviewActivity())
        }

        binding.finishButton.setOnClickListener {

            val firstName = binding.firstNameEditText.text.toString()
            val lastName = binding.lastNameEditText.text.toString()
            val age = binding.ageTextField.text.toString()
            val lang = binding.languageSpinner.selectedItem.toString()
            val sex = binding.sexSpinner.selectedItem.toString()
            val country = binding.countrySpinner.selectedItem.toString()


            val listOfStrings = listOf(firstName, lastName, age)

            if (listOfStrings.contains("")) {
                if(listOfStrings.filter { it == "" }.isNotEmpty()) {
                    Toast.makeText(context, "All fields must be filled", Toast.LENGTH_SHORT).show()
                }
            } else {
                //Test(firstName, lastName)
            }
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
