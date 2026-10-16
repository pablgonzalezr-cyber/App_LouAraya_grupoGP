package com.example.applouaraya_grupogp.model

// Campos originales conservados; los nuevos tienen valores por defecto
// para no romper otras pantallas que ya usen Producto(...).
data class Producto(
    val id: Int,
    val nombre: String,
    val categoria: String,
    val material: String,
    val precio: Int,
    val descripcion: String,
    val anchoCm: Int = 0,
    val largoCm: Int = 0,
    val disponible: Boolean = true,
    val imagenNombre: String = ""
)
