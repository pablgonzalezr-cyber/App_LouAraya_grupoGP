package com.example.applouaraya_grupogp.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import com.example.applouaraya_grupogp.ui.adaptive.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.applouaraya_grupogp.ui.design.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.example.applouaraya_grupogp.viewmodel.AvisosUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AvisosScreen(estado: AvisosUiState, permitido: Boolean, onRevisarPermiso: () -> Unit,
                 onVolver: () -> Unit, onLeida: (Long) -> Unit, onAbrir: (Long) -> Unit) {
    val contexto = LocalContext.current
    var mensaje by remember { mutableStateOf<String?>(null) }
    val permiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        onRevisarPermiso()
        mensaje = if(it) "Permiso concedido" else "Permiso no concedido. Tus avisos siguen disponibles aquí."
    }
    AdaptiveSurface {
        LazyVerticalGrid(columns = GridCells.Fixed(columnasPara(LocalLouWidth.current)),
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LouBackButton(onVolver)
                    LouPageHeading("Centro de avisos", "Novedades y cambios en tus solicitudes")
                    Text("No leídas: ${estado.noLeidas}")
                    Text("Avisos locales de esta cuenta. No hay notificaciones push entre dispositivos.", style = MaterialTheme.typography.bodySmall)
                    Text(if(permitido) "Avisos de Android permitidos; revisa también los canales en Ajustes." else "Avisos de Android desactivados; el centro sigue funcionando.", style = MaterialTheme.typography.bodySmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                }
            }
            items(estado.avisos, key = { it.id }) { aviso ->
                Card(shape = LouShape, colors = CardDefaults.cardColors(containerColor = if(aviso.leida) LouColors.Superficie else LouColors.ArenaClara)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(aviso.titulo, style = MaterialTheme.typography.titleMedium)
                        Text(aviso.mensaje)
                        Text(Instant.ofEpochMilli(aviso.creadaEn).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), style = MaterialTheme.typography.bodySmall)
                        AdminStatus(if(aviso.leida) "Leída" else "Sin leer")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { onAbrir(aviso.id) }, enabled = !estado.ocupado) { Text("Ver contenido") }
                            if(!aviso.leida) TextButton(onClick = { onLeida(aviso.id) }, enabled = !estado.ocupado) { Text("Marcar como leída") }
                        }
                    }
                }
            }
        }
    }
}
