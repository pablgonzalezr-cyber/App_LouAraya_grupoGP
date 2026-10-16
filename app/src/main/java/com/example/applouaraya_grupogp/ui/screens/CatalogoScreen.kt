package com.example.applouaraya_grupogp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.applouaraya_grupogp.model.Producto
import com.example.applouaraya_grupogp.repository.ProductosDemo
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CatalogoScreen(onVolver: () -> Unit) {

    var categoria by rememberSaveable {
        mutableStateOf("Todos")
    }

    var seleccionado by remember {
        mutableStateOf<Producto?>(null)
    }

    val categorias = listOf(
        "Todos", "Interiores", "Matrimonios", "Festivales"
    )

    val productos = ProductosDemo.productos.filter {
        categoria == "Todos" || it.categoria == categoria
    }

    val formato = NumberFormat.getIntegerInstance(
        Locale("es", "CL")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F1E8))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TextButton(onClick = onVolver) {
                Text("← Volver al inicio")
            }

            Text(
                "Catálogo artesanal",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFF51362B)
            )

            Spacer(Modifier.height(12.dp))

            Text("Explora nuestros diseños de macramé.")

            categorias.forEach { opcion ->
                FilterChip(
                    selected = categoria == opcion,
                    onClick = { categoria = opcion },
                    label = { Text(opcion) }
                )
            }
        }

        items(productos, key = { it.id }) { producto ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE9D8C6)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        producto.nombre,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text("Categoría: ${producto.categoria}")
                    Text("Material: ${producto.material}")

                    Text(
                        "$${formato.format(producto.precio)}",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Button(
                        onClick = { seleccionado = producto }
                    ) {
                        Text("Ver detalles")
                    }
                }
            }
        }
    }

    seleccionado?.let { producto ->
        AlertDialog(
            onDismissRequest = { seleccionado = null },
            title = { Text(producto.nombre) },
            text = {
                Text(
                    "${producto.descripcion}\n\n" +
                            "Material: ${producto.material}\n" +
                            "Precio referencial: " +
                            "$${formato.format(producto.precio)}"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { seleccionado = null }
                ) {
                    Text("Cerrar")
                }
            }
        )
    }
}

