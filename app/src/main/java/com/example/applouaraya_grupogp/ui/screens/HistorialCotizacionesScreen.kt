package com.example.applouaraya_grupogp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.applouaraya_grupogp.model.CotizacionEntity
import com.example.applouaraya_grupogp.viewmodel.HistorialCotizacionesViewModel
import kotlinx.coroutines.CancellationException
import java.text.DateFormat
import java.util.Date

@Composable
fun HistorialCotizacionesScreen(usuarioId: Long, onVolver: () -> Unit, onAbrir: (Long) -> Unit) {
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
    Column(Modifier.fillMaxSize().background(Color(0xFFF7F1E8)).safeDrawingPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onVolver) { Text("← Inicio") }
        Text("Mis solicitudes", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF51362B))
        Text("Historial local de tu cuenta en este dispositivo.")
        when {
            cargando -> CircularProgressIndicator()
            error -> { Text("No se pudo cargar el historial."); OutlinedButton(onClick = { reintento++ }) { Text("Reintentar") } }
            solicitudes.isEmpty() -> Text("Todavía no tienes solicitudes. Crea una cotización desde el inicio.")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(solicitudes, key = { it.id }) { solicitud ->
                    Card(onClick = { onAbrir(solicitud.id) }, colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBF5))) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Solicitud #${solicitud.id} · ${solicitud.categoria}", style = MaterialTheme.typography.titleMedium)
                            Text(solicitud.descripcion, maxLines = 2)
                            Text("Estado: ${solicitud.estado}")
                            Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(solicitud.creadaEn)))
                            Text("Ver detalle →")
                        }
                    }
                }
            }
        }
    }
}
