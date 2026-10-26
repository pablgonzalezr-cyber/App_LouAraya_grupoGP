package com.example.applouaraya_grupogp

import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import java.io.File
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.Room
import com.example.applouaraya_grupogp.data.AppDatabase
import com.example.applouaraya_grupogp.model.*
import com.example.applouaraya_grupogp.repository.CotizacionRepository
import com.example.applouaraya_grupogp.ui.screens.*
import com.example.applouaraya_grupogp.ui.theme.AppLouAraya_grupoGPTheme
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Isolated presentation and real repository checks; never opens the application's database. */
class AnimacionesTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun bienvenidaAccesos() {
        var comenzar = 0
        var registro = 0
        compose.setContent { AppLouAraya_grupoGPTheme { BienvenidaScreen({ comenzar++ }, { registro++ }) } }
        compose.onNodeWithText("Comenzar").performScrollTo().performClick()
        compose.onNodeWithText("Crear una cuenta").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, comenzar); assertEquals(1, registro) }
    }

    @Test fun confirmacionSoloTrasExitoYContinuacionUnica() {
        var estado by mutableStateOf(CotizacionUiState(guardando = true))
        var navegaciones = 0
        var guardados = 0
        compose.setContent { AppLouAraya_grupoGPTheme {
            CotizacionContent(estado, {}, { navegaciones++ }, false, {}, { guardados++ })
        } }
        compose.onNodeWithText("Solicitud registrada correctamente").assertDoesNotExist()
        compose.runOnIdle { estado = estado.copy(guardando = false, error = "Error simulado") }
        compose.onNodeWithText("Error simulado").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Solicitud registrada correctamente").assertDoesNotExist()
        compose.runOnIdle { estado = estado.copy(error = null, guardadaId = 42) }
        compose.onNodeWithText("Solicitud registrada correctamente").assertIsDisplayed()
        compose.onNodeWithText("Guardar solicitud").assertDoesNotExist()
        compose.onNodeWithText("Ver mi solicitud").performScrollTo().performClick()
        compose.onNodeWithText("Ver mi solicitud").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(1, navegaciones); assertEquals(0, guardados) }
    }

    @Test fun salirCancelaConfirmacionPendiente() {
        var mostrar by mutableStateOf(true)
        var navegaciones = 0
        compose.setContent { if (mostrar) ConfirmacionCotizacion({}, { navegaciones++ }) }
        compose.runOnIdle { mostrar = false }
        Thread.sleep(1800)
        compose.runOnIdle { assertEquals(0, navegaciones) }
    }

    @Test fun repositorioExitoDuplicadoYErrorRealAislados() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java).build()
        try {
            val usuario = db.usuarioDao().insertar(UsuarioEntity(nombre = "Animacion", correo = "animacion@example.test", salt = "s", claveHash = "h"))
            val repo = CotizacionRepository(db.cotizacionDao())
            val f = CotizacionFormulario(descripcion = "Tapiz artesanal personalizado", categoria = "Interiores",
                material = "Algodon", ancho = "40", largo = "60", cantidad = "1")
            val id = repo.guardar(usuario, "animacion-aislada", f)
            assertTrue(id > 0)
            assertEquals(id, repo.guardar(usuario, "animacion-aislada", f))
            assertEquals(1, db.cotizacionDao().observarPropias(usuario).first().size)
            try {
                repo.guardar(-1, "error-aislado", f)
                fail("Expected rejected save")
            } catch (_: IllegalArgumentException) { }
            assertEquals(1, db.cotizacionDao().observarPropias(usuario).first().size)
        } finally { db.close() }
    }
    @Test fun dashboardClienteTodosLosAccesosYActualizacion() {
        val accesos = IntArray(11)
        var avisos by mutableIntStateOf(0)
        compose.setContent { AppLouAraya_grupoGPTheme {
            DashboardScreen("Prueba", { accesos[0]++ }, { accesos[1]++ }, { accesos[2]++ },
                { accesos[3]++ }, { accesos[4]++ }, { accesos[5]++ }, { accesos[6]++ },
                { accesos[7]++ }, { accesos[8]++ }, { accesos[9]++ }, avisos)
        } }
        val labels = listOf("Ver cat\u00e1logo", "Cerrar sesi\u00f3n", "Cotizaci\u00f3n personalizada",
            "Mis solicitudes", "Mis favoritos", "Perfil y configuraci\u00f3n", "Colecciones y campa\u00f1as",
            "Escanear QR", "Mapa de ferias y showrooms", "Centro de avisos (0 sin leer)")
        labels.forEach { compose.onNodeWithText(it).performScrollTo().performClick() }
        compose.runOnIdle { avisos = 3 }
        compose.onNodeWithText("Centro de avisos (3 sin leer)").performScrollTo().performClick()
        compose.runOnIdle { (0..8).forEach { assertEquals(1, accesos[it]) }; assertEquals(2, accesos[9]) }
    }

    @Test fun dashboardAdminTodosLosAccesos() {
        val accesos = IntArray(6)
        compose.setContent { AppLouAraya_grupoGPTheme {
            AdminDashboardContent(com.example.applouaraya_grupogp.viewmodel.AdminUiState(cargando = false), {},
                { accesos[0]++ }, { accesos[1]++ }, { accesos[2]++ }, { accesos[3]++ },
                { accesos[4]++ }, { accesos[5]++ }, 0)
        } }
        listOf("Gestionar solicitudes", "Cerrar sesi\u00f3n", "Perfil y configuraci\u00f3n", "Administrar productos",
            "Colecciones y campa\u00f1as", "Centro de avisos (0 sin leer)").forEach {
            compose.onNodeWithText(it).performScrollTo().performClick()
        }
        compose.runOnIdle { accesos.forEach { assertEquals(1, it) } }
    }

    private fun formato(width: Int, height: Int) {
        var pantalla by mutableIntStateOf(0)
        compose.setContent {
            BoxWithConstraints {
                val scale = minOf(maxWidth.value / width, maxHeight.value / height, 1f)
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density * scale, 1f)) {
                    Box(Modifier.requiredSize(width.dp, height.dp).testTag("animacion")) {
                        AppLouAraya_grupoGPTheme {
                            when (pantalla) {
                                0 -> BienvenidaScreen()
                                1 -> DashboardScreen("Prueba", {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, 0)
                                else -> ConfirmacionCotizacion({}, {})
                            }
                        }
                    }
                }
            }
        }
        for (index in 0..2) {
            compose.runOnIdle { pantalla = index }
            val label = listOf("Comenzar", "Ver cat\u00e1logo", "Ver mi solicitud")[index]
            compose.onNodeWithText(label).performScrollTo().assertIsDisplayed()
            val root = compose.onNodeWithTag("animacion")
            val capture = root.captureToImage().asAndroidBitmap()
            val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
                "verificacion-animaciones/$width-$height-$index.png")
            file.parentFile!!.mkdirs()
            file.outputStream().use { capture.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
    @Test fun compacto() = formato(360, 900)
    @Test fun mediano() = formato(720, 1000)
    @Test fun expandido() = formato(1100, 1000)
    @Test fun horizontal() = formato(800, 360)

}
