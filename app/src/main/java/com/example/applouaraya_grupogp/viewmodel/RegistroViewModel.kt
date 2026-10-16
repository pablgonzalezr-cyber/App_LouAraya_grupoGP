
package com.example.applouaraya_grupogp.viewmodel

import androidx.lifecycle.ViewModel
import com.example.applouaraya_grupogp.model.RegistroUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class RegistroViewModel : ViewModel() {

    private val _estado = MutableStateFlow(RegistroUiState())
    val estado = _estado.asStateFlow()

    fun cambiarNombre(valor: String) {
        _estado.value = _estado.value.copy(
            nombre = valor,
            errorNombre = null
        )
    }

    fun cambiarCorreo(valor: String) {
        _estado.value = _estado.value.copy(
            correo = valor,
            errorCorreo = null
        )
    }

    fun cambiarClave(valor: String) {
        _estado.value = _estado.value.copy(
            clave = valor,
            errorClave = null
        )
    }

    fun cambiarTerminos(valor: Boolean) {
        _estado.value = _estado.value.copy(
            aceptaTerminos = valor,
            errorTerminos = null
        )
    }

    fun validarFormulario(): Boolean {
        val datos = _estado.value

        val errorNombre = if (datos.nombre.isBlank()) {
            "Ingresa tu nombre"
        } else null

        val errorCorreo = if (
            !android.util.Patterns.EMAIL_ADDRESS
                .matcher(datos.correo).matches()
        ) {
            "Ingresa un correo válido"
        } else null

        val errorClave = if (datos.clave.length < 6) {
            "La contraseña debe tener al menos 6 caracteres"
        } else null

        val errorTerminos = if (!datos.aceptaTerminos) {
            "Debes aceptar los términos"
        } else null

        _estado.value = datos.copy(
            errorNombre = errorNombre,
            errorCorreo = errorCorreo,
            errorClave = errorClave,
            errorTerminos = errorTerminos
        )

        return errorNombre == null &&
                errorCorreo == null &&
                errorClave == null &&
                errorTerminos == null
    }

    fun limpiarFormulario() {
        _estado.value = RegistroUiState()
    }

}

