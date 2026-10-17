package com.example.applouaraya_grupogp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.applouaraya_grupogp.data.AppDatabase
import com.example.applouaraya_grupogp.repository.CotizacionRepository

class HistorialCotizacionesViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = CotizacionRepository(AppDatabase.obtener(application).cotizacionDao())
    fun observar(usuarioId: Long) = repo.observarPropias(usuarioId)
    fun detalle(usuarioId: Long, id: Long) = repo.observarDetalle(usuarioId, id)
}
