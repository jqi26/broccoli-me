package com.example.broccolime

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.isGone
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.MediaType.Companion.get
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.net.SocketTimeoutException


class MainFragment : Fragment() {
    private val registerDetailsViewModel: RegisterDetailsViewModel by activityViewModels()

    private lateinit var button: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (requireActivity().getPreferences(Context.MODE_PRIVATE).getBoolean(getString(R.string.has_registered), false)) {
            findNavController().navigate(R.id.action_mainFragment_to_currentlyRegisteredFragment)
        }

        button = view.findViewById(R.id.inviteButton)

        button.setOnClickListener {
            registerDetailsViewModel.isShowingDialog = true
            setUpButton()
        }

        // Re-open the dialog for a configuration change
        if (registerDetailsViewModel.isShowingDialog) {
            button.performClick()
        }
    }

    private fun setDialogLoading(isLoading: Boolean, progressBar: ProgressBar, dialog: AlertDialog,
                                 nameBox: EditText, emailBox: EditText, confirmEmailBox: EditText) {
        progressBar.isGone = !isLoading
        dialog.setCancelable(!isLoading);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = !isLoading
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = !isLoading
        nameBox.isEnabled = !isLoading
        emailBox.isEnabled = !isLoading
        confirmEmailBox.isEnabled = !isLoading
    }

    private fun setUpButton() {
        val builder = AlertDialog.Builder(context).
                setPositiveButton(getString(R.string.send), null)

        val form = layoutInflater.inflate(R.layout.dialog_details, null, false)
        builder.setView(form)

        val nameBox: EditText = form.findViewById(R.id.detailsNameText)
        val emailBox: EditText = form.findViewById(R.id.detailsEmailText)
        val confirmEmailBox: EditText = form.findViewById(R.id.detailsConfirmEmailText)
        val nameError: TextView = form.findViewById(R.id.detailsNameErrorText)
        val emailError: TextView = form.findViewById(R.id.detailsEmailErrorText)
        val confirmEmailError: TextView = form.findViewById(R.id.detailsConfirmEmailErrorText)
        val serverError: TextView = form.findViewById(R.id.serverErrorText)

        var name = registerDetailsViewModel.name
        var email = registerDetailsViewModel.email
        var confirmEmail = registerDetailsViewModel.confirmEmail

        nameBox.setText(name)
        emailBox.setText(email)
        confirmEmailBox.setText(confirmEmail)
        nameError.text = registerDetailsViewModel.nameError
        emailError.text = registerDetailsViewModel.emailError
        confirmEmailError.text = registerDetailsViewModel.confirmEmailError
        serverError.text = registerDetailsViewModel.serverError
        serverError.isGone = registerDetailsViewModel.serverError.trim().isEmpty()

        nameBox.doOnTextChanged { text, _, _, _ ->
            name = text.toString().trim()
            validateName(name, nameError)
            registerDetailsViewModel.name = name
        }

        emailBox.doOnTextChanged { text, _, _, _ ->
            email = text.toString().trim()
            validateEmail(email, emailError)
            registerDetailsViewModel.email = email
        }

        confirmEmailBox.doOnTextChanged { text, _, _, _ ->
            confirmEmail = text.toString().trim()
            validateConfirmEmail(confirmEmail, email, confirmEmailError)
            registerDetailsViewModel.confirmEmail = confirmEmail
        }

        builder.setNegativeButton(getString(R.string.cancel)) { _, _ -> }

        val dialog = builder.create()
        dialog.show()

        dialog.setOnDismissListener {
            registerDetailsViewModel.reset()
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val progressBar: ProgressBar = dialog.findViewById(R.id.progressBar)
            handleRequestSubmission(dialog, nameBox, emailBox, confirmEmailBox, nameError,
                emailError, confirmEmailError, serverError, progressBar)
        }
    }

    private fun handleRequestSubmission(dialog: AlertDialog,
        nameBox: EditText, emailBox: EditText, confirmEmailBox: EditText,
        nameError: TextView, emailError: TextView, confirmEmailError: TextView,
        serverError: TextView, progressBar: ProgressBar) {

        val name = nameBox.text.toString().trim()
        val email = emailBox.text.toString().trim()
        val confirmEmail = confirmEmailBox.text.toString().trim()

        // Validate everything again since empty warnings only show on change
        val nameHasError = validateName(name, nameError)
        val emailHasError = validateEmail(email, emailError)
        val confirmEmailHasError = validateConfirmEmail(confirmEmail, email, confirmEmailError)

        if (nameHasError || emailHasError || confirmEmailHasError) {
            Toast.makeText(context, getString(R.string.fix_errors_above), Toast.LENGTH_SHORT).show()
        } else {
            setDialogLoading(true, progressBar, dialog, nameBox, emailBox,
                confirmEmailBox)

            lifecycleScope.launch(Dispatchers.IO) {
                val response = sendRequest(name,
                    email)

                withContext(Dispatchers.Main) {
                    setDialogLoading(false, progressBar, dialog, nameBox, emailBox,
                        confirmEmailBox)

                    handleRegisterResponse(response, dialog, serverError)
                }
            }
        }
    }

    private fun handleRegisterResponse(response: Response?, dialog: AlertDialog,
                                       serverError: TextView) {
        if (response != null) {
            if (response.code == 200) {
                val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
                with (sharedPref.edit()) {
                    putBoolean(getString(R.string.has_registered), true)
                    apply()
                }

                dialog.dismiss()
                findNavController().navigate(R.id.action_mainFragment_to_registeredFragment)
            } else {
                // Assume everything else is an error
                var errorMessage = getString(R.string.unknown_error)

                response.body?.let {
                    val gson = Gson()
                    val map = gson.fromJson(it.string(), Map::class.java)
                    errorMessage = map["errorMessage"].toString()
                }

                serverError.text = errorMessage
                serverError.isGone = false
            }
        } else {
            // No response, assume timeout.
            serverError.text = getString(R.string.timeout_error)
            serverError.isGone = false
        }

        registerDetailsViewModel.serverError = serverError.text.toString()
    }

    private fun validateName(text: String?, nameError: TextView): Boolean {
        var isError = true

        if (text != null) {
            val name = text.trim()

            if (name.isEmpty()) {
                nameError.text = getString(R.string.empty_name_warning)
            } else if (name.count() < 3) {
                nameError.text = getString(R.string.name_min_length_warning)
            } else {
                nameError.text = ""
                isError = false
            }
        } else {
            nameError.text = getString(R.string.empty_name_warning)
        }

        registerDetailsViewModel.nameError = nameError.text.toString()
        return isError
    }

    private fun validateEmail(text: String?, emailError: TextView): Boolean {
        var isError = true

        if (text != null) {
            val email = text.trim()

            if (email.isEmpty()) {
                emailError.text = getString(R.string.empty_email_warning)
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailError.text = getString(R.string.valid_email_warning)
            } else {
                emailError.text = ""
                isError = false
            }
        } else {
            emailError.text = getString(R.string.empty_email_warning)
        }

        registerDetailsViewModel.emailError = emailError.text.toString()
        return isError
    }

    private fun validateConfirmEmail(text: String?, emailToCheck: String,
                                     confirmEmailError: TextView): Boolean {
        var isError = true

        if (text != null) {
            val email = text.trim()

            if (email.isEmpty()) {
                confirmEmailError.text = getString(R.string.empty_email_warning)
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                confirmEmailError.text = getString(R.string.valid_email_warning)
            } else if (email != emailToCheck) {
                confirmEmailError.text = getString(R.string.match_emails_warning)
            } else {
                confirmEmailError.text = ""
                isError = false
            }
        } else {
            confirmEmailError.text = getString(R.string.empty_email_warning)
        }

        registerDetailsViewModel.confirmEmailError = confirmEmailError.text.toString()
        return isError
    }

    private fun sendRequest(name: String, email: String): Response? {
        val client = (activity as MainActivity).client

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val jsonObject = JSONObject()
        jsonObject.put("name", name)
        jsonObject.put("email", email)

        val request: Request = Request.Builder()
            .url("https://us-central1-blinkapp-684c1.cloudfunctions.net/fakeAuth")
            .post(jsonObject.toString().toRequestBody(mediaType))
            .build()

        val call: Call = client.newCall(request)
        val response = try {
            call.execute()
        } catch (e: SocketTimeoutException) {
            null
        }

        return response
    }
}