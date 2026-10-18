package com.example.applouaraya_grupogp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.applouaraya_grupogp.repository.PerfilRepository
import com.example.applouaraya_grupogp.validation.PerfilValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PerfilUiState(
    val nombre: String = "", val correo: String = "", val rol: String = "",
    val cargando: Boolean = true, val guardando: Boolean = false, val editando: Boolean = false,
    val errorNombre: String? = null, val error: String? = null, val mensaje: String? = null
)

class PerfilViewModel(private val repo: PerfilRepository) : ViewModel() {
    private val _estado = MutableStateFlow(PerfilUiState())
    val estado = _estado.asStateFlow()
    private var observacion: Job? = null
    init { cargar() }
    fun cargar() {
        observacion?.cancel()
        _estado.value = _estado.value.copy(cargando = true, error = null)
        observacion = viewModelScope.launch {
            try { repo.observar().collect { u ->
                val actual = _estado.value
                _estado.value = actual.copy(nombre = if (actual.editando) actual.nombre else u.nombre,
                    correo = u.correo, rol = u.rol, cargando = false)
            } } catch (e: CancellationException) { throw e
            } catch (e: Exception) { _estado.value = _estado.value.copy(cargando = false, error = e.message ?: "No se pudo cargar el perfil") }
        }
    }
    fun cambiarNombre(nombre: String) {
        if (_estado.value.guardando) return
        _estado.value = _estado.value.copy(nombre = nombre, editando = true, errorNombre = null, mensaje = null)
    }
    fun guardar() {
        val actual = _estado.value
        if (actual.guardando || actual.cargando) return
        val error = PerfilValidator.errorNombre(actual.nombre)
        if (error != null) { _estado.value = actual.copy(errorNombre = error); return }
        _estado.value = actual.copy(guardando = true, error = null, mensaje = null)
        viewModelScope.launch {
            try {
                repo.guardarNombre(actual.nombre)
                _estado.value = _estado.value.copy(nombre = PerfilValidator.normalizarNombre(actual.nombre),
                    guardando = false, editando = false, mensaje = "Nombre guardado")
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { _estado.value = _estado.value.copy(guardando = false, error = e.message ?: "No se pudo guardar") }
        }
    }
}
