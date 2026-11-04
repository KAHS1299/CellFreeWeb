package com.example.cellfreeapp.base

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cellfreeapp.LoginActivity
import com.example.cellfreeapp.MainActivity
import com.example.cellfreeapp.AppBlockerActivity
import com.example.cellfreeapp.CalendarActivity
import com.example.cellfreeapp.UsageReportActivity
import com.example.cellfreeapp.MonitoringConfigActivity
import com.example.cellfreeapp.R
import com.example.cellfreeapp.data.repository.AuthRepository
import kotlinx.coroutines.launch

abstract class BaseActivity : AppCompatActivity() {
    
    protected lateinit var authRepository: AuthRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authRepository = AuthRepository(this)
    }
    
    protected fun setupBaseViews() {
        // Configurar título de la pantalla
        findViewById<TextView>(R.id.tv_screen_title)?.text = getScreenTitle()
        
        // Configurar menú hamburguesa
        findViewById<ImageView>(R.id.iv_menu)?.setOnClickListener {
            showNavigationMenu()
        }
        
        // Configurar ícono de logout del header
        findViewById<ImageView>(R.id.iv_logout)?.setOnClickListener {
            showLogoutConfirmation()
        }
        
        // Configurar ícono de perfil (si existe)
        findViewById<ImageView>(R.id.iv_profile)?.setOnClickListener {
            Toast.makeText(this, "Perfil de usuario", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showNavigationMenu() {
        val options = arrayOf(
            "🏠 Menú Principal",
            "📱 Bloquear Aplicaciones", 
            "📅 Calendario",
            "📊 Reporte de Uso",
            "⚙️ Monitoreo Automático",
            "🚪 Cerrar Sesión"
        )
        
        AlertDialog.Builder(this)
            .setTitle("Navegación")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> navigateToMain()
                    1 -> navigateToAppBlocker()
                    2 -> navigateToCalendar()
                    3 -> navigateToUsageReport()
                    4 -> navigateToMonitoringConfig()
                    5 -> showLogoutConfirmation()
                }
            }
            .show()
    }
    
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que quieres cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun performLogout() {
        lifecycleScope.launch {
            try {
                authRepository.logout()
                Toast.makeText(this@BaseActivity, "Sesión cerrada exitosamente", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // Continuar con logout local aunque falle el servidor
                Toast.makeText(this@BaseActivity, "Sesión cerrada", Toast.LENGTH_SHORT).show()
            }
            
            // Navegar a login
            val intent = Intent(this@BaseActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
    
    private fun navigateToMain() {
        if (this !is MainActivity) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun navigateToAppBlocker() {
        if (this !is AppBlockerActivity) {
            startActivity(Intent(this, AppBlockerActivity::class.java))
        }
    }
    
    private fun navigateToCalendar() {
        if (this !is CalendarActivity) {
            startActivity(Intent(this, CalendarActivity::class.java))
        }
    }
    
    private fun navigateToUsageReport() {
        if (this !is UsageReportActivity) {
            startActivity(Intent(this, UsageReportActivity::class.java))
        }
    }

    private fun navigateToMonitoringConfig() {
        if (this !is MonitoringConfigActivity) {
            startActivity(Intent(this, MonitoringConfigActivity::class.java))
        }
    }
    
    // Método abstracto que cada actividad debe implementar para definir su título
    protected abstract fun getScreenTitle(): String
}