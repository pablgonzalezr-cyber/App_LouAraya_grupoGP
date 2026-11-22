package com.example.applouaraya_grupogp

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.applouaraya_grupogp.data.AppDatabase
import com.example.applouaraya_grupogp.repository.*
import com.example.applouaraya_grupogp.ui.screens.CatalogoScreen
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Opt-in: requiere el catálogo HTTPS de la demo y acceso real a Mi Indicador. */
class DolarFichaHttpsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun fichaRemotaMuestraClpUsdYFecha() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("probarCatalogoHttps") == "true")
        val contexto = compose.activity.applicationContext
        val db = Room.inMemoryDatabaseBuilder(contexto, AppDatabase::class.java).build()
        try {
            val remoto = ProductosRemotos(contexto.getSharedPreferences("lou_catalogo_fotos", 0), db.productoDao()::buscar)
            val repo = ProductosRepository(db, remoto)
            compose.setContent { CatalogoScreen(onVolver = {}, repositorio = repo, productoInicial = 1) }
            compose.waitUntil(25000) { compose.onAllNodesWithText("Tapiz Bohemio").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText(" CLP", substring = true).performScrollTo().assertIsDisplayed()
            compose.waitUntil(45000) { compose.onAllNodesWithText("≈ USD", substring = true).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Referencial", substring = true, ignoreCase = false).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Dólar observado del ", substring = true).assertExists()
            compose.onNodeWithText("Actualizar dólar").performClick()
        } finally {
            compose.activityRule.scenario.close()
            db.close()
        }
    }
}
