package com.example.applouaraya_grupogp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.applouaraya_grupogp.model.Producto
import com.example.applouaraya_grupogp.ui.components.ImagenProducto
import com.example.applouaraya_grupogp.validation.ProductoValidator
import com.example.applouaraya_grupogp.viewmodel.AdminProductosViewModel

@Composable
internal fun CampoContenido(etiqueta: String, valor: String, error: String? = null, habilitado: Boolean = true,
    numero: Boolean = false, multilinea: Boolean = false, cambiar: (String) -> Unit) {
    OutlinedTextField(value = valor, onValueChange = cambiar, label = { Text(etiqueta) }, enabled = habilitado,
        isError = error != null, supportingText = { error?.let { Text(it) } }, modifier = Modifier.fillMaxWidth(),
        singleLine = !multilinea, minLines = if(multilinea) 3 else 1,
        keyboardOptions = KeyboardOptions(keyboardType = if(numero) KeyboardType.Number else KeyboardType.Text))
}

@Composable
internal fun OpcionContenido(texto: String, activo: Boolean, habilitado: Boolean = true, cambiar: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(texto, modifier = Modifier.weight(1f)); Switch(checked = activo, onCheckedChange = cambiar, enabled = habilitado)
    }
}

@Composable
fun AdminProductosScreen(vm: AdminProductosViewModel, onVolver: () -> Unit) {
    val estado by vm.estado.collectAsState()
    val contexto = LocalContext.current.applicationContext
    var desactivar by remember { mutableStateOf<Producto?>(null) }
    val selector = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { vm.importar(contexto, it) } }
    BackHandler(estado.editando) { if(!estado.guardando) vm.cancelar() }
    Column(Modifier.fillMaxSize().background(Color(0xFFF7F1E8)).safeDrawingPadding().imePadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onClick = { if(estado.editando) vm.cancelar() else onVolver() }, enabled = !estado.guardando) { Text("← Volver") }
        Text(if(estado.editando) "Editar producto" else "Administrar productos", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF51362B))
        estado.error?.let { Text(it, color = MaterialTheme.colorScheme.error); if(!estado.editando) TextButton(onClick = vm::cargar) { Text("Reintentar") } }
        estado.mensaje?.let { Text(it, color = Color(0xFF38684E)) }
        if(estado.cargando) CircularProgressIndicator()
        if(estado.editando) {
            val f = estado.formulario
            val habilitado = !estado.guardando
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoContenido("Nombre", f.nombre, estado.errores["nombre"], habilitado) { vm.cambiar(f.copy(nombre = it)) }
                CampoContenido("Descripción", f.descripcion, estado.errores["descripcion"], habilitado, multilinea = true) { vm.cambiar(f.copy(descripcion = it)) }
                Text("Categoría")
                ProductoValidator.categorias.forEach { c -> FilterChip(selected = f.categoria == c, onClick = { vm.cambiar(f.copy(categoria = c)) }, enabled = habilitado, label = { Text(c) }) }
                estado.errores["categoria"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                CampoContenido("Material", f.material, estado.errores["material"], habilitado) { vm.cambiar(f.copy(material = it)) }
                CampoContenido("Ancho (cm)", f.ancho, estado.errores["ancho"], habilitado, numero = true) { vm.cambiar(f.copy(ancho = it)) }
                CampoContenido("Largo / alto (cm)", f.largo, estado.errores["largo"], habilitado, numero = true) { vm.cambiar(f.copy(largo = it)) }
                CampoContenido("Precio referencial (CLP)", f.precio, estado.errores["precio"], habilitado, numero = true) { vm.cambiar(f.copy(precio = it)) }
                OpcionContenido("Disponible (desactivado: a pedido)", f.disponible, habilitado) { vm.cambiar(f.copy(disponible = it)) }
                OpcionContenido("Publicado en catálogo", f.activo, habilitado) { vm.cambiar(f.copy(activo = it)) }
                if(f.imagenArchivo.isNotEmpty() || f.imagenNombre.isNotEmpty()) {
                    ImagenProducto(Producto(nombre = f.nombre, categoria = f.categoria, material = f.material, precio = 0, descripcion = f.descripcion,
                        imagenNombre = f.imagenNombre, imagenArchivo = f.imagenArchivo), Modifier.fillMaxWidth().height(180.dp))
                    TextButton(onClick = { vm.cambiar(f.copy(imagenArchivo = "", imagenNombre = "")) }, enabled = habilitado) { Text("Quitar imagen del producto") }
                }
                OutlinedButton(onClick = { selector.launch(arrayOf("image/*")) }, enabled = habilitado) { Text("Seleccionar imagen del dispositivo") }
                Text("Imagen opcional, hasta 10 MB. Se guarda una copia local.", style = MaterialTheme.typography.bodySmall)
                estado.errores["imagen"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(onClick = vm::guardar, enabled = habilitado, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA65E43))) { Text(if(estado.guardando) "Guardando…" else "Guardar producto") }
            }
        } else {
            Button(onClick = { vm.editar() }, enabled = !estado.guardando) { Text("Crear producto") }
            CampoContenido("Buscar productos", estado.consulta) { vm.buscar(it) }
            val lista = estado.productos.filter { it.nombre.contains(estado.consulta.trim(), true) || it.descripcion.contains(estado.consulta.trim(), true) }
            if(!estado.cargando && lista.isEmpty()) Text("No hay productos con esa búsqueda.")
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(lista, key = { it.id }) { p -> Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBF5))) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text("#${p.id} ${p.nombre}", style = MaterialTheme.typography.titleMedium)
                        Text("${p.precio} CLP · ${if(p.activo) "Publicado" else "Desactivado"} · ${if(p.disponible) "Disponible" else "A pedido"}")
                        Row { TextButton(onClick = { vm.editar(p) }, enabled = !estado.guardando) { Text("Editar") }
                            if(p.activo) TextButton(onClick = { desactivar = p }, enabled = !estado.guardando) { Text("Desactivar") } }
                    }
                } }
            }
        }
    }
    desactivar?.let { p -> AlertDialog(onDismissRequest = { desactivar = null }, title = { Text("Desactivar producto") },
        text = { Text("¿Ocultar ${p.nombre} del catálogo? Se conservarán el producto y sus favoritos.") },
        confirmButton = { TextButton(onClick = { desactivar = null; vm.desactivar(p.id) }) { Text("Confirmar") } },
        dismissButton = { TextButton(onClick = { desactivar = null }) { Text("Cancelar") } }) }
}
