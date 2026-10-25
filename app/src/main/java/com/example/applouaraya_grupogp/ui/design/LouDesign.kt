package com.example.applouaraya_grupogp.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Sistema visual común. Los datos siguen viniendo de los ViewModels existentes. */
object LouColors {
    val Fondo = Color(0xFFFAF5EE)
    val Superficie = Color(0xFFFFFCF8)
    val Cafe = Color(0xFF463126)
    val TextoSecundario = Color(0xFF7D6B60)
    val Terracota = Color(0xFFB35F45)
    val TerracotaOscuro = Color(0xFF914730)
    val Arena = Color(0xFFEADCCB)
    val ArenaClara = Color(0xFFF2E6D8)
    val Verde = Color(0xFF437259)
}

val LouShape = RoundedCornerShape(22.dp)

@Composable
fun LouEyebrow(texto: String) {
    Text(texto.uppercase(), style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold, color = LouColors.Terracota)
}

@Composable
fun LouPageHeading(titulo: String, subtitulo: String? = null, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(titulo, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
            color = LouColors.Cafe)
        subtitulo?.let { Text(it, color = LouColors.TextoSecundario,
            style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
fun LouSectionTitle(titulo: String, nota: String? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            color = LouColors.Cafe)
        nota?.let { Text(it, style = MaterialTheme.typography.labelMedium,
            color = LouColors.Terracota) }
    }
}

@Composable
fun LouPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = modifier, shape = LouShape,
        colors = CardDefaults.cardColors(containerColor = LouColors.Superficie),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp), content = content)
    }
}

@Composable
fun LouPrimaryButton(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier,
                     enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(15.dp),
        colors = ButtonDefaults.buttonColors(containerColor = LouColors.Terracota)) {
        Text(texto, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun LouBadge(texto: String, disponible: Boolean = true) {
    Surface(shape = RoundedCornerShape(50), color = if (disponible) Color(0xFFE3EEE3) else LouColors.ArenaClara) {
        Text(texto, Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (disponible) LouColors.Verde else LouColors.TerracotaOscuro,
            fontWeight = FontWeight.Bold)
    }
}

@Composable
fun LouShortcut(simbolo: String, titulo: String, onClick: () -> Unit,
                modifier: Modifier = Modifier, nota: String? = null) {
    Card(onClick = onClick, modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = LouColors.Superficie),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.Start) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                .background(LouColors.ArenaClara), contentAlignment = Alignment.Center) {
                Text(simbolo, color = LouColors.Terracota, style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold)
            }
            Text(titulo, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium,
                color = LouColors.Cafe)
            nota?.let { Text(it, style = MaterialTheme.typography.bodySmall,
                color = LouColors.TextoSecundario) }
        }
    }
}

@Composable
fun LouBrandMark(modifier: Modifier = Modifier) {
    Surface(modifier, color = LouColors.ArenaClara, shape = CircleShape,
        border = BorderStroke(1.dp, LouColors.Arena)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
            Text("❧", style = MaterialTheme.typography.headlineLarge, color = LouColors.Terracota)
        }
    }
}

@Composable
fun LouHeroBanner(titulo: String, subtitulo: String, accion: String, onAccion: () -> Unit,
                  modifier: Modifier = Modifier) {
    Box(modifier.clip(LouShape).background(Brush.linearGradient(
        listOf(LouColors.Arena, Color(0xFFF8EFE3), Color(0xFFE2C7AE))))) {
        Column(Modifier.fillMaxWidth().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)) {
            LouEyebrow("PIEZAS TEJIDAS A MANO")
            Text(titulo, style = MaterialTheme.typography.headlineSmall,
                color = LouColors.Cafe, fontWeight = FontWeight.Bold)
            Text(subtitulo, color = LouColors.Cafe, style = MaterialTheme.typography.bodyMedium)
            LouPrimaryButton(accion, onAccion)
        }
    }
}

@Composable
fun LouBackButton(onClick: () -> Unit, etiqueta: String = "← Volver") {
    TextButton(onClick = onClick) { Text(etiqueta, color = LouColors.Terracota) }
}

