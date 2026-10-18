package com.example.applouaraya_grupogp.navigation

object PoliticaNavegacion {
    private val publicas = setOf("bienvenida", "login", "registro")
    private val cliente = setOf("dashboard", "catalogo", "favoritos", "cotizacion", "solicitudes", "solicitud/{id}", "novedades", "producto/{id}", "escaner", "qr-demo", "mapa")
    private val admin = setOf("admin", "admin/solicitudes", "admin/solicitud/{id}", "admin/productos", "admin/contenido")
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
