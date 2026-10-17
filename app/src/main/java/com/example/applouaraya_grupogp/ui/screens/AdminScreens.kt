package com.example.applouaraya_grupogp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.applouaraya_grupogp.repository.EstadoCotizacion
import com.example.applouaraya_grupogp.repository.PoliticaAdministracion
import com.example.applouaraya_grupogp.viewmodel.AdminCotizacionesViewModel

@Composable
private fun AdminMarco(titulo: String, volver: () -> Unit, mostrarVolver: Boolean = true, desplazable: Boolean = false, contenido: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xFFF7F1E8)).safeDrawingPadding()
        .then(if (desplazable) Modifier.verticalScroll(rememberScrollState()) else Modifier).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (mostrarVolver) TextButton(onClick = volver) { Text("← Volver") }
        Text(titulo, style = MaterialTheme.typography.headlineMedium, color = Color(0xFF51362B))
        Text("Administración local · solo datos de este dispositivo", style = MaterialTheme.typography.bodySmall)
        contenido()
    }
}

@Composable
fun AdminDashboardScreen(vm: AdminCotizacionesViewModel, onSolicitudes: () -> Unit, onCerrarSesion: () -> Unit, onPerfil: () -> Unit, onProductos: () -> Unit, onContenido: () -> Unit) {
    val estado by vm.estado.collectAsState()
    LaunchedEffect(vm) { vm.cargar() }
    AdminMarco("Administración Lou Araya", onCerrarSesion, mostrarVolver = false, desplazable = true) {
        if (estado.cargando) CircularProgressIndicator()
        estado.error?.let { Text(it, color = MaterialTheme.colorScheme.error); TextButton(onClick = { vm.cargar() }) { Text("Reintentar") } }
        Text("Total de solicitudes: ${estado.solicitudes.size}")
        EstadoCotizacion.entries.forEach { e -> Text("${e.name}: ${estado.solicitudes.count { it.estado == e.name }}") }
        Button(onClick = onSolicitudes, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA65E43))) { Text("Gestionar solicitudes") }
        OutlinedButton(onClick = onPerfil) { Text("Perfil y configuración") }
        OutlinedButton(onClick = onProductos) { Text("Administrar productos") }
        OutlinedButton(onClick = onContenido) { Text("Colecciones y campañas") }
        OutlinedButton(onClick = onCerrarSesion) { Text("Cerrar sesión") }
    }
}

@Composable
fun AdminSolicitudesScreen(vm: AdminCotizacionesViewModel, onVolver: () -> Unit, onAbrir: (Long) -> Unit) {
    val estado by vm.estado.collectAsState()
    LaunchedEffect(vm) { vm.cargar() }
    AdminMarco("Todas las solicitudes", onVolver) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(selected = estado.filtro == null, onClick = { vm.filtrar(null) }, label = { Text("Todas") }) }
            items(EstadoCotizacion.entries.toList()) { e -> FilterChip(selected = estado.filtro == e, onClick = { vm.filtrar(e) }, label = { Text(e.name) }) }
        }
        if (estado.cargando) CircularProgressIndicator()
        estado.error?.let { Text(it, color = MaterialTheme.colorScheme.error); TextButton(onClick = { vm.cargar() }) { Text("Reintentar") } }
        val lista = estado.solicitudes.filter { estado.filtro == null || it.estado == estado.filtro?.name }
        if (!estado.cargando && estado.error == null && lista.isEmpty()) Text("No hay solicitudes con este filtro.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(lista, key = { it.id }) { s ->
                Card(onClick = { onAbrir(s.id) }, colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBF5))) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Solicitud #${s.id} · Cliente #${s.usuarioId}", style = MaterialTheme.typography.titleMedium)
                        Text(s.descripcion, maxLines = 2)
                        Text("${s.categoria} · ${s.estado}")
                        Text("Ver y gestionar →")
                    }
                }
            }
        }
    }
}

@Composable
fun AdminDetalleSolicitudScreen(vm: AdminCotizacionesViewModel, id: Long, onVolver: () -> Unit) {
    val estado by vm.estado.collectAsState()
    var pendiente by remember(id) { mutableStateOf<Pair<String, EstadoCotizacion>?>(null) }
    LaunchedEffect(vm, id) { vm.cargar(id) }
    AdminMarco("Gestionar solicitud #$id", onVolver) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (estado.cargando) CircularProgressIndicator()
            estado.error?.let { Text(it, color = MaterialTheme.colorScheme.error); TextButton(onClick = { vm.cargar(id) }) { Text("Actualizar detalle") } }
            estado.mensaje?.let { Text(it, color = Color(0xFF38684E)) }
            val s = estado.detalle
            if (!estado.cargando && estado.error == null && s == null) Text("Solicitud no disponible.")
            if (s != null) {
                Text("Cliente #${s.usuarioId}")
                Text("Estado: ${s.estado}", style = MaterialTheme.typography.titleLarge)
                Text(s.descripcion)
                Text("Categoría: ${s.categoria}")
                Text("Material: ${s.material}")
                Text("Medidas: ${s.anchoCm} × ${s.largoCm} cm · Cantidad: ${s.cantidad}")
                Text("Fecha requerida: ${s.fechaRequerida ?: "Sin especificar"}")
                Text("Referencia: ${s.productoReferenciaNombre ?: "Sin referencia"}")
                val destinos = PoliticaAdministracion.destinos(s.estado)
                if (destinos.isEmpty()) Text("Estado final: no admite más cambios.")
                destinos.forEach { nuevo ->
                    Button(onClick = { pendiente = s.estado to nuevo }, enabled = !estado.guardando,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA65E43))) { Text("Cambiar a ${nuevo.name}") }
                }
                if (estado.guardando) CircularProgressIndicator()
            }
        }
    }
    pendiente?.let { (anterior, nuevo) ->
        AlertDialog(onDismissRequest = { pendiente = null }, title = { Text("Confirmar cambio") },
            text = { Text("¿Cambiar la solicitud #$id de $anterior a ${nuevo.name}?") },
            confirmButton = { TextButton(onClick = { pendiente = null; vm.cambiar(id, anterior, nuevo) }) { Text("Confirmar") } },
            dismissButton = { TextButton(onClick = { pendiente = null }) { Text("Cancelar") } })
    }
}
