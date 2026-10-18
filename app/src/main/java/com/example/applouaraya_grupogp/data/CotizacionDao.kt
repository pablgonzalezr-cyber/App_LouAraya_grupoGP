package com.example.applouaraya_grupogp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.applouaraya_grupogp.model.CotizacionEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CotizacionDao {
    @Query("SELECT * FROM cotizaciones WHERE EXISTS (SELECT 1 FROM usuarios WHERE id = :actorId AND rol = 'ADMINISTRADOR') ORDER BY creadaEn DESC, id DESC")
    abstract fun observarAdministracion(actorId: Long): Flow<List<CotizacionEntity>>

    @Query("SELECT * FROM cotizaciones WHERE id = :id AND EXISTS (SELECT 1 FROM usuarios WHERE id = :actorId AND rol = 'ADMINISTRADOR') LIMIT 1")
    abstract fun detalleAdministracion(actorId: Long, id: Long): Flow<CotizacionEntity?>

    @Query("SELECT * FROM cotizaciones WHERE id = :id LIMIT 1")
    abstract suspend fun buscarPorId(id: Long): CotizacionEntity?

    @Query("UPDATE cotizaciones SET estado = :nuevo WHERE id = :id AND estado = :anterior AND EXISTS (SELECT 1 FROM usuarios WHERE id = :actorId AND rol = 'ADMINISTRADOR')")
    protected abstract suspend fun actualizarEstado(actorId: Long, id: Long, anterior: String, nuevo: String): Int

    @Query("""INSERT OR IGNORE INTO avisos (usuarioId,tipo,titulo,mensaje,referenciaId,claveEvento,creadaEn,leida)
        SELECT usuarioId,'ESTADO','Estado de cotización actualizado','Tu solicitud #' || id || ' cambió a ' || estado || '.',id,
        'estado:' || id || ':' || estado,:ahora,0 FROM cotizaciones WHERE id=:id""")
    protected abstract suspend fun avisarEstado(id: Long, ahora: Long)

    @Transaction
    open suspend fun cambiarEstadoAdministracion(actorId: Long, id: Long, anterior: String, nuevo: String): Int {
        val cambios = actualizarEstado(actorId, id, anterior, nuevo)
        if (cambios == 1) avisarEstado(id, System.currentTimeMillis())
        return cambios
    }
    @Query("SELECT * FROM cotizaciones WHERE usuarioId = :usuarioId ORDER BY creadaEn DESC, id DESC")
    abstract fun observarPropias(usuarioId: Long): Flow<List<CotizacionEntity>>

    @Query("SELECT * FROM cotizaciones WHERE id = :id AND usuarioId = :usuarioId LIMIT 1")
    abstract fun observarDetalle(usuarioId: Long, id: Long): Flow<CotizacionEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertar(solicitud: CotizacionEntity): Long

    @Query("SELECT id FROM cotizaciones WHERE tokenEnvio = :token AND usuarioId = :usuarioId")
    abstract suspend fun buscarToken(token: String, usuarioId: Long): Long?

    @Query("""INSERT OR IGNORE INTO avisos (usuarioId,tipo,titulo,mensaje,referenciaId,claveEvento,creadaEn,leida)
        SELECT id,'NUEVA_SOLICITUD','Nueva solicitud','Hay una nueva solicitud #' || :solicitud || ' para revisar.',:solicitud,
        'solicitud:' || :solicitud,:ahora,0 FROM usuarios WHERE rol='ADMINISTRADOR'""")
    protected abstract suspend fun avisarAdministradores(solicitud: Long, ahora: Long)

    @Transaction
    open suspend fun guardarUnaVez(solicitud: CotizacionEntity): Long {
        val id = insertar(solicitud)
        if (id != -1L) avisarAdministradores(id, solicitud.creadaEn)
        return if (id != -1L) id else checkNotNull(buscarToken(solicitud.tokenEnvio, solicitud.usuarioId))
    }
}
