package com.example.applouaraya_grupogp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.applouaraya_grupogp.model.Producto
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ProductoCard(
    producto: Producto,
    esFavorito: Boolean,
    permitirFavorito: Boolean,
    onVerDetalle: () -> Unit,
    onFavorito: () -> Unit
) {
    val precio = NumberFormat.getIntegerInstance(Locale("es", "CL"))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBF5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            ImagenProducto(producto, Modifier.fillMaxWidth().height(180.dp))
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(producto.nombre, style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF51362B), fontWeight = FontWeight.SemiBold)
                Text("${producto.categoria} · ${producto.material}",
                    style = MaterialTheme.typography.bodySmall)
                Text("$${precio.format(producto.precio)} CLP",
                    color = Color(0xFFA65E43), fontWeight = FontWeight.Bold)
                Text(if (producto.disponible) "Disponible" else "A pedido",
                    color = if (producto.disponible) Color(0xFF38684E) else Color(0xFF985B42))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onVerDetalle,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA65E43))
                    ) { Text("Ver detalle") }
                    OutlinedButton(onClick = onFavorito, enabled = permitirFavorito) {
                        Text(if (esFavorito) "♥ Guardado" else "♡ Guardar")
                    }
                }
            }
        }
    }
}


