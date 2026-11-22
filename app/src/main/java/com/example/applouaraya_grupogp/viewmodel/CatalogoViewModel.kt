package com.example.applouaraya_grupogp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.applouaraya_grupogp.model.Producto
import com.example.applouaraya_grupogp.repository.CatalogoFiltros
import com.example.applouaraya_grupogp.repository.ProductosRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CatalogoUiState(
    val consulta: String = "",
    val categoria: String = "Todos",
    val soloDisponibles: Boolean = false,
    val ordenarPorPrecio: Boolean = false,
    val productos: List<Producto> = emptyList(),
    val todos: List<Producto> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null
)

data class DetalleCatalogoState(val id: Int? = null, val producto: Producto? = null,
                               val error: String? = null)

class CatalogoViewModel(private val repositorio: ProductosRepository) : ViewModel() {
    private val _estado = MutableStateFlow(CatalogoUiState())
    val estado = _estado.asStateFlow()
    private var trabajo: Job? = null
    private val _detalle = MutableStateFlow(DetalleCatalogoState())
    val detalle = _detalle.asStateFlow()
    private var trabajoDetalle: Job? = null
    init { cargar() }
    fun cargar() {
        trabajo?.cancel()
        _estado.value = _estado.value.copy(cargando = true, error = null)
        trabajo = viewModelScope.launch {
            try { repositorio.observarPublicados().collect { lista ->
                actualizar { it.copy(todos = lista, cargando = false) }
            } } catch (e: CancellationException) { throw e
            } catch (e: Exception) { _estado.value = _estado.value.copy(cargando = false, error = e.message ?: "No se pudo cargar el catálogo") }
        }
    }
    fun cargarDetalle(id: Int?) {
        trabajoDetalle?.cancel()
        _detalle.value = DetalleCatalogoState(id = id)
        if (id == null) return
        trabajoDetalle = viewModelScope.launch {
            try {
                repositorio.observarDetalle(id).collect { _detalle.value = DetalleCatalogoState(id, it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _detalle.value = DetalleCatalogoState(id, error = e.message ?: "No se pudo consultar el producto.")
            }
        }
    }
    fun cambiarConsulta(valor: String) = actualizar { it.copy(consulta = valor) }
    fun cambiarCategoria(valor: String) = actualizar { it.copy(categoria = valor) }
    fun cambiarDisponibles(valor: Boolean) = actualizar { it.copy(soloDisponibles = valor) }
    fun cambiarOrdenPrecio(valor: Boolean) = actualizar { it.copy(ordenarPorPrecio = valor) }

    private fun actualizar(transformar: (CatalogoUiState) -> CatalogoUiState) {
        val nuevo = transformar(_estado.value)
        _estado.value = nuevo.copy(productos = CatalogoFiltros.aplicar(
            nuevo.todos, nuevo.consulta, nuevo.categoria, nuevo.soloDisponibles, nuevo.ordenarPorPrecio
        ))
    }
}
