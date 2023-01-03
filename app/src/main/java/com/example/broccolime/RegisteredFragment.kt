package com.example.broccolime

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide

class RegisteredFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_registered, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val backgroundGif = view.findViewById<ImageView>(R.id.backgroundGif)
        Glide.with(this).load(R.drawable.pug_broccoli).centerCrop().into(backgroundGif)

        view.findViewById<Button>(R.id.registeredOkButton).setOnClickListener {
            findNavController().popBackStack()
        }
    }
}