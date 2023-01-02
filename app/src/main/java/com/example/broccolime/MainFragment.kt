package com.example.broccolime

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import org.w3c.dom.Text

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

    private fun setUpButton() {
        val builder = AlertDialog.Builder(context).
                setPositiveButton("Request", null)

        val form = layoutInflater.inflate(R.layout.dialog_details, null, false)
        builder.setView(form)

        val nameBox: EditText = form.findViewById(R.id.detailsNameText)
        val emailBox: EditText = form.findViewById(R.id.detailsEmailText)
        val confirmEmailBox: EditText = form.findViewById(R.id.detailsConfirmEmailText)
        val nameError: TextView = form.findViewById(R.id.detailsNameErrorText)
        val emailError: TextView = form.findViewById(R.id.detailsEmailErrorText)
        val confirmEmailError: TextView = form.findViewById(R.id.detailsConfirmEmailErrorText)

        var isError: Boolean

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

        builder.setNegativeButton("Cancel") { _, _ -> }

        val dialog = builder.create()
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {


            // Validate everything again since empty warnings only show on change
            isError = validateName(nameBox.text.toString(), nameError)
            isError = validateEmail(emailBox.text.toString(), emailError)
            isError = validateConfirmEmail(confirmEmailBox.text.toString(),
                emailBox.text.toString(), confirmEmailError)

            if (isError) {
                Toast.makeText(context, "Please fix errors above.", Toast.LENGTH_SHORT).show()
            } else {
                // TODO: Send request
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
}