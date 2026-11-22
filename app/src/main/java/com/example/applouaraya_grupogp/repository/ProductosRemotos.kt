package com.example.applouaraya_grupogp.repository

import android.content.SharedPreferences
import com.example.applouaraya_grupogp.BuildConfig
import com.example.applouaraya_grupogp.model.Producto
import com.example.applouaraya_grupogp.validation.ProductoFormulario
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

internal data class DatosProducto(
    val nombre: String, val descripcion: String, val categoria: String, val material: String,
    val anchoCm: BigDecimal, val altoCm: BigDecimal, val precio: BigDecimal,
    val disponible: Boolean, val publicado: Boolean
)

internal data class ProductoRespuesta(
    val id: Long,
    val nombre: String, val descripcion: String, val categoria: String, val material: String,
    val anchoCm: BigDecimal, val altoCm: BigDecimal, val precio: BigDecimal,
    val disponible: Boolean, val publicado: Boolean
) {
    fun idAndroid(): Int {
        require(id in 1..Int.MAX_VALUE.toLong()) { "El producto $id excede el rango de IDs de esta aplicación." }
        return id.toInt()
    }
}

internal data class PublicacionProducto(val publicado: Boolean)

internal interface ProductosApi {
    @GET("api/productos")
    suspend fun publicados(): List<ProductoRespuesta>

    @GET("api/productos/{id}")
    suspend fun detalle(@Path("id") id: Long): ProductoRespuesta

    @GET("api/admin/productos")
    suspend fun administracion(@Header("Authorization") credencial: String): List<ProductoRespuesta>

    @POST("api/admin/productos")
    suspend fun crear(@Header("Authorization") credencial: String, @Body datos: DatosProducto): ProductoRespuesta

    @PUT("api/admin/productos/{id}")
    suspend fun editar(@Header("Authorization") credencial: String, @Path("id") id: Long,
                       @Body datos: DatosProducto): ProductoRespuesta

    @PATCH("api/admin/productos/{id}/publicacion")
    suspend fun publicar(@Header("Authorization") credencial: String, @Path("id") id: Long,
                         @Body datos: PublicacionProducto): ProductoRespuesta

    @DELETE("api/admin/productos/{id}")
    suspend fun archivar(@Header("Authorization") credencial: String, @Path("id") id: Long)
}

