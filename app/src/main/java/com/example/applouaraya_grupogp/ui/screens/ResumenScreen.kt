package com.example.applouaraya_grupogp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.example.applouaraya_grupogp.viewmodel.RegistroViewModel

@Composable
fun ResumenScreen(
    viewModel: RegistroViewModel,
    cargando: Boolean,
    errorGeneral: String?,
    onEditar: () -> Unit,
    onConfirmar: () -> Unit
) {
    val estado by viewModel.estado.collectAsState()
    BackHandler { if(!cargando) onEditar() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F1E8))
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Resumen de registro",
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFF51362B)
        )

        Spacer(Modifier.height(20.dp))

        Text("Nombre: ${estado.nombre.trim()}")
        Text("Correo: ${estado.correo.trim()}")
        Text("Términos y condiciones: ${if(estado.aceptaTerminos) "Aceptados" else "Pendientes"}")
        Text("Tipo de cuenta: Cliente")

        Spacer(Modifier.height(12.dp))

        Text("Revisa tus datos. La cuenta se guardará solamente al confirmar.")
        errorGeneral?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Spacer(Modifier.height(24.dp))

        OutlinedButton(onClick = onEditar, enabled = !cargando, modifier = Modifier.fillMaxWidth()) {
            Text("Volver a editar")
        }
        Button(onClick = { if(viewModel.validarFormulario()) onConfirmar() },
            enabled = !cargando && estado.resumenDisponible,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA65E43))) {
            Text(if(cargando) "Guardando..." else "Confirmar registro")
        }
    }
}
