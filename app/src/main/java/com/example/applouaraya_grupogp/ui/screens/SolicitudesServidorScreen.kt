package com.example.applouaraya_grupogp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.applouaraya_grupogp.repository.*
import com.example.applouaraya_grupogp.ui.design.*
import com.example.applouaraya_grupogp.viewmodel.SolicitudesServidorViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun SolicitudesServidorScreen(vm: SolicitudesServidorViewModel, id: Long?, volver: () -> Unit, abrir: (Long) -> Unit) {
    val estado by vm.estado.collectAsState()
    var usuario by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var confirmar by remember(id) { mutableStateOf<EstadoCotizacionRemota?>(null) }
    LaunchedEffect(id) { vm.actualizar(id) }
    BackHandler(estado.cargando && id != null) { }
    LazyColumn(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            LouBackButton({ if (!estado.cargando || id == null) volver() })
            LouPageHeading(id?.let { "Solicitud remota #$it" } ?: "Solicitudes del servidor",
                if (vm.admin) "Administración de cotizaciones" else "Historial de tu cuenta del servicio")
        }
        if (estado.cargando) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        estado.mensaje?.let { item { Text(it) } }
        estado.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        if (!estado.conectada) {
            item { CampoContenido(if (vm.admin) "Usuario administrador del servicio" else "Correo del servicio", usuario) { usuario = it } }
            item { OutlinedTextField(value = clave, onValueChange = { clave = it }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), label = { Text("Contraseña del servicio") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation()) }
            item { LouPrimaryButton("Conectar", { vm.conectar(usuario, clave, id); clave = "" },
                enabled = !estado.cargando && usuario.isNotBlank() && clave.isNotEmpty()) }
        } else {
            item { Row {
                TextButton(onClick = { vm.actualizar(id) }, enabled = !estado.cargando) { Text("Actualizar") }
                TextButton(onClick = { vm.desconectar(); usuario = ""; confirmar = null }, enabled = !estado.cargando) { Text("Cambiar cuenta") }
            } }
            if (estado.avisos.isNotEmpty()) item { LouPanel {
                Text("Cambios de estado")
                estado.avisos.forEach { Text(it) }
                TextButton(onClick = vm::leerAvisos) { Text("Entendido") }
            } }
            if (id == null) {
                if (!estado.cargando && estado.error == null && estado.lista.isEmpty()) item { Text("Sin solicitudes en el servidor.") }
                items(estado.lista, key = { it.id }) { s ->
                    Card(onClick = { abrir(s.id) }, enabled = !estado.cargando, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Solicitud #${s.id} · ${s.estado}")
                            Text(s.descripcion)
                            Text(DateFormat.getDateTimeInstance().format(Date(s.creadaEn)))
                            Text("Ver detalle")
                        }
                    }
                }
            } else estado.detalle?.takeIf { it.id == id }?.let { s -> item { LouPanel {
                Text("Estado: ${s.estado}"); Text(s.descripcion)
                Text("Recibida: ${DateFormat.getDateTimeInstance().format(Date(s.creadaEn))}")
                if (vm.admin) Text("Cliente #${s.usuarioId}")
                Text("${s.categoria} · ${s.material}")
                Text("${s.anchoCm} × ${s.largoCm} cm · Cantidad: ${s.cantidad}")
                Text("Fecha requerida: ${s.fechaRequerida ?: "Sin especificar"}")
                Text("Referencia: ${s.productoReferenciaNombre ?: "Sin referencia"}${s.productoReferenciaId?.let { " (#$it)" }.orEmpty()}")
                if (vm.admin) PoliticaAdministracion.destinos(s.estado.name).forEach { destino ->
                    Button(onClick = { confirmar = EstadoCotizacionRemota.valueOf(destino.name) }, enabled = !estado.cargando) {
                        Text("Cambiar a ${destino.name}")
                    }
                }
            } } }
        }
    }
    val detalle = estado.detalle
    confirmar?.let { nuevo -> if (detalle != null) AlertDialog(onDismissRequest = { confirmar = null },
        title = { Text("Confirmar cambio") }, text = { Text("Solicitud #${detalle.id}: ${detalle.estado} → $nuevo") },
        confirmButton = { TextButton(onClick = { confirmar = null; vm.cambiar(detalle.id, detalle.estado, nuevo) }) { Text("Confirmar") } },
        dismissButton = { TextButton(onClick = { confirmar = null }) { Text("Cancelar") } }) }
}
