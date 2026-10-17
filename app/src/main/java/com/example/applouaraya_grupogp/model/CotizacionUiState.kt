package com.example.applouaraya_grupogp.model

data class CotizacionFormulario(
    val descripcion: String = "",
    val categoria: String = "",
    val material: String = "",
    val ancho: String = "",
    val largo: String = "",
    val cantidad: String = "1",
    val fecha: String = "",
    val productoId: Int? = null
)

data class CotizacionUiState(
    val productosReferencia: List<Producto> = emptyList(),
    val formulario: CotizacionFormulario = CotizacionFormulario(),
    val errores: Map<String, String> = emptyMap(),
    val guardando: Boolean = false,
    val error: String? = null,
    val guardadaId: Long? = null
)
