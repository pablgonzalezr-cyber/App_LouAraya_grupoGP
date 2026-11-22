package com.example.applouaraya_grupogp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.applouaraya_grupogp.data.AppDatabase
import com.example.applouaraya_grupogp.model.CotizacionFormulario
import com.example.applouaraya_grupogp.model.CotizacionUiState
import com.example.applouaraya_grupogp.repository.CotizacionRepository
import com.example.applouaraya_grupogp.repository.CotizacionesRemotasRepository
import com.example.applouaraya_grupogp.repository.NuevaCotizacion
import java.math.BigDecimal
import com.example.applouaraya_grupogp.validation.CotizacionValidator
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CotizacionViewModel(application: Application, private val saved: SavedStateHandle) : AndroidViewModel(application) {
    private val db = AppDatabase.obtener(application)
    private val repo = CotizacionRepository(db.cotizacionDao(), db.productoDao())
    private val remoto by lazy { CotizacionesRemotasRepository() }
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
        if (_estado.value.guardando || _estado.value.enviandoRemoto || _estado.value.registrandoRemoto ||
            _estado.value.guardadaId != null || _estado.value.guardadaRemotaId != null) return
        saved["descripcion"] = f.descripcion; saved["categoria"] = f.categoria
        saved["material"] = f.material; saved["ancho"] = f.ancho; saved["largo"] = f.largo
        saved["cantidad"] = f.cantidad; saved["fecha"] = f.fecha; saved["productoId"] = f.productoId
        _estado.value = _estado.value.copy(formulario = f, errores = emptyMap(), error = null)
    }

    fun guardar(usuarioId: Long) {
        val actual = _estado.value
        if (actual.guardando || actual.enviandoRemoto || actual.registrandoRemoto ||
            actual.guardadaId != null || actual.guardadaRemotaId != null) return
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

    /** Cuenta independiente de la sesión Room; las credenciales no se guardan. */
    fun registrarCuentaRemota(correo: String, clave: String) {
        val actual = _estado.value
        if (actual.guardando || actual.enviandoRemoto || actual.registrandoRemoto || actual.guardadaId != null) return
        if (correo.isBlank() || clave.isBlank()) {
            _estado.value = actual.copy(error = "Ingresa correo y contraseña para el servicio de cotizaciones.")
            return
        }
        _estado.value = actual.copy(registrandoRemoto = true, error = null, mensajeRemoto = null)
        viewModelScope.launch {
            try {
                remoto.registrar(correo.trim(), clave)
                _estado.value = _estado.value.copy(registrandoRemoto = false,
                    mensajeRemoto = "Cuenta creada en el servidor. Ya puedes enviar tu cotización.")
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                _estado.value = _estado.value.copy(registrandoRemoto = false,
                    error = e.message ?: "No se pudo crear la cuenta en el servidor.")
            }
        }
    }

    /** No vincula un ID remoto a las pantallas de detalle local Room. */
    fun enviarRemoto(correo: String, clave: String) {
        val actual = _estado.value
        if (actual.guardando || actual.enviandoRemoto || actual.registrandoRemoto ||
            actual.guardadaId != null || actual.guardadaRemotaId != null) return
        val errores = CotizacionValidator.validar(actual.formulario, actual.productosReferencia.map { it.id }.toSet())
        if (errores.isNotEmpty()) {
            _estado.value = actual.copy(errores = errores)
            return
        }
        if (correo.isBlank() || clave.isBlank()) {
            _estado.value = actual.copy(error = "Ingresa tu correo y contraseña del servicio de cotizaciones.")
            return
        }
        _estado.value = actual.copy(enviandoRemoto = true, error = null, mensajeRemoto = null)
        viewModelScope.launch {
            try {
                val f = actual.formulario
                val producto = actual.productosReferencia.firstOrNull { it.id == f.productoId }
                val solicitud = NuevaCotizacion(
                    tokenEnvio = token,
                    descripcion = f.descripcion.trim(),
                    categoria = f.categoria,
                    material = f.material.trim(),
                    anchoCm = BigDecimal(requireNotNull(CotizacionValidator.medida(f.ancho)).toString()),
                    largoCm = BigDecimal(requireNotNull(CotizacionValidator.medida(f.largo)).toString()),
                    cantidad = f.cantidad.trim().toInt(),
                    fechaRequerida = CotizacionValidator.fecha(f.fecha)?.toString(),
                    productoReferenciaId = producto?.id?.toLong(),
                    productoReferenciaNombre = producto?.nombre
                )
                val cuenta = remoto.iniciarSesionCliente(correo.trim(), clave)
                val guardada = remoto.crear(cuenta, solicitud)
                _estado.value = _estado.value.copy(enviandoRemoto = false,
                    guardadaRemotaId = guardada.id)
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                _estado.value = _estado.value.copy(enviandoRemoto = false,
                    error = e.message ?: "No se pudo enviar la cotización al servidor.")
            }
        }
    }

}
