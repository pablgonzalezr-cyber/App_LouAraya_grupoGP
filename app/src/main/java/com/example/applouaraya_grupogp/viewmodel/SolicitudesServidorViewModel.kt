package com.example.applouaraya_grupogp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.applouaraya_grupogp.repository.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Persiste únicamente el último estado visto, separado por servicio, usuario local y propietario remoto. */
class SeguimientoSolicitudes(private val leer: (String) -> String?, private val escribir: (String, String) -> Unit) {
    fun observar(cuenta: String, solicitud: CotizacionRemota): String? {
        val clave = "$cuenta/${solicitud.usuarioId}/${solicitud.id}"
        val anterior = leer(clave)
        val actual = solicitud.estado.name
        if (actual == anterior) return null
        escribir(clave, actual)
        return anterior?.let { "Solicitud #${solicitud.id}: $it → $actual" }
    }
}

data class SolicitudesServidorState(
    val conectada: Boolean = false, val cargando: Boolean = false,
    val lista: List<CotizacionRemota> = emptyList(), val detalle: CotizacionRemota? = null,
    val avisos: List<String> = emptyList(), val mensaje: String? = null, val error: String? = null
)

class SolicitudesServidorViewModel(
    private val repo: CotizacionesRemotasRepository,
    val admin: Boolean,
    private val seguimiento: SeguimientoSolicitudes,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
) : ViewModel() {
    private val mutable = MutableStateFlow(SolicitudesServidorState())
    val estado = mutable.asStateFlow()
    private var cuenta: CuentaCotizaciones? = null
    private var trabajo: Job? = null
    private var identificador = ""

    fun conectar(usuario: String, clave: String, id: Long?) = ejecutar {
        identificador = java.util.UUID.nameUUIDFromBytes(usuario.trim().lowercase().toByteArray()).toString()
        cuenta = if (admin) repo.iniciarSesionAdmin(usuario, clave) else repo.iniciarSesionCliente(usuario, clave)
        mutable.value = SolicitudesServidorState(conectada = true, cargando = true)
        cargar(id)
    }

    fun desconectar() {
        trabajo?.cancel()
        cuenta = null
        mutable.value = SolicitudesServidorState()
    }

    fun actualizar(id: Long?) {
        if (cuenta != null) ejecutar { cargar(id) }
    }

    fun leerAvisos() { mutable.value = mutable.value.copy(avisos = emptyList()) }

    private suspend fun cargar(id: Long?) {
        val sesion = checkNotNull(cuenta)
        val lista = if (admin) repo.solicitudes(sesion) else repo.historial(sesion)
        val detalle = id?.let { if (admin) repo.detalleAdmin(sesion, it) else repo.propia(sesion, it) }
        val cambios = if (admin) emptyList() else (lista.filter { it.id != id } + listOfNotNull(detalle)).mapNotNull { seguimiento.observar(identificador, it) }
        mutable.value = mutable.value.copy(lista = lista, detalle = detalle, avisos = mutable.value.avisos + cambios)
    }

    fun cambiar(id: Long, esperado: EstadoCotizacionRemota, nuevo: EstadoCotizacionRemota) {
        val s = mutable.value.detalle ?: return
        if (!admin || s.id != id || s.estado != esperado || PoliticaAdministracion.destinos(esperado.name).none { it.name == nuevo.name }) return
        ejecutar {
            try {
                val confirmado = repo.cambiarEstado(checkNotNull(cuenta), id, esperado, nuevo)
                mutable.value = mutable.value.copy(detalle = confirmado, mensaje = "Cambio confirmado: ${confirmado.estado}")
            } catch (e: ErrorCotizaciones) {
                if (e.codigo != 409) throw e
                mutable.value = mutable.value.copy(detalle = null, mensaje = "Conflicto (409). Revisa el estado actual antes de continuar.")
            }
            cargar(id)
        }
    }

    private fun ejecutar(accion: suspend () -> Unit) {
        if (mutable.value.cargando) return
        mutable.value = mutable.value.copy(cargando = true, error = null, mensaje = null)
        trabajo = viewModelScope.launch(dispatcher) {
            try {
                accion()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (e is ErrorCotizaciones && e.codigo in setOf(401, 403)) {
                    cuenta = null
                    mutable.value = SolicitudesServidorState()
                }
                mutable.value = mutable.value.copy(detalle = null, error =
                    if (e is ErrorCotizaciones) "HTTP ${e.codigo}. ${e.message}" else e.message ?: "No se pudo consultar el servidor.")
            } finally {
                if (isActive)
                    mutable.value = mutable.value.copy(cargando = false)
            }
        }
    }

    override fun onCleared() { cuenta = null }
}
