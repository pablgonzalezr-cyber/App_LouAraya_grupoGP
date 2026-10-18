package com.example.applouaraya_grupogp

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.os.Build
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.example.applouaraya_grupogp.model.AvisoEntity
import com.example.applouaraya_grupogp.notifications.NotificacionesLocales
import com.example.applouaraya_grupogp.repository.PreferenciasLocales
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TestRule
import java.util.UUID

class NotificacionesSistemaTest {
    @get:Rule val permiso: TestRule = if(Build.VERSION.SDK_INT >= 33) GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
        else TestRule { base, _ -> base }

    @Test fun androidSoloPublicaResumenGenericoDeSesionActualYRetiraAlSalir() {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val prefijo = "prueba-avisos-${UUID.randomUUID()}"
        val manager = contexto.getSystemService(NotificationManager::class.java)
        val controlador = NotificacionesLocales(contexto, prefijo)
        val propio = AvisoEntity(1, 10, "ESTADO", "Nombre privado", "Descripción privada EN_REVISION", 7, "estado:7", 1)
        val ajeno = propio.copy(id = 2, usuarioId = 20)
        fun activos() = manager.activeNotifications.filter { it.tag?.startsWith(prefijo) == true }
        // notify/cancel pasan por el servicio Android; su lista activa se actualiza
        // de forma asíncrona, no necesariamente al retornar la llamada Binder.
        fun esperar(cantidad: Int): List<android.service.notification.StatusBarNotification> {
            val limite = SystemClock.elapsedRealtime() + 5000
            var lista = activos()
            while(lista.size != cantidad && SystemClock.elapsedRealtime() < limite) {
                SystemClock.sleep(50)
                lista = activos()
            }
            assertEquals("Cantidad final de avisos temporales de Android", cantidad, lista.size)
            return lista
        }
        try {
            Assume.assumeTrue("Alertas desactivadas globalmente en Ajustes Android", controlador.permitido())
            controlador.actualizar(10, listOf(ajeno), PreferenciasLocales())
            assertTrue(esperar(0).isEmpty())
            controlador.actualizar(10, listOf(propio, ajeno), PreferenciasLocales())
            val notificacion = esperar(1).single().notification
            assertEquals(Notification.VISIBILITY_SECRET, notificacion.visibility)
            assertEquals("Lou Araya Macramé", notificacion.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
            assertEquals(NotificacionesLocales.TEXTO_PUBLICO, notificacion.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
            assertFalse(notificacion.extras.toString().contains("Descripción privada"))
            if(Build.VERSION.SDK_INT >= 31) assertTrue(notificacion.contentIntent.isImmutable)
            controlador.actualizar(20, listOf(propio), PreferenciasLocales())
            assertTrue(esperar(0).isEmpty())
            controlador.actualizar(10, listOf(propio), PreferenciasLocales(avisosCotizaciones = false))
            assertTrue(esperar(0).isEmpty())
            controlador.actualizar(10, listOf(propio), PreferenciasLocales())
            assertEquals(1, esperar(1).size)
            controlador.actualizar(10, listOf(propio.copy(leida = true)), PreferenciasLocales())
            assertTrue(esperar(0).isEmpty())
            controlador.actualizar(10, listOf(propio.copy(tipo = "CAMPANIA")), PreferenciasLocales())
            assertTrue(esperar(1).single().notification.channelId.endsWith("campanias"))
            controlador.actualizar(null, listOf(propio), PreferenciasLocales())
            assertTrue(esperar(0).isEmpty())
        } finally {
            controlador.cancelar()
            listOf("cotizaciones", "campanias").forEach { manager.deleteNotificationChannel("$prefijo-$it") }
        }
    }
}
