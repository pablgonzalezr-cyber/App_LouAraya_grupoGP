package com.example.applouaraya_grupogp.model

data class RegistroUiState(
    val nombre: String = "",
    val correo: String = "",
    val clave: String = "",
    val aceptaTerminos: Boolean = false,
    val errorNombre: String? = null,
    val errorCorreo: String? = null,
    val errorClave: String? = null,
    val errorTerminos: String? = null
)

