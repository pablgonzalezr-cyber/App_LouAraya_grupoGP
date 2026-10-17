package com.example.applouaraya_grupogp.repository

import com.example.applouaraya_grupogp.data.FavoritoDao
import com.example.applouaraya_grupogp.data.FavoritoEntity

class FavoritosRepository(private val dao: FavoritoDao) {
    fun observar(usuarioId: Long) = dao.observarIds(usuarioId)

    suspend fun alternar(usuarioId: Long, productoId: Int) {
        if (dao.existe(usuarioId, productoId)) {
            dao.quitar(usuarioId, productoId)
        } else {
            dao.agregar(FavoritoEntity(usuarioId, productoId))
        }
    }
}
