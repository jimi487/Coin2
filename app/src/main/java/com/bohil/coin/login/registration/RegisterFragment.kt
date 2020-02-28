package com.bohil.coin.login.registration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.bohil.coin.R
import com.bohil.coin.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment() {

    private lateinit var binding:FragmentRegisterBinding
    private lateinit var viewModel:RegisterViewModel
    private var mContext = context
    companion object {
        fun newInstance() = RegisterFragment()

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(RegisterViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        binding = DataBindingUtil.inflate<FragmentRegisterBinding>(inflater, com.bohil.coin.R.layout.fragment_register, container, false)

        // Adding navigation to the camera icon
        binding.faceImageButton.setOnClickListener {
            it.findNavController().navigate(RegisterFragmentDirections.actionRegisterFragmentToPreviewActivity())
        }

        // Verifying the form has been completed successfully
        binding.finishButton.setOnClickListener {
            verify_form()
        }
        // Long click to skip adding user to database
        binding.finishButton.setOnLongClickListener {
            findNavController().navigate(RegisterFragmentDirections.actionRegisterFragmentToPreviewActivity())
            true
        }
        return binding.root
    }

    /**
     * Verifies whether the form was correctly filled out
     */
    private fun verify_form(){

        var completed = false
        val firstName = binding.firstNameEditText.text.toString()
        val lastName = binding.lastNameEditText.text.toString()
        val dob = binding.dobText.text.toString()
        val lang = binding.languageSpinner.selectedItem.toString()
        val sex = binding.sexSpinner.selectedItem.toString()
        val country = binding.countrySpinner.selectedItem.toString()

        val listOfStrings = listOf(firstName, lastName, dob)

        when {
            listOfStrings.contains("") -> if (listOfStrings.filter { it == "" }.isNotEmpty())
                Toast.makeText(context, "All fields must be filled", Toast.LENGTH_SHORT).show()
            !binding.confirmationCheckbox.isChecked ->
                Toast.makeText(
                    context,
                    "You must agree the information is accurate",
                    Toast.LENGTH_SHORT
                ).show()
            else -> completed = true
        }

        // When the form has been successfully completed, navigate to the Preview Activity
        // Also creates the user in the database
        if (completed && binding.confirmationCheckbox.isChecked) {
            viewModel.addUser(firstName,lastName, lang, country,sex, dob, getString(R.string.firestore_table), getString(R.string.cognito_firestore))
            findNavController().navigate(RegisterFragmentDirections.actionRegisterFragmentToPreviewActivity())
        }
    }
}
