package com.example.applouaraya_grupogp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritoDao {
    @Query("SELECT productoId FROM favoritos WHERE usuarioId = :usuarioId")
    fun observarIds(usuarioId: Long): Flow<List<Int>>

    @Query("SELECT EXISTS(SELECT 1 FROM favoritos WHERE usuarioId = :usuarioId AND productoId = :productoId)")
    suspend fun existe(usuarioId: Long, productoId: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun agregar(favorito: FavoritoEntity)

    @Query("DELETE FROM favoritos WHERE usuarioId = :usuarioId AND productoId = :productoId")
    suspend fun quitar(usuarioId: Long, productoId: Int)
}
