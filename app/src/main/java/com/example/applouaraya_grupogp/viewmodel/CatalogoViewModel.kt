package com.example.applouaraya_grupogp.viewmodel

import androidx.lifecycle.ViewModel
import com.example.applouaraya_grupogp.model.Producto
import com.example.applouaraya_grupogp.repository.CatalogoFiltros
import com.example.applouaraya_grupogp.repository.ProductosDemo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CatalogoUiState(
    val consulta: String = "",
    val categoria: String = "Todos",
    val soloDisponibles: Boolean = false,
    val ordenarPorPrecio: Boolean = false,
    val productos: List<Producto> = ProductosDemo.productos
)

class CatalogoViewModel : ViewModel() {
    private val _estado = MutableStateFlow(CatalogoUiState())
    val estado = _estado.asStateFlow()

    fun cambiarConsulta(valor: String) = actualizar { it.copy(consulta = valor) }
    fun cambiarCategoria(valor: String) = actualizar { it.copy(categoria = valor) }
    fun cambiarDisponibles(valor: Boolean) = actualizar { it.copy(soloDisponibles = valor) }
    fun cambiarOrdenPrecio(valor: Boolean) = actualizar { it.copy(ordenarPorPrecio = valor) }

    private fun actualizar(transformar: (CatalogoUiState) -> CatalogoUiState) {
        val nuevo = transformar(_estado.value)
        _estado.value = nuevo.copy(
            productos = CatalogoFiltros.aplicar(
                ProductosDemo.productos,
                nuevo.consulta,
                nuevo.categoria,
                nuevo.soloDisponibles,
                nuevo.ordenarPorPrecio
            )
        )
    }
}
