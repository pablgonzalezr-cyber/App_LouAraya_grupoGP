package com.example.applouaraya_grupogp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.applouaraya_grupogp.ui.adaptive.AdaptiveColumn
import com.example.applouaraya_grupogp.ui.design.*

@Composable
fun LoginScreen(cargando: Boolean, errorGeneral: String?,
                onIngresar: (String, String) -> Unit, onRegistrarse: () -> Unit) {
    var correo by rememberSaveable { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    AdaptiveColumn(centered = true, spacing = 14.dp) {
        LouBrandMark()
        Text("Bienvenido de nuevo", style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold, color = LouColors.Cafe, textAlign = TextAlign.Center)
        Text("Iniciar sesión", style = MaterialTheme.typography.titleMedium,
            color = LouColors.TextoSecundario)
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LouEyebrow("Ingresa a tu cuenta")
            OutlinedTextField(value = correo, onValueChange = { correo = it },
                label = { Text("Correo electrónico") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            OutlinedTextField(value = clave, onValueChange = { clave = it },
                label = { Text("Contraseña") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            errorGeneral?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            LouPrimaryButton(if (cargando) "Ingresando..." else "Ingresar",
                onClick = { onIngresar(correo, clave) },
                modifier = Modifier.fillMaxWidth(), enabled = !cargando)
        }
        TextButton(onClick = onRegistrarse) { Text("¿No tienes cuenta? Regístrate") }
        Text("Decoración artesanal · Diseño personalizado", color = LouColors.TextoSecundario,
            style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
    }
}
