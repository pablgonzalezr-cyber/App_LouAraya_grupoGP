package com.example.applouaraya_grupogp.model

data class PuntoDemostracion(val id: String, val nombre: String, val descripcion: String,
    val direccion: String, val latitud: Double, val longitud: Double) {
    init { require(latitud.isFinite() && latitud in -90.0..90.0); require(longitud.isFinite() && longitud in -180.0..180.0) }
}
