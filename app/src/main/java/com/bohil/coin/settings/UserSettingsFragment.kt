package com.bohil.coin.settings

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bohil.coin.DBUtility
import com.bohil.coin.R
import com.bohil.coin.databinding.FragmentUserSettingsBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class UserSettingsFragment : Fragment() {

    private lateinit var binding: FragmentUserSettingsBinding
    private var _userData : UserManager.User? = UserManager.CurrentUser

    /*private val _fName = _userData?.first
    private val _lName = _userData?.last
    private val _birthday = _userData?.birthdate
    private val _country = _userData?.country
    private val _igHandle = _userData?.igHandle
    private val _snapHandle = _userData?.snapchatHandle
    private val _twitterHandle = _userData?.twitterHandle
    private val _sex = _userData?.sex
    private val _language = _userData?.language
    */

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_user_settings, container, false)

        binding.BtnSave.setOnClickListener {
            saveInfo()
        }

        binding.BtnBack.setOnClickListener{
            findNavController().navigate(UserSettingsFragmentDirections.actionUserSettingsFragmentToCoinFragment())
        }
        setTextboxes()
        return binding.root
    }

    private fun setTextboxes() {
        Log.e("SETTINGS/*/*/*/", "Updating textboxes...")
        binding.TxtFName.setText(_userData?.first)
        binding.TxtLName.setText(_userData?.last)
        binding.TxtDOB.setText(_userData?.birthdate)
        binding.countrySpinner.setSelection(resources.getStringArray(R.array.country_array).indexOf(_userData?.country))
        binding.TxtIgHandle.setText(_userData?.igHandle)
        binding.TxtSnapHandle.setText(_userData?.snapchatHandle)
        binding.TxtTwitterHandle.setText(_userData?.twitterHandle)
        binding.sexSpinner.setSelection(resources.getStringArray(R.array.sex_array).indexOf(_userData?.sex))
        binding.languageSpinner.setSelection(resources.getStringArray(R.array.language_array).indexOf(_userData?.language))
    }

    private fun saveInfo() {
        val doc = DBUtility.FirebaseInstance.collection(context!!.getString(R.string.firestore_table)).document(UserManager.UserID)


        val user : Map<String, String> = hashMapOf(
            "first" to binding.TxtFName.text.toString(),
            "last" to binding.TxtLName.text.toString(),
            "language" to binding.languageSpinner.selectedItem.toString(),
            "country" to binding.countrySpinner.selectedItem.toString(),
            "sex" to binding.sexSpinner.selectedItem.toString(),
            "birthdate" to binding.TxtDOB.text.toString(),
            "igHandle" to binding.TxtIgHandle.text.toString(),
            "twitterHandle" to binding.TxtTwitterHandle.text.toString(),
            "snapchatHandle" to binding.TxtSnapHandle.text.toString()
        )

        val updateJob = doc.update(user)

        updateJob.addOnSuccessListener {
            Toast.makeText(context, "Save successful!", Toast.LENGTH_SHORT).show()

            //Update the doc ref in DBUtility
            //DBUtility.updateUserInfo(doc)
        }

        updateJob.addOnFailureListener{
            Toast.makeText(context, "Error with save", Toast.LENGTH_SHORT).show()
        }
    }
}