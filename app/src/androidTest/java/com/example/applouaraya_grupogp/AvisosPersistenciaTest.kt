package com.example.applouaraya_grupogp

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.example.applouaraya_grupogp.data.AppDatabase
import com.example.applouaraya_grupogp.model.CotizacionFormulario
import com.example.applouaraya_grupogp.repository.*
import com.example.applouaraya_grupogp.validation.CampaniaFormulario
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class AvisosPersistenciaTest {
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)
    private val contexto get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val formulario = CotizacionFormulario("Tapiz para sala de prueba", "Interiores", "Algodón", "40", "60", "1")

    @Test fun eventosAtomicosAisladosSinDuplicadosYPermisosDeDestinos() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(contexto, AppDatabase::class.java).build()
        try {
            val usuarios = UsuarioRepository(db.usuarioDao())
            val cliente = usuarios.registrar("Cliente avisos", "avisos@example.test", "Clave123!")
            val otro = usuarios.registrar("Otro cliente", "otroaviso@example.test", "Clave123!")
            val admin = usuarios.iniciarSesion(UsuarioRepository.CORREO_DEMO, UsuarioRepository.CLAVE_DEMO)!!
            val solicitudes = CotizacionRepository(db.cotizacionDao())
            val id = solicitudes.guardar(cliente.id, "evento-unico", formulario)
            assertEquals(id, solicitudes.guardar(cliente.id, "evento-unico", formulario))
            assertEquals(1, db.avisoDao().observar(admin.id).first().size)
            assertTrue(db.avisoDao().observar(cliente.id).first().isEmpty())
            var actor: Long? = admin.id
            val gestion = AdminCotizacionesRepository(db.usuarioDao(), db.cotizacionDao()) { actor }
            gestion.cambiarEstado(id, "PENDIENTE", EstadoCotizacion.EN_REVISION)
            val aviso = db.avisoDao().observar(cliente.id).first().single()
            assertEquals("ESTADO", aviso.tipo)
            assertEquals(1, db.avisoDao().observar(admin.id).first().size)
            assertTrue(db.avisoDao().observar(otro.id).first().isEmpty())
            try { gestion.cambiarEstado(id, "PENDIENTE", EstadoCotizacion.EN_REVISION); fail("Cambio obsoleto") } catch(_: IllegalStateException) {}
            assertEquals(1, db.avisoDao().observar(cliente.id).first().size)
            val repo = AvisosRepository(db) { actor }
            try { repo.destino(aviso.id); fail("Administrador no debe leer aviso ajeno") } catch(_: SecurityException) {}
            actor = otro.id
            try { repo.marcarLeida(aviso.id); fail("No marcar ajeno") } catch(_: IllegalStateException) {}
            try { repo.destino(aviso.id); fail("No abrir ajeno") } catch(_: SecurityException) {}
            assertFalse(db.avisoDao().buscar(cliente.id, aviso.id)!!.leida)
            actor = cliente.id
            assertEquals("solicitud/$id", repo.destino(aviso.id))
            assertTrue(repo.observar().first().single().leida)
            actor = admin.id
            assertEquals("admin/solicitud/$id", repo.destino(repo.observar().first().single().id))
            actor = null
            try { repo.destino(aviso.id); fail("Sin sesión") } catch(_: SecurityException) {}
        } finally { db.close() }
    }

    @Test fun campaniasPublicadasNoDuplicanYRevalidanVigencia() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(contexto, AppDatabase::class.java).build()
        try {
            val usuarios = UsuarioRepository(db.usuarioDao())
            val cliente = usuarios.registrar("Cliente campaña", "campaviso@example.test", "Clave123!")
            val admin = usuarios.iniciarSesion(UsuarioRepository.CORREO_DEMO, UsuarioRepository.CLAVE_DEMO)!!
            var actor: Long? = admin.id
            val contenido = ContenidoRepository(db, ProductosRepository(db) { actor })
            val formato = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val f = CampaniaFormulario(titulo = "Campaña de prueba", descripcion = "Información ficticia de colección", inicio = LocalDate.now().format(formato), termino = LocalDate.now().plusDays(3).format(formato), publicada = false)
            val id = contenido.guardarCampania(f)
            assertTrue(db.avisoDao().observar(cliente.id).first().isEmpty())
            contenido.guardarCampania(f.copy(id = id, publicada = true))
            contenido.guardarCampania(f.copy(id = id, publicada = true, titulo = "Campaña editada"))
            val aviso = db.avisoDao().observar(cliente.id).first().single()
            assertTrue(db.avisoDao().observar(admin.id).first().isEmpty())
            val avisos = AvisosRepository(db) { actor }
            actor = cliente.id
            assertEquals("novedades", avisos.destino(aviso.id))
            actor = admin.id; contenido.desactivarCampania(id); actor = cliente.id
            try { avisos.destino(aviso.id); fail("Campaña desactivada") } catch(_: IllegalStateException) {}
            assertEquals(1, avisos.observar().first().size)
        } finally { db.close() }
    }

    @Test fun avisosYPreferenciasPersistenTrasReabrir() = runBlocking {
        val nombre = "prueba-avisos-${UUID.randomUUID()}.db"
        val archivo = "prueba-preferencias-avisos-${UUID.randomUUID()}"
        var db = Room.databaseBuilder(contexto, AppDatabase::class.java, nombre).build()
        try {
            val usuarios = UsuarioRepository(db.usuarioDao())
            val cliente = usuarios.registrar("Persistente", "persistaviso@example.test", "Clave123!")
            val admin = usuarios.iniciarSesion(UsuarioRepository.CORREO_DEMO, UsuarioRepository.CLAVE_DEMO)!!
            val id = CotizacionRepository(db.cotizacionDao()).guardar(cliente.id, "persistente", formulario)
            AdminCotizacionesRepository(db.usuarioDao(), db.cotizacionDao()) { admin.id }.cambiarEstado(id, "PENDIENTE", EstadoCotizacion.EN_REVISION)
            val repo = AvisosRepository(db) { cliente.id }
            repo.marcarLeida(repo.observar().first().single().id)
            PreferenciasRepository(contexto.getSharedPreferences(archivo, Context.MODE_PRIVATE)).guardar(cliente.id, PreferenciasLocales(avisosCotizaciones = false))
            db.close(); db = Room.databaseBuilder(contexto, AppDatabase::class.java, nombre).build()
            assertTrue(AvisosRepository(db) { cliente.id }.observar().first().single().leida)
            val prefs = PreferenciasRepository(contexto.getSharedPreferences(archivo, Context.MODE_PRIVATE))
            assertFalse(prefs.leer(cliente.id).avisosCotizaciones)
            assertTrue(prefs.leer(admin.id).avisosCotizaciones)
            assertTrue(prefs.leer(cliente.id).avisosCampanias)
        } finally { db.close(); contexto.deleteDatabase(nombre); contexto.deleteSharedPreferences(archivo) }
    }

    @Test fun migracionCincoASeisPreservaDatos() {
        val nombre = "prueba-migracion-avisos-${UUID.randomUUID()}.db"
        try {
            helper.createDatabase(nombre, 5).use { db ->
                db.execSQL("INSERT INTO usuarios VALUES(9,'Previo','previoaviso@example.test','salt','hash','CLIENTE')")
                db.execSQL("INSERT INTO cotizaciones(id,usuarioId,tokenEnvio,descripcion,categoria,material,anchoCm,largoCm,cantidad,creadaEn,estado) VALUES(8,9,'previa','Solicitud previa','Interiores','Algodón',20,30,1,100,'PENDIENTE')")
                db.execSQL("INSERT INTO productos VALUES(1,'Tapiz editado','Interiores','Algodón',77777,'Descripción previa',45,70,1,'',0,'')")
                db.execSQL("INSERT INTO colecciones VALUES(3,'Colección previa','Descripción',1,1)")
                db.execSQL("INSERT INTO coleccion_productos VALUES(3,1)")
                db.execSQL("INSERT INTO campanias VALUES(4,'Campaña previa','Descripción','2026-10-17','2026-10-20',1,1)")
            }
            helper.runMigrationsAndValidate(nombre, 6, true, AppDatabase.MIGRATION_5_6).use { db ->
                listOf("usuarios", "cotizaciones", "productos", "colecciones", "coleccion_productos", "campanias").forEach { tabla ->
                    db.query("SELECT COUNT(*) FROM $tabla").use { c -> assertTrue(c.moveToFirst()); assertEquals(1, c.getInt(0)) }
                }
                db.query("SELECT nombre,precio,activo FROM productos WHERE id=1").use { c -> assertTrue(c.moveToFirst()); assertEquals("Tapiz editado", c.getString(0)); assertEquals(77777, c.getInt(1)); assertEquals(0, c.getInt(2)) }
                db.query("SELECT salt,claveHash,rol FROM usuarios WHERE id=9").use { c -> assertTrue(c.moveToFirst()); assertEquals("salt",c.getString(0)); assertEquals("hash",c.getString(1)); assertEquals("CLIENTE",c.getString(2)) }
                db.query("SELECT COUNT(*) FROM avisos").use { c -> assertTrue(c.moveToFirst()); assertEquals(0, c.getInt(0)) }
            }
        } finally { contexto.deleteDatabase(nombre) }
    }

    @Test fun cadenaUnoASeisConservaUsuario() {
        val nombre = "prueba-cadena-avisos-${UUID.randomUUID()}.db"
        try {
            helper.createDatabase(nombre, 1).use { it.execSQL("INSERT INTO usuarios VALUES(9,'Previo','cadenaaviso@example.test','salt','hash')") }
            helper.runMigrationsAndValidate(nombre, 6, true, *AppDatabase.MIGRACIONES).use { db ->
                db.query("SELECT nombre,rol FROM usuarios WHERE id=9").use { c -> assertTrue(c.moveToFirst()); assertEquals("Previo", c.getString(0)); assertEquals("CLIENTE", c.getString(1)) }
            }
        } finally { contexto.deleteDatabase(nombre) }
    }
}
