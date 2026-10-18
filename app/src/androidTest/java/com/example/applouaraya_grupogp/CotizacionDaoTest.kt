package com.example.applouaraya_grupogp

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.applouaraya_grupogp.data.AppDatabase
import com.example.applouaraya_grupogp.model.CotizacionEntity
import com.example.applouaraya_grupogp.model.UsuarioEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CotizacionDaoTest {
    private lateinit var db: AppDatabase
    @Before fun preparar() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
    }
    @After fun cerrar() { db.close() }

    @Test fun aislamientoDetalleOrdenYEnvioIdempotente() = runBlocking {
        val a = db.usuarioDao().insertar(UsuarioEntity(nombre = "Cliente A", correo = "a@example.test", salt = "salt", claveHash = "hash"))
        val b = db.usuarioDao().insertar(UsuarioEntity(nombre = "Cliente B", correo = "b@example.test", salt = "salt", claveHash = "hash"))
        val dao = db.cotizacionDao()
        val solicitud = CotizacionEntity(usuarioId = a, tokenEnvio = "envio-1", descripcion = "Tapiz personalizado", categoria = "Interiores", material = "Algodón", anchoCm = 45.5, largoCm = 70.0, cantidad = 2, fechaRequerida = "2026-10-20", productoReferenciaId = 1, productoReferenciaNombre = "Tapiz Bohemio", creadaEn = 100)
        val id = dao.guardarUnaVez(solicitud)
        assertEquals(id, dao.guardarUnaVez(solicitud))
        assertEquals(1, dao.observarPropias(a).first().size)
        assertTrue(dao.observarPropias(b).first().isEmpty())
        assertNull(dao.observarDetalle(b, id).first())
        assertEquals(solicitud.copy(id = id), dao.observarDetalle(a, id).first())
        val reciente = dao.guardarUnaVez(solicitud.copy(tokenEnvio = "envio-2", creadaEn = 200))
        assertEquals(listOf(reciente, id), dao.observarPropias(a).first().map { it.id })
        val propiaB = dao.guardarUnaVez(solicitud.copy(usuarioId = b, tokenEnvio = "envio-b"))
        assertEquals(listOf(propiaB), dao.observarPropias(b).first().map { it.id })
    }
}
