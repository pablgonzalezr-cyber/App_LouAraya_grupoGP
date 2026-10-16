package com.example.applouaraya_grupogp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.applouaraya_grupogp.model.Producto
import com.example.applouaraya_grupogp.ui.components.ProductoCard

@Composable
fun FavoritosScreen(
    productos: List<Producto>,
    idsFavoritos: Set<Int>,
    onVolver: () -> Unit,
    onAbrir: (Int) -> Unit,
    onQuitar: (Int) -> Unit
) {
    val favoritos = productos.filter { it.id in idsFavoritos }
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF7F1E8))
            .padding(16.dp)
    ) {
        TextButton(onClick = onVolver) { Text("← Volver al catálogo") }
        Text("Mis favoritos", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        if (favoritos.isEmpty()) {
            Text("Todavía no guardaste diseños. Vuelve al catálogo y agrega uno.")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(favoritos, key = { it.id }) { producto ->
                    ProductoCard(
                        producto = producto,
                        esFavorito = true,
                        permitirFavorito = true,
                        onVerDetalle = { onAbrir(producto.id) },
                        onFavorito = { onQuitar(producto.id) }
                    )
                }
            }
        }
    }
}
