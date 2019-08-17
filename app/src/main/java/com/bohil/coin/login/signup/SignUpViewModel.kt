package com.bohil.coin.login.signup


import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth


/**
 * ViewModel that handles creating a Firebase User and linking it to a Coin User Profile
 */
class SignUpViewModel : ViewModel(){
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()


}
