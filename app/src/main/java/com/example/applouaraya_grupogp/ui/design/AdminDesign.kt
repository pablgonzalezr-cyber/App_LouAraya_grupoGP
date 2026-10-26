package com.example.applouaraya_grupogp.ui.design

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.applouaraya_grupogp.ui.adaptive.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Solo presentación: no consulta repositorios ni decide permisos o transiciones. */
@Composable
fun AdminPage(titulo: String, volver: () -> Unit, scroll: Boolean = false,
              habilitado: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    AdaptiveSurface {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().imePadding(), contentAlignment = Alignment.TopCenter) {
            val lowHeight = maxHeight < 420.dp
            Column(Modifier.widthIn(max = if (scroll) 760.dp else 1200.dp).fillMaxSize()
                .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if(lowHeight) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = volver, enabled = habilitado) { Text("← Volver", color = LouColors.Terracota) }
                        Text(titulo, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = LouColors.Cafe)
                    }
                } else {
                    TextButton(onClick = volver, enabled = habilitado) { Text("← Volver", color = LouColors.Terracota) }
                    LouEyebrow("LOU ARAYA · GESTIÓN DEL TALLER")
                    LouPageHeading(titulo)
                    Text("Administración local · solo datos de este dispositivo",
                        color = LouColors.TextoSecundario, style = MaterialTheme.typography.bodySmall)
                }
                content()
            }
        }
    }
}

@Composable
fun <T> AdminGrid(values: List<T>, key: (T) -> Any, modifier: Modifier = Modifier,
                  card: @Composable (T) -> Unit) {
    LazyVerticalGrid(columns = GridCells.Fixed(columnasPara(LocalLouWidth.current)), modifier = modifier,
        contentPadding = PaddingValues(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        items(values, key = key) { card(it) }
    }
}

@Composable
fun AdminStatus(texto: String, tono: String = texto) {
    val color = when (tono) {
        "APROBADA", "Publicado", "Publicada", "Activa", "Disponible", "Vigente" -> LouColors.Verde
        "RECHAZADA", "Desactivado", "Desactivada", "Finalizada" -> LouColors.TerracotaOscuro
        "EN_REVISION", "Programada" -> Color(0xFF536879)
        "PENDIENTE" -> Color(0xFF92632E)
        "Sin leer" -> LouColors.Terracota
        "Leída" -> LouColors.TextoSecundario
        else -> LouColors.Cafe
    }
    Surface(color = color.copy(alpha = .10f), shape = LouShape) {
        Text(texto, Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = color,
            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AdminMetrics(values: List<Pair<String, String>>) {
    val columns = if (columnasPara(LocalLouWidth.current) == 1) 2 else 3
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        values.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (label, value) ->
                    LouPanel(Modifier.weight(1f)) {
                        Text(value, style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold, color = LouColors.Terracota)
                        Text(label, style = MaterialTheme.typography.labelMedium, color = LouColors.Cafe)
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun AdminModules(values: List<Triple<String, String, () -> Unit>>) {
    val columns = maxOf(2, columnasPara(LocalLouWidth.current))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        values.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (icon, label, action) ->
                    LouShortcut(icon, label, action, Modifier.weight(1f).heightIn(min = 108.dp))
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

fun adminFecha(timestamp: Long): String = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
