package com.example.applouaraya_grupogp.repository

import androidx.room.withTransaction
import com.example.applouaraya_grupogp.data.AppDatabase
import com.example.applouaraya_grupogp.validation.ProductoFormulario
import com.example.applouaraya_grupogp.validation.ProductoValidator
import kotlinx.coroutines.flow.*

class ProductosRepository(private val db: AppDatabase, val remoto: ProductosRemotos? = null,
                          private val usuarioAutenticado: () -> Long? = { null }) {
    suspend fun inicializar() { db.productoDao().insertarIniciales(ProductosDemo.productos) }
    suspend fun exigirAdministrador(): Long {
        val id = usuarioAutenticado() ?: throw SecurityException("Inicia sesión nuevamente")
        PoliticaAdministracion.exigirAdministrador(db.usuarioDao().buscarPorId(id)?.rol)
        if (usuarioAutenticado() != id) throw SecurityException("La sesión cambió")
        return id
    }
    fun observarPublicados() = remoto?.observarPublicados() ?: flow {
        inicializar()
        emitAll(db.productoDao().observarPublicados())
    }
    fun observarDetalle(id: Int) = remoto?.observarDetalle(id) ?: observarPublicados().map { lista ->
        lista.firstOrNull { it.id == id } ?: error("El producto ya no está disponible.")
    }

    // Los ocho productos iniciales comparten identidad con Spring. Los demás
    // favoritos remotos usan IDs negativos para evitar colisiones con Room.
    fun idFavorito(id: Int): Int =
        if (remoto == null || ProductosDemo.productos.any { it.id == id }) id else -id
    fun observarAdministracion() = flow {
        val id = exigirAdministrador()
        inicializar()
        emitAll(combine(db.usuarioDao().observarPorId(id), db.productoDao().observarAdministracion(id)) { u, lista ->
            if (usuarioAutenticado() != id) throw SecurityException("La sesión cambió")
            PoliticaAdministracion.exigirAdministrador(u?.rol)
            lista
        })
    }
    suspend fun guardar(f: ProductoFormulario): Int = db.withTransaction {
        exigirAdministrador()
        require(ProductoValidator.validar(f).isEmpty()) { "Revisa los campos del producto" }
        inicializar()
        if (f.id == 0) db.productoDao().insertar(f.producto()).toInt()
        else {
            check(db.productoDao().actualizar(f.producto()) == 1) { "El producto no está disponible" }
            f.id
        }
    }
    suspend fun desactivar(id: Int) = db.withTransaction {
        exigirAdministrador()
        val p = checkNotNull(db.productoDao().buscar(id)) { "Producto no encontrado" }
        check(db.productoDao().actualizar(p.copy(activo = false)) == 1)
    }
}
