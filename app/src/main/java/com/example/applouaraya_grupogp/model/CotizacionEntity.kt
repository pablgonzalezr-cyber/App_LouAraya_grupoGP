package com.example.applouaraya_grupogp.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "cotizaciones", foreignKeys = [ForeignKey(
    entity = UsuarioEntity::class, parentColumns = ["id"], childColumns = ["usuarioId"],
    onDelete = ForeignKey.NO_ACTION
)], indices = [Index("usuarioId"), Index(value = ["tokenEnvio"], unique = true)])
data class CotizacionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val usuarioId: Long,
    val tokenEnvio: String,
    val descripcion: String,
    val categoria: String,
    val material: String,
    val anchoCm: Double,
    val largoCm: Double,
    val cantidad: Int,
    val fechaRequerida: String?,
    val productoReferenciaId: Int?,
    val productoReferenciaNombre: String?,
    val creadaEn: Long,
    val estado: String = "PENDIENTE"
)
