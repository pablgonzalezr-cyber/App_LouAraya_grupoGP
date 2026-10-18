package com.example.applouaraya_grupogp.repository

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

data class PreferenciasLocales(val confirmarSalida: Boolean = true, val mostrarAyuda: Boolean = true,
    val avisosCotizaciones: Boolean = true, val avisosCampanias: Boolean = true)

class PreferenciasRepository(private val prefs: SharedPreferences) {
    private fun clave(id: Long, campo: String): String {
        require(id > 0)
        return "usuario_${id}_$campo"
    }
    fun leer(id: Long) = PreferenciasLocales(
        prefs.getBoolean(clave(id, "confirmarSalida"), true),
        prefs.getBoolean(clave(id, "mostrarAyuda"), true),
        prefs.getBoolean(clave(id, "avisosCotizaciones"), true),
        prefs.getBoolean(clave(id, "avisosCampanias"), true)
    )
    fun observar(id: Long) = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(leer(id)) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(leer(id))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    suspend fun guardar(id: Long, valor: PreferenciasLocales) = withContext(Dispatchers.IO) {
        check(prefs.edit().putBoolean(clave(id, "confirmarSalida"), valor.confirmarSalida)
            .putBoolean(clave(id, "mostrarAyuda"), valor.mostrarAyuda)
            .putBoolean(clave(id, "avisosCotizaciones"), valor.avisosCotizaciones)
            .putBoolean(clave(id, "avisosCampanias"), valor.avisosCampanias).commit()) { "No se pudieron guardar las preferencias" }
    }
}
