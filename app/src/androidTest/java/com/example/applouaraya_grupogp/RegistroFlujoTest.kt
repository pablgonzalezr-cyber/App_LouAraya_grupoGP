package com.example.applouaraya_grupogp

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.applouaraya_grupogp.data.AppDatabase
import com.example.applouaraya_grupogp.navigation.AppNavigation
import com.example.applouaraya_grupogp.repository.*
import com.example.applouaraya_grupogp.viewmodel.RegistroViewModel
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import java.util.UUID

class RegistroFlujoTest {
    private val compose = createAndroidComposeRule<ComponentActivity>()
    private val contexto get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: AppDatabase
    private lateinit var nav: NavHostController
    private val archivo = "prueba-flujo-registro-${UUID.randomUUID()}"
    private val recursos = object: ExternalResource() {
        override fun before() { db = Room.inMemoryDatabaseBuilder(contexto, AppDatabase::class.java).build() }
        override fun after() { db.close(); contexto.deleteSharedPreferences(archivo) }
    }
    @get:Rule val reglas: RuleChain = RuleChain.outerRule(recursos).around(compose)
    private fun iniciar() {
        val prefs = PreferenciasRepository(contexto.getSharedPreferences(archivo, Context.MODE_PRIVATE))
        compose.setContent {
            nav = rememberNavController()
            MaterialTheme { AppNavigation(basePrueba = db, preferenciasPrueba = prefs, controlador = nav) }
        }
    }
    private fun esperar(texto: String) {
        compose.waitUntil(15000) { compose.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun formulario(correo: String) {
        compose.onNodeWithText("Crear una cuenta").performClick()
        compose.onNodeWithText("Nombre").performTextInput("Cliente resumen")
        compose.onNodeWithText("Correo ficticio").performTextInput(correo)
        compose.onNodeWithText("Contraseña de prueba").performTextInput("ClaveResumen123!")
        compose.onNode(isToggleable()).performScrollTo().performClick()
    }
    private fun continuar() {
        compose.onNodeWithText("Continuar").performScrollTo().performClick()
        esperar("Resumen de registro")
    }
    @Test fun resumenConservaMismoBorradorEditaYGuardaSoloAlConfirmar() {
        iniciar(); formulario("resumen@example.test"); continuar()
        compose.onNodeWithText("Nombre: Cliente resumen").assertExists()
        compose.onNodeWithText("Correo: resumen@example.test").assertExists()
        compose.onNodeWithText("Términos y condiciones: Aceptados").assertExists()
        compose.onNodeWithText("ClaveResumen123!").assertDoesNotExist()
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        runBlocking { assertNull(db.usuarioDao().buscarPorCorreo("resumen@example.test")) }
        compose.runOnIdle {
            assertEquals("resumen", nav.currentDestination?.route)
            assertFalse(nav.currentBackStackEntry?.arguments?.containsKey("clave") == true)
        }
        compose.onNodeWithText("Volver a editar").performScrollTo().performClick()
        esperar("Crear cuenta")
        compose.onNodeWithText("Nombre").assertTextContains("Cliente resumen")
        compose.onNodeWithText("Correo ficticio").assertTextContains("resumen@example.test")
        compose.runOnIdle {
            val vm = ViewModelProvider(compose.activity)[RegistroViewModel::class.java]
            assertEquals("ClaveResumen123!", vm.estado.value.clave)
            assertTrue(vm.estado.value.aceptaTerminos)
        }
        compose.onNodeWithText("Nombre").performTextClearance()
        compose.onNodeWithText("Ingresa tu nombre").assertExists()
        compose.onNodeWithText("Nombre").performTextInput("Cliente editado")
        compose.onNodeWithText("Ingresa tu nombre").assertDoesNotExist()
        continuar()
        compose.onNodeWithText("Nombre: Cliente editado").assertExists()
        compose.onNodeWithText("Confirmar registro").performScrollTo().performClick()
        esperar("¡Bienvenido, Cliente editado!")
        runBlocking { assertNotNull(db.usuarioDao().buscarPorCorreo("resumen@example.test")) }
        compose.runOnIdle {
            assertEquals("dashboard", nav.currentDestination?.route)
            assertNull(nav.previousBackStackEntry)
            assertEquals("", ViewModelProvider(compose.activity)[RegistroViewModel::class.java].estado.value.clave)
        }
        // Volver al resumen por acceso directo después del éxito tampoco debe
        // reconstruir el borrador ni permitir otra confirmación.
        compose.runOnUiThread { nav.navigate("resumen") }
        compose.waitUntil(10000) { nav.currentDestination?.route == "dashboard" }
        compose.onNodeWithText("Confirmar registro").assertDoesNotExist()
    }
    @Test fun correoDuplicadoPermiteEditarYConfirmarOtroSinModificarCuentaPrevia() {
        val existente = runBlocking { UsuarioRepository(db.usuarioDao()).registrar("Cuenta previa", "duplicado@example.test", "Previa123!") }
        iniciar(); formulario("duplicado@example.test"); continuar()
        compose.onNodeWithText("Confirmar registro").performScrollTo().performClick()
        esperar("Este correo ya está registrado")
        compose.onNodeWithText("Confirmar registro").assertIsEnabled()
        compose.onNodeWithText("Volver a editar").performScrollTo().performClick()
        esperar("Crear cuenta")
        compose.onNodeWithText("Correo ficticio").performTextClearance()
        compose.onNodeWithText("Correo ficticio").performTextInput("corregido@example.test")
        compose.onNodeWithText("Este correo ya está registrado").assertDoesNotExist()
        continuar()
        compose.onNodeWithText("Confirmar registro").performScrollTo().performClick()
        esperar("¡Bienvenido, Cliente resumen!")
        runBlocking {
            assertNotNull(db.usuarioDao().buscarPorCorreo("corregido@example.test"))
            assertEquals(existente, db.usuarioDao().buscarPorCorreo("duplicado@example.test"))
            assertNotNull(UsuarioRepository(db.usuarioDao()).iniciarSesion("duplicado@example.test", "Previa123!"))
        }
    }
    @Test fun atrasDesdeResumenConservaDatosYAccesoSinBorradorRedirigeAlFormulario() {
        iniciar()
        compose.runOnUiThread { nav.navigate("resumen") }
        esperar("Crear cuenta")
        compose.onNodeWithText("Confirmar registro").assertDoesNotExist()
        compose.onNodeWithText("Nombre").performTextInput("Cliente atrás")
        compose.onNodeWithText("Correo ficticio").performTextInput("atras@example.test")
        compose.onNodeWithText("Contraseña de prueba").performTextInput("Clave123!")
        compose.onNode(isToggleable()).performScrollTo().performClick()
        continuar()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        esperar("Crear cuenta")
        compose.onNodeWithText("Nombre").assertTextContains("Cliente atrás")
        runBlocking { assertNull(db.usuarioDao().buscarPorCorreo("atras@example.test")) }
    }
}
