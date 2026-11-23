package com.example.applouaraya_grupogp

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.applouaraya_grupogp.data.AppDatabase
import com.example.applouaraya_grupogp.model.*
import com.example.applouaraya_grupogp.repository.*
import com.example.applouaraya_grupogp.validation.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.util.UUID

class ReglasRoomTest {
    private lateinit var db: AppDatabase
    private var admin = 0L
    private var cliente = 0L
    private var actor: Long? = null
    private lateinit var productos: ProductosRepository
    private lateinit var contenido: ContenidoRepository
    @Before fun abrir() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
        runBlocking {
            val usuarios = UsuarioRepository(db.usuarioDao())
            admin = usuarios.iniciarSesion(UsuarioRepository.CORREO_DEMO, UsuarioRepository.CLAVE_DEMO)!!.id
            cliente = usuarios.registrar("Cliente prueba", "cliente@example.test", UUID.randomUUID().toString()).id
            actor = admin
            productos = ProductosRepository(db) { actor }
            contenido = ContenidoRepository(db, productos)
            productos.inicializar()
        }
    }
    @After fun cerrar() = db.close()

    private inline fun <reified E: Exception> rechaza(accion: () -> Unit) {
        try { accion(); fail("Se esperaba ${E::class.simpleName}") }
        catch (error: Exception) { if (error !is E) throw error }
    }

    @Test fun edicionesInvalidasMantienenElContenidoOriginal() = runBlocking {
        val coleccion = ColeccionFormulario(nombre="Tejidos", descripcion="Tejidos de la casa", publicada=true, productos=setOf(1))
        val id = contenido.guardarColeccion(coleccion)
        rechaza<IllegalArgumentException> { contenido.guardarColeccion(coleccion.copy(id=id, productos=setOf(2,999))) }
        assertEquals(listOf(1), contenido.observarCliente().first().colecciones.single().productos.map { it.id })
        rechaza<IllegalArgumentException> { contenido.guardarColeccion(coleccion.copy(nombre="")) }
        rechaza<IllegalStateException> { contenido.guardarColeccion(coleccion.copy(id=999)) }
        val camp = CampaniaFormulario(titulo="Taller", descripcion="Jornada de tejidos", inicio="01/01/2020", termino="31/12/2020", publicada=true)
        rechaza<IllegalArgumentException> { contenido.guardarCampania(camp.copy(termino="invalida")) }
        rechaza<IllegalStateException> { contenido.guardarCampania(camp.copy(id=999)) }
        rechaza<IllegalStateException> { contenido.desactivarCampania(999) }
        rechaza<IllegalStateException> { contenido.desactivarColeccion(999) }
        val campId = contenido.guardarCampania(camp)
        contenido.desactivarCampania(campId)
        assertFalse(db.contenidoDao().buscarCampania(campId)!!.activa)
        assertTrue(db.avisoDao().observar(cliente).first().isEmpty())
        val original = db.productoDao().buscar(1)!!
        val f = ProductoFormulario.desde(original)
        rechaza<IllegalArgumentException> { productos.guardar(f.copy(precio="-1")) }
        rechaza<IllegalStateException> { productos.guardar(f.copy(id=999)) }
        rechaza<IllegalStateException> { productos.desactivar(999) }
        assertEquals(original, db.productoDao().buscar(1))
    }

    @Test fun unaSesionQueCambiaPierdeAccesoAObservacionesYEscrituras() = runBlocking {
        val adminRepo = AdminCotizacionesRepository(db.usuarioDao(),db.cotizacionDao()) { actor }
        val lista = adminRepo.observar()
        val detalle = adminRepo.detalle(1)
        val perfil = PerfilRepository(db.usuarioDao()) { actor }
        val perfilAnterior = perfil.observar()
        actor = cliente
        rechaza<SecurityException> { lista.first() }
        rechaza<SecurityException> { detalle.first() }
        rechaza<SecurityException> { perfilAnterior.first() }
        actor = 9999
        rechaza<SecurityException> { productos.exigirAdministrador() }
        rechaza<SecurityException> { adminRepo.observar().first() }
        rechaza<SecurityException> { adminRepo.detalle(1).first() }
        rechaza<SecurityException> { adminRepo.cambiarEstado(1,"PENDIENTE",EstadoCotizacion.EN_REVISION) }
        rechaza<SecurityException> { perfil.observar().first() }
        rechaza<IllegalStateException> { perfil.guardarNombre("Nombre válido") }
        var llamadas = 0
        val interrumpido = ProductosRepository(db) { if (llamadas++ == 0) admin else cliente }
        rechaza<SecurityException> { interrumpido.exigirAdministrador() }
        llamadas = 0
        val gestion = AdminCotizacionesRepository(db.usuarioDao(), db.cotizacionDao()) { if (llamadas++ == 0) admin else cliente }
        rechaza<IllegalStateException> { gestion.cambiarEstado(1,"PENDIENTE",EstadoCotizacion.EN_REVISION) }
        llamadas = 0
        val edicion = PerfilRepository(db.usuarioDao()) { if (llamadas++ == 0) admin else cliente }
        rechaza<IllegalStateException> { edicion.guardarNombre("Nombre válido") }
        for (id in listOf(null,0L)) {
            actor = id
            rechaza<SecurityException> { perfil.observar() }
            rechaza<SecurityException> { AvisosRepository(db) { actor }.observar() }
        }
    }

    @Test fun avisoRemotoRevalidaAntesDeMarcarLeidoYSigueAislado() = runBlocking {
        val c = CampaniaEntity(18,"Taller","Tejidos","2020-01-01","2099-12-31",true,true)
        contenido.avisarPublicacion(c.copy(activa=false))
        contenido.avisarPublicacion(c.copy(publicada=false))
        contenido.avisarPublicacion(c.copy(termino="2020-12-31"))
        assertTrue(db.avisoDao().observar(cliente).first().isEmpty())
        repeat(2) { contenido.avisarPublicacion(c) }
        actor = cliente
        val aviso = db.avisoDao().observar(cliente).first().single()
        val sinRed = AvisosRepository(db,{ throw java.io.IOException("sin red") }) { actor }
        rechaza<java.io.IOException> { sinRed.destino(aviso.id) }
        val cambiada = AvisosRepository(db,{ actor=admin }) { actor }
        rechaza<IllegalStateException> { cambiada.destino(aviso.id) }
        assertFalse(db.avisoDao().buscar(cliente,aviso.id)!!.leida)
        val repo = AvisosRepository(db,{ assertEquals(c.id,it) }) { actor }
        rechaza<SecurityException> { repo.destino(aviso.id) }
        actor = cliente
        assertEquals("novedades",repo.destino(aviso.id))
        assertTrue(db.avisoDao().buscar(cliente,aviso.id)!!.leida)
    }

    @Test fun cotizacionConservaLaReferenciaYFechaDelFormulario() = runBlocking {
        val repo = CotizacionRepository(db.cotizacionDao(),db.productoDao())
        val original = repo.observarProductos().first().first()
        val f = CotizacionFormulario("Un tapiz para la sala","Interiores","Algodón","45","70","1", "31/12/2099", original.id)
        rechaza<IllegalArgumentException> { repo.guardar(cliente,"invalido",f.copy(productoId=999)) }
        val id = repo.guardar(cliente,"token",f)
        val guardada = repo.observarDetalle(cliente,id).first()!!
        assertEquals(original.nombre,guardada.productoReferenciaNombre)
        assertEquals("2099-12-31",guardada.fechaRequerida)
        assertNull(repo.observarDetalle(admin,id).first())
    }
}
