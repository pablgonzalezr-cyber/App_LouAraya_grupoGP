package com.example.applouaraya_grupogp

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.applouaraya_grupogp.data.*
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/** Mismo servidor HTTPS descartable y truststore que las pruebas de contrato. */
@OptIn(ExperimentalCoroutinesApi::class)
class CatalogoAdministrativoIntegracionTest {
    private val modelos = mutableListOf<ViewModel>()
    private val propiedades = mutableMapOf<String, String?>()
    private lateinit var productos: ProductosRepository
    private lateinit var contenido: ContenidoRepository
    private val usuario get() = requireNotNull(System.getenv("LOU_ADMIN_USUARIO"))
    private val clave get() = requireNotNull(System.getenv("LOU_ADMIN_CLAVE"))
    private var actor: Long? = 1L

    @Before fun preparar() {
        val url = System.getenv("LOU_PRUEBA_RETROFIT_URL")
        Assume.assumeTrue(url != null && url == BuildConfig.CATALOGO_URL && url == BuildConfig.CATALOGO_ADMIN_URL)
        for ((key, variable) in mapOf("javax.net.ssl.trustStore" to "LOU_PRUEBA_RETROFIT_TRUSTSTORE",
            "javax.net.ssl.trustStorePassword" to "LOU_PRUEBA_RETROFIT_TRUSTSTORE_CLAVE")) {
            propiedades[key] = System.setProperty(key, requireNotNull(System.getenv(variable)))
        }
        Dispatchers.setMain(Dispatchers.Unconfined)
        val db = mock(AppDatabase::class.java)
        val usuarios = mock(UsuarioDao::class.java)
        `when`(db.usuarioDao()).thenReturn(usuarios)
        `when`(db.avisoDao()).thenReturn(mock(AvisoDao::class.java))
        runBlocking {
            `when`(usuarios.buscarPorId(1L)).thenReturn(UsuarioEntity(1L, "Prueba", "local@example.test", "", "", "ADMINISTRADOR"))
        }
        val prefs = mock(SharedPreferences::class.java)
        val editor = mock(SharedPreferences.Editor::class.java, RETURNS_SELF)
        `when`(prefs.edit()).thenReturn(editor)
        `when`(prefs.getString(anyString(), anyString())).thenAnswer { it.arguments[1] }
        productos = ProductosRepository(db, ProductosRemotos(prefs, { null })) { actor }
        contenido = ContenidoRepository(db, productos)
    }

    @After fun cerrar() {
        modelos.forEach { it.viewModelScope.cancel() }
        if (propiedades.isNotEmpty()) Dispatchers.resetMain()
        propiedades.forEach { (key, valor) ->
            if (valor == null) System.clearProperty(key) else System.setProperty(key, valor)
        }
    }

    private suspend fun <T> StateFlow<T>.esperar(listo: (T) -> Boolean): T =
        withTimeout(15000) { first(listo) }

    private suspend fun AdminProductosViewModel.fin() = estado.esperar { !it.cargando && !it.guardando }
    private suspend fun AdminContenidoViewModel.fin() = estado.esperar { !it.cargando && !it.guardando }

    @Test fun productoConDecimalesSePublicaOcultaYArchiva() = runBlocking {
        val vm = AdminProductosViewModel(productos).also(modelos::add)
        vm.conectar(usuario, UUID.randomUUID().toString())
        assertTrue(vm.fin().error.orEmpty().contains("401"))
        vm.conectar(usuario, clave)
        assertTrue(vm.fin().conectado)
        vm.editar()
        vm.guardar()
        assertTrue(vm.estado.value.errores.containsKey("precio"))
        val nombre = "Tapiz " + UUID.randomUUID()
        vm.cambiar(ProductoFormulario(nombre = nombre, descripcion = "Producto para prueba HTTPS",
            categoria = "Interiores", material = "Algodón", ancho = "15,75", largo = "31,2", precio = "1234,56"))
        vm.guardar()
        val nuevo = vm.fin().productos.single { it.nombre == nombre }
        assertEquals(1235, nuevo.precio)
        assertEquals(16, nuevo.anchoCm)
        vm.editar(nuevo)
        val form = vm.estado.value.formulario
        assertEquals("1234.56", form.precio)
        assertEquals("15.75", form.ancho)
        vm.cambiar(form.copy(precio = "999.01"))
        vm.guardar()
        assertEquals(999, vm.fin().productos.single { it.id == nuevo.id }.precio)
        vm.publicar(nuevo.id, false)
        assertFalse(vm.fin().productos.single { it.id == nuevo.id }.activo)
        assertFalse(productos.observarPublicados().first().any { it.id == nuevo.id })
        vm.publicar(nuevo.id, true)
        assertTrue(vm.fin().productos.single { it.id == nuevo.id }.activo)
        assertEquals(nombre, productos.observarDetalle(nuevo.id).first().nombre)
        vm.desactivar(nuevo.id)
        assertFalse(vm.fin().productos.any { it.id == nuevo.id })
        vm.publicar(nuevo.id, true)
        assertTrue(vm.fin().error.orEmpty().contains("404"))
        vm.desconectar()
        assertFalse(vm.estado.value.conectado)
    }

