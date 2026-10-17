package com.example.applouaraya_grupogp

import com.example.applouaraya_grupogp.model.CotizacionFormulario
import com.example.applouaraya_grupogp.validation.CotizacionValidator
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class CotizacionValidatorTest {
    private val hoy = LocalDate.of(2026, 10, 17)
    private val valido = CotizacionFormulario("Tapiz personalizado para living", "Interiores", "Algodón", "45,5", "70", "2")
    private fun validar(f: CotizacionFormulario) = CotizacionValidator.validar(f, setOf(1, 2), hoy)

    @Test fun formularioValidoSinOpcionales() { assertTrue(validar(valido).isEmpty()) }
    @Test fun formularioVacioMuestraObligatorios() {
        val errores = validar(CotizacionFormulario(cantidad = ""))
        assertEquals(setOf("descripcion", "categoria", "material", "ancho", "largo", "cantidad"), errores.keys)
    }
    @Test fun descripcionYMaterialRespetanLimites() {
        assertTrue(validar(valido.copy(descripcion = "  breve  ", material = " ")).keys.containsAll(listOf("descripcion", "material")))
        assertTrue(validar(valido.copy(descripcion = "x".repeat(1001), material = "x".repeat(101))).keys.containsAll(listOf("descripcion", "material")))
        assertTrue(validar(valido.copy(descripcion = "x".repeat(1000), material = "ab")).isEmpty())
    }
    @Test fun medidasInvalidasNoSeAceptan() {
        listOf("", "texto", "0", "-1", "NaN", "Infinity", "1e309", "10001").forEach {
            assertTrue("Medida: $it", validar(valido.copy(ancho = it, largo = it)).keys.containsAll(listOf("ancho", "largo")))
        }
    }
    @Test fun medidasDecimalesConPuntoYComa() {
        assertTrue(validar(valido.copy(ancho = "0.1", largo = "10000")).isEmpty())
        assertEquals(45.5, CotizacionValidator.medida("45,5")!!, 0.001)
    }
    @Test fun cantidadDebeSerEnteraPositivaAcotada() {
        listOf("0", "-1", "1.5", "1001", "999999999999", "abc").forEach {
            assertTrue(validar(valido.copy(cantidad = it)).containsKey("cantidad"))
        }
        assertTrue(validar(valido.copy(cantidad = "1000")).isEmpty())
    }
    @Test fun fechasInvalidasYPasadasSeRechazan() {
        listOf("16/10/2026", "31/02/2027", "29/02/2027", "2026-10-20", "texto").forEach {
            assertTrue("Fecha: $it", validar(valido.copy(fecha = it)).containsKey("fecha"))
        }
    }
    @Test fun fechaHoyFuturaYBisiestaSonValidas() {
        listOf("17/10/2026", "20/10/2026", "29/02/2028", " ").forEach {
            assertTrue(validar(valido.copy(fecha = it)).isEmpty())
        }
    }
    @Test fun referenciaDebeExistirYCategoriaSerConocida() {
        assertTrue(validar(valido.copy(productoId = 1)).isEmpty())
        assertTrue(validar(valido.copy(productoId = 99)).containsKey("producto"))
        assertTrue(validar(valido.copy(categoria = "Inventada")).containsKey("categoria"))
    }
}
