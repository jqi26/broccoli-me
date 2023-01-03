package com.example.broccolime

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController


class CurrentlyRegisteredFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_currently_registered, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.cancelInvitationButton).setOnClickListener {
            val builder = AlertDialog.Builder(context)

            builder.setTitle(getString(R.string.sad_to_go))
            builder.setMessage(getString(R.string.sure_to_cancel_invite))

            builder.setNegativeButton(getString(R.string.back)) { _, _ -> }
            builder.setPositiveButton(getString(R.string.cancel_invite)) { _, _ ->
                val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
                with (sharedPref.edit()) {
                    putBoolean(getString(R.string.has_registered), false)
                    apply()
                }

                findNavController().navigate(R.id.action_currentlyRegisteredFragment_to_cancelledFragment)
            }

            builder.show()
        }
    }
}