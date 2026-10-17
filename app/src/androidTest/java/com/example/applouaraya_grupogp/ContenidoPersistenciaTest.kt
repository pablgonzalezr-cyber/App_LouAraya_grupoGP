package com.example.applouaraya_grupogp

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.applouaraya_grupogp.data.AppDatabase
import com.example.applouaraya_grupogp.repository.*
import com.example.applouaraya_grupogp.validation.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ContenidoPersistenciaTest {
    @get:Rule val helper=MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),AppDatabase::class.java)
    private val contexto get()=InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun coleccionesCampaniasPublicacionFechasYPermisos()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(contexto,AppDatabase::class.java).build()
        try {
            val usuarios=UsuarioRepository(db.usuarioDao())
            val cliente=usuarios.registrar("Cliente contenido","contenido@example.test","Clave123!")
            val admin=usuarios.iniciarSesion(UsuarioRepository.CORREO_DEMO,UsuarioRepository.CLAVE_DEMO)!!
            var actor:Long?=admin.id
            val productos=ProductosRepository(db){actor};productos.inicializar()
            val repo=ContenidoRepository(db,productos)
            val f=ColeccionFormulario(nombre="Colección natural",descripcion="Diseños de algodón natural",publicada=true,productos=setOf(1,2))
            val id=repo.guardarColeccion(f)
            val borrador=repo.guardarColeccion(f.copy(nombre="Colección borrador",publicada=false))
            val campania=CampaniaFormulario(titulo="Campaña vigente",descripcion="Una colección para tu hogar",inicio="17/10/2026",termino="20/10/2026",publicada=true)
            val vigente=repo.guardarCampania(campania)
            repo.guardarCampania(campania.copy(titulo="Campaña borrador",publicada=false))
            repo.guardarCampania(campania.copy(titulo="Campaña futura",inicio="21/10/2026",termino="25/10/2026"))
            repo.guardarCampania(campania.copy(titulo="Campaña terminada",inicio="01/10/2026",termino="16/10/2026"))
            actor=cliente.id
            val vista=repo.observarCliente(LocalDate.of(2026,10,17)).first()
            assertEquals(listOf(id),vista.colecciones.map{it.coleccion.id})
            assertEquals(listOf(1,2),vista.colecciones.single().productos.map{it.id})
            assertEquals(listOf(vigente),vista.campanias.map{it.id})
            assertEquals(1,repo.observarCliente(LocalDate.of(2026,10,20)).first().campanias.size)
            try{repo.guardarColeccion(f);fail("Cliente no puede crear")}catch(_:SecurityException){}
            try{repo.guardarCampania(campania);fail("Cliente no puede publicar")}catch(_:SecurityException){}
            try{repo.desactivarColeccion(id);fail("Cliente no puede desactivar")}catch(_:SecurityException){}
            try{repo.desactivarCampania(vigente);fail("Cliente no puede desactivar campaña")}catch(_:SecurityException){}
            try{repo.observarAdministracion().first();fail("Cliente no puede leer borradores")}catch(_:SecurityException){}
            actor=admin.id
            productos.desactivar(1)
            assertEquals(listOf(2),repo.observarCliente(LocalDate.of(2026,10,17)).first().colecciones.single().productos.map{it.id})
            repo.guardarColeccion(f.copy(id=id,nombre="Colección editada",productos=setOf(2,3)))
            assertEquals(setOf(2,3),repo.observarCliente().first().colecciones.single().productos.map{it.id}.toSet())
            repo.guardarCampania(campania.copy(id=vigente,titulo="Campaña editada"))
            assertEquals("Campaña editada",repo.observarCliente(LocalDate.of(2026,10,17)).first().campanias.single().titulo)
            repo.desactivarColeccion(id);repo.desactivarCampania(vigente)
            assertTrue(repo.observarCliente(LocalDate.of(2026,10,17)).first().colecciones.isEmpty())
            assertTrue(repo.observarCliente(LocalDate.of(2026,10,17)).first().campanias.isEmpty())
            assertEquals(2,repo.observarAdministracion().first().colecciones.size)
            assertTrue(repo.observarAdministracion().first().colecciones.any{it.id==borrador})
            actor=null
            try{repo.guardarColeccion(f);fail("Sin sesión")}catch(_:SecurityException){}
        }finally{db.close()}
    }
    @Test fun contenidoPersisteTrasReabrir()=runBlocking {
        val nombre="prueba-contenido-${UUID.randomUUID()}.db"
        var db=Room.databaseBuilder(contexto,AppDatabase::class.java,nombre).build()
        try{
            val admin=UsuarioRepository(db.usuarioDao()).iniciarSesion(UsuarioRepository.CORREO_DEMO,UsuarioRepository.CLAVE_DEMO)!!
            val productos=ProductosRepository(db){admin.id};productos.inicializar()
            val repo=ContenidoRepository(db,productos)
            val id=repo.guardarColeccion(ColeccionFormulario(nombre="Colección persistente",descripcion="Diseños que conservan sus datos",publicada=true,productos=setOf(1)))
            repo.guardarCampania(CampaniaFormulario(titulo="Campaña persistente",descripcion="Promoción informativa de macramé",inicio="17/10/2026",termino="20/10/2026",publicada=true))
            db.close();db=Room.databaseBuilder(contexto,AppDatabase::class.java,nombre).build()
            val reabierto=ContenidoRepository(db,ProductosRepository(db){admin.id}).observarCliente(LocalDate.of(2026,10,18)).first()
            assertEquals(id,reabierto.colecciones.single().coleccion.id)
            assertEquals(listOf(1),reabierto.colecciones.single().productos.map{it.id})
            assertEquals("Campaña persistente",reabierto.campanias.single().titulo)
        }finally{db.close();contexto.deleteDatabase(nombre)}
    }
    @Test fun migracionCuatroACincoYCadenaTresACinco(){
        listOf(3,4).forEach{version->
            val nombre="prueba-migracion-contenido-${UUID.randomUUID()}.db"
            try{
                helper.createDatabase(nombre,version).use{db->
                    db.execSQL("INSERT INTO usuarios(id,nombre,correo,salt,claveHash,rol) VALUES(9,'Previo','previo-campania@example.test','salt','hash','CLIENTE')")
                    db.execSQL("INSERT INTO cotizaciones(id,usuarioId,tokenEnvio,descripcion,categoria,material,anchoCm,largoCm,cantidad,creadaEn,estado) VALUES(8,9,'previa','Solicitud previa','Interiores','Algodón',20,30,1,100,'PENDIENTE')")
                    if(version==4)db.execSQL("INSERT INTO productos(id,nombre,categoria,material,precio,descripcion,anchoCm,largoCm,disponible,imagenNombre,activo,imagenArchivo) VALUES(1,'Tapiz editado','Interiores','Algodón',77777,'Descripción previa',45,70,1,'',0,'')")
                }
                val migraciones=if(version==3)arrayOf(AppDatabase.MIGRATION_3_4,AppDatabase.MIGRATION_4_5) else arrayOf(AppDatabase.MIGRATION_4_5)
                helper.runMigrationsAndValidate(nombre,5,true,*migraciones).use{db->
                    db.query("SELECT nombre FROM usuarios WHERE id=9").use{c->assertTrue(c.moveToFirst());assertEquals("Previo",c.getString(0))}
                    db.query("SELECT estado FROM cotizaciones WHERE id=8").use{c->assertTrue(c.moveToFirst());assertEquals("PENDIENTE",c.getString(0))}
                    db.query("SELECT nombre,precio,activo FROM productos WHERE id=1").use{c->assertTrue(c.moveToFirst());if(version==4){assertEquals("Tapiz editado",c.getString(0));assertEquals(77777,c.getInt(1));assertEquals(0,c.getInt(2))}}
                }
            }finally{contexto.deleteDatabase(nombre)}
        }
    }
}
