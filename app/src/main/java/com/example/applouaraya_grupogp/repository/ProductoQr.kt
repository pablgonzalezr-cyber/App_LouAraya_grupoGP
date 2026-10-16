package com.example.applouaraya_grupogp.repository

// Formato de QR elegido para el MVP: LOUARAYA:PRODUCTO:3
// No navega ni abre cámara; solo valida e interpreta un texto escaneado.
object ProductoQr {
    fun codigoPara(id: Int): String = "LOUARAYA:PRODUCTO:$id"

    fun productoDesde(codigo: String?): Int? {
        val partes = codigo?.trim()?.split(":") ?: return null
        if (partes.size != 3) return null
        if (partes[0] != "LOUARAYA" || partes[1] != "PRODUCTO") return null
        val id = partes[2].toIntOrNull() ?: return null
        return ProductosDemo.productos.firstOrNull { it.id == id }?.id
    }
}
