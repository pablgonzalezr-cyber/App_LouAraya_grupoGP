package com.example.applouaraya_grupogp.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.applouaraya_grupogp.repository.PreferenciasLocales
import com.example.applouaraya_grupogp.viewmodel.PerfilViewModel

@Composable
fun PerfilScreen(vm: PerfilViewModel, preferencias: PreferenciasLocales, guardandoPreferencias: Boolean,
    errorPreferencias: String?, onPreferencias: (PreferenciasLocales) -> Unit,
    onVolver: () -> Unit, onCerrarSesion: () -> Unit, onCambiarUsuario: () -> Unit) {
    val estado by vm.estado.collectAsState()
    val contexto = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var revision by remember { mutableIntStateOf(0) }
    var errorAjustes by remember { mutableStateOf<String?>(null) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, evento -> if (evento == Lifecycle.Event.ON_RESUME) revision++ }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    @Suppress("DEPRECATION")
    val permisos = remember(revision) { contexto.packageManager.getPackageInfo(contexto.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions?.toSet().orEmpty() }
    fun estadoPermiso(permiso: String): String = when {
        permiso !in permisos -> "No declarado; sin uso actual"
        ContextCompat.checkSelfPermission(contexto, permiso) == PackageManager.PERMISSION_GRANTED -> "Concedido"
        else -> "No concedido"
    }
    Column(Modifier.fillMaxSize().background(Color(0xFFF7F1E8)).safeDrawingPadding()
        .verticalScroll(rememberScrollState()).imePadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onVolver) { Text("← Inicio") }
        Text("Mi perfil", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF51362B))
        if (estado.cargando) CircularProgressIndicator()
        if (!estado.cargando) {
            Text("Correo: ${estado.correo}")
            Text("Rol: ${estado.rol}")
            OutlinedTextField(value = estado.nombre, onValueChange = vm::cambiarNombre, enabled = !estado.guardando,
                label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                isError = estado.errorNombre != null, supportingText = { estado.errorNombre?.let { Text(it) } })
            Button(onClick = vm::guardar, enabled = estado.editando && !estado.guardando,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA65E43))) {
                Text(if (estado.guardando) "Guardando…" else "Guardar nombre")
            }
        }
        estado.error?.let { Text(it, color = MaterialTheme.colorScheme.error); TextButton(onClick = vm::cargar) { Text("Reintentar") } }
        estado.mensaje?.let { Text(it, color = Color(0xFF38684E)) }
        HorizontalDivider()
        Text("Configuración de esta cuenta", style = MaterialTheme.typography.titleLarge, color = Color(0xFF51362B))
        Text("Preferencias locales guardadas en este dispositivo.")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Confirmar cierre o cambio\nde usuario", modifier = Modifier.weight(1f))
            Switch(checked = preferencias.confirmarSalida, enabled = !guardandoPreferencias,
                onCheckedChange = { onPreferencias(preferencias.copy(confirmarSalida = it)) })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Mostrar ayuda en cotizaciones", modifier = Modifier.weight(1f))
            Switch(checked = preferencias.mostrarAyuda, enabled = !guardandoPreferencias,
                onCheckedChange = { onPreferencias(preferencias.copy(mostrarAyuda = it)) })
        }
        errorPreferencias?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        HorizontalDivider()
        Text("Permisos del dispositivo", style = MaterialTheme.typography.titleLarge, color = Color(0xFF51362B))
        Text("Cámara: ${estadoPermiso(Manifest.permission.CAMERA)}")
        Text("Ubicación aproximada: ${estadoPermiso(Manifest.permission.ACCESS_COARSE_LOCATION)}")
        Text("Ubicación precisa: ${estadoPermiso(Manifest.permission.ACCESS_FINE_LOCATION)}")
        Text("Esta pantalla solo consulta el estado. No activa la cámara ni obtiene tu ubicación.", style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = {
            try { contexto.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${contexto.packageName}"))) }
            catch (_: android.content.ActivityNotFoundException) { errorAjustes = "No se pudieron abrir los ajustes del sistema" }
        }) { Text("Abrir ajustes del sistema") }
        errorAjustes?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        OutlinedButton(onClick = onCambiarUsuario, enabled = !estado.guardando && !guardandoPreferencias, modifier = Modifier.fillMaxWidth()) { Text("Cambiar de usuario") }
        OutlinedButton(onClick = onCerrarSesion, enabled = !estado.guardando && !guardandoPreferencias, modifier = Modifier.fillMaxWidth()) { Text("Cerrar sesión") }
    }
}
