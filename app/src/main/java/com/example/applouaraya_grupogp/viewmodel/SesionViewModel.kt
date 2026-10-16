package com.example.applouaraya_grupogp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.applouaraya_grupogp.data.AppDatabase
import com.example.applouaraya_grupogp.model.UsuarioEntity
import com.example.applouaraya_grupogp.repository.UsuarioRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SesionUiState(
    val usuarioId: Long? = null,
    val nombre: String = "",
    val correo: String = "",
    val cargando: Boolean = false,
    val error: String? = null
)

class SesionViewModel(application: Application) :
    AndroidViewModel(application) {

    private val repositorio = UsuarioRepository(
        AppDatabase.obtener(application).usuarioDao()
    )

    private val _estado = MutableStateFlow(SesionUiState())
    val estado = _estado.asStateFlow()

    fun registrar(
        nombre: String,
        correo: String,
        clave: String
    ) {
        procesar {
            repositorio.registrar(nombre, correo, clave)
        }
    }

    fun iniciarSesion(correo: String, clave: String) {
        if (correo.isBlank() || clave.isBlank()) {
            _estado.value = _estado.value.copy(
                error = "Completa todos los campos"
            )
            return
        }

        procesar {
            repositorio.iniciarSesion(correo, clave)
                ?: throw IllegalArgumentException(
                    "Correo o contraseña incorrectos"
                )
        }
    }

    private fun procesar(
        operacion: suspend () -> UsuarioEntity
    ) {
        if (_estado.value.cargando) return

        viewModelScope.launch {
            _estado.value = _estado.value.copy(
                cargando = true,
                error = null
            )

            try {
                val usuario = operacion()

                _estado.value = SesionUiState(
                    usuarioId = usuario.id,
                    nombre = usuario.nombre,
                    correo = usuario.correo
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.value = _estado.value.copy(
                    cargando = false,
                    error = if (e is IllegalArgumentException) {
                        e.message
                    } else {
                        "No se pudo completar la operación"
                    }
                )
            }
        }
    }

    fun limpiarError() {
        _estado.value = _estado.value.copy(error = null)
    }

    fun cerrarSesion() {
        _estado.value = SesionUiState()
    }
}

