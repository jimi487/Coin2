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
    private var _userData : MutableMap<String?, Any>? = UserManager.UserDocs[UserManager.UserID]

    private val _fName = _userData?.get("first").toString()
    private val _lName = _userData?.get("last").toString()
    private val _birthday = _userData?.get("birthdate").toString()
    private val _country = _userData?.get("country").toString()
    private val _igHandle = _userData?.get("igHandle").toString()
    private val _snapHandle = _userData?.get("snapchatHandle").toString()
    private val _twitterHandle = _userData?.get("twitterHandle").toString()
    private val _sex = _userData?.get("sex").toString()
    private val _language = _userData?.get("language").toString()


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
        Log.e(TAG, "Updating textboxes...")
        binding.TxtFName.setText(_fName)
        binding.TxtLName.setText(_lName)
        binding.TxtDOB.setText(_birthday)
        binding.countrySpinner.setSelection(resources.getStringArray(R.array.country_array).indexOf(_country))
        binding.TxtIgHandle.setText(_igHandle)
        binding.TxtSnapHandle.setText(_snapHandle)
        binding.TxtTwitterHandle.setText(_twitterHandle)
        binding.sexSpinner.setSelection(resources.getStringArray(R.array.sex_array).indexOf(_sex))
        binding.languageSpinner.setSelection(resources.getStringArray(R.array.language_array).indexOf(_language))
        Log.e(TAG, "Set textboxes successfully")
    }

    private fun saveInfo() {
        val doc = DBUtility.FirebaseInstance.collection(context!!.getString(R.string.firestore_table)).document(UserManager.UserID)


        val userInfo : MutableMap<String?, Any> = hashMapOf(
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

        //Change the data of current user to updated data
        UserManager.UserDocs[UserManager.UserID] = userInfo

        val updateJob = doc.update(userInfo)

        updateJob.addOnSuccessListener {
            Toast.makeText(context, "Save successful!", Toast.LENGTH_SHORT).show()
        }

        updateJob.addOnFailureListener{
            Toast.makeText(context, "Error with save", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val TAG = "SETTINGS"
    }
}