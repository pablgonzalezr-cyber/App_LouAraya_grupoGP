package com.example.applouaraya_grupogp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.applouaraya_grupogp.ui.design.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.applouaraya_grupogp.model.CotizacionEntity
import com.example.applouaraya_grupogp.viewmodel.HistorialCotizacionesViewModel
import kotlinx.coroutines.CancellationException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.text.DateFormat
import java.util.Date

@Composable
fun DetalleCotizacionScreen(usuarioId: Long, id: Long, onVolver: () -> Unit, vm: HistorialCotizacionesViewModel = viewModel()) {
    var solicitud by remember(usuarioId, id) { mutableStateOf<CotizacionEntity?>(null) }
    var cargando by remember(usuarioId, id) { mutableStateOf(true) }
    var error by remember(usuarioId, id) { mutableStateOf(false) }
    var reintento by remember { mutableIntStateOf(0) }
    LaunchedEffect(usuarioId, id, reintento) {
        cargando = true; error = false
        try { vm.detalle(usuarioId, id).collect { solicitud = it; cargando = false } }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = true; cargando = false }
    }
    Column(Modifier.fillMaxSize().background(Color(0xFFF7F1E8)).safeDrawingPadding()
        .verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LouBackButton(onVolver, "← Mis solicitudes")
        LouPageHeading("Detalle de solicitud", "Consulta el estado de tu pedido personalizado")
        when {
            cargando -> CircularProgressIndicator()
            error -> { Text("No se pudo cargar la solicitud."); OutlinedButton(onClick = { reintento++ }) { Text("Reintentar") } }
            solicitud == null -> Text("Solicitud no disponible para esta cuenta.")
            else -> solicitud?.let { s ->
                Text("Solicitud #${s.id}", style = MaterialTheme.typography.titleLarge)
                Text("Estado: ${s.estado}", style = MaterialTheme.typography.titleMedium)
                Text("Creada: ${DateFormat.getDateTimeInstance().format(Date(s.creadaEn))}")
                HorizontalDivider()
                Text("Descripción: ${s.descripcion}")
                Text("Categoría: ${s.categoria}")
                Text("Material: ${s.material}")
                Text("Medidas: ${s.anchoCm} cm de ancho × ${s.largoCm} cm de largo / alto")
                Text("Cantidad: ${s.cantidad}")
                val fecha = s.fechaRequerida?.let { LocalDate.parse(it).format(DateTimeFormatter.ofPattern("dd/MM/uuuu")) } ?: "Sin especificar"
                Text("Fecha requerida: $fecha")
                Text("Producto de referencia: ${s.productoReferenciaNombre ?: "Sin referencia"}")
                Text("Guardada localmente. Pendiente de integración remota.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
