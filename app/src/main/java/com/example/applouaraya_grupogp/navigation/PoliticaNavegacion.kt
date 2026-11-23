package com.example.applouaraya_grupogp.navigation

object PoliticaNavegacion {
    private val publicas = setOf("bienvenida", "login", "registro", "resumen")
    private val cliente = setOf("dashboard", "catalogo", "favoritos", "favoritos-locales", "cotizacion",
        "solicitudes", "solicitudes-servidor", "solicitudes-servidor/{id}", "solicitud/{id}", "novedades", "novedades-locales", "producto/{id}", "producto-coleccion/{id}", "escaner", "qr-demo", "mapa")
    private val admin = setOf("admin", "admin/solicitudes-servidor", "admin/solicitudes-servidor/{id}", "admin/solicitudes", "admin/solicitud/{id}", "admin/productos", "admin/contenido")
    fun inicio(rol: String) = if (rol == "ADMINISTRADOR") "admin" else "dashboard"
    fun permite(ruta: String?, usuarioId: Long?, rol: String): Boolean = when {
        ruta in publicas -> true
        usuarioId == null || usuarioId <= 0 -> false
        ruta in setOf("perfil", "avisos") -> rol == "CLIENTE" || rol == "ADMINISTRADOR"
        ruta in admin -> rol == "ADMINISTRADOR"
        ruta in cliente -> rol == "CLIENTE"
        else -> false
    }
}
