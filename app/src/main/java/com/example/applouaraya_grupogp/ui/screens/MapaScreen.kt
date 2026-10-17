package com.example.applouaraya_grupogp.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.applouaraya_grupogp.repository.PuntosDemostracion
import com.example.applouaraya_grupogp.ui.components.MapaInteractivo
import com.example.applouaraya_grupogp.viewmodel.MapaViewModel
import kotlinx.coroutines.delay
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.maps.MapLibreMap
import java.util.Locale

@Composable
fun MapaScreen(vm: MapaViewModel, onVolver: () -> Unit) {
    val estado by vm.estado.collectAsState()
    val contexto = LocalContext.current
    var mapa by remember(estado.intento) { mutableStateOf<MapLibreMap?>(null) }
    var errorIndicaciones by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(estado.intento) {
        delay(25000)
        if(vm.estado.value.cargando) vm.error("El mapa está tardando en cargar. Revisa Internet o reintenta.")
    }
    Column(Modifier.fillMaxSize().background(Color(0xFFF7F1E8)).safeDrawingPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onVolver) { Text("← Volver") }
        Text("Ferias y showrooms", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF51362B))
        Text("Todos los puntos son FICTICIOS. No hay ferias, locales ni direcciones confirmadas.", color = Color(0xFFA65E43), style = MaterialTheme.typography.bodySmall)
        key(estado.intento) {
            MapaInteractivo(estado.puntos, estado.seleccionado, Modifier.fillMaxWidth().weight(1f).testTag("mapa-real"),
                onListo = { mapa = it; vm.listo() }, onSeleccion = vm::seleccionar, onZoom = vm::zoom, onError = vm::error)
        }
        if(estado.cargando) LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(if(estado.cargando) "Cargando cartografía…" else if(estado.error == null) "Mapa cargado" else "Cartografía no disponible", style = MaterialTheme.typography.bodySmall)
        estado.error?.let { Text(it, color = MaterialTheme.colorScheme.error); TextButton(onClick = vm::reintentar) { Text("Reintentar mapa") } }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { mapa?.animateCamera(CameraUpdateFactory.zoomIn()) }, enabled = mapa != null) { Text("Zoom +") }
            OutlinedButton(onClick = { mapa?.animateCamera(CameraUpdateFactory.zoomOut()) }, enabled = mapa != null) { Text("Zoom −") }
            Text(String.format(Locale.ROOT,"Zoom: %.1f", estado.zoom), modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodySmall)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(estado.puntos, key = { it.id }) { p -> FilterChip(selected = estado.seleccionado == p.id, onClick = { vm.seleccionar(p.id); errorIndicaciones = null }, label = { Text(p.nombre) }) }
        }
        estado.puntos.find { it.id == estado.seleccionado }?.let { p ->
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE9D8C6))) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(p.nombre, style = MaterialTheme.typography.titleMedium, color = Color(0xFF51362B))
                    Text(p.descripcion, style = MaterialTheme.typography.bodySmall)
                    Text("Dirección: ${p.direccion}", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = {
                        try { contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PuntosDemostracion.navegacion(p)))) }
                        catch(_: android.content.ActivityNotFoundException) {
                            try { contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PuntosDemostracion.ubicacion(p)))) }
                            catch(_: android.content.ActivityNotFoundException) { errorIndicaciones = "No hay una aplicación de mapas instalada que pueda abrir el destino." }
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA65E43))) { Text("Indicaciones a punto ficticio") }
                }
            }
        } ?: Text("Toca un marcador o elige un punto de demostración.", style = MaterialTheme.typography.bodySmall)
        errorIndicaciones?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("© OpenStreetMap · © OpenMapTiles · OpenFreeMap", style = MaterialTheme.typography.labelSmall)
    }
}
