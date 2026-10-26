package com.example.applouaraya_grupogp.ui.components

import androidx.compose.runtime.Composable
import com.example.applouaraya_grupogp.repository.ProductosDemo
import com.example.applouaraya_grupogp.ui.adaptive.TresTamanos
import com.example.applouaraya_grupogp.ui.screens.DetalleProductoScreen
import com.example.applouaraya_grupogp.ui.screens.FavoritosScreen
import com.example.applouaraya_grupogp.ui.theme.AppLouAraya_grupoGPTheme

@TresTamanos
@Composable
fun DetalleFotografiaPreview() {
    AppLouAraya_grupoGPTheme {
        DetalleProductoScreen(ProductosDemo.productos.first().copy(imagenNombre = "macrame_tapiz"),
            false, true, {}, {})
    }
}

@TresTamanos
@Composable
fun FavoritosFotografiasPreview() {
    AppLouAraya_grupoGPTheme {
        val products = ProductosDemo.productos.take(3).map { it.copy(imagenNombre = "macrame_tapiz") }
        FavoritosScreen(products, products.map { it.id }.toSet(), {}, {}, {})
    }
}
