package com.example.cellfreeapp

import android.content.Intent
import android.os.Bundle
import android.widget.TextView // Para tv_app_name y tv_time_left
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton // El botón de tu XML

class BlockActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block) // Un layout con un TextView para el tiempo

        val appName = intent.getStringExtra("APP_NAME") ?: "App"
        val timeLeft = intent.getStringExtra("TIME_FORMATTED") ?: "00:00"

        val tvAppName = findViewById<TextView>(R.id.tv_app_name)
        tvAppName.text = getString(R.string.app_is_restricted, appName)

        val tvTimeLeft = findViewById<TextView>(R.id.tv_time_left)
        tvTimeLeft.text = timeLeft

        findViewById<MaterialButton>(R.id.btn_exit).setOnClickListener {
            val startMain = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(startMain)
            finish()
        }
    }
}