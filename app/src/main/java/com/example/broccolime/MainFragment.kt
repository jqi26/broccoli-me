package com.example.broccolime

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.isGone
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject


class MainFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.inviteButton).setOnClickListener { setUpButton() }
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
                setPositiveButton(getString(R.string.request), null)

        val form = layoutInflater.inflate(R.layout.dialog_details, null, false)
        builder.setView(form)

        val nameBox: EditText = form.findViewById(R.id.detailsNameText)
        val emailBox: EditText = form.findViewById(R.id.detailsEmailText)
        val confirmEmailBox: EditText = form.findViewById(R.id.detailsConfirmEmailText)
        val nameError: TextView = form.findViewById(R.id.detailsNameErrorText)
        val emailError: TextView = form.findViewById(R.id.detailsEmailErrorText)
        val confirmEmailError: TextView = form.findViewById(R.id.detailsConfirmEmailErrorText)
        val serverError: TextView = form.findViewById(R.id.serverErrorText)

        nameBox.doOnTextChanged { text, _, _, _ ->
            validateName(text.toString(), nameError)
        }

        emailBox.doOnTextChanged { text, _, _, _ ->
            validateEmail(text.toString(), emailError)
        }

        confirmEmailBox.doOnTextChanged { text, _, _, _ ->
            validateConfirmEmail(text.toString(), emailBox.text.toString(),
                confirmEmailError)
        }

        builder.setNegativeButton(getString(R.string.cancel)) { _, _ -> }

        val dialog = builder.create()
        dialog.show()

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

        // Validate everything again since empty warnings only show on change
        val nameHasError = validateName(nameBox.text.toString(), nameError)
        val emailHasError = validateEmail(emailBox.text.toString(), emailError)
        val confirmEmailHasError = validateConfirmEmail(confirmEmailBox.text.toString(),
                    emailBox.text.toString(), confirmEmailError)

        if (nameHasError || emailHasError || confirmEmailHasError) {
            Toast.makeText(context, getString(R.string.fix_errors_above), Toast.LENGTH_SHORT).show()
        } else {
            setDialogLoading(true, progressBar, dialog, nameBox, emailBox,
                confirmEmailBox)


            lifecycleScope.launch(Dispatchers.IO) {
                val response = sendRequest(nameBox.text.toString().trim(),
                    emailBox.text.toString())

                withContext(Dispatchers.Main) {
                    setDialogLoading(false, progressBar, dialog, nameBox, emailBox,
                        confirmEmailBox)

                    if (response.code == 200) {
                        // TODO: Navigate to congratulations screen
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
                }
            }
        }
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

        return isError
    }

    private fun sendRequest(name: String, email: String): Response {
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
        return call.execute()
    }
}