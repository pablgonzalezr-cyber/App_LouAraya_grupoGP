package com.example.applouaraya_grupogp.repository

import androidx.room.withTransaction
import com.example.applouaraya_grupogp.data.AppDatabase
import com.example.applouaraya_grupogp.model.AvisoEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.combine

object PoliticaAvisos {
    fun habilitado(tipo: String, preferencias: PreferenciasLocales) = when(tipo) {
        "ESTADO", "NUEVA_SOLICITUD" -> preferencias.avisosCotizaciones
        "CAMPANIA" -> preferencias.avisosCampanias
        else -> false
    }
    fun ruta(tipo: String, rol: String, referencia: Long): String? = when {
        referencia <= 0 -> null
        tipo == "ESTADO" && rol == "CLIENTE" -> "solicitud/$referencia"
        tipo == "NUEVA_SOLICITUD" && rol == "ADMINISTRADOR" -> "admin/solicitud/$referencia"
        tipo == "CAMPANIA" && rol == "CLIENTE" -> "novedades"
        else -> null
    }
}

class AvisosRepository(private val db: AppDatabase, private val sesion: () -> Long?) {
    private fun actor() = sesion()?.takeIf { it > 0 } ?: throw SecurityException("Inicia sesión para consultar avisos")
    fun observar() = actor().let { id ->
        combine(db.usuarioDao().observarPorId(id), db.avisoDao().observar(id)) { usuario, avisos ->
            check(sesion() == id) { "La sesión cambió" }
            require(usuario?.rol in setOf("CLIENTE", "ADMINISTRADOR")) { "Cuenta no disponible" }
            avisos
        }
    }
    suspend fun marcarLeida(id: Long) = db.withTransaction {
        val usuario = actor()
        check(db.avisoDao().marcarLeida(usuario, id) == 1) { "Aviso no disponible para esta cuenta" }
    }
    suspend fun destino(id: Long): String = db.withTransaction {
        val usuario = actor()
        val rol = db.usuarioDao().buscarPorId(usuario)?.rol ?: throw SecurityException("Cuenta no disponible")
        val aviso = db.avisoDao().buscar(usuario, id) ?: throw SecurityException("Aviso no disponible para esta cuenta")
        val ruta = PoliticaAvisos.ruta(aviso.tipo, rol, aviso.referenciaId)
            ?: throw SecurityException("No tienes permiso para abrir este contenido")
        when (aviso.tipo) {
            "ESTADO" -> check(db.cotizacionDao().buscarPorId(aviso.referenciaId)?.usuarioId == usuario) { "Solicitud no disponible" }
            "NUEVA_SOLICITUD" -> check(db.cotizacionDao().buscarPorId(aviso.referenciaId) != null) { "Solicitud no disponible" }
            "CAMPANIA" -> {
                val campania = db.contenidoDao().buscarCampania(aviso.referenciaId)
                val hoy = LocalDate.now().toString()
                check(campania != null && campania.activa && campania.publicada && hoy >= campania.inicio && hoy <= campania.termino) {
                    "Esta campaña todavía no está vigente o ya no está publicada. El aviso se conserva como historial."
                }
            }
        }
        check(sesion() == usuario) { "La sesión cambió" }
        db.avisoDao().marcarLeida(usuario, id)
        ruta
    }
}
