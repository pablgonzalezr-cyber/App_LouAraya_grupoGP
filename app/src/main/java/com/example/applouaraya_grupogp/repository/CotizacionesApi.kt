package com.example.applouaraya_grupogp.repository

import java.math.BigDecimal
import retrofit2.http.*

internal class RegistroClienteRequest(val correo: String, val clave: String)
data class ClienteRegistrado(val id: Long, val correo: String)
enum class EstadoCotizacionRemota { PENDIENTE, EN_REVISION, APROBADA, RECHAZADA }
internal data class CambioEstadoRequest(val esperado: EstadoCotizacionRemota, val nuevo: EstadoCotizacionRemota)

data class NuevaCotizacion(
    val tokenEnvio: String, val descripcion: String, val categoria: String, val material: String,
    val anchoCm: BigDecimal, val largoCm: BigDecimal, val cantidad: Int, val fechaRequerida: String? = null,
    val productoReferenciaId: Long? = null, val productoReferenciaNombre: String? = null
)

data class CotizacionRemota(
    val id: Long, val usuarioId: Long, val tokenEnvio: String, val descripcion: String,
    val categoria: String, val material: String, val anchoCm: BigDecimal, val largoCm: BigDecimal,
    val cantidad: Int, val fechaRequerida: String?, val productoReferenciaId: Long?,
    val productoReferenciaNombre: String?, val creadaEn: Long, val estado: EstadoCotizacionRemota
)

internal interface CotizacionesApi {
    @POST("api/clientes/registro")
    suspend fun registrar(@Body datos: RegistroClienteRequest): ClienteRegistrado

    @POST("api/cotizaciones")
    suspend fun crear(@Header("Authorization") cuenta: String, @Body datos: NuevaCotizacion): CotizacionRemota

    @GET("api/cotizaciones/mias")
    suspend fun historial(@Header("Authorization") cuenta: String): List<CotizacionRemota>

    @GET("api/cotizaciones/mias/{id}")
    suspend fun propia(@Header("Authorization") cuenta: String, @Path("id") id: Long): CotizacionRemota

    @GET("api/admin/cotizaciones")
    suspend fun solicitudes(@Header("Authorization") cuenta: String, @Query("estado") estado: EstadoCotizacionRemota?): List<CotizacionRemota>

    @GET("api/admin/cotizaciones/{id}")
    suspend fun detalleAdmin(@Header("Authorization") cuenta: String, @Path("id") id: Long): CotizacionRemota

    @PATCH("api/admin/cotizaciones/{id}/estado")
    suspend fun cambiarEstado(@Header("Authorization") cuenta: String, @Path("id") id: Long, @Body datos: CambioEstadoRequest): CotizacionRemota
}
