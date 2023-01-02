package com.example.broccolime

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.chromium.net.CronetEngine
import javax.inject.Inject

class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var cronetEngine: CronetEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        (application as BroccoliMeApplication).appComponent.inject(this)
    }
}