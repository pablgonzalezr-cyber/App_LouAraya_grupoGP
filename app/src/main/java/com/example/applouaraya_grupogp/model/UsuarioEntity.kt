package com.example.applouaraya_grupogp.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(
    tableName = "usuarios",
    indices = [Index(value = ["correo"], unique = true)]
)
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val correo: String,
    val salt: String,
    val claveHash: String,
    @ColumnInfo(defaultValue = "'CLIENTE'") val rol: String = RolUsuario.CLIENTE.name
)

