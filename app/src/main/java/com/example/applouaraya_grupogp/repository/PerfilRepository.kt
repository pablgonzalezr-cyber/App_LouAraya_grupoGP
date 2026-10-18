package com.example.applouaraya_grupogp.repository

import com.example.applouaraya_grupogp.data.UsuarioDao
import com.example.applouaraya_grupogp.validation.PerfilValidator
import kotlinx.coroutines.flow.map

class PerfilRepository(private val dao: UsuarioDao, private val usuarioAutenticado: () -> Long?) {
    private fun actor() = usuarioAutenticado()?.takeIf { it > 0 } ?: throw SecurityException("Inicia sesión nuevamente")
    fun observar() = actor().let { id ->
        dao.observarPorId(id).map { usuario ->
            if (usuarioAutenticado() != id) throw SecurityException("La sesión cambió")
            usuario ?: throw SecurityException("La cuenta no está disponible")
        }
    }
    suspend fun guardarNombre(nombre: String) {
        val id = actor()
        require(PerfilValidator.errorNombre(nombre) == null) { PerfilValidator.errorNombre(nombre)!! }
        check(usuarioAutenticado() == id) { "La sesión cambió" }
        check(dao.actualizarNombre(id, PerfilValidator.normalizarNombre(nombre)) == 1) { "No se pudo actualizar tu perfil" }
    }
}
