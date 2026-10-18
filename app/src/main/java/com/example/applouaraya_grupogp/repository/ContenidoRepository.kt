package com.example.applouaraya_grupogp.repository

import androidx.room.withTransaction
import com.example.applouaraya_grupogp.data.AppDatabase
import com.example.applouaraya_grupogp.model.*
import com.example.applouaraya_grupogp.validation.*
import java.time.LocalDate
import kotlinx.coroutines.flow.*

data class ContenidoAdminDatos(val colecciones: List<ColeccionEntity>, val miembros: List<ColeccionProducto>,
    val campanias: List<CampaniaEntity>, val productos: List<Producto>)
data class ContenidoClienteDatos(val colecciones: List<ColeccionVista>, val campanias: List<CampaniaEntity>)

class ContenidoRepository(private val db: AppDatabase, private val productos: ProductosRepository) {
    fun observarCliente(hoy: LocalDate = LocalDate.now()) = combine(db.contenidoDao().coleccionesPublicadas(),
        db.contenidoDao().miembrosPublicados(), db.contenidoDao().campaniasVigentes(hoy.toString())) { colecciones, miembros, campanias ->
        ContenidoClienteDatos(colecciones.map { c -> ColeccionVista(c, miembros.filter { it.coleccionId == c.id }.map { it.producto }) }, campanias)
    }
    fun observarAdministracion() = flow {
        val id=productos.exigirAdministrador()
        productos.inicializar()
        val datos=combine(db.contenidoDao().coleccionesAdmin(id), db.contenidoDao().miembrosAdmin(id),
            db.contenidoDao().campaniasAdmin(id), db.productoDao().observarAdministracion(id)) { c,m,p,productos -> ContenidoAdminDatos(c,m,p,productos) }
        emitAll(combine(db.usuarioDao().observarPorId(id), datos) { u, d ->
            check(productos.exigirAdministrador() == id) { "La sesión cambió" }
            PoliticaAdministracion.exigirAdministrador(u?.rol)
            d
        })
    }
    suspend fun guardarColeccion(f: ColeccionFormulario): Long = db.withTransaction {
        productos.exigirAdministrador()
        require(ContenidoValidator.coleccion(f).isEmpty()) { "Revisa los campos de la colección" }
        f.productos.forEach { require(db.productoDao().buscar(it) != null) { "Uno de los productos ya no existe" } }
        val c=ColeccionEntity(f.id,f.nombre.trim(),f.descripcion.trim(),f.activa,f.publicada)
        val id=if(f.id==0L) db.contenidoDao().insertarColeccion(c) else {
            check(db.contenidoDao().actualizarColeccion(c)==1) { "Colección no encontrada" }; f.id
        }
        db.contenidoDao().limpiarAsociaciones(id)
        db.contenidoDao().asociar(f.productos.map { ColeccionProducto(id,it) })
        id
    }
    suspend fun guardarCampania(f: CampaniaFormulario): Long = db.withTransaction {
        productos.exigirAdministrador()
        require(ContenidoValidator.campania(f).isEmpty()) { "Revisa las fechas y campos de la campaña" }
        val c=CampaniaEntity(f.id,f.titulo.trim(),f.descripcion.trim(),requireNotNull(ContenidoValidator.fecha(f.inicio)).toString(),
            requireNotNull(ContenidoValidator.fecha(f.termino)).toString(),f.publicada,f.activa)
        val id = if(f.id==0L) db.contenidoDao().insertarCampania(c) else {
            check(db.contenidoDao().actualizarCampania(c)==1) { "Campaña no encontrada" }; f.id
        }
        if(c.activa && c.publicada && c.termino >= LocalDate.now().toString()) {
            db.avisoDao().campaniaPublicada(id, System.currentTimeMillis())
        }
        id
    }
    suspend fun desactivarColeccion(id: Long) = db.withTransaction { productos.exigirAdministrador(); check(db.contenidoDao().desactivarColeccion(id)==1) }
    suspend fun desactivarCampania(id: Long) = db.withTransaction { productos.exigirAdministrador(); check(db.contenidoDao().desactivarCampania(id)==1) }
}
