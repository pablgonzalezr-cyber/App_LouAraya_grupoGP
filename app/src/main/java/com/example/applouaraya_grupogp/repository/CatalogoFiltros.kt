package com.example.applouaraya_grupogp.repository

import com.example.applouaraya_grupogp.model.Producto
import java.util.Locale

// Función pura: fácil de probar con JUnit sin teléfono.
object CatalogoFiltros {
    fun aplicar(
        productos: List<Producto>,
        consulta: String,
        categoria: String,
        soloDisponibles: Boolean,
        ordenarPorPrecio: Boolean
    ): List<Producto> {
        val texto = consulta.trim().lowercase(Locale.ROOT)
        val filtrados = productos.filter { producto ->
            val coincideTexto = texto.isEmpty() ||
                    producto.nombre.lowercase(Locale.ROOT).contains(texto) ||
                    producto.material.lowercase(Locale.ROOT).contains(texto) ||
                    producto.descripcion.lowercase(Locale.ROOT).contains(texto)
            val coincideCategoria = categoria == "Todos" ||
                    producto.categoria == categoria
            val coincideDisponibilidad = !soloDisponibles || producto.disponible
            coincideTexto && coincideCategoria && coincideDisponibilidad
        }
        return if (ordenarPorPrecio) filtrados.sortedBy { it.precio } else filtrados
    }
}


