package com.example.applouaraya_grupogp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.applouaraya_grupogp.model.UsuarioEntity

@Dao
interface UsuarioDao {

    @Insert
    suspend fun insertar(usuario: UsuarioEntity): Long

    @Query("SELECT * FROM usuarios WHERE correo = :correo LIMIT 1")
    suspend fun buscarPorCorreo(correo: String): UsuarioEntity?
}

