package com.example.applouaraya_grupogp.data

import androidx.room.Entity

// La clave compuesta impide duplicados y separa favoritos por usuario.
@Entity(tableName = "favoritos", primaryKeys = ["usuarioId", "productoId"])
data class FavoritoEntity(
    val usuarioId: Long,
    val productoId: Int
)
