package com.example.applouaraya_grupogp.model

import androidx.room.*

@Entity(tableName = "avisos", foreignKeys = [ForeignKey(entity = UsuarioEntity::class,
    parentColumns = ["id"], childColumns = ["usuarioId"])],
    indices = [Index("usuarioId"), Index(value = ["usuarioId", "claveEvento"], unique = true)])
data class AvisoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val usuarioId: Long,
    val tipo: String,
    val titulo: String,
    val mensaje: String,
    val referenciaId: Long,
    val claveEvento: String,
    val creadaEn: Long,
    val leida: Boolean = false
)
