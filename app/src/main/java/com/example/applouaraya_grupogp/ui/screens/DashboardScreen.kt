package com.example.applouaraya_grupogp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DashboardScreen(
    nombre: String,
    onCatalogo: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F1E8))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        Text(
            "Lou Araya Macramé",
            style = MaterialTheme.typography.headlineLarge,
            color = Color(0xFF51362B)
        )

        Text(
            "¡Bienvenido, $nombre!",
            style = MaterialTheme.typography.titleLarge
        )

        Text("Descubre el arte del macramé hecho a mano.")

        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE9D8C6)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Explora nuestra colección",
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    "Piezas únicas para decorar hogares y eventos."
                )

                Button(
                    onClick = onCatalogo,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFA65E43)
                    )
                ) {
                    Text("Ver catálogo")
                }
            }
        }

        Spacer(Modifier.weight(1f))

        OutlinedButton(
            onClick = onCerrarSesion,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cerrar sesión")
        }
    }
}

