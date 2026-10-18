package com.example.applouaraya_grupogp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.applouaraya_grupogp.viewmodel.RegistroViewModel

@Composable
fun RegistroScreen(
    viewModel: RegistroViewModel,
    cargando: Boolean,
    errorGeneral: String?,
    onContinuar: () -> Unit
) {
    val estado by viewModel.estado.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F1E8))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        Text(
            "Crear cuenta",
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFF51362B)
        )

        Text("Regístrate para descubrir nuestros diseños.")

        OutlinedTextField(
            value = estado.nombre,
            onValueChange = viewModel::cambiarNombre,
            label = { Text("Nombre") },
            singleLine = true,
            enabled = !cargando,
            isError = estado.errorNombre != null,
            supportingText = {
                estado.errorNombre?.let { Text(it) }
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = estado.correo,
            onValueChange = viewModel::cambiarCorreo,
            label = { Text("Correo ficticio") },
            singleLine = true,
            enabled = !cargando,
            isError = estado.errorCorreo != null,
            supportingText = {
                estado.errorCorreo?.let { Text(it) }
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = estado.clave,
            onValueChange = viewModel::cambiarClave,
            label = { Text("Contraseña de prueba") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            enabled = !cargando,
            isError = estado.errorClave != null,
            supportingText = {
                estado.errorClave?.let { Text(it) }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row {
            Checkbox(
                checked = estado.aceptaTerminos,
                onCheckedChange = viewModel::cambiarTerminos,
                enabled = !cargando
            )
            Text(
                "Acepto los términos y condiciones",
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        estado.errorTerminos?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        errorGeneral?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = {
                if (viewModel.validarFormulario()) {
                    onContinuar()
                }
            },
            enabled = !cargando,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFA65E43)
            )
        ) {
            Text(
                if (cargando) "Guardando..."
                else "Continuar"
            )
        }
    }
}
