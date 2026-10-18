
package com.example.applouaraya_grupogp

import android.os.Bundle
import android.content.Intent
import androidx.compose.runtime.*
import com.example.applouaraya_grupogp.notifications.NotificacionesLocales
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.example.applouaraya_grupogp.navigation.AppNavigation

class MainActivity : ComponentActivity() {
    private var abrirAvisos by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificacionesLocales(applicationContext).cancelar()
        abrirAvisos = intent?.action == NotificacionesLocales.ACCION_CENTRO

        setContent {
            MaterialTheme {
                AppNavigation(abrirCentro = abrirAvisos, centroConsumido = { abrirAvisos = false })
            }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if(intent.action == NotificacionesLocales.ACCION_CENTRO) abrirAvisos = true
    }
}
