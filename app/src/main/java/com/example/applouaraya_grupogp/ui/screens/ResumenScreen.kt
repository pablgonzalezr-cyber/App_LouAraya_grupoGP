package com.example.applouaraya_grupogp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.applouaraya_grupogp.ui.adaptive.AdaptiveColumn
import com.example.applouaraya_grupogp.ui.design.*
import com.example.applouaraya_grupogp.viewmodel.RegistroViewModel

@Composable
fun ResumenScreen(viewModel: RegistroViewModel, cargando: Boolean,
                  errorGeneral: String?, onEditar: () -> Unit, onConfirmar: () -> Unit) {
    val estado by viewModel.estado.collectAsState()
    BackHandler { if (!cargando) onEditar() }
    AdaptiveColumn(spacing = 16.dp) {
        LouEyebrow("ÚLTIMO PASO")
        LouPageHeading("Resumen de registro", "Revisa tus datos antes de crear tu cuenta")
        LouPanel(Modifier.fillMaxWidth()) {
            LouSectionTitle("Datos de tu cuenta")
            Text("Nombre", color = LouColors.TextoSecundario, style = MaterialTheme.typography.labelMedium)
            Text(estado.nombre.trim(), color = LouColors.Cafe, style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(color = LouColors.Arena)
            Text("Correo", color = LouColors.TextoSecundario, style = MaterialTheme.typography.labelMedium)
            Text(estado.correo.trim(), color = LouColors.Cafe)
            HorizontalDivider(color = LouColors.Arena)
            Text("Términos y condiciones: ${if (estado.aceptaTerminos) "Aceptados" else "Pendientes"}",
                color = LouColors.TextoSecundario)
            LouBadge("Tipo de cuenta: Cliente")
        }
        Text("Revisa tus datos. La cuenta se guardará solamente al confirmar.",
            color = LouColors.TextoSecundario)
        errorGeneral?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        OutlinedButton(onClick = onEditar, enabled = !cargando,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text("Volver a editar")
        }
        LouPrimaryButton(if (cargando) "Guardando..." else "Confirmar registro",
            onClick = { if (viewModel.validarFormulario()) onConfirmar() },
            modifier = Modifier.fillMaxWidth(), enabled = !cargando && estado.resumenDisponible)
    }
}
