package com.example.applouaraya_grupogp.repository

import com.example.applouaraya_grupogp.data.CotizacionDao
import com.example.applouaraya_grupogp.model.CotizacionEntity
import com.example.applouaraya_grupogp.model.CotizacionFormulario
import com.example.applouaraya_grupogp.validation.CotizacionValidator

class CotizacionRepository(private val dao: CotizacionDao) {
    fun observarPropias(usuarioId: Long) = dao.observarPropias(usuarioId)
    fun observarDetalle(usuarioId: Long, id: Long) = dao.observarDetalle(usuarioId, id)

    suspend fun guardar(usuarioId: Long, token: String, f: CotizacionFormulario): Long {
        require(usuarioId > 0)
        require(CotizacionValidator.validar(f, ProductosDemo.productos.map { it.id }.toSet()).isEmpty())
        val producto = ProductosDemo.productos.find { it.id == f.productoId }
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
