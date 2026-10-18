package com.example.applouaraya_grupogp.data

import androidx.room.*
import com.example.applouaraya_grupogp.model.Producto
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao {
    @Query("SELECT * FROM productos WHERE activo = 1 ORDER BY id")
    fun observarPublicados(): Flow<List<Producto>>
    @Query("SELECT * FROM productos WHERE activo = 1 ORDER BY id")
    suspend fun publicados(): List<Producto>
    @Query("SELECT * FROM productos WHERE EXISTS (SELECT 1 FROM usuarios WHERE id = :actor AND rol = 'ADMINISTRADOR') ORDER BY id")
    fun observarAdministracion(actor: Long): Flow<List<Producto>>
    @Query("SELECT * FROM productos WHERE id = :id")
    suspend fun buscar(id: Int): Producto?
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarIniciales(productos: List<Producto>)
    @Insert
    suspend fun insertar(producto: Producto): Long
    @Update
    suspend fun actualizar(producto: Producto): Int
}
