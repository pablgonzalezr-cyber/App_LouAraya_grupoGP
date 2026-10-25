package com.example.applouaraya_grupogp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.applouaraya_grupogp.ui.adaptive.AdaptiveColumn
import com.example.applouaraya_grupogp.ui.design.*
import com.example.applouaraya_grupogp.viewmodel.RegistroViewModel

@Composable
fun RegistroScreen(viewModel: RegistroViewModel, cargando: Boolean,
                   errorGeneral: String?, onContinuar: () -> Unit) {
    val estado by viewModel.estado.collectAsState()
    AdaptiveColumn(spacing = 16.dp) {
        Spacer(Modifier.height(10.dp))
        LouEyebrow("ÚNETE A NUESTRA COMUNIDAD")
        LouPageHeading("Crear cuenta", "Regístrate para descubrir nuestros diseños.")
        LouPanel(Modifier.fillMaxWidth()) {
            OutlinedTextField(value = estado.nombre, onValueChange = viewModel::cambiarNombre,
                label = { Text("Nombre") }, singleLine = true, enabled = !cargando,
                isError = estado.errorNombre != null,
                supportingText = { estado.errorNombre?.let { Text(it) } },
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = estado.correo, onValueChange = viewModel::cambiarCorreo,
                label = { Text("Correo ficticio") }, singleLine = true, enabled = !cargando,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = estado.errorCorreo != null,
                supportingText = { estado.errorCorreo?.let { Text(it) } },
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = estado.clave, onValueChange = viewModel::cambiarClave,
                label = { Text("Contraseña de prueba") }, singleLine = true, enabled = !cargando,
                visualTransformation = PasswordVisualTransformation(),
                isError = estado.errorClave != null,
                supportingText = { estado.errorClave?.let { Text(it) } },
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = estado.aceptaTerminos,
                    onCheckedChange = viewModel::cambiarTerminos, enabled = !cargando)
                Text("Acepto los términos y condiciones", color = LouColors.Cafe,
                    style = MaterialTheme.typography.bodyMedium)
            }
            estado.errorTerminos?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            errorGeneral?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            LouPrimaryButton(if (cargando) "Guardando..." else "Continuar",
                onClick = { if (viewModel.validarFormulario()) onContinuar() },
                modifier = Modifier.fillMaxWidth(), enabled = !cargando)
        }
        Text("En la siguiente pantalla podrás revisar tus datos antes de registrarte.",
            color = LouColors.TextoSecundario, style = MaterialTheme.typography.bodySmall)
    }
}
