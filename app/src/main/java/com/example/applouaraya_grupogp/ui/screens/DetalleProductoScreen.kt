package com.example.applouaraya_grupogp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.applouaraya_grupogp.model.Producto
import com.example.applouaraya_grupogp.ui.components.ImagenProducto
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DetalleProductoScreen(
    producto: Producto,
    esFavorito: Boolean,
    permitirFavorito: Boolean,
    onVolver: () -> Unit,
    onFavorito: () -> Unit
) {
    val precio = NumberFormat.getIntegerInstance(Locale("es", "CL"))
    Column(
        modifier = Modifier.fillMaxSize()
            .background(Color(0xFFF7F1E8))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onVolver) { Text("← Volver al catálogo") }
        ImagenProducto(producto, Modifier.fillMaxWidth().height(250.dp))
        Text(producto.nombre, style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFF51362B))
        Text("Precio referencial: $${precio.format(producto.precio)} CLP",
            style = MaterialTheme.typography.titleMedium)
        Text("Categoría: ${producto.categoria}")
        Text("Material: ${producto.material}")
        Text("Medidas: ${producto.anchoCm} cm ancho x ${producto.largoCm} cm largo")
        Text(if (producto.disponible) "Disponible" else "Fabricación a pedido")
        HorizontalDivider()
        Text(producto.descripcion)
        Button(
            onClick = onFavorito,
            enabled = permitirFavorito,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA65E43))
        ) { Text(if (esFavorito) "Quitar de favoritos" else "Agregar a favoritos") }
        Text("La cotización se conectará al módulo comercial del otro integrante.",
            style = MaterialTheme.typography.bodySmall)
    }
}
