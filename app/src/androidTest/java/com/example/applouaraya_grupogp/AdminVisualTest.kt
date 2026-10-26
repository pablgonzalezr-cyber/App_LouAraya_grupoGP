package com.example.applouaraya_grupogp

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.applouaraya_grupogp.data.*
import com.example.applouaraya_grupogp.model.CotizacionFormulario
import com.example.applouaraya_grupogp.navigation.AppNavigation
import com.example.applouaraya_grupogp.repository.*
import com.example.applouaraya_grupogp.ui.theme.AppLouAraya_grupoGPTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/** Integración UI real, Room en memoria y preferencias UUID. No usa bases personales. */
class AdminVisualTest {
    private val compose = createAndroidComposeRule<ComponentActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: AppDatabase
    private lateinit var favorites: FavoritosDatabase
    private var clientId = 0L
    private var adminId = 0L
    private val prefsName = "prueba-admin-visual-${UUID.randomUUID()}"
    private val resources = object : ExternalResource() {
        override fun before() {
            db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
            favorites = Room.inMemoryDatabaseBuilder(context, FavoritosDatabase::class.java).build()
            runBlocking {
                val users = UsuarioRepository(db.usuarioDao())
                clientId = users.registrar("Cliente ficticio", "admin-visual@example.test", "Clave123!").id
                adminId = users.iniciarSesion(UsuarioRepository.CORREO_DEMO, UsuarioRepository.CLAVE_DEMO)!!.id
                ProductosRepository(db) { adminId }.inicializar()
            }
        }
        override fun after() { favorites.close(); db.close(); context.deleteSharedPreferences(prefsName) }
    }
    @get:Rule val rules: RuleChain = RuleChain.outerRule(resources).around(compose)

