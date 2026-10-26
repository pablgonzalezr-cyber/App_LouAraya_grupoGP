package com.example.applouaraya_grupogp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.applouaraya_grupogp.ui.adaptive.AdaptiveSurface
import com.example.applouaraya_grupogp.ui.design.*
import com.example.applouaraya_grupogp.validation.CotizacionValidator
import com.example.applouaraya_grupogp.viewmodel.CotizacionViewModel

@Composable
fun CotizacionScreen(usuarioId: Long, onVolver: () -> Unit, onGuardada: (Long) -> Unit,
                     mostrarAyuda: Boolean = true) {
    val vm: CotizacionViewModel = viewModel()
    val estado by vm.estado.collectAsState()
    CotizacionContent(estado, onVolver, onGuardada, mostrarAyuda, vm::cambiar) { vm.guardar(usuarioId) }
}

@Composable
internal fun CotizacionContent(
    estado: com.example.applouaraya_grupogp.model.CotizacionUiState,
    onVolver: () -> Unit, onGuardada: (Long) -> Unit, mostrarAyuda: Boolean = true,
    onCambiar: (com.example.applouaraya_grupogp.model.CotizacionFormulario) -> Unit,
    onGuardar: () -> Unit
) {
    val f = estado.formulario
    val guardadaId = estado.guardadaId
    if (guardadaId != null) {
        ConfirmacionCotizacion(onVolver = onVolver, onContinuar = { onGuardada(guardadaId) })
        return
    }
    AdaptiveSurface {
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)) {
            LouBackButton(onVolver)
            LouEyebrow("HECHO ESPECIALMENTE PARA TI")
            LouPageHeading("Cotización personalizada", "Cuéntanos cómo imaginas tu pieza artesanal")
            LouPanel(Modifier.fillMaxWidth()) {
                LouSectionTitle("Tu diseño")
                Text("Completa las características de tu solicitud.", color = LouColors.TextoSecundario)
                CampoCotizacion("Descripción", f.descripcion, estado.errores["descripcion"],
                    !estado.guardando, multilinea = true) { onCambiar(f.copy(descripcion = it)) }
                SelectorCotizacion("Categoría",
                    listOf("" to "Selecciona una categoría") + CotizacionValidator.categorias.map { it to it },
                    f.categoria, !estado.guardando) { onCambiar(f.copy(categoria = it)) }
                estado.errores["categoria"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                CampoCotizacion("Material", f.material, estado.errores["material"], !estado.guardando) {
                    onCambiar(f.copy(material = it))
                }
            }
            LouPanel(Modifier.fillMaxWidth()) {
                LouSectionTitle("Medidas y detalles")
                CampoCotizacion("Ancho (cm)", f.ancho, estado.errores["ancho"],
                    !estado.guardando, KeyboardType.Decimal) { onCambiar(f.copy(ancho = it)) }
                CampoCotizacion("Largo / alto (cm)", f.largo, estado.errores["largo"],
                    !estado.guardando, KeyboardType.Decimal) { onCambiar(f.copy(largo = it)) }
                CampoCotizacion("Cantidad", f.cantidad, estado.errores["cantidad"],
                    !estado.guardando, KeyboardType.Number) { onCambiar(f.copy(cantidad = it)) }
                CampoCotizacion("Fecha requerida (opcional, dd/mm/aaaa)", f.fecha,
                    estado.errores["fecha"], !estado.guardando) { onCambiar(f.copy(fecha = it)) }
                SelectorCotizacion("Producto de referencia (opcional)",
                    listOf("" to "Sin referencia") + estado.productosReferencia.map { it.id.toString() to it.nombre },
                    f.productoId?.toString() ?: "", !estado.guardando) {
                    onCambiar(f.copy(productoId = it.toIntOrNull()))
                }
                estado.errores["producto"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            if (mostrarAyuda) {
                LouPanel(Modifier.fillMaxWidth()) {
                    Text("Importante", style = MaterialTheme.typography.titleMedium,
                        color = LouColors.Cafe)
                    Text("Tu solicitud se guardará en este dispositivo. No se envía a Lou Araya por Internet.",
                        style = MaterialTheme.typography.bodySmall, color = LouColors.TextoSecundario)
                }
            }
            estado.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            LouPrimaryButton(if (estado.guardando) "Guardando…" else "Guardar solicitud",
                onGuardar, Modifier.fillMaxWidth(),
                enabled = !estado.guardando)
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun CampoCotizacion(label: String, valor: String, error: String?, enabled: Boolean,
                            teclado: KeyboardType = KeyboardType.Text, multilinea: Boolean = false,
                            cambiar: (String) -> Unit) {
    OutlinedTextField(value = valor, onValueChange = cambiar, label = { Text(label) }, enabled = enabled,
        isError = error != null, supportingText = { error?.let { Text(it) } },
        singleLine = !multilinea, minLines = if (multilinea) 3 else 1,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
}

@Composable
private fun SelectorCotizacion(label: String, opciones: List<Pair<String, String>>, actual: String,
                               enabled: Boolean, cambiar: (String) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = LouColors.Cafe)
        OutlinedButton(onClick = { abierto = true }, enabled = enabled,
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Text(opciones.find { it.first == actual }?.second ?: "Seleccionar")
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEach { (valor, texto) -> DropdownMenuItem(text = { Text(texto) },
                onClick = { cambiar(valor); abierto = false }) }
        }
    }
}

/** Only composed after the repository has returned a saved ID. No save action here. */
@Composable
internal fun ConfirmacionCotizacion(onVolver: () -> Unit, onContinuar: () -> Unit) {
    var continuando by remember { mutableStateOf(false) }
    val continuarActual by rememberUpdatedState(onContinuar)
    val accesibilidad = androidx.compose.ui.platform.LocalAccessibilityManager.current
    LaunchedEffect(Unit) {
        val espera = accesibilidad?.calculateRecommendedTimeoutMillis(
            1600L, containsIcons = true, containsText = true, containsControls = true
        ) ?: 1600L
        kotlinx.coroutines.delay(espera)
        if (!continuando) {
            continuando = true
            continuarActual()
        }
    }
    com.example.applouaraya_grupogp.ui.adaptive.AdaptiveColumn(spacing = 20.dp) {
        LouBackButton(onVolver)
        LouPanel(Modifier.fillMaxWidth().louEntrance(scale = true)) {
            Text("\u2713", color = LouColors.Terracota, style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            LouPageHeading("Solicitud registrada correctamente",
                "Tu cotizaci\u00f3n se guard\u00f3 en este dispositivo.")
            LouPrimaryButton("Ver mi solicitud", {
                if (!continuando) {
                    continuando = true
                    onContinuar()
                }
            }, Modifier.fillMaxWidth(), enabled = !continuando)
        }
    }
}
