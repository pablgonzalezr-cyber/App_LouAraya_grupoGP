package com.example.applouaraya_grupogp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.applouaraya_grupogp.ui.design.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.applouaraya_grupogp.model.Producto
import com.example.applouaraya_grupogp.ui.components.ImagenProducto
import com.example.applouaraya_grupogp.validation.ProductoValidator
import com.example.applouaraya_grupogp.viewmodel.AdminProductosViewModel

@Composable
internal fun CampoContenido(etiqueta: String, valor: String, error: String? = null, habilitado: Boolean = true,
    numero: Boolean = false, multilinea: Boolean = false, decimal: Boolean = false, cambiar: (String) -> Unit) {
    OutlinedTextField(value = valor, onValueChange = cambiar, label = { Text(etiqueta) }, enabled = habilitado,
        shape = RoundedCornerShape(15.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = LouColors.Superficie, unfocusedContainerColor = LouColors.Superficie),
        isError = error != null, supportingText = { error?.let { Text(it) } }, modifier = Modifier.fillMaxWidth(),
        singleLine = !multilinea, minLines = if(multilinea) 3 else 1,
        keyboardOptions = KeyboardOptions(keyboardType = when {
            decimal -> KeyboardType.Decimal
            numero -> KeyboardType.Number
            else -> KeyboardType.Text
        }))
}

