package com.example.applouaraya_grupogp.repository

// Formato de QR elegido para el MVP: LOUARAYA:PRODUCTO:3
// Solo interpreta el formato. La existencia/publicación se consulta en Room.
object ProductoQr {
    fun codigoPara(id: Int): String { require(id > 0); return "LOUARAYA:PRODUCTO:$id" }

    fun productoDesde(codigo: String?): Int? {
        val texto = codigo?.trim() ?: return null
        val valor = Regex("LOUARAYA:PRODUCTO:([1-9][0-9]{0,9})").matchEntire(texto)?.groupValues?.get(1) ?: return null
        return valor.toIntOrNull()?.takeIf { it > 0 }
    }
}
