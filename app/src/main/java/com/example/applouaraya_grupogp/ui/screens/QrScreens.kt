package com.example.applouaraya_grupogp.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.applouaraya_grupogp.repository.ProductoQr
import com.example.applouaraya_grupogp.repository.QrImagen
import com.example.applouaraya_grupogp.ui.components.CamaraQr
import com.example.applouaraya_grupogp.viewmodel.QrDemoViewModel
import com.example.applouaraya_grupogp.viewmodel.QrViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun QrScannerScreen(vm: QrViewModel, onVolver: () -> Unit, onProducto: (Int) -> Unit, onDemo: () -> Unit) {
    val estado by vm.estado.collectAsState()
    val contexto = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    fun concedido() = ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    var permiso by remember { mutableStateOf(concedido()) }
    var errorCamara by remember { mutableStateOf<String?>(null) }
    var frame by remember { mutableStateOf(false) }
    var intento by remember { mutableIntStateOf(0) }
    var denegado by rememberSaveable { mutableStateOf(false) }
    val solicitar = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { autorizado ->
        permiso = autorizado; denegado = !autorizado; errorCamara = null
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, e -> if(e == Lifecycle.Event.ON_RESUME) permiso = concedido() }
        lifecycle.addObserver(observer); onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(estado.productoId) { estado.productoId?.let(onProducto) }
    Column(Modifier.fillMaxSize().background(Color(0xFFF7F1E8)).safeDrawingPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onClick = onVolver) { Text("← Volver") }
        Text("Escanear QR", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF51362B))
        Text("Apunta la cámara a un QR de producto Lou Araya. La lectura y consulta se realizan en este teléfono.")
        if(!permiso) {
            Text(if(denegado) "Permiso de cámara no concedido. Puedes intentarlo otra vez o habilitarlo en Ajustes." else "Necesitamos permiso de cámara solo para usar este escáner.")
            Button(onClick = { solicitar.launch(Manifest.permission.CAMERA) }) { Text("Permitir cámara") }
            OutlinedButton(onClick = {
                try { contexto.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${contexto.packageName}"))) }
                catch(_: android.content.ActivityNotFoundException) { errorCamara = "No se pudieron abrir los ajustes del sistema" }
            }) { Text("Abrir ajustes de cámara") }
        } else if(errorCamara == null) {
            key(intento) {
                CamaraQr(Modifier.fillMaxWidth().weight(1f), habilitado = !estado.bloqueado,
                    onCodigo = vm::leer, onError = { errorCamara = it }, onPrimerFrame = { frame = true })
            }
            Text(if(frame) "Cámara activa" else "Iniciando cámara…", style = MaterialTheme.typography.bodySmall)
        }
        errorCamara?.let { mensaje ->
            Text(mensaje, color = MaterialTheme.colorScheme.error)
            if(permiso) OutlinedButton(onClick = { errorCamara = null; frame = false; intento++ }) { Text("Reintentar cámara") }
        }
        if(estado.procesando) CircularProgressIndicator()
        estado.error?.let { Text(it, color = MaterialTheme.colorScheme.error); Button(onClick = vm::reintentar) { Text("Escanear de nuevo") } }
        OutlinedButton(onClick = onDemo) { Text("Ver QR de demostración") }
    }
}

@Composable
fun QrDemoScreen(vm: QrDemoViewModel, onVolver: () -> Unit) {
    val estado by vm.estado.collectAsState()
    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()
    var seleccionado by rememberSaveable { mutableStateOf<Int?>(null) }
    var menu by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var exportando by remember { mutableStateOf(false) }
    val producto = if(seleccionado == null) estado.productos.firstOrNull() else estado.productos.find { it.id == seleccionado }
    val codigo = producto?.let { ProductoQr.codigoPara(it.id) }
    val bitmap by produceState<Bitmap?>(null, codigo) {
        value = null
        value = codigo?.let { withContext(Dispatchers.Default) { QrImagen.generar(it) } }
    }
    val guardar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        val imagen = bitmap
        if(uri != null && imagen != null) scope.launch {
            exportando = true
            mensaje = try {
                withContext(Dispatchers.IO) { requireNotNull(contexto.contentResolver.openOutputStream(uri)).use { check(imagen.compress(Bitmap.CompressFormat.PNG, 100, it)) } }
                "QR guardado. Puedes mostrarlo en otra pantalla para escanearlo."
            } catch(_: Exception) { "No se pudo guardar el QR" }
            exportando = false
        }
    }
    Column(Modifier.fillMaxSize().background(Color(0xFFF7F1E8)).safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onVolver) { Text("← Volver") }
        Text("QR de demostración", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF51362B))
        Text("Este QR identifica un producto local. Muestra la imagen en otro dispositivo o guárdala para abrirla en una pantalla.")
        if(estado.cargando) CircularProgressIndicator()
        estado.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if(!estado.cargando && estado.productos.isEmpty()) Text("No hay productos publicados para generar un QR.")
        if(estado.productos.isNotEmpty()) {
            Box {
                OutlinedButton(onClick = { menu = true }) { Text(producto?.let { "#${it.id} ${it.nombre}" } ?: "Selecciona un producto publicado") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    estado.productos.forEach { p -> DropdownMenuItem(text = { Text("#${p.id} ${p.nombre}") }, onClick = { seleccionado = p.id; menu = false; mensaje = null }) }
                }
            }
        }
        bitmap?.let { Image(it.asImageBitmap(), contentDescription = "QR del producto ${producto?.nombre}", modifier = Modifier.fillMaxWidth().aspectRatio(1f)) }
        codigo?.let { Text(it) }
        Button(onClick = { guardar.launch("lou-araya-producto-${producto?.id}.png") }, enabled = bitmap != null && !exportando) { Text(if(exportando) "Guardando…" else "Guardar QR como imagen") }
        mensaje?.let { Text(it) }
    }
}
