package com.example.applouaraya_grupogp

import com.example.applouaraya_grupogp.repository.CatalogoFiltros
import com.example.applouaraya_grupogp.repository.ProductosDemo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogoFiltrosTest {
    private val catalogo = ProductosDemo.productos

    @Test fun buscarPorNombre() {
        val resultado = CatalogoFiltros.aplicar(catalogo, "tapiz", "Todos", false, false)
        assertEquals(1, resultado.size)
        assertEquals("Tapiz Bohemio", resultado.first().nombre)
    }

    @Test fun filtrarPorCategoria() {
        val resultado = CatalogoFiltros.aplicar(catalogo, "", "Matrimonios", false, false)
        assertTrue(resultado.isNotEmpty())
        assertTrue(resultado.all { it.categoria == "Matrimonios" })
    }

    @Test fun excluirNoDisponibles() {
        val resultado = CatalogoFiltros.aplicar(catalogo, "", "Todos", true, false)
        assertTrue(resultado.all { it.disponible })
    }

    @Test fun ordenarPrecioAscendente() {
        val resultado = CatalogoFiltros.aplicar(catalogo, "", "Todos", false, true)
        assertEquals(resultado.map { it.precio }.sorted(), resultado.map { it.precio })
    }
}
