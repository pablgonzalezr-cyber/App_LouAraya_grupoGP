package com.example.applouaraya_grupogp.validation

object PerfilValidator {
    fun normalizarNombre(nombre: String) = nombre.trim().replace(Regex("\\s+"), " ")
    fun errorNombre(nombre: String): String? {
        if (nombre.any { it.isISOControl() }) return "El nombre no admite caracteres de control"
        val normalizado = normalizarNombre(nombre)
        return if (normalizado.length !in 2..80) "Ingresa un nombre de entre 2 y 80 caracteres" else null
    }
}
