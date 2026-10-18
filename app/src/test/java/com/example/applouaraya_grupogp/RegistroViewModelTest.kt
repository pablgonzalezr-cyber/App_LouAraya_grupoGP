package com.example.applouaraya_grupogp

import com.example.applouaraya_grupogp.model.RegistroUiState
import com.example.applouaraya_grupogp.validation.RegistroValidator
import com.example.applouaraya_grupogp.viewmodel.RegistroViewModel
import org.junit.Assert.*
import org.junit.Test

class RegistroViewModelTest {
    @Test fun formularioInicialNoMuestraErroresNiAutorizaResumen() {
        val vm = RegistroViewModel()
        assertEquals(RegistroUiState(), vm.estado.value)
        assertFalse(vm.validarFormulario())
        val s = vm.estado.value
        assertNotNull(s.errorNombre); assertNotNull(s.errorCorreo); assertNotNull(s.errorClave); assertNotNull(s.errorTerminos)
        assertFalse(s.resumenDisponible)
    }
    @Test fun nombreSeRevalidaEnCadaCambio() {
        val vm = RegistroViewModel(); vm.validarFormulario()
        vm.cambiarNombre(" "); assertNotNull(vm.estado.value.errorNombre)
        vm.cambiarNombre("María José"); assertNull(vm.estado.value.errorNombre)
        vm.cambiarNombre(""); assertNotNull(vm.estado.value.errorNombre)
    }
    @Test fun correoInvalidoSigueConErrorHastaCorregirlo() {
        val vm = RegistroViewModel(); vm.validarFormulario()
        vm.cambiarCorreo("cliente@"); assertNotNull(vm.estado.value.errorCorreo)
        vm.cambiarCorreo("cliente@example.test"); assertNull(vm.estado.value.errorCorreo)
        vm.cambiarCorreo("sin-arroba"); assertNotNull(vm.estado.value.errorCorreo)
        assertNull(RegistroValidator.correo("  cliente@example.test  "))
    }
    @Test fun claveYTerminosSeRevalidanSinOtroEnvio() {
        val vm = RegistroViewModel(); vm.validarFormulario()
        vm.cambiarClave("123"); assertNotNull(vm.estado.value.errorClave)
        vm.cambiarClave("123456"); assertNull(vm.estado.value.errorClave)
        vm.cambiarTerminos(true); assertNull(vm.estado.value.errorTerminos)
        vm.cambiarTerminos(false); assertNotNull(vm.estado.value.errorTerminos)
    }
    @Test fun corregirUnCampoNoBorraErroresDeOtros() {
        val vm = RegistroViewModel(); vm.validarFormulario(); vm.cambiarNombre("Cliente")
        assertNull(vm.estado.value.errorNombre)
        assertNotNull(vm.estado.value.errorCorreo); assertNotNull(vm.estado.value.errorClave); assertNotNull(vm.estado.value.errorTerminos)
    }
    @Test fun resumenRequiereValidarYEditarConservaDatos() {
        val vm = completo()
        assertFalse(vm.estado.value.resumenDisponible)
        assertTrue(vm.validarFormulario())
        val validado = vm.estado.value
        vm.volverAEditar()
        assertEquals(validado.copy(resumenDisponible = false), vm.estado.value)
        assertTrue(vm.validarFormulario())
        vm.cambiarCorreo("invalido")
        assertFalse(vm.estado.value.resumenDisponible); assertFalse(vm.validarFormulario())
    }
    @Test fun limpiarEliminaBorradorIncluidaCredencial() {
        val vm = completo(); vm.validarFormulario(); vm.limpiarFormulario()
        assertEquals(RegistroUiState(), vm.estado.value)
    }
    private fun completo() = RegistroViewModel().apply {
        cambiarNombre("Cliente prueba"); cambiarCorreo("cliente@example.test"); cambiarClave("Clave123!"); cambiarTerminos(true)
    }
}
