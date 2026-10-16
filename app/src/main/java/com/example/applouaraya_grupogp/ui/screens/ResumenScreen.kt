package com.example.applouaraya_grupogp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.applouaraya_grupogp.viewmodel.RegistroViewModel

@Composable
fun ResumenScreen(
    viewModel: RegistroViewModel,
    onVolver: () -> Unit
) {
    val estado by viewModel.estado.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Registro validado",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(20.dp))

        Text("Nombre: ${estado.nombre}")
        Text("Correo: ${estado.correo}")

        Spacer(Modifier.height(12.dp))

        Text("Datos de prueba. Aún no se han guardado.")

        Spacer(Modifier.height(24.dp))

        Button(onClick = onVolver) {
            Text("Volver al inicio")
        }
    }
}
