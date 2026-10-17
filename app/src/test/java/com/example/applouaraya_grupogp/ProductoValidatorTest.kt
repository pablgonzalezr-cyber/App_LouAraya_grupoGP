package com.example.applouaraya_grupogp

import com.example.applouaraya_grupogp.validation.*
import org.junit.Assert.*
import org.junit.Test

class ProductoValidatorTest {
    private val valido = ProductoFormulario(nombre = "Tapiz de prueba", descripcion = "Descripción del tapiz artesanal", categoria = "Interiores", material = "Algodón", ancho = "45", largo = "70", precio = "25000")
    @Test fun formularioValido() { assertTrue(ProductoValidator.validar(valido).isEmpty()); assertEquals(25000, valido.producto().precio) }
    @Test fun camposObligatorios() { assertEquals(setOf("nombre","descripcion","categoria","material","precio","ancho","largo"), ProductoValidator.validar(ProductoFormulario()).keys) }
    @Test fun limitesNumericos() {
        listOf("0","-1","1.5","NaN","999999999999999").forEach { valor ->
            assertTrue(ProductoValidator.validar(valido.copy(ancho = valor, largo = valor, precio = valor)).keys.containsAll(listOf("ancho","largo","precio")))
        }
        assertTrue(ProductoValidator.validar(valido.copy(ancho="10000", precio="1000000000")).isEmpty())
    }
    @Test fun textosEImagen() {
        assertTrue(ProductoValidator.validar(valido.copy(nombre="a", descripcion="breve", categoria="inexistente", material="a", imagenArchivo="../externo")).keys.containsAll(listOf("nombre","descripcion","categoria","material","imagen")))
        assertTrue(ProductoValidator.validar(valido.copy(nombre="a".repeat(101), descripcion="a".repeat(2001))).keys.containsAll(listOf("nombre","descripcion")))
    }
}
