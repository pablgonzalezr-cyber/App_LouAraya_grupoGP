package com.example.applouaraya_grupogp.repository

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

internal data class RespuestaIndicadores(val dolar: DolarDto?)
internal data class DolarDto(val codigo: String?, val unidad_medida: String?, val fecha: String?, val valor: BigDecimal?)

internal interface IndicadoresApi {
    @GET("api") suspend fun indicadores(): RespuestaIndicadores
}

data class DolarObservado(val pesosPorDolar: BigDecimal, val fecha: LocalDate) {
    init { require(pesosPorDolar.signum() > 0) }
    fun convertir(precioClp: Int): BigDecimal =
        precioClp.toBigDecimal().divide(pesosPorDolar, 2, RoundingMode.HALF_UP)
}

data class ConsultaDolar(val indicador: DolarObservado? = null, val error: String? = null)

internal class IndicadoresRepository(
    private val api: IndicadoresApi,
    private val relojMs: () -> Long = { System.nanoTime() / 1_000_000L }
) {
    private val mutex = Mutex()
    private var ultimoIntento: Long? = null
    private var resultado = ConsultaDolar()

    suspend fun obtener(actualizar: Boolean = false): ConsultaDolar = mutex.withLock {
        val intervalo = if (actualizar || resultado.error != null) 30_000L else 3_600_000L
        if (ultimoIntento?.let { relojMs() - it < intervalo } == true) return@withLock resultado
        try {
            val d = requireNotNull(api.indicadores().dolar)
            require(d.codigo == "dolar" && d.unidad_medida == "Pesos")
            val dia = Instant.parse(requireNotNull(d.fecha)).atZone(ZoneId.of("America/Santiago")).toLocalDate()
            resultado = ConsultaDolar(DolarObservado(requireNotNull(d.valor), dia))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            resultado = resultado.copy(error = "No se pudo actualizar el dólar observado.")
        }
        ultimoIntento = relojMs()
        resultado
    }

    companion object {
        val compartido by lazy { IndicadoresRepository(servicio("https://mindicador.cl/")) }

        internal fun servicio(url: String): IndicadoresApi = Retrofit.Builder().baseUrl(url)
            .client(OkHttpClient.Builder().followSslRedirects(false).build())
            .addConverterFactory(GsonConverterFactory.create()).build().create(IndicadoresApi::class.java)
    }
}
