package com.example.applouaraya_grupogp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.applouaraya_grupogp.model.Producto
import com.example.applouaraya_grupogp.ui.design.*
import java.text.NumberFormat
import java.util.Locale

/** Tarjeta fotográfica: deja intactos los callbacks de favoritos y detalle. */
@Composable
fun ProductoCard(producto: Producto, esFavorito: Boolean, permitirFavorito: Boolean,
                 onVerDetalle: () -> Unit, onFavorito: () -> Unit) {
    val precio = NumberFormat.getIntegerInstance(Locale("es", "CL"))
    Card(modifier = Modifier.fillMaxWidth(), shape = LouShape,
        onClick = onVerDetalle,
        colors = CardDefaults.cardColors(containerColor = LouColors.Superficie),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Column {
            ImagenProducto(producto, Modifier.fillMaxWidth().aspectRatio(1f))
            Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(producto.categoria.uppercase(), color = LouColors.Terracota,
                    style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text(producto.nombre, style = MaterialTheme.typography.titleMedium,
                    color = LouColors.Cafe, fontWeight = FontWeight.Bold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(producto.material, style = MaterialTheme.typography.bodySmall,
                    color = LouColors.TextoSecundario, maxLines = 1)
                Text("$${precio.format(producto.precio)} CLP", color = LouColors.TerracotaOscuro,
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                LouBadge(if (producto.disponible) "Disponible" else "A pedido", producto.disponible)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(onClick = onVerDetalle, modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 7.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LouColors.Terracota)) {
                        Text("Ver detalle", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(onClick = onFavorito, enabled = permitirFavorito,
                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 7.dp),
                        shape = RoundedCornerShape(12.dp)) {
                        Text(if (esFavorito) "♥ Guardado" else "♡ Guardar",
                            color = LouColors.Terracota, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
