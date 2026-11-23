package com.example.applouaraya_grupogp

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.applouaraya_grupogp.data.*
import com.example.applouaraya_grupogp.navigation.AppNavigation
import com.example.applouaraya_grupogp.repository.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID

/** Ejecutar únicamente con el túnel dirigido a H2 descartable y credenciales efímeras. */
@RunWith(AndroidJUnit4::class)
class CotizacionesServidorUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val marca = UUID.randomUUID().toString()
    private val correo = "cliente-$marca@example.test"
    private val clave = UUID.randomUUID().toString() + "Aa1"
    private lateinit var nav: NavHostController
    private lateinit var foco: androidx.compose.ui.focus.FocusManager

    private fun esperar(texto: String) {
        compose.waitUntil(15000) { compose.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun pulsar(texto: String) {
        esperar(texto)
        val nodo = compose.onNodeWithText(texto)
        if (compose.onAllNodes(hasText(texto) and hasAnyAncestor(hasScrollAction())).fetchSemanticsNodes().isNotEmpty())
            nodo.performScrollTo()
        nodo.performClick()
    }

    private fun campo(texto: String, valor: String) {
        esperar(texto)
        val nodo = compose.onNodeWithText(texto)
        if (compose.onAllNodes(hasText(texto) and hasAnyAncestor(hasScrollAction())).fetchSemanticsNodes().isNotEmpty())
            nodo.performScrollTo()
        nodo.performTextInput(valor)
        compose.runOnIdle { foco.clearFocus(force = true) }
    }

    private fun entrar(admin: Boolean) {
        pulsar("Comenzar")
        campo("Correo electrónico", if (admin) UsuarioRepository.CORREO_DEMO else correo)
        campo("Contraseña", if (admin) UsuarioRepository.CLAVE_DEMO else clave)
        pulsar("Ingresar")
        esperar(if (admin) "Administración Lou Araya" else "¡Bienvenido, Cliente prueba!")
    }

    private fun salir(admin: Boolean) {
        compose.runOnIdle { nav.popBackStack(if (admin) "admin" else "dashboard", false) }
        pulsar("Cerrar sesión")
        pulsar("Confirmar")
        esperar("Comenzar")
    }

    private fun historial(admin: Boolean, usuario: String, password: String) {
        pulsar(if (admin) "Gestionar solicitudes" else "Mis solicitudes")
        pulsar("Solicitudes del servidor")
        campo(if (admin) "Usuario administrador del servicio" else "Correo del servicio", usuario)
        campo("Contraseña del servicio", password)
        pulsar("Conectar")
        esperar("Cambiar cuenta")
    }

    @Test fun permisosConflictoYRechazoConSpringBoot() {
        val args = InstrumentationRegistry.getArguments()
        Assume.assumeTrue(args.getString("h2Aislada") == "true")
        val repo = CotizacionesRemotasRepository()
        runBlocking {
            suspend fun verificarHttp(codigo: Int, operacion: suspend () -> Unit) {
                try {
                    operacion()
                    fail("Se esperaba HTTP $codigo")
                } catch (e: ErrorCotizaciones) { assertEquals(codigo, e.codigo) }
            }
            repo.registrar(correo, clave)
            verificarHttp(401) { repo.iniciarSesionCliente(correo, UUID.randomUUID().toString()) }
            val cliente = repo.iniciarSesionCliente(correo, clave)
            verificarHttp(403) { repo.solicitudes(cliente) }
            val admin = repo.iniciarSesionAdmin(requireNotNull(args.getString("usuarioPrueba")),
                requireNotNull(args.getString("clavePrueba")))
            val datos = NuevaCotizacion(marca, "Tapiz para comprobar estados", "Interiores", "Algodón",
                20.toBigDecimal(), 30.toBigDecimal(), 1)
            val solicitud = repo.crear(cliente, datos)
            val pendiente = EstadoCotizacionRemota.PENDIENTE
            val revision = EstadoCotizacionRemota.EN_REVISION
            val final = EstadoCotizacionRemota.RECHAZADA
            verificarHttp(400) { repo.cambiarEstado(admin, solicitud.id, pendiente, final) }
            assertEquals(revision, repo.cambiarEstado(admin, solicitud.id, pendiente, revision).estado)
            verificarHttp(409) { repo.cambiarEstado(admin, solicitud.id, pendiente, revision) }
            assertEquals(revision, repo.propia(cliente, solicitud.id).estado)
            assertEquals(final, repo.cambiarEstado(admin, solicitud.id, revision, final).estado)
            verificarHttp(400) { repo.cambiarEstado(admin, solicitud.id, final, revision) }
            assertEquals(final, repo.historial(cliente).single().estado)
        }
    }

    @Test fun clienteAdminClienteConSpringBoot() {
        val args = InstrumentationRegistry.getArguments()
        Assume.assumeTrue(args.getString("h2Aislada") == "true")
        val admin = requireNotNull(args.getString("usuarioPrueba"))
        val password = requireNotNull(args.getString("clavePrueba"))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val fav = Room.inMemoryDatabaseBuilder(context, FavoritosDatabase::class.java).build()
        val prefs = PreferenciasRepository(context.getSharedPreferences(marca, Context.MODE_PRIVATE))
        val remoto = CotizacionesRemotasRepository()
        try {
            runBlocking {
                UsuarioRepository(db.usuarioDao()).registrar("Cliente prueba", correo, clave)
                remoto.registrar(correo, clave)
            }
            compose.setContent {
                foco = androidx.compose.ui.platform.LocalFocusManager.current
                nav = rememberNavController()
                MaterialTheme { AppNavigation(db, prefs, nav, FavoritosRepository(fav.favoritoDao())) }
            }
            entrar(false)
            pulsar("Cotización personalizada")
            campo("Descripción", "Tapiz de prueba para el circuito completo")
            pulsar("Selecciona una categoría")
            pulsar("Interiores")
            campo("Material", "Algodón")
            campo("Ancho (cm)", "30")
            campo("Largo / alto (cm)", "50")
            campo("Correo del servicio", correo)
            campo("Contraseña del servicio", clave)
            pulsar("Enviar a Spring Boot")
            esperar("Cotización enviada al servidor")
            val id = runBlocking {
                val cuenta = remoto.iniciarSesionCliente(correo, clave)
                val solicitud = remoto.historial(cuenta).single()
                val otro = "otro-$marca@example.test"
                remoto.registrar(otro, clave)
                val otraCuenta = remoto.iniciarSesionCliente(otro, clave)
                assertTrue(remoto.historial(otraCuenta).isEmpty())
                try {
                    remoto.propia(otraCuenta, solicitud.id)
                    fail("El servicio permitió leer una solicitud ajena")
                } catch (e: ErrorCotizaciones) { assertEquals(404, e.codigo) }
                solicitud.id
            }
            pulsar("Volver")
            historial(false, correo, clave)
            pulsar("Solicitud #$id · PENDIENTE")
            esperar("Estado: PENDIENTE")
            var anterior = "PENDIENTE"
            for (nuevo in listOf("EN_REVISION", "APROBADA")) {
                salir(false)
                entrar(true)
                historial(true, admin, password)
                pulsar("Solicitud #$id · $anterior")
                pulsar("Cambiar a $nuevo")
                pulsar("Confirmar")
                esperar("Estado: $nuevo")
                esperar("Cambio confirmado: $nuevo")
                salir(true)
                entrar(false)
                historial(false, correo, clave)
                pulsar("Actualizar")
                esperar("Solicitud #$id: $anterior → $nuevo")
                pulsar("Entendido")
                pulsar("Solicitud #$id · $nuevo")
                esperar("Estado: $nuevo")
                compose.onNodeWithText("Cambios de estado").assertDoesNotExist()
                anterior = nuevo
            }
        } finally {
            compose.activityRule.scenario.close()
            fav.close()
            db.close()
            context.deleteSharedPreferences(marca)
        }
    }
}
