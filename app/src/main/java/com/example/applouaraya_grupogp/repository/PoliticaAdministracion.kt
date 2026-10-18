package com.example.applouaraya_grupogp.repository

import com.example.applouaraya_grupogp.model.RolUsuario

enum class EstadoCotizacion { PENDIENTE, EN_REVISION, APROBADA, RECHAZADA }

object PoliticaAdministracion {
    fun esAdministrador(rol: String?) = rol == RolUsuario.ADMINISTRADOR.name
    fun exigirAdministrador(rol: String?) {
        if (!esAdministrador(rol)) throw SecurityException("Acceso exclusivo para administradores")
    }
    fun destinos(estado: String): List<EstadoCotizacion> = when (estado) {
        "PENDIENTE" -> listOf(EstadoCotizacion.EN_REVISION)
        "EN_REVISION" -> listOf(EstadoCotizacion.APROBADA, EstadoCotizacion.RECHAZADA)
        else -> emptyList()
    }
    fun validarCambio(rol: String?, actual: String, nuevo: EstadoCotizacion) {
        exigirAdministrador(rol)
        require(nuevo in destinos(actual)) { "Transición de estado no permitida" }
    }
}
