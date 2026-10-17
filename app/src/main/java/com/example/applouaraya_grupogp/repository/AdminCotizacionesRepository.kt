package com.example.applouaraya_grupogp.repository

import com.example.applouaraya_grupogp.data.CotizacionDao
import com.example.applouaraya_grupogp.data.UsuarioDao
import kotlinx.coroutines.flow.combine

class AdminCotizacionesRepository(
    private val usuarios: UsuarioDao,
    private val solicitudes: CotizacionDao,
    private val usuarioAutenticado: () -> Long?
) {
    private fun actor() = usuarioAutenticado() ?: throw SecurityException("Inicia sesión nuevamente")
    fun observar() = actor().let { id ->
        combine(usuarios.observarPorId(id), solicitudes.observarAdministracion(id)) { usuario, lista ->
            if (usuarioAutenticado() != id) throw SecurityException("La sesión cambió")
            PoliticaAdministracion.exigirAdministrador(usuario?.rol)
            lista
        }
    }
    fun detalle(solicitudId: Long) = actor().let { id ->
        combine(usuarios.observarPorId(id), solicitudes.detalleAdministracion(id, solicitudId)) { usuario, solicitud ->
            if (usuarioAutenticado() != id) throw SecurityException("La sesión cambió")
            PoliticaAdministracion.exigirAdministrador(usuario?.rol)
            solicitud
        }
    }
    suspend fun cambiarEstado(id: Long, esperado: String, nuevo: EstadoCotizacion) {
        val actorId = actor()
        val rol = usuarios.buscarPorId(actorId)?.rol
        PoliticaAdministracion.validarCambio(rol, esperado, nuevo)
        check(usuarioAutenticado() == actorId) { "La sesión cambió" }
        // La condición SQL vuelve a comprobar el rol y el estado en la misma escritura.
        check(solicitudes.cambiarEstadoAdministracion(actorId, id, esperado, nuevo.name) == 1) {
            "La solicitud cambió o ya no tienes permiso. Actualiza el detalle."
        }
    }
}
