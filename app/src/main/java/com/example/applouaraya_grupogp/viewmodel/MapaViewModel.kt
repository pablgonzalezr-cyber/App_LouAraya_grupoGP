package com.example.applouaraya_grupogp.viewmodel

import androidx.lifecycle.ViewModel
import com.example.applouaraya_grupogp.model.PuntoDemostracion
import com.example.applouaraya_grupogp.repository.PuntosDemostracion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MapaUiState(val puntos: List<PuntoDemostracion> = PuntosDemostracion.puntos,
    val seleccionado: String? = null, val cargando: Boolean = true, val error: String? = null,
    val intento: Int = 0, val zoom: Double = 12.0)

class MapaViewModel: ViewModel() {
    private val _estado = MutableStateFlow(MapaUiState())
    val estado = _estado.asStateFlow()
    fun seleccionar(id: String) { if(_estado.value.puntos.any { it.id == id }) _estado.value = _estado.value.copy(seleccionado = id) }
    fun listo() { _estado.value = _estado.value.copy(cargando = false, error = null) }
    fun error(mensaje: String) { _estado.value = _estado.value.copy(cargando = false, error = mensaje) }
    fun reintentar() { _estado.value = _estado.value.copy(cargando = true, error = null, intento = _estado.value.intento + 1) }
    fun zoom(valor: Double) { _estado.value = _estado.value.copy(zoom = valor) }
}
