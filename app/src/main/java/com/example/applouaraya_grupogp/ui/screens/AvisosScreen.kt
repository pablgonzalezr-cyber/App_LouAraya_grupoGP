package com.example.applouaraya_grupogp.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.example.applouaraya_grupogp.viewmodel.AvisosUiState

@Composable
fun AvisosScreen(estado: AvisosUiState, permitido: Boolean, onRevisarPermiso: () -> Unit,
    onVolver: () -> Unit, onLeida: (Long) -> Unit, onAbrir: (Long) -> Unit) {
    val contexto = LocalContext.current
    var mensaje by remember { mutableStateOf<String?>(null) }
    val permiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        onRevisarPermiso()
        mensaje = if(it) "Permiso concedido" else "Permiso no concedido. Tus avisos siguen disponibles aquí."
    }
    Column(Modifier.fillMaxSize().background(Color(0xFFF7F1E8)).safeDrawingPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onVolver) { Text("← Volver") }
        Text("Centro de avisos", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF51362B))
        Text("No leídas: ${estado.noLeidas}")
        Text("Avisos locales de esta cuenta. No hay notificaciones push entre dispositivos.", style = MaterialTheme.typography.bodySmall)
        Text(if(permitido) "Avisos de Android permitidos; revisa también los canales en Ajustes." else "Avisos de Android desactivados; el centro sigue funcionando.", style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if(Build.VERSION.SDK_INT >= 33 && !permitido) OutlinedButton(onClick = { permiso.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Permitir avisos") }
            TextButton(onClick = {
                try { contexto.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName)) }
                catch(_: android.content.ActivityNotFoundException) { mensaje = "No se pudieron abrir los ajustes de notificaciones" }
            }) { Text("Ajustes Android") }
        }
        mensaje?.let { Text(it) }
        Text("Puedes elegir cotizaciones y campañas en Perfil. Desactivarlas silencia Android; el historial se conserva aquí.", style = MaterialTheme.typography.bodySmall)
        if(estado.cargando) LinearProgressIndicator(Modifier.fillMaxWidth())
        estado.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if(!estado.cargando && estado.avisos.isEmpty()) Text("Todavía no tienes avisos.")
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(estado.avisos, key = { it.id }) { aviso ->
                Card(colors = CardDefaults.cardColors(containerColor = if(aviso.leida) Color(0xFFFFFBF5) else Color(0xFFE9D8C6))) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(aviso.titulo, style = MaterialTheme.typography.titleMedium)
                        Text(aviso.mensaje)
                        Text(Instant.ofEpochMilli(aviso.creadaEn).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), style = MaterialTheme.typography.bodySmall)
                        Text(if(aviso.leida) "Leída" else "Sin leer", style = MaterialTheme.typography.labelMedium)
                        Row {
                            TextButton(onClick = { onAbrir(aviso.id) }, enabled = !estado.ocupado) { Text("Ver contenido") }
                            if(!aviso.leida) TextButton(onClick = { onLeida(aviso.id) }, enabled = !estado.ocupado) { Text("Marcar como leída") }
                        }
                    }
                }
            }
        }
    }
}
