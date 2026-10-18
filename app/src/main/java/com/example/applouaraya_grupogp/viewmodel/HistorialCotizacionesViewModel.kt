package com.example.applouaraya_grupogp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.applouaraya_grupogp.data.AppDatabase
import com.example.applouaraya_grupogp.repository.CotizacionRepository

class HistorialCotizacionesViewModel @JvmOverloads constructor(application: Application,
    private val repo: CotizacionRepository = CotizacionRepository(AppDatabase.obtener(application).cotizacionDao())) : AndroidViewModel(application) {
    fun observar(usuarioId: Long) = repo.observarPropias(usuarioId)
    fun detalle(usuarioId: Long, id: Long) = repo.observarDetalle(usuarioId, id)
}
