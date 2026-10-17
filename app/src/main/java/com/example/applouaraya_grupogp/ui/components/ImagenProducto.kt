package com.example.applouaraya_grupogp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.applouaraya_grupogp.model.Producto

@Composable
fun ImagenProducto(producto: Producto, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // La foto es opcional: nunca impide compilar si todavía no está en drawable.
    @Suppress("DiscouragedApi")
    val idImagen = if (producto.imagenNombre.isNotBlank()) {
        context.resources.getIdentifier(producto.imagenNombre, "drawable", context.packageName)
    } else 0

    if (idImagen != 0) {
        Image(
            painter = painterResource(idImagen),
            contentDescription = "Fotografía de ${producto.nombre}",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier.background(Color(0xFFE4D3C0)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🧶", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Fotografía pendiente",
                    color = Color(0xFF60483B),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
