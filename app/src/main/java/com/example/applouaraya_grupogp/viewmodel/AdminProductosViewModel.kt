package com.example.applouaraya_grupogp.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.applouaraya_grupogp.model.Producto
import com.example.applouaraya_grupogp.repository.ImagenesProducto
import com.example.applouaraya_grupogp.repository.ProductosRepository
import com.example.applouaraya_grupogp.validation.ProductoFormulario
import com.example.applouaraya_grupogp.validation.ProductoValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminProductosState(val productos: List<Producto> = emptyList(), val consulta: String = "",
    val formulario: ProductoFormulario = ProductoFormulario(), val editando: Boolean = false,
    val errores: Map<String,String> = emptyMap(), val cargando: Boolean = true,
    val guardando: Boolean = false, val error: String? = null, val mensaje: String? = null)

class AdminProductosViewModel(private val repo: ProductosRepository) : ViewModel() {
    private val _estado = MutableStateFlow(AdminProductosState())
    val estado = _estado.asStateFlow()
    private var job: Job? = null
    init { cargar() }
    fun cargar() {
        job?.cancel(); _estado.value = _estado.value.copy(cargando = true, error = null)
        job = viewModelScope.launch {
            try { repo.observarAdministracion().collect { _estado.value = _estado.value.copy(productos = it, cargando = false) } }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) { _estado.value = _estado.value.copy(cargando = false, productos = emptyList(), error = e.message) }
        }
    }
    fun buscar(valor: String) { _estado.value = _estado.value.copy(consulta = valor) }
    fun editar(p: Producto? = null) { if (!_estado.value.guardando) _estado.value = _estado.value.copy(editando = true,
        formulario = p?.let { ProductoFormulario.desde(it) } ?: ProductoFormulario(), errores = emptyMap(), error = null, mensaje = null) }
    fun cambiar(f: ProductoFormulario) { if (!_estado.value.guardando) _estado.value = _estado.value.copy(formulario = f, errores = emptyMap(), error = null) }
    fun cancelar() { if (!_estado.value.guardando) _estado.value = _estado.value.copy(editando = false, error = null) }
    private fun ejecutar(accion: suspend () -> Unit) {
        if (_estado.value.guardando) return
        _estado.value = _estado.value.copy(guardando = true, error = null, mensaje = null)
        viewModelScope.launch {
            try { accion(); _estado.value = _estado.value.copy(guardando = false) }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) { _estado.value = _estado.value.copy(guardando = false, error = e.message ?: "No se pudo completar la operación") }
        }
    }
    fun guardar() {
        val f = _estado.value.formulario
        val errores = ProductoValidator.validar(f)
        if (errores.isNotEmpty()) { _estado.value = _estado.value.copy(errores = errores); return }
        ejecutar { repo.guardar(f); _estado.value = _estado.value.copy(editando = false, mensaje = "Producto guardado") }
    }
    fun desactivar(id: Int) = ejecutar { repo.desactivar(id); _estado.value = _estado.value.copy(mensaje = "Producto desactivado; sus datos se conservan") }
    fun importar(context: Context, uri: Uri) = ejecutar {
        repo.exigirAdministrador()
        val nombre = ImagenesProducto.importar(context, uri)
        _estado.value = _estado.value.copy(formulario = _estado.value.formulario.copy(imagenArchivo = nombre, imagenNombre = ""))
    }
}