    @Test fun contenidoYVisibilidadTrasCadaOperacion() = runBlocking {
        val vm = AdminContenidoViewModel(contenido).also(modelos::add)
        val publico = NovedadesViewModel(contenido).also(modelos::add)
        vm.cargar()
        assertFalse(vm.estado.value.conectado)
        vm.conectar(usuario, UUID.randomUUID().toString())
        assertTrue(vm.fin().error.orEmpty().contains("401"))
        vm.conectar(usuario, clave)
        assertTrue(vm.fin().conectado)
        val nombre = "Colección " + UUID.randomUUID()
        vm.nueva()
        vm.guardar()
        assertTrue(vm.estado.value.errores.containsKey("nombre"))
        vm.cambiar(ColeccionFormulario(nombre = nombre, descripcion = "Piezas de algodón", productos = setOf(1), publicada = true))
        vm.guardar()
        val coleccion = vm.fin().datos.colecciones.single { it.nombre == nombre }
        val vista = publico.estado.esperar { s -> s.datos.colecciones.any { it.coleccion.id == coleccion.id } }
        assertEquals(listOf(1), vista.datos.colecciones.single { it.coleccion.id == coleccion.id }.productos.map { it.id })
        vm.editar(coleccion)
        assertEquals(setOf(1), vm.estado.value.coleccion.productos)
        vm.cambiar(vm.estado.value.coleccion.copy(productos = setOf(2, 3)))
        vm.guardar()
        assertEquals(setOf(2, 3), vm.fin().datos.miembros.filter { it.coleccionId == coleccion.id }.map { it.productoId }.toSet())
        vm.publicar(coleccion.id, false, false)
        assertFalse(vm.fin().datos.colecciones.single { it.id == coleccion.id }.publicada)
        vm.desactivar(coleccion.id, false)
        assertFalse(vm.fin().datos.colecciones.single { it.id == coleccion.id }.activa)
        vm.seccion(true)
        vm.nueva()
        vm.guardar()
        assertTrue(vm.estado.value.errores.containsKey("inicio"))
        val hoy = LocalDate.now(java.time.ZoneId.of("America/Santiago"))
        val formato = DateTimeFormatter.ofPattern("dd/MM/uuuu")
        val camp = CampaniaFormulario(titulo = nombre, descripcion = "Campaña de prueba",
            inicio = hoy.minusDays(1).format(formato), termino = hoy.plusDays(2).format(formato))
        vm.cambiar(camp)
        vm.guardar()
        val creada = vm.fin().datos.campanias.single { it.titulo == nombre }
        vm.editar(creada)
        assertEquals(camp.inicio, vm.estado.value.campania.inicio)
        vm.cambiar(vm.estado.value.campania.copy(publicada = true))
        vm.guardar()
        assertTrue(vm.fin().datos.campanias.single { it.id == creada.id }.publicada)
        publico.estado.esperar { s -> s.datos.campanias.any { it.id == creada.id } }
        contenido.remoto!!.comprobarCampania(creada.id)
        vm.publicar(creada.id, true, false)
        assertFalse(vm.fin().datos.campanias.single { it.id == creada.id }.publicada)
        vm.desactivar(creada.id, true)
        assertFalse(vm.fin().datos.campanias.single { it.id == creada.id }.activa)
        publico.estado.esperar { s -> s.datos.campanias.none { it.id == creada.id } }
        vm.desactivar(Long.MAX_VALUE, true)
        assertTrue(vm.fin().error.orEmpty().contains("404"))
        actor = null
        vm.cargar()
        assertFalse(vm.fin().conectado)
        assertTrue(vm.estado.value.datos.productos.isEmpty())
        vm.desconectar()
        assertNull(vm.estado.value.error)
    }
}
