package com.example.applouaraya_grupogp

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.applouaraya_grupogp.data.AppDatabase
import com.example.applouaraya_grupogp.model.CotizacionFormulario
import com.example.applouaraya_grupogp.repository.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdministracionTest {
    @Test fun permisosProvisionEstadosYVisibilidad() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
        try {
            val usuarios = UsuarioRepository(db.usuarioDao())
            val cliente = usuarios.registrar("Cliente ficticio", "cliente@example.test", "Clave123!")
            assertEquals("CLIENTE", cliente.rol)
            val admin = usuarios.iniciarSesion(UsuarioRepository.CORREO_DEMO, UsuarioRepository.CLAVE_DEMO)!!
            assertEquals("ADMINISTRADOR", admin.rol)
            assertEquals(admin.id, usuarios.iniciarSesion(UsuarioRepository.CORREO_DEMO, UsuarioRepository.CLAVE_DEMO)!!.id)
            try { usuarios.registrar("Intento", UsuarioRepository.CORREO_DEMO, "OtraClave!"); fail("Correo reservado") } catch (_: IllegalArgumentException) { }
            val id = CotizacionRepository(db.cotizacionDao()).guardar(cliente.id, "token-admin-test", CotizacionFormulario("Tapiz personalizado", "Interiores", "Algodón", "40", "60", "1"))
            var actor: Long? = cliente.id
            val repo = AdminCotizacionesRepository(db.usuarioDao(), db.cotizacionDao()) { actor }
            try { repo.observar().first(); fail("Cliente no debe listar todo") } catch (_: SecurityException) { }
            try { repo.detalle(id).first(); fail("Cliente no debe consultar detalle administrativo") } catch (_: SecurityException) { }
            try { repo.cambiarEstado(id, "PENDIENTE", EstadoCotizacion.EN_REVISION); fail("Cliente no debe actualizar") } catch (_: SecurityException) { }
            assertEquals(0, db.cotizacionDao().cambiarEstadoAdministracion(cliente.id, id, "PENDIENTE", "APROBADA"))
            actor = admin.id
            assertEquals(1, repo.observar().first().size)
            repo.cambiarEstado(id, "PENDIENTE", EstadoCotizacion.EN_REVISION)
            assertEquals("EN_REVISION", repo.detalle(id).first()!!.estado)
            try { repo.cambiarEstado(id, "PENDIENTE", EstadoCotizacion.EN_REVISION); fail("Estado obsoleto") } catch (_: IllegalStateException) { }
            repo.cambiarEstado(id, "EN_REVISION", EstadoCotizacion.APROBADA)
            assertEquals("APROBADA", db.cotizacionDao().observarDetalle(cliente.id, id).first()!!.estado)
            try { repo.cambiarEstado(id, "APROBADA", EstadoCotizacion.RECHAZADA); fail("Estado final") } catch (_: IllegalArgumentException) { }
            actor = null
            try { repo.cambiarEstado(id, "EN_REVISION", EstadoCotizacion.RECHAZADA); fail("Sin sesión") } catch (_: SecurityException) { }
        } finally { db.close() }
    }

    @Test fun correoReservadoExistenteNoSePromueveNiSobrescribe() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
        try {
            val original = com.example.applouaraya_grupogp.model.UsuarioEntity(nombre = "Cuenta previa", correo = UsuarioRepository.CORREO_DEMO, salt = "anterior", claveHash = "anterior")
            val id = db.usuarioDao().insertar(original)
            val repo = UsuarioRepository(db.usuarioDao())
            try { repo.iniciarSesion(UsuarioRepository.CORREO_DEMO, UsuarioRepository.CLAVE_DEMO); fail("No promover") } catch (_: IllegalArgumentException) { }
            assertEquals(original.copy(id = id), db.usuarioDao().buscarPorId(id))
        } finally { db.close() }
    }
}
