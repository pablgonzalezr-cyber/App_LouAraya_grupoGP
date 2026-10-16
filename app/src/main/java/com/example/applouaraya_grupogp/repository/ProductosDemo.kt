package com.example.applouaraya_grupogp.repository

import com.example.applouaraya_grupogp.model.Producto

// Catálogo completamente ficticio para el MVP de DSY1105.
// Los nombres de imagen se usarán si agregas los archivos a res/drawable.
object ProductosDemo {
    val productos = listOf(
        Producto(1, "Tapiz Bohemio", "Interiores", "Algodón natural", 24990,
            "Tapiz ornamental tejido a mano, ideal para muros interiores.",
            45, 70, true, "macrame_tapiz"),
        Producto(2, "Arco de Matrimonio", "Matrimonios", "Algodón trenzado", 89990,
            "Decoración colgante diseñada para ceremonias y celebraciones.",
            180, 220, true, "macrame_arco"),
        Producto(3, "Colgante Artesanal", "Interiores", "Hilo de algodón", 18990,
            "Pieza compacta ideal para espacios pequeños.",
            28, 50, true, "macrame_colgante"),
        Producto(4, "Decoración Festival", "Festivales", "Algodón y cáñamo", 45990,
            "Adorno textil para ferias y ambientaciones temáticas.",
            80, 120, true, "macrame_festival"),
        Producto(5, "Atrapasueños Natural", "Interiores", "Algodón", 16990,
            "Atrapasueños decorativo con terminaciones artesanales.",
            30, 60, true, "macrame_atrapasuenos"),
        Producto(6, "Cortina Macramé", "Interiores", "Algodón grueso", 64990,
            "Cortina tejida para separar y ambientar espacios.",
            100, 200, false, "macrame_cortina"),
        Producto(7, "Centro de Mesa Nupcial", "Matrimonios", "Cuerda de algodón", 32990,
            "Adorno de mesa personalizado para matrimonios.",
            40, 30, true, "macrame_centro"),
        Producto(8, "Guirnalda Cultural", "Festivales", "Algodón reciclado", 27990,
            "Guirnalda textil pensada para exhibiciones y eventos.",
            150, 25, true, "macrame_guirnalda")
    )
}
