package com.example.applouaraya_grupogp.repository

import com.example.applouaraya_grupogp.data.ProductoDao

sealed interface ResultadoQr {
    data class Encontrado(val productoId: Int): ResultadoQr
    data class Error(val mensaje: String): ResultadoQr
}

class QrRepository(private val productos: ProductoDao) {
    suspend fun resolver(codigo: String?): ResultadoQr {
        val id = ProductoQr.productoDesde(codigo) ?: return ResultadoQr.Error("QR inválido. Usa un código de producto de Lou Araya.")
        val producto = productos.buscar(id) ?: return ResultadoQr.Error("Este producto no existe en el catálogo local de este teléfono.")
        if (!producto.activo) return ResultadoQr.Error("Este producto está desactivado y no se puede mostrar.")
        return ResultadoQr.Encontrado(id)
    }
}
