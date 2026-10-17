package com.example.applouaraya_grupogp

import com.example.applouaraya_grupogp.validation.PerfilValidator as Validador
import org.junit.Assert.*
import org.junit.Test

class PerfilValidatorTest {
    @Test fun nombreVacioOCortoSeRechaza() { listOf("", " ", "a", " a ").forEach { assertNotNull(Validador.errorNombre(it)) } }
    @Test fun limitesNormalizados() {
        assertNull(Validador.errorNombre(" Ana ")); assertNull(Validador.errorNombre("a".repeat(80)))
        assertNotNull(Validador.errorNombre("a".repeat(81)))
    }
    @Test fun conservaNombresInternacionales() { listOf("María José", "O'Connor", "李明", "Jean-Luc").forEach { assertNull(Validador.errorNombre(it)) } }
    @Test fun normalizaEspaciosYRechazaControles() {
        assertEquals("María José", Validador.normalizarNombre("  María   José  "))
        listOf("Ana\nMaría", "Ana\tMaría", "Ana\u0000").forEach { assertNotNull(Validador.errorNombre(it)) }
    }
}
