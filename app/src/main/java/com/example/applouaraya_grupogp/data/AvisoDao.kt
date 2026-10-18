package com.example.applouaraya_grupogp.data

import androidx.room.*
import com.example.applouaraya_grupogp.model.AvisoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AvisoDao {
    @Query("SELECT * FROM avisos WHERE usuarioId=:usuario ORDER BY creadaEn DESC, id DESC")
    fun observar(usuario: Long): Flow<List<AvisoEntity>>
    @Query("SELECT * FROM avisos WHERE usuarioId=:usuario AND id=:id")
    suspend fun buscar(usuario: Long, id: Long): AvisoEntity?
    @Query("UPDATE avisos SET leida=1 WHERE usuarioId=:usuario AND id=:id")
    suspend fun marcarLeida(usuario: Long, id: Long): Int

    // Una publicación genera como máximo un aviso por cliente. Editar o republicar no duplica.
    @Query("""INSERT OR IGNORE INTO avisos (usuarioId,tipo,titulo,mensaje,referenciaId,claveEvento,creadaEn,leida)
        SELECT id,'CAMPANIA','Campaña publicada','Hay una campaña publicada para consultar.',:campania,
        'campania:' || :campania,:ahora,0 FROM usuarios WHERE rol='CLIENTE'""")
    suspend fun campaniaPublicada(campania: Long, ahora: Long)
}