class ProductosRemotos(
    private val fotos: SharedPreferences,
    private val buscarSemillaLocal: suspend (Int) -> Producto?,
    urlPublica: String = BuildConfig.CATALOGO_URL,
    private val urlAdmin: String = BuildConfig.CATALOGO_ADMIN_URL
) {
    private val cambios = MutableStateFlow(0L)
    private val publica by lazy { api(urlPublica, administrativa = false) }
    private val admin by lazy { api(urlAdmin, administrativa = true) }
    private var respuestasAdmin = emptyMap<Int, ProductoRespuesta>()

    val permiteAdministracion: Boolean get() = HttpUrl.parse(urlAdmin)?.isHttps == true

    fun observarPublicados() = cambios.map { consultar { publica.publicados().map { producto(it) } } }
    fun observarDetalle(id: Int) = cambios.map { consultar { producto(publica.detalle(id.toLong())) } }

    suspend fun administracion(credencial: String): List<Producto> = consultar {
        val respuestas = admin.administracion(credencial)
        val productos = respuestas.map { producto(it) }
        respuestasAdmin = respuestas.associateBy { it.idAndroid() }
        productos
    }

    fun formulario(p: Producto): ProductoFormulario {
        val f = ProductoFormulario.desde(p)
        val remoto = respuestasAdmin[p.id] ?: return f
        return f.copy(ancho = remoto.anchoCm.stripTrailingZeros().toPlainString(),
            largo = remoto.altoCm.stripTrailingZeros().toPlainString(),
            precio = remoto.precio.stripTrailingZeros().toPlainString())
    }

    suspend fun guardar(credencial: String, f: ProductoFormulario) = consultar {
        val datos = DatosProducto(f.nombre.trim(), f.descripcion.trim(), f.categoria.trim(), f.material.trim(),
            decimal(f.ancho)!!, decimal(f.largo)!!, decimal(f.precio)!!, f.disponible, f.activo)
        val respuesta = if (f.id == 0) admin.crear(credencial, datos)
            else admin.editar(credencial, f.id.toLong(), datos)
        val id = respuesta.idAndroid()
        fotos.edit().putString("$id.nombre", f.imagenNombre).putString("$id.archivo", f.imagenArchivo).apply()
        cambios.value++
    }

    suspend fun publicar(credencial: String, id: Int, publicado: Boolean) = consultar {
        admin.publicar(credencial, id.toLong(), PublicacionProducto(publicado))
        cambios.value++
    }

    suspend fun archivar(credencial: String, id: Int) = consultar {
        admin.archivar(credencial, id.toLong())
        cambios.value++
    }

    internal suspend fun producto(r: ProductoRespuesta): Producto {
        val id = r.idAndroid()
        val semilla = ProductosDemo.productos.firstOrNull { it.id == id }
        val local = if (semilla != null) buscarSemillaLocal(id) ?: semilla else null
        return Producto(id, r.nombre, r.categoria, r.material, entero(r.precio), r.descripcion,
            entero(r.anchoCm), entero(r.altoCm), r.disponible,
            fotos.getString("$id.nombre", local?.imagenNombre.orEmpty()).orEmpty(), r.publicado,
            fotos.getString("$id.archivo", local?.imagenArchivo.orEmpty()).orEmpty())
    }

    companion object {
        fun credencial(usuario: String, clave: String): String {
            require(usuario.isNotBlank() && ':' !in usuario && clave.isNotEmpty()) { "Ingresa las credenciales del servidor." }
            return Credentials.basic(usuario, clave, StandardCharsets.UTF_8)
        }

        fun decimal(valor: String): BigDecimal? = valor.trim().replace(',', '.').toBigDecimalOrNull()

        private fun entero(valor: BigDecimal): Int = valor.setScale(0, RoundingMode.HALF_UP).intValueExact()

        internal fun api(valor: String, administrativa: Boolean): ProductosApi =
            retrofit(valor, administrativa).create(ProductosApi::class.java)

        internal fun retrofit(valor: String, administrativa: Boolean): Retrofit {
            val url = HttpUrl.parse(valor)
            require(url != null && url.username().isEmpty() && url.password().isEmpty() && url.encodedPath().endsWith('/') &&
                url.query() == null && url.fragment() == null) { "Configura una URL de catálogo válida terminada en /." }
            require(url.isHttps || (!administrativa && BuildConfig.DEBUG && url.host() in setOf("127.0.0.1", "10.0.2.2"))) {
                "Configura HTTPS para conectar con el servidor de catálogo."
            }
            val cliente = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
                .retryOnConnectionFailure(false).build()
            return Retrofit.Builder().baseUrl(url).client(cliente)
                .addConverterFactory(GsonConverterFactory.create()).build()
        }

        internal suspend fun <T> consultar(bloque: suspend () -> T): T =
            consultar("El producto ya no está disponible (404).", bloque)

        internal suspend fun <T> consultar(noEncontrado: String, bloque: suspend () -> T): T = try {
            bloque()
        } catch (e: HttpException) {
            val mensaje = when (e.code()) {
                400 -> "El servidor rechazó los datos (400). Revisa los campos."
                401 -> "Credenciales del catálogo requeridas o incorrectas (401)."
                403 -> "La cuenta del servidor no tiene permiso administrativo (403)."
                404 -> noEncontrado
                else -> "El servidor respondió con HTTP ${e.code()}."
            }
            throw IllegalStateException(mensaje, e)
        } catch (e: IOException) {
            throw IllegalStateException("No se pudo comunicar con el servidor de catálogo. Revisa la conexión y el certificado HTTPS.", e)
        }
    }
}
