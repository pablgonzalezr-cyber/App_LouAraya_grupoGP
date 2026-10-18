package com.example.applouaraya_grupogp

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.example.applouaraya_grupogp.ui.components.CamaraQr
import org.junit.*
import org.junit.runner.RunWith
import org.junit.rules.RuleChain
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class CamaraQrTest {
    private val compose = createAndroidComposeRule<ComponentActivity>()
    @get:Rule val reglas: RuleChain = RuleChain.outerRule(GrantPermissionRule.grant(Manifest.permission.CAMERA)).around(compose)
    @Test fun camaraFisicaEntregaFramesAlAnalizadorReal() {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        Assume.assumeTrue("Dispositivo sin cámara", contexto.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY))
        val frame = AtomicBoolean(false)
        val error = AtomicReference<String?>(null)
        compose.setContent {
            MaterialTheme {
                CamaraQr(Modifier.fillMaxSize(), true, onCodigo = {}, onError = error::set, onPrimerFrame = { frame.set(true) })
            }
        }
        // Esperar el callback real del analizador, independiente del momento en
        // que Compose publica su árbol de semántica tras arrancar la actividad.
        compose.waitUntil(20000) {
            frame.get() || error.get() != null
        }
        Assert.assertNull(error.get(), error.get())
        Assert.assertTrue("La cámara debe entregar un fotograma a ML Kit", frame.get())
    }
}
