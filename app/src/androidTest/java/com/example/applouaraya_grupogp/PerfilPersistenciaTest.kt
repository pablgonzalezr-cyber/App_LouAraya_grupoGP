package com.example.applouaraya_grupogp

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.applouaraya_grupogp.data.AppDatabase
import com.example.applouaraya_grupogp.repository.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PerfilPersistenciaTest {
    @Test fun nombrePersisteTrasReabrirSinCambiarCorreoRolNiCredenciales() = runBlocking {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val nombre = "prueba-perfil-${UUID.randomUUID()}.db"
        var db = Room.databaseBuilder(contexto, AppDatabase::class.java, nombre).build()
        try {
            val usuario = UsuarioRepository(db.usuarioDao()).registrar("Cliente previo", "perfil@example.test", "Clave123!")
            val repo = PerfilRepository(db.usuarioDao()) { usuario.id }
            repo.guardarNombre("  María   José  ")
            assertEquals(usuario.copy(nombre = "María José"), repo.observar().first())
            db.close()
            db = Room.databaseBuilder(contexto, AppDatabase::class.java, nombre).build()
            assertEquals(usuario.copy(nombre = "María José"), UsuarioRepository(db.usuarioDao()).iniciarSesion("perfil@example.test", "Clave123!"))
            val admin = UsuarioRepository(db.usuarioDao()).iniciarSesion(UsuarioRepository.CORREO_DEMO, UsuarioRepository.CLAVE_DEMO)!!
            PerfilRepository(db.usuarioDao()) { admin.id }.guardarNombre("Administración Lou")
            assertEquals(admin.copy(nombre = "Administración Lou"), db.usuarioDao().buscarPorId(admin.id))
        } finally { db.close(); contexto.deleteDatabase(nombre) }
    }
    @Test fun nombreInvalidoYSesionAusenteNoModificanCuentas() = runBlocking {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(contexto, AppDatabase::class.java).build()
        try {
            val u = UsuarioRepository(db.usuarioDao()).registrar("Cliente", "propietario@example.test", "Clave123!")
            var actor: Long? = u.id
            val repo = PerfilRepository(db.usuarioDao()) { actor }
            try { repo.guardarNombre(" "); fail("Nombre inválido") } catch (_: IllegalArgumentException) { }
            actor = null
            try { repo.guardarNombre("Otro nombre"); fail("Sin sesión") } catch (_: SecurityException) { }
            try { repo.observar().first(); fail("Sin sesión") } catch (_: SecurityException) { }
            assertEquals(u, db.usuarioDao().buscarPorId(u.id))
        } finally { db.close() }
    }
    @Test fun preferenciasPersistenYSeSeparanPorUsuario() = runBlocking {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val archivo = "prueba-preferencias-${UUID.randomUUID()}"
        val prefs = contexto.getSharedPreferences(archivo, Context.MODE_PRIVATE)
        try {
            val repo = PreferenciasRepository(prefs)
            assertEquals(PreferenciasLocales(), repo.leer(1))
            repo.guardar(1, PreferenciasLocales(confirmarSalida = false, mostrarAyuda = false))
            val reabierto = PreferenciasRepository(contexto.getSharedPreferences(archivo, Context.MODE_PRIVATE))
            assertEquals(PreferenciasLocales(false, false), reabierto.observar(1).first())
            assertEquals(PreferenciasLocales(), reabierto.leer(2))
        } finally { contexto.deleteSharedPreferences(archivo) }
    }
}
