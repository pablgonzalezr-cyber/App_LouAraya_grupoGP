package com.example.applouaraya_grupogp.validation

import com.example.applouaraya_grupogp.model.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

data class ColeccionFormulario(val id: Long = 0, val nombre: String = "", val descripcion: String = "",
    val activa: Boolean = true, val publicada: Boolean = false, val productos: Set<Int> = emptySet())
data class CampaniaFormulario(val id: Long = 0, val titulo: String = "", val descripcion: String = "",
    val inicio: String = "", val termino: String = "", val publicada: Boolean = false, val activa: Boolean = true)

object ContenidoValidator {
    private val formato = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)
    fun fecha(valor: String): LocalDate? = runCatching { LocalDate.parse(valor.trim(), formato) }.getOrNull()?.takeIf { it.year in 1..9999 }
    fun mostrarFecha(iso: String): String = LocalDate.parse(iso).format(formato)
    fun coleccion(f: ColeccionFormulario): Map<String,String> = buildMap {
        if(f.nombre.trim().length !in 2..100) put("nombre", "Ingresa entre 2 y 100 caracteres")
        if(f.descripcion.trim().length !in 10..2000) put("descripcion", "Ingresa entre 10 y 2000 caracteres")
        if(f.publicada && f.productos.isEmpty()) put("productos", "Asocia al menos un producto para publicar")
    }
    fun campania(f: CampaniaFormulario): Map<String,String> = buildMap {
        if(f.titulo.trim().length !in 2..100) put("titulo", "Ingresa entre 2 y 100 caracteres")
        if(f.descripcion.trim().length !in 10..2000) put("descripcion", "Ingresa entre 10 y 2000 caracteres")
        val desde = fecha(f.inicio); val hasta = fecha(f.termino)
        if(desde == null) put("inicio", "Usa una fecha válida dd/mm/aaaa")
        if(hasta == null) put("termino", "Usa una fecha válida dd/mm/aaaa")
        if(desde != null && hasta != null && hasta.isBefore(desde)) put("termino", "El término debe ser igual o posterior al inicio")
    }
}
