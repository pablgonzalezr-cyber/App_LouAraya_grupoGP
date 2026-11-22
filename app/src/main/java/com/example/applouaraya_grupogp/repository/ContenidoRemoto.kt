package com.example.applouaraya_grupogp.repository

import com.example.applouaraya_grupogp.BuildConfig
import com.example.applouaraya_grupogp.model.*
import com.example.applouaraya_grupogp.validation.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import retrofit2.http.*

internal data class ColeccionRespuesta<T>(val id: Long, val nombre: String, val descripcion: String,
    val activa: Boolean, val publicada: Boolean, val productos: List<T>) {
    fun coleccion() = ColeccionEntity(id, nombre, descripcion, activa, publicada)
}

internal data class DatosColeccion(val nombre: String, val descripcion: String, val activa: Boolean,
    val publicada: Boolean, val productos: Set<Int>)

internal data class DatosCampania(val titulo: String, val descripcion: String, val inicio: String,
    val termino: String, val publicada: Boolean, val activa: Boolean)

internal fun ColeccionFormulario.datos() = DatosColeccion(nombre.trim(), descripcion.trim(), activa, publicada, productos)
internal fun CampaniaFormulario.datos() = DatosCampania(titulo.trim(), descripcion.trim(),
    requireNotNull(ContenidoValidator.fecha(inicio)).toString(),
    requireNotNull(ContenidoValidator.fecha(termino)).toString(), publicada, activa)

internal data class PublicacionContenido(val publicada: Boolean)

internal interface ContenidoApi {
    @GET("api/colecciones") suspend fun colecciones(): List<ColeccionRespuesta<ProductoRespuesta>>
    @GET("api/campanias") suspend fun campanias(): List<CampaniaEntity>
    @GET("api/campanias/{id}") suspend fun campania(@Path("id") id: Long): CampaniaEntity

    @GET("api/admin/colecciones") suspend fun coleccionesAdmin(@Header("Authorization") cuenta: String): List<ColeccionRespuesta<Long>>
    @GET("api/admin/campanias") suspend fun campaniasAdmin(@Header("Authorization") cuenta: String): List<CampaniaEntity>
    @POST("api/admin/colecciones") suspend fun crearColeccion(@Header("Authorization") cuenta: String, @Body datos: DatosColeccion): ColeccionRespuesta<Long>
    @PUT("api/admin/colecciones/{id}") suspend fun editarColeccion(@Header("Authorization") cuenta: String, @Path("id") id: Long, @Body datos: DatosColeccion): ColeccionRespuesta<Long>
    @POST("api/admin/campanias") suspend fun crearCampania(@Header("Authorization") cuenta: String, @Body datos: DatosCampania): CampaniaEntity
    @PUT("api/admin/campanias/{id}") suspend fun editarCampania(@Header("Authorization") cuenta: String, @Path("id") id: Long, @Body datos: DatosCampania): CampaniaEntity
    @PATCH("api/admin/colecciones/{id}/publicacion") suspend fun publicarColeccion(@Header("Authorization") cuenta: String, @Path("id") id: Long, @Body datos: PublicacionContenido): ColeccionRespuesta<Long>
    @PATCH("api/admin/campanias/{id}/publicacion") suspend fun publicarCampania(@Header("Authorization") cuenta: String, @Path("id") id: Long, @Body datos: PublicacionContenido): CampaniaEntity
    @DELETE("api/admin/colecciones/{id}") suspend fun desactivarColeccion(@Header("Authorization") cuenta: String, @Path("id") id: Long)
    @DELETE("api/admin/campanias/{id}") suspend fun desactivarCampania(@Header("Authorization") cuenta: String, @Path("id") id: Long)
}

internal class ContenidoRemoto(private val productos: ProductosRemotos) {
    private val publica by lazy { ProductosRemotos.retrofit(BuildConfig.CATALOGO_URL, false).create(ContenidoApi::class.java) }
    private val admin by lazy { ProductosRemotos.retrofit(BuildConfig.CATALOGO_ADMIN_URL, true).create(ContenidoApi::class.java) }
    private val revision = MutableStateFlow(0L)

    fun observarCliente() = revision.map {
        consultar {
            val vistas = publica.colecciones().map { c -> ColeccionVista(c.coleccion(), c.productos.map { productos.producto(it) }) }
            ContenidoClienteDatos(vistas, publica.campanias())
        }
    }

    suspend fun comprobarCampania(id: Long) { consultar { publica.campania(id) } }

    suspend fun administracion(cuenta: String) = consultar {
        val colecciones = admin.coleccionesAdmin(cuenta)
        val miembros = colecciones.flatMap { c -> c.productos.map { id ->
            require(id in 1..Int.MAX_VALUE.toLong()) { "El ID de producto excede el rango de Android." }
            ColeccionProducto(c.id, id.toInt())
        } }
        ContenidoAdminDatos(colecciones.map { it.coleccion() }, miembros, admin.campaniasAdmin(cuenta), productos.administracion(cuenta))
    }

    suspend fun guardar(cuenta: String, f: ColeccionFormulario) = modificar {
        if (f.id == 0L) admin.crearColeccion(cuenta, f.datos()) else admin.editarColeccion(cuenta, f.id, f.datos())
    }

    suspend fun guardar(cuenta: String, f: CampaniaFormulario) = modificar {
        if (f.id == 0L) admin.crearCampania(cuenta, f.datos()) else admin.editarCampania(cuenta, f.id, f.datos())
    }

    suspend fun publicar(cuenta: String, id: Long, campania: Boolean, publicada: Boolean): CampaniaEntity? = modificar {
        val datos = PublicacionContenido(publicada)
        if (campania) admin.publicarCampania(cuenta, id, datos)
        else { admin.publicarColeccion(cuenta, id, datos); null }
    }

    suspend fun desactivar(cuenta: String, id: Long, campania: Boolean) = modificar {
        if (campania) admin.desactivarCampania(cuenta, id) else admin.desactivarColeccion(cuenta, id)
    }

    private suspend fun <T> modificar(accion: suspend () -> T): T =
        consultar(accion).also { revision.update { it + 1 } }

    private suspend fun <T> consultar(accion: suspend () -> T): T =
        ProductosRemotos.consultar("La colección o campaña ya no está disponible (404).", accion)
}
