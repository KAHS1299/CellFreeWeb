package com.example.cellfreeapp

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cellfreeapp.data.repository.AuthRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var progressIndicator: CircularProgressIndicator
    
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initViews()
        setupClickListeners()
        
        authRepository = AuthRepository(this)
    }

    private fun initViews() {
        tilEmail = findViewById(R.id.til_email)
        tilPassword = findViewById(R.id.til_password)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)
        
        // Crear el progress indicator programáticamente
        progressIndicator = CircularProgressIndicator(this).apply {
            visibility = View.GONE
        }
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            performLogin()
        }

        findViewById<android.widget.TextView>(R.id.tv_forgot_password).setOnClickListener {
            Toast.makeText(this, getString(R.string.forgot_password_clicked), Toast.LENGTH_SHORT).show()
        }

        findViewById<android.widget.TextView>(R.id.tv_create_account).setOnClickListener {
            // Navegar a pantalla de registro (la crearemos después)
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun performLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        // Clear previous errors
        tilEmail.error = null
        tilPassword.error = null

        // Validate inputs
        when {
            email.isEmpty() -> {
                tilEmail.error = getString(R.string.error_empty_email)
                etEmail.requestFocus()
                return
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                tilEmail.error = getString(R.string.error_invalid_email)
                etEmail.requestFocus()
                return
            }
            password.isEmpty() -> {
                tilPassword.error = getString(R.string.error_empty_password)
                etPassword.requestFocus()
                return
            }
            password.length < 4 -> {
                tilPassword.error = getString(R.string.error_short_password)
                etPassword.requestFocus()
                return
            }
        }

        // Show loading
        showLoading(true)
        
        // Perform login via API
        lifecycleScope.launch {
            try {
                val result = authRepository.login(email, password)
                
                result.onSuccess { user ->
                    showLoading(false)
                    Toast.makeText(
                        this@LoginActivity,
                        getString(R.string.login_success, user.name),
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Navigate to MainActivity
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }.onFailure { exception ->
                    showLoading(false)
                    Toast.makeText(
                        this@LoginActivity,
                        "Error: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(
                    this@LoginActivity,
                    "Error de conexión: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun showLoading(show: Boolean) {
        if (show) {
            btnLogin.visibility = View.INVISIBLE
            progressIndicator.visibility = View.VISIBLE
        } else {
            btnLogin.visibility = View.VISIBLE
            progressIndicator.visibility = View.GONE
        }
        
        // Deshabilitar campos mientras se carga
        etEmail.isEnabled = !show
        etPassword.isEnabled = !show
    }
}