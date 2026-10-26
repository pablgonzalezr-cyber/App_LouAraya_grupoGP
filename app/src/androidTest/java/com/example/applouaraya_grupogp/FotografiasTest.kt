package com.example.applouaraya_grupogp

import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.applouaraya_grupogp.model.Producto
import com.example.applouaraya_grupogp.ui.components.ImagenProducto
import com.example.applouaraya_grupogp.ui.theme.AppLouAraya_grupoGPTheme
import java.io.File
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain

/** Archivos sintéticos en caché aislada; no toca Room, galería ni imágenes del usuario. */
class FotografiasTest {
    private val compose = createAndroidComposeRule<ComponentActivity>()
    private val target get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val directory = File(target.cacheDir, "foto-prueba-${UUID.randomUUID()}")
    private val localContext by lazy { object : ContextWrapper(target) {
        override fun getFilesDir(): File = directory
    } }
    private var fixture: File? = null

    private val resources = object : ExternalResource() {
        override fun after() {
            fixture?.delete()
            File(directory, "imagenes_productos").delete()
            directory.delete()
        }
    }
    // La regla Compose desmonta primero la composición y cierra la actividad una sola vez.
    @get:Rule val rules: RuleChain = RuleChain.outerRule(resources).around(compose)

    private fun verificar(width: Int, height: Int, name: String) {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.LTGRAY)
        val marker = minOf(width, height) / 4f
        val colors = listOf(android.graphics.Color.RED, android.graphics.Color.GREEN,
            android.graphics.Color.BLUE, android.graphics.Color.YELLOW)
        val positions = listOf(0f to 0f, (width - marker) to 0f,
            0f to (height - marker), (width - marker) to (height - marker))
        positions.forEachIndexed { i, (x, y) ->
            canvas.drawRect(x, y, x + marker, y + marker, Paint().apply { color = colors[i] })
        }
        val fileName = "${UUID.randomUUID()}.img"
        fixture = File(directory, "imagenes_productos/$fileName").apply {
            parentFile!!.mkdirs()
            outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        bitmap.recycle()
        val product = Producto(nombre = "Foto de prueba", categoria = "Otro", material = "Algodón",
            precio = 1, descripcion = "Fixture", imagenArchivo = fileName)
        compose.setContent {
            CompositionLocalProvider(LocalContext provides localContext) {
                AppLouAraya_grupoGPTheme {
                    ImagenProducto(product, Modifier.size(220.dp).testTag("foto"))
                }
            }
        }
        compose.waitUntil(10000) {
            compose.onAllNodesWithContentDescription("Fotografía de Foto de prueba").fetchSemanticsNodes().isNotEmpty()
        }
        val capture = compose.onNodeWithTag("foto").captureToImage()
        val pixels = capture.toPixelMap()
        val density = capture.width / 220f
        val scale = minOf((capture.width - 24 * density) / width, (capture.height - 24 * density) / height)
        val left = (capture.width - width * scale) / 2
        val top = (capture.height - height * scale) / 2
        // Las cuatro marcas del borde deben verse en sus posiciones proporcionales.
        // Crop pierde marcas; FillBounds altera estas coordenadas en fotos no cuadradas.
        positions.forEachIndexed { i, (x, y) ->
            val actual = pixels[(left + (x + marker / 2) * scale).toInt(), (top + (y + marker / 2) * scale).toInt()]
            val expected = androidx.compose.ui.graphics.Color(colors[i])
            assertEquals(expected.red, actual.red, 0.06f)
            assertEquals(expected.green, actual.green, 0.06f)
            assertEquals(expected.blue, actual.blue, 0.06f)
        }
        // Solo captura del componente de prueba para inspección visual, nunca de la pantalla personal.
        val report = File(target.cacheDir, "verificacion-fotografias/$name.png")
        report.parentFile!!.mkdirs()
        report.outputStream().use { capture.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun archivoCuadradoCompleto() = verificar(160, 160, "cuadrada")
    @Test fun archivoVerticalCompleto() = verificar(80, 160, "vertical")
    @Test fun archivoHorizontalCompleto() = verificar(160, 80, "horizontal")
}
