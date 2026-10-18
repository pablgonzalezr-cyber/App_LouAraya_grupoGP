package com.example.applouaraya_grupogp.validation

import androidx.core.util.PatternsCompat

object RegistroValidator {
    fun nombre(valor: String): String? = if(valor.isBlank()) "Ingresa tu nombre" else null
    fun correo(valor: String): String? =
        if(PatternsCompat.EMAIL_ADDRESS.matcher(valor.trim()).matches()) null else "Ingresa un correo válido"
    fun clave(valor: String): String? =
        if(valor.length < 6) "La contraseña debe tener al menos 6 caracteres" else null
    fun terminos(aceptados: Boolean): String? = if(aceptados) null else "Debes aceptar los términos"
}
