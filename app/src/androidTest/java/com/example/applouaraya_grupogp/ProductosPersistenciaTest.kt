package com.example.applouaraya_grupogp

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.applouaraya_grupogp.data.*
import com.example.applouaraya_grupogp.repository.*
import com.example.applouaraya_grupogp.validation.ProductoFormulario
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ProductosPersistenciaTest {
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)
    private val contexto get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val formulario = ProductoFormulario(nombre="Nuevo tapiz", descripcion="Tapiz artesanal personalizado", categoria="Interiores", material="Algodón", ancho="45", largo="70", precio="15000", activo=true)

    @Test fun crudReactivoPermisosYFavoritosConservados() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(contexto, AppDatabase::class.java).build()
        val favoritos = Room.inMemoryDatabaseBuilder(contexto, FavoritosDatabase::class.java).build()
        try {
            val usuarios = UsuarioRepository(db.usuarioDao())
            val cliente = usuarios.registrar("Cliente", "productos@example.test", "Clave123!")
            val admin = usuarios.iniciarSesion(UsuarioRepository.CORREO_DEMO, UsuarioRepository.CLAVE_DEMO)!!
            var actor: Long? = admin.id
            val repo = ProductosRepository(db) { actor }
            repo.inicializar(); repo.inicializar()
            assertEquals((1..8).toList(), repo.observarPublicados().first().map { it.id })
            favoritos.favoritoDao().agregar(FavoritoEntity(cliente.id, 1))
            val id = repo.guardar(formulario)
            assertTrue(id > 8)
            actor = cliente.id
            assertEquals(15000, repo.observarPublicados().first().first { it.id == id }.precio)
            try { repo.guardar(formulario); fail("Cliente no puede crear") } catch (_: SecurityException) { }
            try { repo.desactivar(1); fail("Cliente no puede desactivar") } catch (_: SecurityException) { }
            try { repo.observarAdministracion().first(); fail("Cliente no debe listar borradores") } catch (_: SecurityException) { }
            actor = admin.id
            repo.guardar(formulario.copy(id=id, precio="22000", descripcion="Descripción actualizada del tapiz", disponible=false))
            assertEquals(22000, repo.observarPublicados().first().first { it.id == id }.precio)
            assertFalse(repo.observarPublicados().first().first { it.id == id }.disponible)
            repo.desactivar(1)
            assertNotNull(db.productoDao().buscar(1)); assertFalse(db.productoDao().buscar(1)!!.activo)
            assertFalse(repo.observarPublicados().first().any { it.id == 1 })
            assertEquals(listOf(1), favoritos.favoritoDao().observarIds(cliente.id).first())
            repo.inicializar()
            assertFalse(db.productoDao().buscar(1)!!.activo)
            assertEquals(22000, db.productoDao().buscar(id)!!.precio)
            actor = null
            try { repo.guardar(formulario); fail("Sin sesión") } catch (_: SecurityException) { }
        } finally { favoritos.close(); db.close() }
    }

    @Test fun cambiosPersistenTrasReabrirSinSobrescribirSemilla() = runBlocking {
        val nombre="prueba-productos-${UUID.randomUUID()}.db"
        var db = Room.databaseBuilder(contexto, AppDatabase::class.java,nombre).build()
        try {
            val admin=UsuarioRepository(db.usuarioDao()).iniciarSesion(UsuarioRepository.CORREO_DEMO, UsuarioRepository.CLAVE_DEMO)!!
            var repo=ProductosRepository(db) { admin.id }
            repo.inicializar()
            repo.guardar(ProductoFormulario.desde(db.productoDao().buscar(1)!!).copy(precio="99990", activo=false))
            val nuevo=repo.guardar(formulario)
            db.close(); db=Room.databaseBuilder(contexto,AppDatabase::class.java,nombre).build()
            repo=ProductosRepository(db) { admin.id }; repo.inicializar()
            assertEquals(99990,db.productoDao().buscar(1)!!.precio); assertFalse(db.productoDao().buscar(1)!!.activo)
            assertEquals("Nuevo tapiz", db.productoDao().buscar(nuevo)!!.nombre)
            assertEquals(9,repo.observarAdministracion().first().size)
        } finally { db.close(); contexto.deleteDatabase(nombre) }
    }

    @Test fun migracionTresACuatroConservaUsuariosYCotizaciones() {
        val nombre="prueba-migracion-productos-${UUID.randomUUID()}.db"
        try {
            helper.createDatabase(nombre,3).use { db ->
                db.execSQL("INSERT INTO usuarios (id,nombre,correo,salt,claveHash,rol) VALUES(5,'Anterior','anterior-producto@example.test','salt','hash','CLIENTE')")
                db.execSQL("INSERT INTO cotizaciones (id,usuarioId,tokenEnvio,descripcion,categoria,material,anchoCm,largoCm,cantidad,creadaEn,estado) VALUES(7,5,'anterior','Solicitud anterior','Interiores','Algodón',45,70,1,100,'EN_REVISION')")
            }
            helper.runMigrationsAndValidate(nombre,4,true,AppDatabase.MIGRATION_3_4).use { db ->
                db.query("SELECT nombre,rol FROM usuarios WHERE id=5").use { c -> assertTrue(c.moveToFirst()); assertEquals("Anterior",c.getString(0)); assertEquals("CLIENTE",c.getString(1)) }
                db.query("SELECT estado FROM cotizaciones WHERE id=7").use { c -> assertTrue(c.moveToFirst()); assertEquals("EN_REVISION",c.getString(0)) }
                db.query("SELECT id FROM productos ORDER BY id").use { c -> val ids=mutableListOf<Int>(); while(c.moveToNext()) ids+=c.getInt(0); assertEquals((1..8).toList(),ids) }
            }
        } finally { contexto.deleteDatabase(nombre) }
    }
}
