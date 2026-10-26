package com.example.applouaraya_grupogp

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.applouaraya_grupogp.ui.adaptive.*
import com.example.applouaraya_grupogp.ui.screens.LoginScreen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Solo UI ficticia: no repositorios, bases de datos ni cambios de configuración del teléfono. */
class AdaptabilidadTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private var layoutDensity = 1f

    private fun mostrar(width: Int, height: Int, content: @Composable () -> Unit) {
        compose.setContent {
            // Ajusta la densidad únicamente de esta composición para caber en el teléfono físico.
            BoxWithConstraints {
                val density = LocalDensity.current
                val scale = minOf(maxWidth.value / width, maxHeight.value / height, 1f)
                layoutDensity = density.density * scale
                CompositionLocalProvider(LocalDensity provides Density(density.density * scale, 1f)) {
                    Box(Modifier.requiredSize(width.dp, height.dp)) { MaterialTheme { content() } }
                }
            }
        }
    }

    private fun columnas(width: Int, expected: Int) {
        mostrar(width, 800) {
            AdaptiveColumn(form = false) {
                AdaptiveActions(listOf("Uno" to {}, "Dos" to {}, "Tres" to {}, "Cuatro" to {}))
            }
        }
        val first = compose.onNodeWithText("Uno").fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithText("Dos").fetchSemanticsNode().boundsInRoot
        val third = compose.onNodeWithText("Tres").fetchSemanticsNode().boundsInRoot
        if (expected == 1) assertTrue(second.top > first.top) else assertEquals(first.top, second.top, 2f)
        if (expected == 3) assertEquals(first.top, third.top, 2f) else assertTrue(third.top > first.top)
        compose.onNodeWithText("Cuatro").assertIsDisplayed()
    }

    @Test fun compactoUnaColumna() = columnas(360, 1)
    @Test fun medianoDosColumnas() = columnas(720, 2)
    @Test fun expandidoTresColumnas() = columnas(1100, 3)

    private fun formulario(width: Int, height: Int) {
        mostrar(width, height) { LoginScreen(false, null, { _, _ -> }, {}) }
        compose.onNodeWithText("Correo electrónico").performScrollTo().performTextInput("demo@example.test")
        // El IME físico no comparte la densidad del lienzo simulado. Medimos sin IME;
        // su comportamiento real se comprueba en el caso nativo independiente.
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Correo electrónico").performScrollTo().assertIsDisplayed()
        val field = compose.onNode(hasSetTextAction() and hasText("Correo electrónico")).fetchSemanticsNode().boundsInRoot
        val expectedWidth = minOf(width, if (width >= 840) 560 else 520) - 48f
        assertEquals(expectedWidth, field.width / layoutDensity, 3f)
        compose.onNodeWithText("Contraseña").performScrollTo().performTextInput("Clave123!")
        // En el lienzo escalado el IME físico no comparte su densidad. Este caso
        // comprueba altura reducida; el teclado real se prueba sin escalar aparte.
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Ingresar").performScrollTo().assertIsDisplayed()
        val button = compose.onNodeWithText("Ingresar").fetchSemanticsNode().boundsInRoot
        assertTrue(button.width > 0)
    }
    @Test fun formularioCompacto() = formulario(360, 800)
    @Test fun formularioMediano() = formulario(720, 1024)
    @Test fun formularioExpandido() = formulario(1100, 800)
    @Test fun formularioHorizontalAlturaReducida() = formulario(800, 360)

    @Test fun tecladoSamsungPermiteDesplazarHastaIngresar() {
        compose.setContent { MaterialTheme { LoginScreen(false, null, { _, _ -> }, {}) } }
        compose.onNodeWithText("Correo electrónico").performClick().performTextInput("demo@example.test")
        compose.onNodeWithText("Contraseña").performScrollTo().performClick().performTextInput("Clave123!")
        compose.onNodeWithText("Ingresar").performScrollTo().assertIsDisplayed()
    }
}
