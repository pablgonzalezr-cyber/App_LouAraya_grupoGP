package com.example.applouaraya_grupogp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.applouaraya_grupogp.ui.adaptive.*
import com.example.applouaraya_grupogp.ui.design.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.applouaraya_grupogp.repository.EstadoCotizacion
import com.example.applouaraya_grupogp.repository.PoliticaAdministracion
import com.example.applouaraya_grupogp.viewmodel.AdminCotizacionesViewModel

@Composable
fun AdminDashboardScreen(vm: AdminCotizacionesViewModel, onSolicitudes: () -> Unit, onCerrarSesion: () -> Unit, onPerfil: () -> Unit, onProductos: () -> Unit, onContenido: () -> Unit,
    onAvisos: () -> Unit, avisosNoLeidos: Int) {
    val estado by vm.estado.collectAsState()
    LaunchedEffect(vm) { vm.cargar() }
    AdminDashboardContent(estado, { vm.cargar() }, onSolicitudes, onCerrarSesion, onPerfil, onProductos, onContenido, onAvisos, avisosNoLeidos)
}

@Composable
fun AdminDashboardContent(estado: com.example.applouaraya_grupogp.viewmodel.AdminUiState, reintentar: () -> Unit,
    onSolicitudes: () -> Unit, onCerrarSesion: () -> Unit, onPerfil: () -> Unit, onProductos: () -> Unit,
    onContenido: () -> Unit, onAvisos: () -> Unit, avisosNoLeidos: Int) {
    AdaptiveColumn(form = false, padding = 20.dp, spacing = 12.dp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                LouEyebrow("LOU ARAYA MACRAMÉ")
                LouPageHeading("Administración Lou Araya", "Bienvenida al espacio de administración")
            }
            LouBrandMark()
        }
        Text("Administración local · solo datos de este dispositivo", style = MaterialTheme.typography.bodySmall,
            color = LouColors.TextoSecundario)
        if (estado.cargando) LinearProgressIndicator(Modifier.fillMaxWidth())
        estado.error?.let { Text(it, color = MaterialTheme.colorScheme.error); TextButton(onClick = reintentar) { Text("Reintentar") } }
        LouSectionTitle("Resumen del taller")
        Box(Modifier.fillMaxWidth().louEntrance(60)) {
            AdminMetrics(listOf("Total de solicitudes" to if (estado.cargando || estado.error != null) "—" else estado.solicitudes.size.toString()) +
                EstadoCotizacion.entries.map { e -> e.name to if (estado.cargando || estado.error != null) "—" else estado.solicitudes.count { it.estado == e.name }.toString() })
        }
        LouSectionTitle("Gestiona tu emprendimiento")
        Box(Modifier.fillMaxWidth().louEntrance(140)) {
            AdminModules(listOf(
                Triple("≡", "Gestionar solicitudes", onSolicitudes),
                Triple("▦", "Administrar productos", onProductos),
                Triple("❀", "Colecciones y campañas", onContenido),
                Triple("◉", "Centro de avisos ($avisosNoLeidos sin leer)", onAvisos),
                Triple("☺", "Perfil y configuración", onPerfil)
            ))
        }
        OutlinedButton(onClick = onCerrarSesion, modifier = Modifier.fillMaxWidth()) { Text("Cerrar sesión") }

    }
}

@Composable
fun AdminSolicitudesScreen(vm: AdminCotizacionesViewModel, onVolver: () -> Unit, onAbrir: (Long) -> Unit) {
    val estado by vm.estado.collectAsState()
    LaunchedEffect(vm) { vm.cargar() }
    AdminPage("Todas las solicitudes", onVolver) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(selected = estado.filtro == null, onClick = { vm.filtrar(null) }, label = { Text("Todas") }) }
            items(EstadoCotizacion.entries.toList()) { e -> FilterChip(selected = estado.filtro == e, onClick = { vm.filtrar(e) }, label = { Text(e.name) }) }
        }
        if (estado.cargando) CircularProgressIndicator()
        estado.error?.let { Text(it, color = MaterialTheme.colorScheme.error); TextButton(onClick = { vm.cargar() }) { Text("Reintentar") } }
        val lista = estado.solicitudes.filter { estado.filtro == null || it.estado == estado.filtro?.name }
        if (!estado.cargando && estado.error == null && lista.isEmpty()) Text("No hay solicitudes con este filtro.")
        AdminGrid(lista, { it.id }, Modifier.weight(1f)) { s ->
            AdminSolicitudCard(s) { onAbrir(s.id) }
        }
    }
}

@Composable
fun AdminDetalleSolicitudScreen(vm: AdminCotizacionesViewModel, id: Long, onVolver: () -> Unit) {
    val estado by vm.estado.collectAsState()
    var pendiente by remember(id) { mutableStateOf<Pair<String, EstadoCotizacion>?>(null) }
    LaunchedEffect(vm, id) { vm.cargar(id) }
    AdminPage("Gestionar solicitud #$id", onVolver, scroll = true) {
        LouPanel(Modifier.fillMaxWidth()) {
            if (estado.cargando) CircularProgressIndicator()
            estado.error?.let { Text(it, color = MaterialTheme.colorScheme.error); TextButton(onClick = { vm.cargar(id) }) { Text("Actualizar detalle") } }
            estado.mensaje?.let { Text(it, color = Color(0xFF38684E)) }
            val s = estado.detalle
            if (!estado.cargando && estado.error == null && s == null) Text("Solicitud no disponible.")
            if (s != null) {
                Text("Cliente #${s.usuarioId}")
                AdminStatus("Estado: ${s.estado}", s.estado)
                Text("Recibida: ${adminFecha(s.creadaEn)}", color = LouColors.TextoSecundario)
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

@Composable
fun AdminSolicitudCard(s: com.example.applouaraya_grupogp.model.CotizacionEntity, abrir: () -> Unit) {
    Card(onClick = abrir, shape = LouShape, colors = CardDefaults.cardColors(containerColor = LouColors.Superficie)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LouEyebrow("Solicitud #${s.id}")
            Text("Solicitud #${s.id} · Cliente #${s.usuarioId}", style = MaterialTheme.typography.titleMedium, color = LouColors.Cafe)
            AdminStatus(s.estado)
            Text(adminFecha(s.creadaEn), style = MaterialTheme.typography.bodySmall, color = LouColors.TextoSecundario)
            Text(s.descripcion, maxLines = 3, color = LouColors.Cafe)
            Text(s.categoria, style = MaterialTheme.typography.labelMedium, color = LouColors.TextoSecundario)
            Text("Ver y gestionar →", color = LouColors.Terracota)
        }
    }
}
