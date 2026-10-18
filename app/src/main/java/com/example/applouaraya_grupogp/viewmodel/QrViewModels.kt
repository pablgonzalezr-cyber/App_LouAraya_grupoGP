package com.example.applouaraya_grupogp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.applouaraya_grupogp.model.Producto
import com.example.applouaraya_grupogp.repository.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class QrUiState(val bloqueado: Boolean = false, val procesando: Boolean = false,
    val error: String? = null, val productoId: Int? = null)

class QrViewModel(private val repo: QrRepository): ViewModel() {
    private val _estado = MutableStateFlow(QrUiState())
    val estado = _estado.asStateFlow()
    fun leer(codigo: String?) {
        if(_estado.value.bloqueado) return
        _estado.value = QrUiState(bloqueado = true, procesando = true)
        viewModelScope.launch {
            try {
                _estado.value = when(val resultado = repo.resolver(codigo)) {
                    is ResultadoQr.Encontrado -> QrUiState(bloqueado = true, productoId = resultado.productoId)
                    is ResultadoQr.Error -> QrUiState(bloqueado = true, error = resultado.mensaje)
                }
            } catch(e: CancellationException) { throw e
            } catch(_: Exception) { _estado.value = QrUiState(bloqueado = true, error = "No se pudo consultar el catálogo. Inténtalo nuevamente.") }
        }
    }
    fun reintentar() { if(!_estado.value.procesando) _estado.value = QrUiState() }
}

data class QrDemoState(val productos: List<Producto> = emptyList(), val cargando: Boolean = true, val error: String? = null)
class QrDemoViewModel(private val repo: ProductosRepository): ViewModel() {
    private val _estado = MutableStateFlow(QrDemoState())
    val estado = _estado.asStateFlow()
    init {
        viewModelScope.launch {
            try { repo.observarPublicados().collect { _estado.value = QrDemoState(it, false) } }
            catch(e: CancellationException) { throw e }
            catch(_: Exception) { _estado.value = QrDemoState(cargando = false, error = "No se pudieron cargar los productos") }
        }
    }
}
