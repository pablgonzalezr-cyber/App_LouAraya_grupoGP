package com.example.applouaraya_grupogp.repository

import com.example.applouaraya_grupogp.model.PuntoDemostracion
import java.net.URLEncoder

object PuntosDemostracion {
    // Coordenadas de prueba; no representan ferias, sedes ni horarios confirmados.
    val puntos = listOf(
        PuntoDemostracion("feria-a", "Feria demo A (ficticia)", "Punto inventado para probar marcadores. No hay una feria confirmada.",
            "Sin dirección real confirmada. Punto de prueba: -33.4420, -70.6540.", -33.4420, -70.6540),
        PuntoDemostracion("showroom-b", "Showroom demo B (ficticio)", "Ubicación de demostración. No corresponde a un local de Lou Araya ni ofrece atención.",
            "Sin dirección real confirmada. Punto de prueba: -33.4370, -70.6280.", -33.4370, -70.6280),
        PuntoDemostracion("feria-c", "Feria demo C (ficticia)", "Tercer punto ficticio para explorar el mapa y probar indicaciones.",
            "Sin dirección real confirmada. Punto de prueba: -33.4610, -70.6400.", -33.4610, -70.6400)
    )
    fun navegacion(punto: PuntoDemostracion) = "google.navigation:q=${punto.latitud},${punto.longitud}"
    fun ubicacion(punto: PuntoDemostracion): String {
        val nombre = URLEncoder.encode(punto.nombre,"UTF-8")
        return "geo:${punto.latitud},${punto.longitud}?q=${punto.latitud},${punto.longitud}($nombre)"
    }
}
