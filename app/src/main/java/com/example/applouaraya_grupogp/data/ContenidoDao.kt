package com.example.applouaraya_grupogp.data

import androidx.room.*
import com.example.applouaraya_grupogp.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ContenidoDao {
    @Query("SELECT * FROM colecciones WHERE activa=1 AND publicada=1 ORDER BY id DESC")
    fun coleccionesPublicadas(): Flow<List<ColeccionEntity>>
    @Query("SELECT cp.coleccionId, p.* FROM coleccion_productos cp JOIN productos p ON p.id=cp.productoId JOIN colecciones c ON c.id=cp.coleccionId WHERE c.activa=1 AND c.publicada=1 AND p.activo=1 ORDER BY p.id")
    fun miembrosPublicados(): Flow<List<MiembroPublicado>>
    @Query("SELECT * FROM campanias WHERE activa=1 AND publicada=1 AND inicio<=:hoy AND termino>=:hoy ORDER BY inicio DESC, id DESC")
    fun campaniasVigentes(hoy: String): Flow<List<CampaniaEntity>>
    @Query("SELECT * FROM colecciones WHERE EXISTS(SELECT 1 FROM usuarios WHERE id=:actor AND rol='ADMINISTRADOR') ORDER BY id DESC")
    fun coleccionesAdmin(actor: Long): Flow<List<ColeccionEntity>>
    @Query("SELECT * FROM coleccion_productos WHERE EXISTS(SELECT 1 FROM usuarios WHERE id=:actor AND rol='ADMINISTRADOR')")
    fun miembrosAdmin(actor: Long): Flow<List<ColeccionProducto>>
    @Query("SELECT * FROM campanias WHERE EXISTS(SELECT 1 FROM usuarios WHERE id=:actor AND rol='ADMINISTRADOR') ORDER BY id DESC")
    fun campaniasAdmin(actor: Long): Flow<List<CampaniaEntity>>
    @Insert suspend fun insertarColeccion(c: ColeccionEntity): Long
    @Update suspend fun actualizarColeccion(c: ColeccionEntity): Int
    @Query("UPDATE colecciones SET activa=0 WHERE id=:id") suspend fun desactivarColeccion(id: Long): Int
    @Query("DELETE FROM coleccion_productos WHERE coleccionId=:id") suspend fun limpiarAsociaciones(id: Long)
    @Insert suspend fun asociar(miembros: List<ColeccionProducto>)
    @Insert suspend fun insertarCampania(c: CampaniaEntity): Long
    @Update suspend fun actualizarCampania(c: CampaniaEntity): Int
    @Query("UPDATE campanias SET activa=0 WHERE id=:id") suspend fun desactivarCampania(id: Long): Int
}
