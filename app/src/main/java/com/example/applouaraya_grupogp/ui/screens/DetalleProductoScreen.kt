package com.example.applouaraya_grupogp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.applouaraya_grupogp.model.Producto
import com.example.applouaraya_grupogp.ui.adaptive.AdaptiveSurface
import com.example.applouaraya_grupogp.ui.components.ImagenProducto
import com.example.applouaraya_grupogp.ui.design.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DetalleProductoScreen(producto: Producto, esFavorito: Boolean, permitirFavorito: Boolean,
                          onVolver: () -> Unit, onFavorito: () -> Unit) {
    val precio = NumberFormat.getIntegerInstance(Locale("es", "CL"))
    AdaptiveSurface {
        Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())
            .padding(18.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
            LouBackButton(onVolver, "← Volver al catálogo")
            ImagenProducto(producto, Modifier.align(Alignment.CenterHorizontally)
                .widthIn(max = 600.dp).fillMaxWidth().aspectRatio(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LouEyebrow(producto.categoria)
                LouBadge(if (producto.disponible) "Disponible" else "Fabricación a pedido", producto.disponible)
            }
            LouPageHeading(producto.nombre, "Diseño artesanal Lou Araya")
            Text("$${precio.format(producto.precio)} CLP", style = MaterialTheme.typography.headlineSmall,
                color = LouColors.Terracota, fontWeight = FontWeight.Bold)
            LouSectionTitle("Detalles de la pieza")
            LouPanel(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Material", color = LouColors.TextoSecundario)
                    Text(producto.material, color = LouColors.Cafe, fontWeight = FontWeight.SemiBold)
                }
                HorizontalDivider(color = LouColors.Arena)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Dimensiones", color = LouColors.TextoSecundario)
                    Text("${producto.anchoCm} × ${producto.largoCm} cm", color = LouColors.Cafe)
                }
                HorizontalDivider(color = LouColors.Arena)
                Text(producto.descripcion, color = LouColors.Cafe)
            }
            LouPrimaryButton(if (esFavorito) "Quitar de favoritos" else "Agregar a favoritos",
                onFavorito, Modifier.fillMaxWidth(), enabled = permitirFavorito)
            Text("Los precios son referenciales. Puedes solicitar un diseño personalizado en Cotizaciones.",
                color = LouColors.TextoSecundario, style = MaterialTheme.typography.bodySmall)
        }
    }
}
