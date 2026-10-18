package com.example.applouaraya_grupogp.notifications

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.applouaraya_grupogp.MainActivity
import com.example.applouaraya_grupogp.model.AvisoEntity
import com.example.applouaraya_grupogp.repository.*

/** Nunca pone nombres, IDs, estados, descripciones ni IDs de cuenta en Android/PendingIntent. */
class NotificacionesLocales(private val contexto: Context, private val prefijo: String = "lou-avisos") {
    companion object {
        const val ACCION_CENTRO = "com.example.applouaraya_grupogp.ABRIR_AVISOS"
        const val TEXTO_PUBLICO = "Tienes avisos sin leer. Abre tu centro de avisos."
    }
    private val manager = contexto.getSystemService(NotificationManager::class.java)
    private var cuenta: Long? = null
    private val ultimos = mutableMapOf<String, List<Long>>()
    fun permitido(): Boolean = (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(contexto,
        Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) && manager.areNotificationsEnabled()
    fun canales() {
        listOf("cotizaciones" to "Cotizaciones y solicitudes", "campanias" to "Campañas").forEach { (id, nombre) ->
            manager.createNotificationChannel(NotificationChannel("$prefijo-$id", nombre, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Avisos locales de la cuenta con sesión activa"
                lockscreenVisibility = Notification.VISIBILITY_SECRET
                setShowBadge(true)
            })
        }
    }
    fun cancelar() {
        listOf("cotizaciones", "campanias").forEach { manager.cancel("$prefijo-$it", 1) }
        ultimos.clear(); cuenta = null
    }
    fun actualizar(usuario: Long?, avisos: List<AvisoEntity>, preferencias: PreferenciasLocales) {
        if(cuenta != usuario) { cancelar(); cuenta = usuario }
        if(usuario == null || !permitido()) { cancelar(); return }
        canales()
        listOf("cotizaciones", "campanias").forEach { grupo ->
            val propios = avisos.filter { it.usuarioId == usuario && !it.leida && PoliticaAvisos.habilitado(it.tipo, preferencias) &&
                (if(grupo == "campanias") it.tipo == "CAMPANIA" else it.tipo in setOf("ESTADO", "NUEVA_SOLICITUD")) }
            val canal = "$prefijo-$grupo"
            if(propios.isEmpty() || manager.getNotificationChannel(canal)?.importance == NotificationManager.IMPORTANCE_NONE) {
                manager.cancel(canal, 1); ultimos.remove(grupo)
            } else if(ultimos[grupo] != propios.map { it.id }) {
                val intent = Intent(contexto, MainActivity::class.java).setAction(ACCION_CENTRO)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                val pendiente = PendingIntent.getActivity(contexto, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val notificacion = NotificationCompat.Builder(contexto, canal)
                    .setSmallIcon(com.example.applouaraya_grupogp.R.drawable.ic_aviso)
                    .setContentTitle("Lou Araya Macramé").setContentText(TEXTO_PUBLICO)
                    .setVisibility(NotificationCompat.VISIBILITY_SECRET).setOnlyAlertOnce(true)
                    .setAutoCancel(true).setContentIntent(pendiente).build()
                try {
                    manager.notify(canal, 1, notificacion)
                    ultimos[grupo] = propios.map { it.id }
                } catch(_: SecurityException) { /* Revocación del permiso: el centro Room sigue funcionando. */ }
            }
        }
    }
}
