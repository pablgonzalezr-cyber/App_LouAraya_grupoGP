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
import com.example.applouaraya_grupogp.ui.design.AdminPreviewScene
import java.io.File
import org.junit.Rule
import org.junit.Test

/** Escenas de preview con fixtures; no abre bases de datos ni cambia ajustes del dispositivo. */
class AdminAdaptableTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private fun verificar(width: Int, height: Int = 1000) {
        var screen by mutableIntStateOf(0)
        compose.setContent {
            BoxWithConstraints {
                val scale = minOf(maxWidth.value / width, maxHeight.value / height, 1f)
                val density = LocalDensity.current.density * scale
                CompositionLocalProvider(LocalDensity provides Density(density, 1f)) {
                    Box(Modifier.requiredSize(width.dp, height.dp).testTag("admin-preview")) { AdminPreviewScene(screen) }
                }
            }
        }
        val headings = listOf("Administración Lou Araya", "Administrar productos", "Administrar colecciones", "Administrar campañas", "Todas las solicitudes")
        for (index in 0..4) {
            compose.runOnIdle { screen = index }
            compose.onNodeWithText(headings[index]).assertIsDisplayed()
            if(height < 500 && index in 1..3) {
                compose.onAllNodesWithText("Editar").onFirst().performScrollTo().assertIsDisplayed()
            }
            val capture = compose.onNodeWithTag("admin-preview").captureToImage()
            val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "verificacion-admin/$width-$height-$index.png")
            file.parentFile!!.mkdirs()
            file.outputStream().use { capture.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
    @Test fun compacto() = verificar(360)
    @Test fun mediano() = verificar(720)
    @Test fun expandido() = verificar(1100)
    @Test fun horizontal() = verificar(800, 360)
}
