package com.example.applouaraya_grupogp.validation

import com.example.applouaraya_grupogp.model.Producto
import java.math.BigDecimal

data class ProductoFormulario(
    val id: Int = 0, val nombre: String = "", val descripcion: String = "",
    val categoria: String = "", val material: String = "", val ancho: String = "",
    val largo: String = "", val precio: String = "", val disponible: Boolean = true,
    val activo: Boolean = true, val imagenNombre: String = "", val imagenArchivo: String = ""
) {
    fun producto() = Producto(id, nombre.trim(), categoria, material.trim(), precio.trim().toInt(),
        descripcion.trim(), ancho.trim().toInt(), largo.trim().toInt(), disponible, imagenNombre, activo, imagenArchivo)
    companion object {
        fun desde(p: Producto) = ProductoFormulario(p.id, p.nombre, p.descripcion, p.categoria, p.material,
            p.anchoCm.toString(), p.largoCm.toString(), p.precio.toString(), p.disponible, p.activo, p.imagenNombre, p.imagenArchivo)
    }
}

object ProductoValidator {
    fun validarRemoto(f: ProductoFormulario): Map<String, String> = buildMap {
        listOf("nombre" to f.nombre, "material" to f.material, "categoria" to f.categoria,
            "descripcion" to f.descripcion).forEach { (campo, texto) ->
            val maximo = if (campo == "descripcion") 2000 else 100
            if (texto.trim().length !in 1..maximo) put(campo, "Ingresa entre 1 y $maximo caracteres")
        }
        listOf("ancho" to f.ancho, "largo" to f.largo, "precio" to f.precio).forEach { (campo, texto) ->
            val valor = texto.trim().replace(',', '.')
            val maximo = if (campo == "precio") "999999999.99" else "10000"
            if (!Regex("[0-9]+(\\.[0-9]{1,2})?").matches(valor) ||
                valor.toBigDecimal() !in BigDecimal("0.01")..BigDecimal(maximo)) {
                put(campo, "Ingresa un valor entre 0,01 y $maximo con hasta dos decimales")
            }
        }
        if (f.imagenArchivo.isNotEmpty() && !Regex("[a-f0-9-]{36}\\.img").matches(f.imagenArchivo)) put("imagen", "Imagen local no válida")
    }

    val categorias = listOf("Interiores", "Matrimonios", "Festivales", "Otro")
    fun validar(f: ProductoFormulario): Map<String, String> = buildMap {
        if (f.nombre.trim().length !in 2..100) put("nombre", "Ingresa entre 2 y 100 caracteres")
        if (f.descripcion.trim().length !in 10..2000) put("descripcion", "Ingresa entre 10 y 2000 caracteres")
        if (f.categoria !in categorias) put("categoria", "Selecciona una categoría")
        if (f.material.trim().length !in 2..100) put("material", "Ingresa entre 2 y 100 caracteres")
        if (f.precio.trim().toIntOrNull() !in 1..1_000_000_000) put("precio", "Ingresa pesos CLP enteros entre 1 y 1000000000")
        listOf("ancho" to f.ancho, "largo" to f.largo).forEach { (campo, valor) ->
            if (valor.trim().toIntOrNull() !in 1..10000) put(campo, "Ingresa centímetros enteros entre 1 y 10000")
        }
        if (f.imagenArchivo.isNotEmpty() && !Regex("[a-f0-9-]{36}\\.img").matches(f.imagenArchivo)) put("imagen", "Imagen local no válida")
    }
}
