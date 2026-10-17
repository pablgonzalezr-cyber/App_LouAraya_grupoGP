package com.example.applouaraya_grupogp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.applouaraya_grupogp.data.AppDatabase
import com.example.applouaraya_grupogp.model.CotizacionFormulario
import com.example.applouaraya_grupogp.model.CotizacionUiState
import com.example.applouaraya_grupogp.repository.CotizacionRepository
import com.example.applouaraya_grupogp.validation.CotizacionValidator
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CotizacionViewModel(application: Application, private val saved: SavedStateHandle) : AndroidViewModel(application) {
    private val db = AppDatabase.obtener(application)
    private val repo = CotizacionRepository(db.cotizacionDao(), db.productoDao())
    private val token = saved.get<String>("tokenEnvio") ?: UUID.randomUUID().toString().also { saved["tokenEnvio"] = it }
    private val _estado = MutableStateFlow(CotizacionUiState(formulario = CotizacionFormulario(
        descripcion = saved["descripcion"] ?: "", categoria = saved["categoria"] ?: "",
        material = saved["material"] ?: "", ancho = saved["ancho"] ?: "", largo = saved["largo"] ?: "",
        cantidad = saved["cantidad"] ?: "1", fecha = saved["fecha"] ?: "", productoId = saved["productoId"]
    )))
    val estado = _estado.asStateFlow()
    init {
        viewModelScope.launch {
            try { repo.observarProductos().collect { _estado.value = _estado.value.copy(productosReferencia = it) } }
            catch(e: CancellationException) { throw e }
            catch(_: Exception) { _estado.value = _estado.value.copy(error = "No se pudieron cargar los productos de referencia") }
        }
    }

    fun cambiar(f: CotizacionFormulario) {
        if (_estado.value.guardando || _estado.value.guardadaId != null) return
        saved["descripcion"] = f.descripcion; saved["categoria"] = f.categoria
        saved["material"] = f.material; saved["ancho"] = f.ancho; saved["largo"] = f.largo
        saved["cantidad"] = f.cantidad; saved["fecha"] = f.fecha; saved["productoId"] = f.productoId
        _estado.value = _estado.value.copy(formulario = f, errores = emptyMap(), error = null)
    }

    fun guardar(usuarioId: Long) {
        val actual = _estado.value
        if (actual.guardando || actual.guardadaId != null) return
        val errores = CotizacionValidator.validar(actual.formulario, actual.productosReferencia.map { it.id }.toSet())
        if (errores.isNotEmpty()) { _estado.value = actual.copy(errores = errores); return }
        _estado.value = actual.copy(guardando = true, error = null)
        viewModelScope.launch {
            try {
                val id = repo.guardar(usuarioId, token, actual.formulario)
                _estado.value = _estado.value.copy(guardando = false, guardadaId = id)
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                _estado.value = _estado.value.copy(guardando = false, error = "No se pudo guardar. Revisa los datos e inténtalo nuevamente.")
            }
        }
    }
}
