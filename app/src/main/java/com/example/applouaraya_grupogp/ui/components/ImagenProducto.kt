package com.example.applouaraya_grupogp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import com.example.applouaraya_grupogp.repository.ImagenesProducto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    val imagenLocal by produceState<ImageBitmap?>(null, producto.imagenArchivo) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val archivo = ImagenesProducto.archivo(context, producto.imagenArchivo) ?: return@runCatching null
                val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(archivo.path, opciones)
                var muestra = 1
                while(opciones.outWidth / muestra > 1024 || opciones.outHeight / muestra > 1024) muestra *= 2
                BitmapFactory.decodeFile(archivo.path, BitmapFactory.Options().apply { inSampleSize = muestra })?.asImageBitmap()
            }.getOrNull()
        }
    }
    // La foto es opcional: nunca impide compilar si todavía no está en drawable.
    @Suppress("DiscouragedApi")
    val idImagen = if (producto.imagenNombre.isNotBlank()) {
        context.resources.getIdentifier(producto.imagenNombre, "drawable", context.packageName)
    } else 0

    if (imagenLocal != null) {
        Image(bitmap = requireNotNull(imagenLocal), contentDescription = "Fotografía de ${producto.nombre}", modifier = modifier, contentScale = ContentScale.Crop)
    } else if (idImagen != 0) {
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