    private fun start() {
        val prefs = PreferenciasRepository(context.getSharedPreferences(prefsName, Context.MODE_PRIVATE))
        compose.setContent { AppLouAraya_grupoGPTheme {
            AppNavigation(basePrueba = db, preferenciasPrueba = prefs, favoritosPrueba = FavoritosRepository(favorites.favoritoDao()))
        } }
        click("Comenzar")
        compose.onNodeWithText("Correo electrónico").performTextInput(UsuarioRepository.CORREO_DEMO)
        compose.onNodeWithText("Contraseña").performTextInput(UsuarioRepository.CLAVE_DEMO)
        click("Ingresar")
        waitFor("Administración Lou Araya")
    }
    private fun waitFor(text: String) { compose.waitUntil(15000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() } }
    private fun visible(text: String): SemanticsNodeInteraction {
        val node = compose.onNodeWithText(text)
        try { node.assertIsDisplayed() } catch (_: AssertionError) { node.performScrollTo() }
        return node.assertIsDisplayed()
    }
    private fun click(text: String) { visible(text).performClick() }
    private fun field(label: String, value: String, replace: Boolean = false) {
        val node = visible(label)
        if(replace) node.performTextClearance()
        node.performTextInput(value)
    }
    private fun back() { compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() } }

    @Test fun adminAbreTodosLosModulosYCierraSesion() {
        start()
        for ((access, title) in listOf("Gestionar solicitudes" to "Todas las solicitudes",
            "Administrar productos" to "Crear producto", "Colecciones y campañas" to "Crear colección",
            "Centro de avisos (0 sin leer)" to "Centro de avisos", "Perfil y configuración" to "Mi perfil")) {
            click(access); waitFor(title)
            back(); waitFor("Administración Lou Araya")
        }
        click("Cerrar sesión"); click("Confirmar"); waitFor("Comenzar")
        compose.onNodeWithText("Administración Lou Araya").assertDoesNotExist()
    }

    @Test fun productoSeEditaDespublicaYRepublicaSinTocarLosOchoOriginales() {
        val original = runBlocking { ProductosRepository(db) { adminId }.observarPublicados().first() }
        start(); click("Administrar productos"); click("Crear producto")
        field("Nombre", "Producto visual de prueba"); field("Descripción", "Diseño artesanal para verificar la nueva interfaz")
        click("Interiores"); field("Material", "Algodón"); field("Ancho (cm)", "40")
        field("Largo / alto (cm)", "60"); field("Precio referencial (CLP)", "15000")
        click("Guardar producto"); waitFor("Producto guardado")
        field("Buscar productos", "Producto visual de prueba")
        click("Editar"); field("Precio referencial (CLP)", "25000", true)
        compose.onAllNodes(isToggleable()).onLast().performScrollTo().performClick()
        click("Guardar producto"); waitFor("Producto guardado")
        val product = runBlocking { db.productoDao().observarAdministracion(adminId).first().single { it.nombre == "Producto visual de prueba" } }
        assertEquals(25000, product.precio); assertFalse(product.activo)
        click("Editar")
        compose.onAllNodes(isToggleable()).onLast().performScrollTo().performClick()
        click("Guardar producto"); waitFor("Producto guardado")
        runBlocking {
            assertTrue(db.productoDao().buscar(product.id)!!.activo)
            original.forEach { assertEquals(it, db.productoDao().buscar(it.id)) }
        }
    }

    @Test fun solicitudPermiteRevisarYAprobarConConfirmacion() {
        val id = runBlocking { CotizacionRepository(db.cotizacionDao()).guardar(clientId, UUID.randomUUID().toString(),
            CotizacionFormulario("Tapiz para verificar administración visual", "Interiores", "Algodón", "40", "60", "1")) }
        start(); click("Gestionar solicitudes"); waitFor("Todas las solicitudes")
        click("Ver y gestionar →"); waitFor("Estado: PENDIENTE")
        click("Cambiar a EN_REVISION"); click("Confirmar"); waitFor("Estado: EN_REVISION")
        click("Cambiar a APROBADA"); click("Confirmar"); waitFor("Estado: APROBADA")
        compose.onNodeWithText("Estado final: no admite más cambios.").assertExists()
        runBlocking { assertEquals("APROBADA", db.cotizacionDao().observarDetalle(clientId, id).first()!!.estado) }
    }

    @Test fun coleccionYCampaniaSeCreanYConservanAsociaciones() {
        start(); click("Colecciones y campañas"); click("Crear colección")
        field("Nombre", "Colección visual de prueba"); field("Descripción", "Piezas naturales para demostrar el diseño")
        compose.onNode(isToggleable() and hasAnyAncestor(hasTestTag("coleccion-producto-1"))).performScrollTo().performClick()
        compose.onAllNodes(isToggleable()).onFirst().performScrollTo().performClick()
        click("Guardar cambios"); waitFor("Colección visual de prueba")
        waitFor("1 productos asociados")
        click("Editar"); field("Descripción", "Descripción editada sin alterar productos asociados", true)
        click("Guardar cambios"); waitFor("Descripción editada sin alterar productos asociados")
        click("Campañas"); click("Crear campaña")
        field("Título", "Campaña visual de prueba"); field("Descripción", "Aviso de temporada para comprobar publicación")
        val format = DateTimeFormatter.ofPattern("dd/MM/uuuu")
        field("Inicio (dd/mm/aaaa)", LocalDate.now().format(format))
        field("Término (dd/mm/aaaa)", LocalDate.now().plusDays(5).format(format))
        compose.onAllNodes(isToggleable()).onFirst().performScrollTo().performClick()
        click("Guardar cambios"); waitFor("Campaña visual de prueba")
        runBlocking {
            val products = ProductosRepository(db) { adminId }
            val data = ContenidoRepository(db, products).observarAdministracion().first()
            assertEquals(1, data.colecciones.size); assertEquals(1, data.campanias.size)
            assertTrue(data.campanias.single().publicada)
            assertEquals(setOf(1), data.miembros.map { it.productoId }.toSet())
        }
    }
}
