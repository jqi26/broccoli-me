package com.example.broccolime

import androidx.lifecycle.ViewModel

class RegisterDetailsViewModel: ViewModel() {
    var isShowingDialog = false
    var name = ""
    var email = ""
    var confirmEmail = ""

    var nameError = ""
    var emailError = ""
    var confirmEmailError = ""
    var serverError = ""

    fun reset() {
        isShowingDialog = false
        name = ""
        email = ""
        confirmEmail = ""
        nameError = ""
        emailError = ""
        confirmEmailError = ""
        serverError = ""
    }
}