package com.example.applouaraya_grupogp.repository

import com.example.applouaraya_grupogp.data.CotizacionDao
import com.example.applouaraya_grupogp.data.ProductoDao
import kotlinx.coroutines.flow.flowOf
import com.example.applouaraya_grupogp.model.CotizacionEntity
import com.example.applouaraya_grupogp.model.CotizacionFormulario
import com.example.applouaraya_grupogp.validation.CotizacionValidator

class CotizacionRepository(private val dao: CotizacionDao, private val productos: ProductoDao? = null) {
    fun observarProductos() = productos?.observarPublicados() ?: flowOf(ProductosDemo.productos)
    fun observarPropias(usuarioId: Long) = dao.observarPropias(usuarioId)
    fun observarDetalle(usuarioId: Long, id: Long) = dao.observarDetalle(usuarioId, id)

    suspend fun guardar(usuarioId: Long, token: String, f: CotizacionFormulario): Long {
        require(usuarioId > 0)
        val catalogo = productos?.publicados() ?: ProductosDemo.productos
        require(CotizacionValidator.validar(f, catalogo.map { it.id }.toSet()).isEmpty())
        val producto = catalogo.find { it.id == f.productoId }
        return dao.guardarUnaVez(CotizacionEntity(
            usuarioId = usuarioId, tokenEnvio = token, descripcion = f.descripcion.trim(),
            categoria = f.categoria, material = f.material.trim(),
            anchoCm = requireNotNull(CotizacionValidator.medida(f.ancho)),
            largoCm = requireNotNull(CotizacionValidator.medida(f.largo)),
            cantidad = f.cantidad.trim().toInt(),
            fechaRequerida = CotizacionValidator.fecha(f.fecha)?.toString(),
            productoReferenciaId = producto?.id, productoReferenciaNombre = producto?.nombre,
            creadaEn = System.currentTimeMillis()
        ))
    }
}
