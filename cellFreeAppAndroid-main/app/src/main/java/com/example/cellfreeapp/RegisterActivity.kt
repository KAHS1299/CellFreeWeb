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

class RegisterActivity : AppCompatActivity() {

    private lateinit var tilName: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilPasswordConfirm: TextInputLayout
    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etPasswordConfirm: TextInputEditText
    private lateinit var btnRegister: MaterialButton
    private lateinit var progressIndicator: CircularProgressIndicator
    
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initViews()
        setupClickListeners()
        
        authRepository = AuthRepository(this)
    }

    private fun initViews() {
        tilName = findViewById(R.id.til_name)
        tilEmail = findViewById(R.id.til_email)
        tilPassword = findViewById(R.id.til_password)
        tilPasswordConfirm = findViewById(R.id.til_password_confirm)
        etName = findViewById(R.id.et_name)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        etPasswordConfirm = findViewById(R.id.et_password_confirm)
        btnRegister = findViewById(R.id.btn_register)
        
        // Crear el progress indicator programáticamente
        progressIndicator = CircularProgressIndicator(this).apply {
            visibility = View.GONE
        }
    }

    private fun setupClickListeners() {
        btnRegister.setOnClickListener {
            performRegister()
        }

        findViewById<android.widget.TextView>(R.id.tv_already_have_account).setOnClickListener {
            finish() // Volver a login
        }
    }

    private fun performRegister() {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val passwordConfirm = etPasswordConfirm.text.toString()

        // Clear previous errors
        tilName.error = null
        tilEmail.error = null
        tilPassword.error = null
        tilPasswordConfirm.error = null

        // Validate inputs
        when {
            name.isEmpty() -> {
                tilName.error = getString(R.string.error_empty_name)
                etName.requestFocus()
                return
            }
            name.length < 2 -> {
                tilName.error = getString(R.string.error_short_name)
                etName.requestFocus()
                return
            }
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
            password.length < 8 -> {
                tilPassword.error = getString(R.string.error_password_too_short)
                etPassword.requestFocus()
                return
            }
            passwordConfirm.isEmpty() -> {
                tilPasswordConfirm.error = getString(R.string.error_empty_password_confirm)
                etPasswordConfirm.requestFocus()
                return
            }
            password != passwordConfirm -> {
                tilPasswordConfirm.error = getString(R.string.error_password_mismatch)
                etPasswordConfirm.requestFocus()
                return
            }
        }

        // Show loading
        showLoading(true)
        
        // Perform register via API
        lifecycleScope.launch {
            try {
                val result = authRepository.register(name, email, password, passwordConfirm)
                
                result.onSuccess { user ->
                    showLoading(false)
                    Toast.makeText(
                        this@RegisterActivity,
                        getString(R.string.register_success, user.name),
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Navigate to MainActivity
                    val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }.onFailure { exception ->
                    showLoading(false)
                    Toast.makeText(
                        this@RegisterActivity,
                        "Error: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(
                    this@RegisterActivity,
                    "Error de conexión: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun showLoading(show: Boolean) {
        if (show) {
            btnRegister.visibility = View.INVISIBLE
            progressIndicator.visibility = View.VISIBLE
        } else {
            btnRegister.visibility = View.VISIBLE
            progressIndicator.visibility = View.GONE
        }
        
        // Deshabilitar campos mientras se carga
        etName.isEnabled = !show
        etEmail.isEnabled = !show
        etPassword.isEnabled = !show
        etPasswordConfirm.isEnabled = !show
    }
}