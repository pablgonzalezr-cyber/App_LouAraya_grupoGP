package com.example.applouaraya_grupogp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.applouaraya_grupogp.model.*
import com.example.applouaraya_grupogp.repository.*
import com.example.applouaraya_grupogp.validation.*
import com.example.applouaraya_grupogp.viewmodel.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.mockito.Mockito.*

@OptIn(ExperimentalCoroutinesApi::class)
class EstadosViewModelTest {
    private val principal = StandardTestDispatcher()
    private val modelos = mutableListOf<ViewModel>()
    private val producto = ProductosDemo.productos.first()
    @Before fun iniciar() = Dispatchers.setMain(principal)
    @After fun cerrar() {
        modelos.forEach { it.viewModelScope.cancel() }
        principal.scheduler.runCurrent()
        Dispatchers.resetMain()
    }
    private fun completar() = principal.scheduler.runCurrent()

    @Test fun productoBloqueaEdicionDuranteGuardadoYExplicaFalloDeRecarga() = runTest {
        val repo = mock(ProductosRepository::class.java)
        val remoto = mock(ProductosRemotos::class.java)
        `when`(repo.remoto).thenReturn(remoto)
        `when`(remoto.permiteAdministracion).thenReturn(true)
        val cuenta = ProductosRemotos.credencial("usuario","temporal")
        `when`(remoto.administracion(cuenta)).thenReturn(listOf(producto)).thenThrow(IllegalStateException("red interrumpida"))
        val vm = AdminProductosViewModel(repo).also(modelos::add)
        vm.conectar("usuario","temporal")
        completar()
        assertEquals(listOf(producto),vm.estado.value.productos)
        vm.editar()
        val form = ProductoFormulario.desde(producto)
        vm.cambiar(form)
        vm.guardar()
        vm.cargar()
        vm.conectar("otro","otra")
        vm.desconectar()
        vm.editar()
        vm.cambiar(ProductoFormulario())
        vm.cancelar()
        vm.desactivar(producto.id)
        assertTrue(vm.estado.value.guardando && vm.estado.value.editando)
        assertEquals(form, vm.estado.value.formulario)
        completar()
        verify(remoto).guardar(cuenta,form)
        verify(remoto,never()).archivar(cuenta,producto.id)
        val fin = vm.estado.value
        assertFalse(fin.guardando || fin.conectado)
        assertTrue(fin.productos.isEmpty())
        assertTrue(fin.error.orEmpty().contains("confirmada"))
        assertEquals("Producto guardado",fin.mensaje)
    }

    @Test fun contenidoDistingueRechazoDeCambioConfirmadoYDescartaDobleClic() = runTest {
        val repo = mock(ContenidoRepository::class.java)
        val remoto = mock(ContenidoRemoto::class.java)
        `when`(repo.remoto).thenReturn(remoto)
        val cuenta = ProductosRemotos.credencial("usuario","temporal")
        val datos = ContenidoAdminDatos(emptyList(),emptyList(),emptyList(),listOf(producto))
        `when`(repo.administracionRemota(cuenta)).thenReturn(datos).thenThrow(IllegalStateException("sin conexión"))
        val vm = AdminContenidoViewModel(repo).also(modelos::add)
        vm.conectar("usuario","temporal")
        vm.desactivar(1,false)
        completar()
        verify(remoto,never()).desactivar(cuenta,1,false)
        val form = ColeccionFormulario(nombre="Colección", descripcion="Piezas para casa", productos=setOf(1))
        vm.nueva()
        vm.cambiar(form)
        vm.guardar()
        vm.cargar()
        vm.seccion(true)
        vm.nueva()
        vm.cancelar()
        vm.cambiar(CampaniaFormulario(titulo="Ignorar"))
        vm.cambiar(ColeccionFormulario())
        vm.guardar()
        val ocupado = vm.estado.value
        assertTrue(ocupado.guardando && ocupado.editando)
        assertFalse(ocupado.campanias)
        assertEquals(form, ocupado.coleccion)
        completar()
        verify(remoto,times(1)).guardar(cuenta,form)
        assertTrue(vm.estado.value.error.orEmpty().contains("guardó el cambio"))
        `when`(remoto.desactivar(cuenta,999,true)).thenThrow(IllegalStateException())
        vm.desactivar(999,true)
        completar()
        assertNull(vm.estado.value.mensaje)
        assertEquals("No se pudo guardar",vm.estado.value.error)
    }

