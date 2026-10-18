package com.example.applouaraya_grupogp.ui.adaptive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.windowsizeclass.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*

val LocalLouWidth = staticCompositionLocalOf { WindowWidthSizeClass.Compact }

fun columnasPara(ancho: WindowWidthSizeClass): Int = when (ancho) {
    WindowWidthSizeClass.Expanded -> 3
    WindowWidthSizeClass.Medium -> 2
    else -> 1
}

/** Clasifica el espacio de la ventana antes de aplicar márgenes o limitar el contenido. */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun AdaptiveSurface(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFFF7F1E8))) {
        val size = WindowSizeClass.calculateFromSize(DpSize(maxWidth, maxHeight))
        CompositionLocalProvider(LocalLouWidth provides size.widthSizeClass) { content() }
    }
}

/** Un solo scroll, insets seguros y espacio para IME; formularios centrados en ventanas amplias. */
@Composable
fun AdaptiveColumn(
    form: Boolean = true,
    padding: Dp = 24.dp,
    centered: Boolean = false,
    spacing: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    AdaptiveSurface {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().imePadding(), contentAlignment = Alignment.TopCenter) {
            val minHeight = maxHeight
            val width = if (form) {
                if (LocalLouWidth.current == WindowWidthSizeClass.Expanded) 560.dp else 520.dp
            } else 1200.dp
            Column(
                Modifier.widthIn(max = width).fillMaxWidth().verticalScroll(rememberScrollState())
                    .heightIn(min = minHeight).padding(padding),
                horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
                verticalArrangement = if (centered) Arrangement.Center else Arrangement.spacedBy(spacing),
                content = content
            )
        }
    }
}

/** Conserva el orden de lectura y las acciones existentes, distribuidas en 1/2/3 columnas. */
@Composable
fun AdaptiveActions(actions: List<Pair<String, () -> Unit>>) {
    val columns = columnasPara(LocalLouWidth.current)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        actions.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (label, click) ->
                    if (label == actions.first().first) {
                        androidx.compose.material3.Button(onClick = click,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFA65E43)),
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                            androidx.compose.material3.Text(label)
                        }
                    } else androidx.compose.material3.OutlinedButton(onClick = click, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                        androidx.compose.material3.Text(label)
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
