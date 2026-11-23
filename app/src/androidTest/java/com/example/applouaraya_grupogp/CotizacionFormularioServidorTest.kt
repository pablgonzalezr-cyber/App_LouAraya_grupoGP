package com.example.applouaraya_grupogp

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.test.platform.app.InstrumentationRegistry
import com.example.applouaraya_grupogp.model.CotizacionFormulario
import com.example.applouaraya_grupogp.repository.CotizacionesRemotasRepository
import com.example.applouaraya_grupogp.viewmodel.CotizacionViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import java.util.UUID

class CotizacionFormularioServidorTest {
    @Test fun restauraBorradorYBloqueaReenviosHastaConfirmar() = runBlocking {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("h2Aislada") == "true")
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        val saved = SavedStateHandle()
        val vm = CotizacionViewModel(app, saved)
        val correo = "form-${UUID.randomUUID()}@example.test"
        val clave = UUID.randomUUID().toString()
        val repo = CotizacionesRemotasRepository()
        try {
            withContext(Dispatchers.Main) {
                vm.guardar(0)
                assertTrue(vm.estado.value.errores.isNotEmpty())
                vm.enviarRemoto(correo, clave)
                assertTrue(vm.estado.value.errores.containsKey("material"))
                vm.registrarCuentaRemota(" ",clave)
                assertNotNull(vm.estado.value.error)
                vm.registrarCuentaRemota(correo,"")
                assertNotNull(vm.estado.value.error)
                vm.registrarCuentaRemota(correo,clave)
                assertTrue(vm.estado.value.registrandoRemoto)
                vm.cambiar(CotizacionFormulario(material="Ignorado"))
                vm.enviarRemoto(correo,clave)
                vm.guardar(0)
                vm.registrarCuentaRemota(correo,clave)
                assertEquals("",vm.estado.value.formulario.material)
            }
            withTimeout(10000) { vm.estado.first { it.mensajeRemoto != null } }
            vm.registrarCuentaRemota(correo,clave)
            withTimeout(10000) { vm.estado.first { !it.registrandoRemoto && it.error != null } }
            val ref = withTimeout(10000) { vm.estado.first { it.productosReferencia.isNotEmpty() } }.productosReferencia.first()
            val f = CotizacionFormulario("Tapiz de prueba con referencia", "Interiores","Algodón","30","45","2","31/12/2099",ref.id)
            withContext(Dispatchers.Main) {
                vm.cambiar(f)
                vm.enviarRemoto("",clave)
                assertNotNull(vm.estado.value.error)
                vm.enviarRemoto(correo,"")
                assertNotNull(vm.estado.value.error)
                vm.guardar(0)
            }
            withTimeout(10000) { vm.estado.first { !it.guardando && it.error != null } }
            vm.enviarRemoto(correo,UUID.randomUUID().toString())
            withTimeout(10000) { vm.estado.first { !it.enviandoRemoto && it.error != null } }
            assertNull(vm.estado.value.guardadaRemotaId)
            val restaurado = CotizacionViewModel(app,saved)
            try {
                assertEquals(f,restaurado.estado.value.formulario)
                withTimeout(10000) { restaurado.estado.first { it.productosReferencia.any { p -> p.id == ref.id } } }
                withContext(Dispatchers.Main) {
                    restaurado.enviarRemoto(correo,clave)
                    assertTrue(restaurado.estado.value.enviandoRemoto)
                    restaurado.cambiar(f.copy(material="Ignorado"))
                    restaurado.guardar(0)
                    restaurado.registrarCuentaRemota(correo,clave)
                    restaurado.enviarRemoto(correo,clave)
                }
                val confirmado = withTimeout(10000) { restaurado.estado.first { it.guardadaRemotaId != null } }
                restaurado.enviarRemoto(correo,clave)
                restaurado.guardar(0)
                restaurado.cambiar(f.copy(cantidad="3"))
                assertEquals(confirmado.formulario,restaurado.estado.value.formulario)
                val cuenta = repo.iniciarSesionCliente(correo,clave)
                val solicitud = repo.historial(cuenta).single()
                assertEquals(confirmado.guardadaRemotaId,solicitud.id)
                assertEquals(ref.nombre,solicitud.productoReferenciaNombre)
                assertEquals("2099-12-31",solicitud.fechaRequerida)
                assertEquals(saved.get<String>("tokenEnvio"),solicitud.tokenEnvio)
            } finally { restaurado.viewModelScope.cancel() }
        } finally { vm.viewModelScope.cancel() }
    }
}
