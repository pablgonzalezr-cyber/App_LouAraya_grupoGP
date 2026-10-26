package com.example.applouaraya_grupogp

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.applouaraya_grupogp.repository.ProductosDemo
import com.example.applouaraya_grupogp.ui.screens.*
import com.example.applouaraya_grupogp.ui.theme.AppLouAraya_grupoGPTheme
import com.example.applouaraya_grupogp.viewmodel.CatalogoUiState
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Equivalentes a previews de las pantallas reales, con datos ficticios y sin Room. */
class FotografiasPantallasTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun verificar(width: Int) {
        var screen by mutableIntStateOf(0)
        val products = ProductosDemo.productos.take(3).map { it.copy(imagenNombre = "macrame_tapiz") }
        compose.setContent {
            BoxWithConstraints {
                val scale = minOf(maxWidth.value / width, maxHeight.value / 900, 1f)
                val density = LocalDensity.current.density * scale
                CompositionLocalProvider(LocalDensity provides Density(density, 1f)) {
                    Box(Modifier.requiredSize(width.dp, 900.dp).testTag("pantalla")) {
                        AppLouAraya_grupoGPTheme {
                            when (screen) {
                                0 -> BienvenidaScreen()
                                1 -> DashboardScreen("Cliente demo", {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, 0)
                                2 -> CatalogoContenido(CatalogoUiState(productos = products, todos = products, cargando = false),
                                    emptySet(), true, {}, {}, {}, {}, {}, {}, {}, {}, {})
                                3 -> DetalleProductoScreen(products.first(), false, true, {}, {})
                                else -> FavoritosScreen(products, products.map { it.id }.toSet(), {}, {}, {})
                            }
                        }
                    }
                }
            }
        }
        val descriptions = listOf("Tapiz artesanal de Lou Araya Macramé", "Colección artesanal destacada")
        for (index in 0..4) {
            compose.runOnIdle { screen = index }
            // Las tarjetas clicables agrupan sus textos e imagen en semántica combinada.
            // Medimos la fotografía real, no la altura de toda la tarjeta.
            val photo = compose.onAllNodesWithContentDescription(if (index < 2) descriptions[index] else "Fotografía de ${products.first().nombre}",
                useUnmergedTree = true)
                .onFirst()
            photo.performScrollTo().assertIsDisplayed()
            val bounds = photo.fetchSemanticsNode().boundsInRoot
            assertEquals(bounds.width, bounds.height, 3f)
            if (index in listOf(0, 1, 3)) {
                val container = compose.onNodeWithTag("pantalla").fetchSemanticsNode().boundsInRoot
                assertEquals(container.center.x, bounds.center.x, 3f)
            }
            val capture = compose.onNodeWithTag("pantalla").captureToImage()
            val report = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
                "verificacion-fotografias/$width-$index.png")
            report.parentFile!!.mkdirs()
            report.outputStream().use { capture.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun compactoFotografiasEnCincoPantallas() = verificar(360)
    @Test fun medianoFotografiasEnCincoPantallas() = verificar(720)
    @Test fun expandidoFotografiasEnCincoPantallas() = verificar(1100)
}
