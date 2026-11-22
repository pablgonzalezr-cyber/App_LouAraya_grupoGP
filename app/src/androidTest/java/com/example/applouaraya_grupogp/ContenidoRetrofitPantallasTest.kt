package com.example.applouaraya_grupogp

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.applouaraya_grupogp.data.AppDatabase
import com.example.applouaraya_grupogp.repository.*
import com.example.applouaraya_grupogp.ui.screens.*
import com.example.applouaraya_grupogp.viewmodel.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

/** Pruebas de lectura contra la demo HTTPS. La sesión y los avisos usan Room en memoria. */
class ContenidoRetrofitPantallasTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val args get() = InstrumentationRegistry.getArguments()
    private lateinit var db: AppDatabase
    private lateinit var repo: ContenidoRepository

    @Before fun preparar() {
        Assume.assumeTrue(args.getString("probarCatalogoHttps") == "true")
        val context = compose.activity.applicationContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val admin = runBlocking {
            UsuarioRepository(db.usuarioDao()).iniciarSesion(UsuarioRepository.CORREO_DEMO, UsuarioRepository.CLAVE_DEMO)!!
        }
        val remoto = ProductosRemotos(context.getSharedPreferences("lou_catalogo_fotos", 0), db.productoDao()::buscar)
        repo = ContenidoRepository(db, ProductosRepository(db, remoto) { admin.id })
    }

    @After fun cerrar() { if (::db.isInitialized) db.close() }

    @Test fun novedadesPublicasSeActualizan() {
        val vm = compose.runOnIdle {
            ViewModelProvider(compose.activity, viewModelFactory { initializer { NovedadesViewModel(repo) } })[NovedadesViewModel::class.java]
        }
        compose.setContent { NovedadesScreen(vm, {}, {}) }
        compose.waitUntil(20000) { !vm.estado.value.cargando }
        assertNull(vm.estado.value.error)
        compose.onNodeWithText("Actualizar").performClick()
        compose.waitUntil(20000) { !vm.estado.value.cargando }
        assertNull(vm.estado.value.error)
        compose.onNodeWithText("Colecciones y campañas").assertIsDisplayed()
    }

    @Test fun panelAutenticaYConsultaAmbasSecciones() {
        val usuario = args.getString("cuentaCatalogo")
        val clave = args.getString("claveCatalogo")
        Assume.assumeTrue(usuario != null && clave != null)
        val vm = compose.runOnIdle {
            ViewModelProvider(compose.activity, viewModelFactory { initializer { AdminContenidoViewModel(repo) } })[AdminContenidoViewModel::class.java]
        }
        compose.setContent { AdminContenidoScreen(vm, {}) }
        compose.onNodeWithText("Usuario del servidor").performTextInput(requireNotNull(usuario))
        compose.onNodeWithText("Contraseña del servidor").performTextInput(requireNotNull(clave))
        compose.onNodeWithText("Conectar al catálogo").performScrollTo().performClick()
        compose.waitUntil(20000) { vm.estado.value.conectado || vm.estado.value.error != null }
        assertNull(vm.estado.value.error)
        compose.onNodeWithText("Crear colección").assertExists()
        compose.onNodeWithText("Campañas").performClick()
        compose.onNodeWithText("Crear campaña").assertExists()
        assertTrue(vm.estado.value.datos.productos.isNotEmpty())
    }
}
