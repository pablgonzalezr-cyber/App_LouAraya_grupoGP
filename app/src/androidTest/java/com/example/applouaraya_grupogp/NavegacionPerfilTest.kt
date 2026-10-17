package com.example.applouaraya_grupogp

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.applouaraya_grupogp.data.AppDatabase
import com.example.applouaraya_grupogp.navigation.AppNavigation
import com.example.applouaraya_grupogp.repository.PreferenciasRepository
import com.example.applouaraya_grupogp.repository.UsuarioRepository
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class NavegacionPerfilTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var db: AppDatabase
    private lateinit var nav: NavHostController
    private val preferenciasArchivo = "prueba-navegacion-${UUID.randomUUID()}"
    private val contexto get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before fun preparar() {
        db = Room.inMemoryDatabaseBuilder(contexto, AppDatabase::class.java).build()
        runBlocking { UsuarioRepository(db.usuarioDao()).registrar("Cliente UI", "ui@example.test", "Clave123!") }
        val prefs = PreferenciasRepository(contexto.getSharedPreferences(preferenciasArchivo, Context.MODE_PRIVATE))
        compose.setContent {
            nav = rememberNavController()
            MaterialTheme { AppNavigation(basePrueba = db, preferenciasPrueba = prefs, controlador = nav) }
        }
    }
    @After fun cerrar() {
        compose.activityRule.scenario.close()
        db.close()
        contexto.deleteSharedPreferences(preferenciasArchivo)
    }
    private fun esperar(texto: String) {
        compose.waitUntil(10000) { compose.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun entrar(correo: String, clave: String) {
        esperar("Iniciar sesión")
        compose.onNodeWithText("Correo electrónico").performTextInput(correo)
        compose.onNodeWithText("Contraseña").performTextInput(clave)
        compose.onNodeWithText("Ingresar").performClick()
    }
    private fun perfil() {
        compose.onNodeWithText("Perfil y configuración").performScrollTo().performClick()
        esperar("Mi perfil")
    }
    @Test fun editarPerfilCambiarCuentaYVolverConRolesCorrectos() {
        compose.onNodeWithText("Comenzar").performClick()
        entrar("ui@example.test", "Clave123!")
        esperar("¡Bienvenido, Cliente UI!")
        perfil()
        esperar("Correo: ui@example.test")
        compose.onNodeWithText("Nombre").performTextClearance()
        compose.onNodeWithText("Nombre").performTextInput("Cliente Editado")
        compose.onNodeWithText("Guardar nombre").performScrollTo().performClick()
        esperar("Nombre guardado")
        compose.onNodeWithText("← Inicio").performScrollTo().performClick()
        esperar("¡Bienvenido, Cliente Editado!")
        perfil()
        compose.onNodeWithText("Cambiar de usuario").performScrollTo().performClick()
        compose.onNodeWithText("Confirmar").performClick()
        entrar(UsuarioRepository.CORREO_DEMO, UsuarioRepository.CLAVE_DEMO)
        esperar("Administración Lou Araya")
        compose.onNodeWithText("Mis solicitudes").assertDoesNotExist()
        perfil()
        esperar("Rol: ADMINISTRADOR")
        compose.onNodeWithText("Cambiar de usuario").performScrollTo().performClick()
        compose.onNodeWithText("Confirmar").performClick()
        entrar("ui@example.test", "Clave123!")
        esperar("¡Bienvenido, Cliente Editado!")
        compose.onNodeWithText("Gestionar solicitudes").assertDoesNotExist()
    }
    @Test fun salirLimpiaPilaYBotonAtrasNoRecuperaPerfil() {
        compose.onNodeWithText("Comenzar").performClick()
        entrar("ui@example.test", "Clave123!")
        esperar("¡Bienvenido, Cliente UI!")
        perfil()
        compose.onNodeWithText("Cerrar sesión").performScrollTo().performClick()
        compose.onNodeWithText("Confirmar").performClick()
        esperar("Comenzar")
        compose.onNodeWithText("Comenzar").assertIsDisplayed()
        comprobarAusenciaDePantallasPrivadas()
        compose.runOnIdle {
            assertEquals("bienvenida", nav.currentDestination?.route)
            assertNull(nav.previousBackStackEntry)
        }

        // Acceso directo con la sesión ya cerrada: debe rechazarse cada destino,
        // sin montar sus pantallas ni recuperar una sesión anterior.
        listOf("perfil", "cotizacion", "solicitudes", "solicitud/1", "admin", "admin/solicitudes", "admin/solicitud/1", "admin/productos", "admin/contenido", "novedades", "producto/1").forEach { ruta ->
            compose.runOnUiThread { nav.navigate(ruta) }
            esperar("Iniciar sesión")
            compose.onNodeWithText("Iniciar sesión").assertIsDisplayed()
            comprobarAusenciaDePantallasPrivadas()
            compose.runOnIdle {
                assertEquals("login", nav.currentDestination?.route)
                assertNull(nav.previousBackStackEntry)
            }
        }

        // Volver a la raíz pública para probar Atrás. Se comprueba la pila, no
        // se exige que Android destruya la actividad al abandonar esa raíz.
        compose.runOnUiThread {
            nav.navigate("bienvenida") { popUpTo(nav.graph.id) { inclusive = true }; launchSingleTop = true }
        }
        esperar("Comenzar")
        comprobarAusenciaDePantallasPrivadas()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            // Si el host se cierra, puede no quedar destino; si permanece, solo
            // puede conservar Bienvenida. Ningún destino privado puede reaparecer.
            assertTrue(nav.currentDestination?.route in listOf(null, "bienvenida"))
            assertNull(nav.previousBackStackEntry)
        }
    }

    private fun comprobarAusenciaDePantallasPrivadas() {
        listOf("Mi perfil", "Perfil y configuración", "Cotización personalizada", "Mis solicitudes",
            "Detalle de solicitud", "Administración Lou Araya", "Todas las solicitudes", "Gestionar solicitud #1",
            "Administrar productos", "Administrar colecciones", "Colecciones y campañas",
            "¡Bienvenido, Cliente UI!").forEach { texto -> compose.onNodeWithText(texto).assertDoesNotExist() }
    }
}