@Composable
internal fun OpcionContenido(texto: String, activo: Boolean, habilitado: Boolean = true, cambiar: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(texto, modifier = Modifier.weight(1f)); Switch(checked = activo, onCheckedChange = cambiar, enabled = habilitado)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminProductosScreen(vm: AdminProductosViewModel, onVolver: () -> Unit) {
    val estado by vm.estado.collectAsState()
    val contexto = LocalContext.current.applicationContext
    var desactivar by remember { mutableStateOf<Producto?>(null) }
    var usuario by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    val selector = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { vm.importar(contexto, it) } }
    BackHandler(estado.editando) { if(!estado.guardando) vm.cancelar() }
    AdminPage(if(estado.editando) "Editar producto" else "Administrar productos",
        { if(estado.editando) vm.cancelar() else onVolver() },
        scroll = estado.editando || (vm.usaServidor && !estado.conectado), habilitado = !estado.guardando) {
        estado.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        estado.mensaje?.let { Text(it, color = Color(0xFF38684E)) }
        if(estado.cargando) CircularProgressIndicator()
        if (vm.usaServidor && !estado.conectado) {
            if (vm.permiteAdministracion) {
                Text("Ingresa la cuenta administrativa del servidor de catálogo.")
                CampoContenido("Usuario del servidor", usuario, habilitado = !estado.cargando) { usuario = it }
                OutlinedTextField(value = clave, onValueChange = { clave = it }, label = { Text("Contraseña del servidor") },
                    enabled = !estado.cargando, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                LouPrimaryButton("Conectar al catálogo", {
                    vm.conectar(usuario.trim(), clave)
                    clave = ""
                }, enabled = !estado.cargando && usuario.isNotBlank() && ':' !in usuario && clave.isNotEmpty())
                if (estado.error != null) TextButton(onClick = vm::cargar, enabled = !estado.cargando) { Text("Reintentar") }
            } else Text("Configura la URL HTTPS de administración para conectar al servidor.")
        } else if(estado.editando) {
            val f = estado.formulario
            val habilitado = !estado.guardando
            LouPanel(Modifier.fillMaxWidth()) {
                CampoContenido("Nombre", f.nombre, estado.errores["nombre"], habilitado) { vm.cambiar(f.copy(nombre = it)) }
                CampoContenido("Descripción", f.descripcion, estado.errores["descripcion"], habilitado, multilinea = true) { vm.cambiar(f.copy(descripcion = it)) }
                if (vm.usaServidor) {
                    CampoContenido("Categoría", f.categoria, estado.errores["categoria"], habilitado) { vm.cambiar(f.copy(categoria = it)) }
                } else {
                    Text("Categoría")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProductoValidator.categorias.forEach { c -> FilterChip(selected = f.categoria == c, onClick = { vm.cambiar(f.copy(categoria = c)) }, enabled = habilitado, label = { Text(c) }) }
                    }
                    estado.errores["categoria"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
                CampoContenido("Material", f.material, estado.errores["material"], habilitado) { vm.cambiar(f.copy(material = it)) }
                CampoContenido("Ancho (cm)", f.ancho, estado.errores["ancho"], habilitado, numero = true, decimal = vm.usaServidor) { vm.cambiar(f.copy(ancho = it)) }
                CampoContenido("Largo / alto (cm)", f.largo, estado.errores["largo"], habilitado, numero = true, decimal = vm.usaServidor) { vm.cambiar(f.copy(largo = it)) }
                CampoContenido("Precio referencial (CLP)", f.precio, estado.errores["precio"], habilitado, numero = true, decimal = vm.usaServidor) { vm.cambiar(f.copy(precio = it)) }
                OpcionContenido("Disponible (desactivado: a pedido)", f.disponible, habilitado) { vm.cambiar(f.copy(disponible = it)) }
                OpcionContenido("Publicado en catálogo", f.activo, habilitado) { vm.cambiar(f.copy(activo = it)) }
                if(f.imagenArchivo.isNotEmpty() || f.imagenNombre.isNotEmpty()) {
                    ImagenProducto(Producto(nombre = f.nombre, categoria = f.categoria, material = f.material, precio = 0, descripcion = f.descripcion,
                        imagenNombre = f.imagenNombre, imagenArchivo = f.imagenArchivo), Modifier.align(Alignment.CenterHorizontally).widthIn(max = 320.dp).fillMaxWidth().aspectRatio(1f))
                    TextButton(onClick = { vm.cambiar(f.copy(imagenArchivo = "", imagenNombre = "")) }, enabled = habilitado) { Text("Quitar imagen del producto") }
                }
                OutlinedButton(onClick = { selector.launch(arrayOf("image/*")) }, enabled = habilitado) { Text("Seleccionar imagen del dispositivo") }
                Text("Imagen opcional, hasta 10 MB. Se guarda una copia local.", style = MaterialTheme.typography.bodySmall)
                estado.errores["imagen"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                LouPrimaryButton(if(estado.guardando) "Guardando…" else "Guardar producto", vm::guardar, Modifier.fillMaxWidth(), habilitado)
            }
        } else {
            if (vm.usaServidor) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = vm::cargar, enabled = !estado.guardando && !estado.cargando) { Text("Actualizar") }
                    TextButton(onClick = vm::desconectar, enabled = !estado.guardando) { Text("Cambiar cuenta") }
                }
            } else if (estado.error != null) TextButton(onClick = vm::cargar) { Text("Reintentar") }
            val habilitado = !estado.guardando && (!vm.usaServidor || !estado.cargando)
            LouPrimaryButton("Crear producto", { vm.editar() }, enabled = habilitado)
            CampoContenido("Buscar productos", estado.consulta) { vm.buscar(it) }
            val lista = estado.productos.filter { it.nombre.contains(estado.consulta.trim(), true) || it.descripcion.contains(estado.consulta.trim(), true) }
            if(!estado.cargando && lista.isEmpty()) Text("No hay productos con esa búsqueda.")
            AdminGrid(lista, { it.id }, Modifier.weight(1f)) { p ->
                AdminProductoCard(p, habilitado, { vm.editar(p) }, { desactivar = p },
                    onPublicar = if (vm.usaServidor) ({ vm.publicar(p.id, !p.activo) }) else null)
            }
        }
    }
    desactivar?.let { p -> AlertDialog(onDismissRequest = { desactivar = null },
        title = { Text(if (vm.usaServidor) "Archivar producto" else "Desactivar producto") },
        text = { Text(if (vm.usaServidor) "¿Archivar ${p.nombre}? Saldrá del catálogo y del panel de administración."
            else "¿Ocultar ${p.nombre} del catálogo? Se conservarán el producto y sus favoritos.") },
        confirmButton = { TextButton(onClick = { desactivar = null; vm.desactivar(p.id) }) { Text("Confirmar") } },
        dismissButton = { TextButton(onClick = { desactivar = null }) { Text("Cancelar") } }) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminProductoCard(p: Producto, habilitado: Boolean, editar: () -> Unit, desactivar: () -> Unit,
                      onPublicar: (() -> Unit)? = null) {
    LouPanel(Modifier.fillMaxWidth()) {
        ImagenProducto(p, Modifier.fillMaxWidth().aspectRatio(1f))
        LouEyebrow(p.categoria)
        Text("#${p.id} ${p.nombre}", style = MaterialTheme.typography.titleLarge, color = LouColors.Cafe)
        Text("${java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag("es-CL")).format(p.precio)} CLP",
            style = MaterialTheme.typography.titleLarge, color = LouColors.Terracota)
        Text(p.material, color = LouColors.TextoSecundario)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AdminStatus(if(p.activo) "Publicado" else if (onPublicar != null) "Oculto" else "Desactivado")
            AdminStatus(if(p.disponible) "Disponible" else "A pedido")
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = editar, enabled = habilitado) { Text("Editar") }
            if (onPublicar != null) {
                TextButton(onClick = onPublicar, enabled = habilitado) { Text(if (p.activo) "Ocultar" else "Publicar") }
                TextButton(onClick = desactivar, enabled = habilitado) { Text("Archivar") }
            } else if(p.activo) TextButton(onClick = desactivar, enabled = habilitado) { Text("Desactivar") }
        }
    }
}
