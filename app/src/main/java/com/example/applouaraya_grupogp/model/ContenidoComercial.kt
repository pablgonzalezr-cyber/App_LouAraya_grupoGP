package com.example.applouaraya_grupogp.model

import androidx.room.*

@Entity(tableName = "colecciones")
data class ColeccionEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String, val descripcion: String, val activa: Boolean = true, val publicada: Boolean = false)

@Entity(tableName = "coleccion_productos", primaryKeys = ["coleccionId", "productoId"],
    foreignKeys = [ForeignKey(entity = ColeccionEntity::class, parentColumns = ["id"], childColumns = ["coleccionId"]),
        ForeignKey(entity = Producto::class, parentColumns = ["id"], childColumns = ["productoId"])],
    indices = [Index("productoId")])
data class ColeccionProducto(val coleccionId: Long, val productoId: Int)

@Entity(tableName = "campanias")
data class CampaniaEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String, val descripcion: String, val inicio: String, val termino: String,
    val publicada: Boolean = false, val activa: Boolean = true)

data class MiembroPublicado(val coleccionId: Long, @Embedded val producto: Producto)
data class ColeccionVista(val coleccion: ColeccionEntity, val productos: List<Producto>)
