package com.example.applouaraya_grupogp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.applouaraya_grupogp.model.CotizacionEntity
import com.example.applouaraya_grupogp.ui.adaptive.AdaptiveSurface
import com.example.applouaraya_grupogp.ui.design.*
import com.example.applouaraya_grupogp.viewmodel.HistorialCotizacionesViewModel
import kotlinx.coroutines.CancellationException
import java.text.DateFormat
import java.util.Date

@Composable
fun HistorialCotizacionesScreen(usuarioId: Long, onVolver: () -> Unit, onServidor: (() -> Unit)? = null, onAbrir: (Long) -> Unit) {
    val vm: HistorialCotizacionesViewModel = viewModel()
    var solicitudes by remember(usuarioId) { mutableStateOf<List<CotizacionEntity>>(emptyList()) }
    var cargando by remember(usuarioId) { mutableStateOf(true) }
    var error by remember(usuarioId) { mutableStateOf(false) }
    var reintento by remember { mutableIntStateOf(0) }
    LaunchedEffect(usuarioId, reintento) {
        cargando = true; error = false
        try { vm.observar(usuarioId).collect { solicitudes = it; cargando = false } }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = true; cargando = false }
    }
    AdaptiveSurface {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)) {
            LouBackButton(onVolver, "← Inicio")
            LouEyebrow("TU HISTORIAL")
            LouPageHeading("Mis solicitudes", "Historial local de tu cuenta en este dispositivo.")
            onServidor?.let { OutlinedButton(onClick = it) { Text("Solicitudes del servidor") } }
            LouBadge("${solicitudes.size} solicitudes")
            when {
                cargando -> CircularProgressIndicator()
                error -> LouPanel { Text("No se pudo cargar el historial.");
                    OutlinedButton(onClick = { reintento++ }) { Text("Reintentar") } }
                solicitudes.isEmpty() -> LouPanel(Modifier.fillMaxWidth()) {
                    Text("Todavía no tienes solicitudes.", style = MaterialTheme.typography.titleMedium,
                        color = LouColors.Cafe)
                    Text("Crea una cotización desde el inicio para verla aquí.",
                        color = LouColors.TextoSecundario)
                }
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(solicitudes, key = { it.id }) { solicitud ->
                        Card(onClick = { onAbrir(solicitud.id) }, shape = LouShape,
                            colors = CardDefaults.cardColors(containerColor = LouColors.Superficie)) {
                            Column(Modifier.fillMaxWidth().padding(17.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Solicitud #${solicitud.id} · ${solicitud.categoria}",
                                        style = MaterialTheme.typography.titleMedium, color = LouColors.Cafe)
                                    LouBadge(solicitud.estado)
                                }
                                Text(solicitud.descripcion, maxLines = 2,
                                    color = LouColors.TextoSecundario)
                                Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                                    .format(Date(solicitud.creadaEn)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LouColors.TextoSecundario)
                                Text("Ver detalle →", color = LouColors.Terracota,
                                    style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}