    @Test fun catalogoYDetalleRecuperanLosDatosTrasError() {
        val repo = mock(ProductosRepository::class.java)
        `when`(repo.observarPublicados()).thenReturn(flow { throw IllegalStateException() }, flowOf(listOf(producto)))
        `when`(repo.observarDetalle(producto.id)).thenReturn(flow { throw IllegalStateException("404") }, flowOf(producto))
        val vm = CatalogoViewModel(repo).also(modelos::add)
        completar()
        assertEquals("No se pudo cargar el catálogo",vm.estado.value.error)
        vm.cargar()
        completar()
        assertNull(vm.estado.value.error)
        assertEquals(listOf(producto),vm.estado.value.productos)
        vm.cargarDetalle(producto.id)
        completar()
        assertEquals("404",vm.detalle.value.error)
        vm.cargarDetalle(producto.id)
        completar()
        assertEquals(producto,vm.detalle.value.producto)
        vm.cargarDetalle(null)
        assertNull(vm.detalle.value.producto)
    }

    @Test fun preferenciasNoFiltranUnGuardadoEntreSesiones() = runTest {
        val repo = mock(PreferenciasRepository::class.java)
        val sesion = MutableStateFlow(SesionUiState())
        val preferencias = PreferenciasLocales(mostrarAyuda=false)
        val vm = ConfiguracionViewModel(repo,sesion).also(modelos::add)
        vm.guardar(preferencias)
        completar()
        verifyNoInteractions(repo)
        `when`(repo.observar(1)).thenReturn(flowOf(PreferenciasLocales()))
        `when`(repo.observar(2)).thenReturn(flowOf(PreferenciasLocales()))
        sesion.value = SesionUiState(usuarioId=1)
        completar()
        vm.guardar(preferencias)
        vm.guardar(preferencias)
        sesion.value = SesionUiState(usuarioId=2)
        completar()
        verify(repo,times(1)).guardar(1,preferencias)
        assertTrue(vm.estado.value.preferencias.mostrarAyuda)
        `when`(repo.guardar(2,preferencias)).thenThrow(IllegalStateException())
        vm.guardar(preferencias)
        completar()
        assertEquals("No se pudo guardar",vm.estado.value.error)
        assertFalse(vm.estado.value.guardando)
        sesion.value = SesionUiState()
        completar()
        assertNull(vm.estado.value.error)
    }

    @Test fun avisosSoloNaveganSiLaCuentaSigueActiva() = runTest {
        val repo = mock(AvisosRepository::class.java)
        val prefs = mock(PreferenciasRepository::class.java)
        val sesion = MutableStateFlow(SesionUiState())
        val vm = AvisosViewModel(repo,prefs,sesion).also(modelos::add)
        var destino: String? = null
        vm.abrir(1) { destino=it }
        completar()
        verifyNoInteractions(repo)
        `when`(repo.observar()).thenReturn(flowOf(emptyList()), flow { throw IllegalStateException() })
        `when`(prefs.observar(1)).thenReturn(flowOf(PreferenciasLocales()))
        `when`(prefs.observar(2)).thenReturn(flowOf(PreferenciasLocales()))
        `when`(repo.destino(1)).thenReturn("solicitud/1")
        sesion.value = SesionUiState(usuarioId=1)
        vm.abrir(1) { destino=it }
        completar()
        assertNull(destino)
        vm.abrir(1) { destino=it }
        vm.leida(1)
        sesion.value = SesionUiState(usuarioId=2)
        completar()
        verify(repo).destino(1)
        verify(repo,never()).marcarLeida(1)
        assertNull(destino)
        assertEquals(2L,vm.estado.value.usuarioId)
        assertEquals("No se pudieron cargar tus avisos",vm.estado.value.error)
        `when`(repo.destino(2)).thenThrow(IllegalStateException())
        vm.abrir(2) { destino=it }
        completar()
        assertEquals("No se pudo completar la acción",vm.estado.value.error)
        assertNull(destino)
    }
}
