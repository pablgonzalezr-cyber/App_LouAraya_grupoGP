package com.example.applouaraya_grupogp.repository

import com.example.applouaraya_grupogp.BuildConfig
import java.io.IOException
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Credencial de sesión Basic. Su propietario controla su vida en memoria. */
class CuentaCotizaciones internal constructor(internal val autorizacion: String)

class ErrorCotizaciones(val codigo: Int, causa: HttpException) : Exception(when (codigo) {
    400 -> "Revisa los datos de la solicitud."
    401 -> "Credenciales de cotizaciones incorrectas."
    403 -> "Esta cuenta no tiene permiso para la operación."
    404 -> "La cotización no está disponible."
    409 -> "Conflicto en el servidor. Revisa el estado o el token de envío."
    else -> "El servicio de cotizaciones respondió HTTP $codigo."
}, causa)

class CotizacionesRemotasRepository(url: String = BuildConfig.COTIZACIONES_URL) {
    private val api: CotizacionesApi by lazy {
        val base = HttpUrl.parse(url)
        require(base != null && base.username().isEmpty() && base.password().isEmpty() &&
            base.query() == null && base.fragment() == null && base.encodedPath().endsWith('/')) {
            "Configura louCotizacionesUrl con una URL base válida."
        }
        require(base.isHttps || (BuildConfig.DEBUG && base.host() in setOf("127.0.0.1", "10.0.2.2"))) {
            "Cotizaciones requiere HTTPS o un puerto local de pruebas en debug."
        }
        val cliente = OkHttpClient.Builder().followRedirects(false).retryOnConnectionFailure(false).build()
        Retrofit.Builder().baseUrl(base).client(cliente).addConverterFactory(GsonConverterFactory.create())
            .build().create(CotizacionesApi::class.java)
    }

    suspend fun registrar(correo: String, clave: String) = solicitar { api.registrar(RegistroClienteRequest(correo.trim(), clave)) }

    suspend fun iniciarSesionCliente(correo: String, clave: String): CuentaCotizaciones =
        cuenta(correo, clave).also { historial(it) }

    suspend fun iniciarSesionAdmin(usuario: String, clave: String): CuentaCotizaciones =
        cuenta(usuario, clave).also { solicitudes(it) }

    suspend fun crear(cuenta: CuentaCotizaciones, datos: NuevaCotizacion) = solicitar { api.crear(cuenta.autorizacion, datos) }
    suspend fun historial(cuenta: CuentaCotizaciones) = solicitar { api.historial(cuenta.autorizacion) }
    suspend fun propia(cuenta: CuentaCotizaciones, id: Long) = solicitar { api.propia(cuenta.autorizacion, id) }
    suspend fun solicitudes(cuenta: CuentaCotizaciones, estado: EstadoCotizacionRemota? = null) =
        solicitar { api.solicitudes(cuenta.autorizacion, estado) }
    suspend fun detalleAdmin(cuenta: CuentaCotizaciones, id: Long) = solicitar { api.detalleAdmin(cuenta.autorizacion, id) }
    suspend fun cambiarEstado(cuenta: CuentaCotizaciones, id: Long, esperado: EstadoCotizacionRemota, nuevo: EstadoCotizacionRemota) =
        solicitar { api.cambiarEstado(cuenta.autorizacion, id, CambioEstadoRequest(esperado, nuevo)) }

    private fun cuenta(usuario: String, clave: String): CuentaCotizaciones {
        require(usuario.isNotBlank() && ':' !in usuario && clave.isNotEmpty()) { "Ingresa usuario y contraseña." }
        return CuentaCotizaciones(Credentials.basic(usuario.trim(), clave, Charsets.UTF_8))
    }

    private suspend fun <T> solicitar(accion: suspend () -> T): T = try {
        accion()
    } catch (e: HttpException) {
        throw ErrorCotizaciones(e.code(), e)
    } catch (e: IOException) {
        throw IOException("No se pudo leer la respuesta del servicio de cotizaciones.", e)
    }
}
