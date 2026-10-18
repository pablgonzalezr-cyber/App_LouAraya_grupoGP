package com.example.applouaraya_grupogp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.example.applouaraya_grupogp.ui.adaptive.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DashboardScreen(
    nombre: String,
    onCatalogo: () -> Unit,
    onCerrarSesion: () -> Unit,
    onCotizacion: () -> Unit,
    onHistorial: () -> Unit,
    onFavoritos: () -> Unit,
    onPerfil: () -> Unit,
    onNovedades: () -> Unit,
    onEscaner: () -> Unit,
    onMapa: () -> Unit,
    onAvisos: () -> Unit,
    avisosNoLeidos: Int
) {
    AdaptiveColumn(form = false, spacing = 20.dp) {
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

        AdaptiveActions(listOf(
            "Cotización personalizada" to onCotizacion,
            "Mis solicitudes" to onHistorial,
            "Mis favoritos" to onFavoritos,
            "Perfil y configuración" to onPerfil,
            "Colecciones y campañas" to onNovedades,
            "Escanear QR" to onEscaner,
            "Mapa de ferias y showrooms" to onMapa,
            "Centro de avisos ($avisosNoLeidos sin leer)" to onAvisos,
            "Cerrar sesión" to onCerrarSesion
        ))
    }
}
