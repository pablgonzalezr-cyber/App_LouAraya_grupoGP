package com.example.applouaraya_grupogp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.applouaraya_grupogp.model.Producto
import com.example.applouaraya_grupogp.ui.adaptive.*
import com.example.applouaraya_grupogp.ui.components.ProductoCard
import com.example.applouaraya_grupogp.ui.design.*

@Composable
fun FavoritosScreen(productos: List<Producto>, idsFavoritos: Set<Int>,
                    onVolver: () -> Unit, onAbrir: (Int) -> Unit, onQuitar: (Int) -> Unit) {
    val favoritos = productos.filter { it.id in idsFavoritos }
    AdaptiveSurface {
        LazyVerticalGrid(columns = GridCells.Fixed(columnasPara(LocalLouWidth.current)),
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 28.dp)) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LouBackButton(onVolver, "← Volver al catálogo")
                    LouEyebrow("TUS SELECCIONES")
                    LouPageHeading("Mis favoritos", "Guarda y encuentra tus diseños preferidos")
                    LouBadge("${favoritos.size} piezas guardadas")
                    if (favoritos.isEmpty()) {
                        LouPanel(Modifier.fillMaxWidth()) {
                            Text("Aún no tienes diseños favoritos.",
                                style = MaterialTheme.typography.titleMedium, color = LouColors.Cafe)
                            Text("Explora el catálogo y toca Guardar en los productos que más te gusten.",
                                color = LouColors.TextoSecundario)
                            TextButton(onClick = onVolver) { Text("Explorar catálogo") }
                        }
                    }
                }
            }
            items(favoritos, key = { it.id }) { producto ->
                ProductoCard(producto, true, true, { onAbrir(producto.id) },
                    { onQuitar(producto.id) })
            }
        }
    }
}
