package com.example.applouaraya_grupogp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.applouaraya_grupogp.R
import com.example.applouaraya_grupogp.ui.adaptive.*
import com.example.applouaraya_grupogp.ui.components.FotografiaProducto
import com.example.applouaraya_grupogp.ui.design.*

@Composable
fun DashboardScreen(nombre: String, onCatalogo: () -> Unit, onCerrarSesion: () -> Unit,
                    onCotizacion: () -> Unit, onHistorial: () -> Unit, onFavoritos: () -> Unit,
                    onPerfil: () -> Unit, onNovedades: () -> Unit, onEscaner: () -> Unit,
                    onMapa: () -> Unit, onAvisos: () -> Unit, avisosNoLeidos: Int) {
    AdaptiveColumn(form = false, spacing = 18.dp, padding = 20.dp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column {
                LouEyebrow("LOU ARAYA · TALLER TEXTIL")
                Text("Lou Araya Macramé", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = LouColors.Cafe)
            }
            LouBrandMark()
        }
        Text("¡Bienvenido, $nombre!", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, color = LouColors.Cafe)
        Text("Descubre el arte del macramé hecho a mano.", color = LouColors.TextoSecundario)
        Card(shape = LouShape, colors = CardDefaults.cardColors(containerColor = LouColors.ArenaClara),
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth()) {
                FotografiaProducto(painter = painterResource(R.drawable.macrame_tapiz),
                    descripcion = "Colección artesanal destacada",
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                        .widthIn(max = 440.dp).fillMaxWidth().aspectRatio(1f))
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    LouEyebrow("COLECCIÓN DESTACADA")
                    Text("Colección de diseño artesanal", style = MaterialTheme.typography.titleLarge,
                        color = LouColors.Cafe, fontWeight = FontWeight.Bold)
                    Text("Piezas únicas para decorar hogares y eventos.", color = LouColors.TextoSecundario)
                    LouPrimaryButton("Ver catálogo", onCatalogo)
                }
            }
        }
        LouSectionTitle("Explora Lou Araya", "Accesos rápidos")
        val acciones = listOf(
            Triple("▦", "Catálogo", onCatalogo),
            Triple("♡", "Mis favoritos", onFavoritos),
            Triple("◇", "Mapa de ferias y showrooms", onMapa),
            Triple("▣", "Escanear QR", onEscaner),
            Triple("✦", "Cotización personalizada", onCotizacion),
            Triple("≡", "Mis solicitudes", onHistorial),
            Triple("❀", "Colecciones y campañas", onNovedades),
            Triple("◉", "Centro de avisos ($avisosNoLeidos sin leer)", onAvisos),
            Triple("☺", "Perfil y configuración", onPerfil)
        )
        val columnas = if (columnasPara(LocalLouWidth.current) == 1) 2 else 3
        acciones.chunked(columnas).forEach { fila ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                fila.forEach { (simbolo, texto, accion) ->
                    LouShortcut(simbolo, texto, accion, Modifier.weight(1f))
                }
                repeat(columnas - fila.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        TextButton(onClick = onCerrarSesion, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Cerrar sesión", color = LouColors.Terracota)
        }
    }
}
