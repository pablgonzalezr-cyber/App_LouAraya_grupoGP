package com.example.applouaraya_grupogp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.applouaraya_grupogp.repository.PreferenciasLocales
import com.example.applouaraya_grupogp.repository.PreferenciasRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ConfiguracionUiState(val preferencias: PreferenciasLocales = PreferenciasLocales(), val guardando: Boolean = false, val error: String? = null)

class ConfiguracionViewModel(private val repo: PreferenciasRepository, private val sesion: StateFlow<SesionUiState>) : ViewModel() {
    private val _estado = MutableStateFlow(ConfiguracionUiState())
    val estado = _estado.asStateFlow()
    init {
        viewModelScope.launch {
            sesion.map { it.usuarioId }.distinctUntilChanged().collectLatest { id ->
                _estado.value = ConfiguracionUiState()
                if (id != null) repo.observar(id).collect { preferencias -> _estado.value = _estado.value.copy(preferencias = preferencias) }
            }
        }
    }
    fun guardar(preferencias: PreferenciasLocales) {
        val id = sesion.value.usuarioId ?: return
        if (_estado.value.guardando) return
        _estado.value = _estado.value.copy(guardando = true, error = null)
        viewModelScope.launch {
            try {
                repo.guardar(id, preferencias)
                if (sesion.value.usuarioId == id) _estado.value = _estado.value.copy(preferencias = preferencias, guardando = false)
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { if (sesion.value.usuarioId == id) _estado.value = _estado.value.copy(guardando = false, error = e.message ?: "No se pudo guardar") }
        }
    }
}
