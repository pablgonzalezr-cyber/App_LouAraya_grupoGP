package com.example.applouaraya_grupogp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.applouaraya_grupogp.repository.ConsultaDolar
import com.example.applouaraya_grupogp.repository.IndicadoresRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DolarUiState(val consulta: ConsultaDolar = ConsultaDolar(), val cargando: Boolean = false)

class DolarViewModel : ViewModel() {
    private val _estado = MutableStateFlow(DolarUiState())
    val estado = _estado.asStateFlow()

    fun cargar(actualizar: Boolean = false) {
        if (_estado.value.cargando) return
        _estado.value = _estado.value.copy(cargando = true)
        viewModelScope.launch {
            _estado.value = DolarUiState(IndicadoresRepository.compartido.obtener(actualizar))
        }
    }
}
