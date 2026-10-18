
package com.example.applouaraya_grupogp.viewmodel

import androidx.lifecycle.ViewModel
import com.example.applouaraya_grupogp.model.RegistroUiState
import com.example.applouaraya_grupogp.validation.RegistroValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class RegistroViewModel : ViewModel() {

    private val _estado = MutableStateFlow(RegistroUiState())
    val estado = _estado.asStateFlow()

    fun cambiarNombre(valor: String) {
        _estado.value = _estado.value.copy(
            nombre = valor,
            errorNombre = RegistroValidator.nombre(valor), resumenDisponible = false
        )
    }

    fun cambiarCorreo(valor: String) {
        _estado.value = _estado.value.copy(
            correo = valor,
            errorCorreo = RegistroValidator.correo(valor), resumenDisponible = false
        )
    }

    fun cambiarClave(valor: String) {
        _estado.value = _estado.value.copy(
            clave = valor,
            errorClave = RegistroValidator.clave(valor), resumenDisponible = false
        )
    }

    fun cambiarTerminos(valor: Boolean) {
        _estado.value = _estado.value.copy(
            aceptaTerminos = valor,
            errorTerminos = RegistroValidator.terminos(valor), resumenDisponible = false
        )
    }

    fun validarFormulario(): Boolean {
        val datos = _estado.value

        val errorNombre = RegistroValidator.nombre(datos.nombre)
        val errorCorreo = RegistroValidator.correo(datos.correo)
        val errorClave = RegistroValidator.clave(datos.clave)
        val errorTerminos = RegistroValidator.terminos(datos.aceptaTerminos)
        val valido = errorNombre == null && errorCorreo == null && errorClave == null && errorTerminos == null

        _estado.value = datos.copy(
            errorNombre = errorNombre,
            errorCorreo = errorCorreo,
            errorClave = errorClave,
            errorTerminos = errorTerminos,
            resumenDisponible = valido
        )

        return valido
    }

    fun volverAEditar() { _estado.value = _estado.value.copy(resumenDisponible = false) }

    fun limpiarFormulario() {
        _estado.value = RegistroUiState()
    }

}

