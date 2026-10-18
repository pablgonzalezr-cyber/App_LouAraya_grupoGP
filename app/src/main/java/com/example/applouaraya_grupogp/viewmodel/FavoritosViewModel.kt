package com.example.applouaraya_grupogp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.applouaraya_grupogp.data.FavoritosDatabase
import com.example.applouaraya_grupogp.repository.FavoritosRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FavoritosViewModel @JvmOverloads constructor(application: Application,
    private val repositorio: FavoritosRepository = FavoritosRepository(FavoritosDatabase.obtener(application).favoritoDao())) : AndroidViewModel(application) {
    private val _ids = MutableStateFlow<Set<Int>>(emptySet())
    val ids = _ids.asStateFlow()
    private var usuarioActual: Long? = null
    private var observarTrabajo: Job? = null

    fun seleccionarUsuario(usuarioId: Long?) {
        if (usuarioActual == usuarioId) return
        usuarioActual = usuarioId
        observarTrabajo?.cancel()
        _ids.value = emptySet()
        if (usuarioId != null && usuarioId > 0) {
            observarTrabajo = viewModelScope.launch {
                repositorio.observar(usuarioId).collect { lista ->
                    _ids.value = lista.toSet()
                }
            }
        }
    }

    fun alternar(usuarioId: Long?, productoId: Int) {
        if (usuarioId == null || usuarioId <= 0) return
        viewModelScope.launch { repositorio.alternar(usuarioId, productoId) }
    }
}
