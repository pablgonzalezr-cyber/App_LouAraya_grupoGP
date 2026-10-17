package com.example.applouaraya_grupogp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.applouaraya_grupogp.model.CotizacionEntity
import com.example.applouaraya_grupogp.repository.AdminCotizacionesRepository
import com.example.applouaraya_grupogp.repository.EstadoCotizacion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminUiState(
    val solicitudes: List<CotizacionEntity> = emptyList(),
    val detalle: CotizacionEntity? = null,
    val filtro: EstadoCotizacion? = null,
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val error: String? = null,
    val mensaje: String? = null
)

class AdminCotizacionesViewModel(private val repo: AdminCotizacionesRepository) : ViewModel() {
    private val _estado = MutableStateFlow(AdminUiState())
    val estado = _estado.asStateFlow()
    private var trabajo: Job? = null
    fun cargar(id: Long? = null) {
        trabajo?.cancel()
        _estado.value = _estado.value.copy(cargando = true, error = null, detalle = null, mensaje = null)
        trabajo = viewModelScope.launch {
            try {
                if (id == null) repo.observar().collect { lista ->
                    _estado.value = _estado.value.copy(solicitudes = lista, cargando = false)
                } else repo.detalle(id).collect { solicitud ->
                    _estado.value = _estado.value.copy(detalle = solicitud, cargando = false)
                }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { _estado.value = _estado.value.copy(cargando = false, solicitudes = emptyList(), detalle = null, error = e.message ?: "No se pudo cargar") }
        }
    }
    fun filtrar(estado: EstadoCotizacion?) { _estado.value = _estado.value.copy(filtro = estado) }
    fun cambiar(id: Long, anterior: String, nuevo: EstadoCotizacion) {
        if (_estado.value.guardando) return
        _estado.value = _estado.value.copy(guardando = true, error = null, mensaje = null)
        viewModelScope.launch {
            try {
                repo.cambiarEstado(id, anterior, nuevo)
                _estado.value = _estado.value.copy(guardando = false, mensaje = "Estado actualizado a ${nuevo.name}")
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { _estado.value = _estado.value.copy(guardando = false, error = e.message ?: "No se pudo actualizar") }
        }
    }
}
