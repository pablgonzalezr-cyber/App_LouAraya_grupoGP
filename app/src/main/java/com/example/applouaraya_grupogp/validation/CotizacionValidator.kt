package com.example.applouaraya_grupogp.validation

import com.example.applouaraya_grupogp.model.CotizacionFormulario
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

object CotizacionValidator {
    val categorias = listOf("Interiores", "Matrimonios", "Festivales", "Otro")
    private val formato = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)
    fun fecha(valor: String): LocalDate? = runCatching { LocalDate.parse(valor.trim(), formato) }.getOrNull()
    fun medida(valor: String): Double? = valor.trim().replace(',', '.').toDoubleOrNull()

    fun validar(f: CotizacionFormulario, productosValidos: Set<Int>, hoy: LocalDate = LocalDate.now()): Map<String, String> = buildMap {
        if (f.descripcion.trim().length !in 10..1000) put("descripcion", "Describe tu solicitud usando entre 10 y 1000 caracteres")
        if (f.categoria !in categorias) put("categoria", "Selecciona una categoría")
        if (f.material.trim().length !in 2..100) put("material", "Ingresa un material de entre 2 y 100 caracteres")
        for ((campo, valor) in listOf("ancho" to f.ancho, "largo" to f.largo)) {
            val numero = medida(valor)
            if (numero == null || !numero.isFinite() || numero <= 0 || numero > 10000) put(campo, "Ingresa una medida mayor que 0 y hasta 10000 cm")
        }
        if (f.cantidad.trim().toIntOrNull() !in 1..1000) put("cantidad", "Ingresa una cantidad entera entre 1 y 1000")
        if (f.fecha.isNotBlank()) {
            val fecha = fecha(f.fecha)
            if (fecha == null || fecha.isBefore(hoy)) put("fecha", "Usa dd/mm/aaaa, con fecha de hoy o posterior")
        }
        if (f.productoId != null && f.productoId !in productosValidos) put("producto", "Selecciona un producto válido o sin referencia")
    }
}
