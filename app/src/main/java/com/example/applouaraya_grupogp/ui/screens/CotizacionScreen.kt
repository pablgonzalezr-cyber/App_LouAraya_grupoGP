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
    val f = estado.formulario
    LaunchedEffect(estado.guardadaId) { estado.guardadaId?.let(onGuardada) }
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
                    !estado.guardando, multilinea = true) { vm.cambiar(f.copy(descripcion = it)) }
                SelectorCotizacion("Categoría",
                    listOf("" to "Selecciona una categoría") + CotizacionValidator.categorias.map { it to it },
                    f.categoria, !estado.guardando) { vm.cambiar(f.copy(categoria = it)) }
                estado.errores["categoria"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                CampoCotizacion("Material", f.material, estado.errores["material"], !estado.guardando) {
                    vm.cambiar(f.copy(material = it))
                }
            }
            LouPanel(Modifier.fillMaxWidth()) {
                LouSectionTitle("Medidas y detalles")
                CampoCotizacion("Ancho (cm)", f.ancho, estado.errores["ancho"],
                    !estado.guardando, KeyboardType.Decimal) { vm.cambiar(f.copy(ancho = it)) }
                CampoCotizacion("Largo / alto (cm)", f.largo, estado.errores["largo"],
                    !estado.guardando, KeyboardType.Decimal) { vm.cambiar(f.copy(largo = it)) }
                CampoCotizacion("Cantidad", f.cantidad, estado.errores["cantidad"],
                    !estado.guardando, KeyboardType.Number) { vm.cambiar(f.copy(cantidad = it)) }
                CampoCotizacion("Fecha requerida (opcional, dd/mm/aaaa)", f.fecha,
                    estado.errores["fecha"], !estado.guardando) { vm.cambiar(f.copy(fecha = it)) }
                SelectorCotizacion("Producto de referencia (opcional)",
                    listOf("" to "Sin referencia") + estado.productosReferencia.map { it.id.toString() to it.nombre },
                    f.productoId?.toString() ?: "", !estado.guardando) {
                    vm.cambiar(f.copy(productoId = it.toIntOrNull()))
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
                { vm.guardar(usuarioId) }, Modifier.fillMaxWidth(),
                enabled = !estado.guardando && estado.guardadaId == null)
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
