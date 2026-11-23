package com.example.applouaraya_grupogp

import android.content.SharedPreferences
import com.example.applouaraya_grupogp.repository.*
import com.example.applouaraya_grupogp.validation.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*

class CatalogoReglasTest {
    private val valido = ProductoFormulario(nombre = "Tapiz", descripcion = "Descripción artesanal", material = "Algodón",
        categoria = "Interiores", ancho = "10000", largo = "0,01", precio = "999999999,99")

    @Test fun precisionYLimitesDelProductoRemoto() {
        assertTrue(ProductoValidator.validarRemoto(valido).isEmpty())
        for (texto in listOf("-1", "1.234", "1e3", "0", "10000,01", "")) {
            assertEquals(setOf("ancho", "largo"), ProductoValidator.validarRemoto(valido.copy(ancho=texto, largo=texto)).keys)
        }
        assertEquals(setOf("precio"), ProductoValidator.validarRemoto(valido.copy(precio="1000000000")).keys)
        val larga = "a".repeat(101)
        assertEquals(setOf("nombre","categoria","material","descripcion","imagen"), ProductoValidator.validarRemoto(
            valido.copy(nombre=larga, categoria=" ", material=larga, descripcion="x".repeat(2001), imagenArchivo="../foto")).keys)
        val archivo = java.util.UUID.randomUUID().toString() + ".img"
        assertTrue(ProductoValidator.validarRemoto(valido.copy(imagenArchivo=archivo)).isEmpty())
        assertTrue(ProductoValidator.validar(valido.copy(largo="1", precio="1", imagenArchivo=archivo)).isEmpty())
        assertTrue(ProductoValidator.validar(valido.copy(material="x".repeat(101), ancho="0", largo="10001")).keys.containsAll(listOf("material","ancho","largo")))
    }

    @Test fun limitesDeTextosYFechasComerciales() {
        val c = ColeccionFormulario(nombre="x".repeat(101), descripcion="x".repeat(2001))
        assertEquals(setOf("nombre", "descripcion"), ContenidoValidator.coleccion(c).keys)
        val f = CampaniaFormulario(titulo=c.nombre, descripcion=c.descripcion, inicio="01/01/0000", termino="31/12/9999")
        assertEquals(setOf("titulo","descripcion","inicio"), ContenidoValidator.campania(f).keys)
        assertNull(ContenidoValidator.fecha("01/01/+10000"))
        assertEquals(1, ContenidoValidator.fecha("01/01/0001")!!.year)
    }

    @Test fun urlYBasicRechazanConfiguracionesAmbiguas() {
        for (url in listOf("", "https://a@ejemplo.test/", "https://:abc@ejemplo.test/",
            "https://ejemplo.test/ruta", "https://ejemplo.test/?dato", "https://ejemplo.test/#dato",
            "http://ejemplo.test/")) {
            assertThrows(IllegalArgumentException::class.java) { ProductosRemotos.retrofit(url, false) }
        }
        for ((nombre,clave) in listOf("" to "abc", "a:b" to "abc", "persona" to "")) {
            assertThrows(IllegalArgumentException::class.java) { ProductosRemotos.credencial(nombre,clave) }
        }
        val prefs = mock(SharedPreferences::class.java)
        for (url in listOf("", "http://127.0.0.1/")) assertFalse(ProductosRemotos(prefs, { null }, url, url).permiteAdministracion)
    }

    @Test fun mapeoFotosSemillasPreciosYExtremosDeId() = runBlocking {
        val base = ProductosDemo.productos.first()
        val prefs = mock(SharedPreferences::class.java)
        `when`(prefs.getString(anyString(), anyString())).thenAnswer { it.arguments[1] }
        val repo = ProductosRemotos(prefs, { base.copy(imagenArchivo="local.img") })
        fun dato(id: Long) = ProductoRespuesta(id, "Tapiz", "Detalle", "Interiores", "Algodón",
            "10.49".toBigDecimal(), "10.50".toBigDecimal(), "123.50".toBigDecimal(), true, true)
        val producto = repo.producto(dato(base.id.toLong()))
        assertEquals("local.img",producto.imagenArchivo)
        assertEquals(10,producto.anchoCm)
        assertEquals(11,producto.largoCm)
        assertEquals(124,producto.precio)
        assertEquals("124", repo.formulario(producto).precio)
        val nuevo = repo.producto(dato(Int.MAX_VALUE.toLong()))
        assertEquals("", nuevo.imagenArchivo)
        for (id in listOf(0L, Int.MAX_VALUE.toLong()+1))
            assertThrows(IllegalArgumentException::class.java) { dato(id).idAndroid() }
        `when`(prefs.getString(anyString(), anyString())).thenReturn(null)
        val sinFoto = repo.producto(dato(1))
        assertEquals("",sinFoto.imagenNombre)
        assertEquals("",sinFoto.imagenArchivo)
    }
}
