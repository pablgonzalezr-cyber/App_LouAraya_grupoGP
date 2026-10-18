package com.example.applouaraya_grupogp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.applouaraya_grupogp.model.AvisoEntity
import com.example.applouaraya_grupogp.repository.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AvisosUiState(val usuarioId: Long? = null, val avisos: List<AvisoEntity> = emptyList(),
    val preferencias: PreferenciasLocales = PreferenciasLocales(), val cargando: Boolean = true,
    val error: String? = null, val ocupado: Boolean = false) {
    val noLeidas get() = avisos.count { !it.leida }
}

class AvisosViewModel(private val repo: AvisosRepository, private val preferencias: PreferenciasRepository,
    private val sesion: StateFlow<SesionUiState>): ViewModel() {
    private val _estado = MutableStateFlow(AvisosUiState())
    val estado = _estado.asStateFlow()
    init {
        viewModelScope.launch {
            sesion.map { it.usuarioId to it.rol }.distinctUntilChanged().collectLatest { (id, _) ->
                _estado.value = AvisosUiState(usuarioId = id, cargando = id != null)
                if(id != null) try {
                    combine(repo.observar(), preferencias.observar(id)) { lista, prefs -> lista to prefs }.collect { (lista, prefs) ->
                        if(sesion.value.usuarioId == id) _estado.value = _estado.value.copy(avisos = lista, preferencias = prefs, cargando = false)
                    }
                } catch(e: CancellationException) { throw e
                } catch(_: Exception) { if(sesion.value.usuarioId == id) _estado.value = AvisosUiState(id, cargando = false, error = "No se pudieron cargar tus avisos") }
            }
        }
    }
    fun leida(id: Long) = operar { repo.marcarLeida(id) }
    fun abrir(id: Long, navegar: (String) -> Unit) {
        val usuario = sesion.value.usuarioId
        operar {
            val ruta = repo.destino(id)
            if(sesion.value.usuarioId == usuario) navegar(ruta)
        }
    }
    private fun operar(accion: suspend () -> Unit) {
        val id = sesion.value.usuarioId ?: return
        if(_estado.value.ocupado || _estado.value.usuarioId != id) return
        _estado.value = _estado.value.copy(ocupado = true, error = null)
        viewModelScope.launch {
            try { accion()
            } catch(e: CancellationException) { throw e
            } catch(e: Exception) { if(sesion.value.usuarioId == id) _estado.value = _estado.value.copy(error = e.message ?: "No se pudo completar la acción")
            } finally { if(sesion.value.usuarioId == id) _estado.value = _estado.value.copy(ocupado = false) }
        }
    }
}
