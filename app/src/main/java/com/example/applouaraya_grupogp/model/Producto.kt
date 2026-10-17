package com.example.applouaraya_grupogp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// Campos originales conservados; los nuevos tienen valores por defecto
// para no romper otras pantallas que ya usen Producto(...).
@Entity(tableName = "productos")
data class Producto(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val categoria: String,
    val material: String,
    val precio: Int,
    val descripcion: String,
    val anchoCm: Int = 0,
    val largoCm: Int = 0,
    val disponible: Boolean = true,
    val imagenNombre: String = "",
    val activo: Boolean = true,
    val imagenArchivo: String = ""
)
