package com.example.applouaraya_grupogp

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.applouaraya_grupogp.data.AppDatabase
import com.example.applouaraya_grupogp.model.CotizacionFormulario
import com.example.applouaraya_grupogp.navigation.AppNavigation
import com.example.applouaraya_grupogp.repository.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import java.util.UUID

class AvisosUiTest {
    private val compose = createAndroidComposeRule<ComponentActivity>()
    private val contexto get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: AppDatabase
    private val archivo = "prueba-ui-avisos-${UUID.randomUUID()}"
    // Cerrar Room después de que Compose termine de desmontar navegación y actividad.
    private val recursos = object: ExternalResource() {
        override fun before() { db = Room.inMemoryDatabaseBuilder(contexto, AppDatabase::class.java).build() }
        override fun after() { db.close(); contexto.deleteSharedPreferences(archivo) }
    }
    @get:Rule val reglas: RuleChain = RuleChain.outerRule(recursos).around(compose)
    @Test fun centroClienteMuestraSoloSusAvisosYActualizaContador() {
            runBlocking {
                val usuarios = UsuarioRepository(db.usuarioDao())
                val cliente = usuarios.registrar("Cliente centro", "centro@example.test", "Clave123!")
                val admin = usuarios.iniciarSesion(UsuarioRepository.CORREO_DEMO, UsuarioRepository.CLAVE_DEMO)!!
                val id = CotizacionRepository(db.cotizacionDao()).guardar(cliente.id, "centro", CotizacionFormulario("Tapiz para probar el centro", "Interiores", "Algodón", "40", "60", "1"))
                AdminCotizacionesRepository(db.usuarioDao(), db.cotizacionDao()) { admin.id }.cambiarEstado(id, "PENDIENTE", EstadoCotizacion.EN_REVISION)
            }
            compose.setContent { MaterialTheme { AppNavigation(basePrueba = db, preferenciasPrueba = PreferenciasRepository(contexto.getSharedPreferences(archivo, Context.MODE_PRIVATE))) } }
            compose.onNodeWithText("Comenzar").performClick()
            compose.onNodeWithText("Correo electrónico").performTextInput("centro@example.test")
            compose.onNodeWithText("Contraseña").performTextInput("Clave123!")
            compose.onNodeWithText("Ingresar").performClick()
            esperar("Centro de avisos (1 sin leer)")
            compose.onNodeWithText("Centro de avisos (1 sin leer)").performScrollTo().performClick()
            esperar("Estado de cotización actualizado")
            compose.onNodeWithText("Nueva solicitud").assertDoesNotExist()
            compose.onNodeWithText("No leídas: 1").assertExists()
            compose.onNodeWithText("Marcar como leída").performScrollTo().performClick()
            esperar("No leídas: 0")
            compose.onNodeWithText("Leída").assertExists()
            compose.onNodeWithText("Ver contenido").performScrollTo().performClick()
            esperar("Detalle de solicitud")
            esperar("Descripción: Tapiz para probar el centro")
            compose.onNodeWithText("Estado: EN_REVISION").assertExists()
    }
    private fun esperar(texto: String) {
        compose.waitUntil(10000) { compose.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty() }
    }
}
