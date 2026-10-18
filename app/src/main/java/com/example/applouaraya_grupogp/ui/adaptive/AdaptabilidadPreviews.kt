package com.example.applouaraya_grupogp.ui.adaptive

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.example.applouaraya_grupogp.ui.screens.*
import com.example.applouaraya_grupogp.viewmodel.*
import com.example.applouaraya_grupogp.repository.ProductosDemo

@Preview(name = "Compacto 360", group = "Guía 9", widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "Mediano 720", group = "Guía 9", widthDp = 720, heightDp = 1024, showBackground = true)
@Preview(name = "Expandido 1100", group = "Guía 9", widthDp = 1100, heightDp = 800, showBackground = true)
annotation class TresTamanos

@TresTamanos @Composable
fun BienvenidaAdaptablePreview() { MaterialTheme { BienvenidaScreen() } }

@TresTamanos @Composable
fun LoginAdaptablePreview() { MaterialTheme { LoginScreen(false, null, { _, _ -> }, {}) } }

@TresTamanos @Composable
fun RegistroAdaptablePreview() {
    MaterialTheme { RegistroScreen(remember { RegistroViewModel() }, false, null, {}) }
}

@TresTamanos @Composable
fun ResumenAdaptablePreview() {
    val vm = remember { RegistroViewModel().apply {
        cambiarNombre("Cliente de demostración")
        cambiarCorreo("demo@example.test")
        cambiarClave("Clave123!")
        cambiarTerminos(true)
        validarFormulario()
    } }
    MaterialTheme { ResumenScreen(vm, false, null, {}, {}) }
}

@TresTamanos @Composable
fun ClienteAdaptablePreview() {
    MaterialTheme { DashboardScreen("Cliente demo", {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, 2) }
}

@TresTamanos @Composable
fun AdministradorAdaptablePreview() {
    MaterialTheme { AdminDashboardContent(AdminUiState(cargando = false), {}, {}, {}, {}, {}, {}, {}, 0) }
}

@TresTamanos @Composable
fun CatalogoAdaptablePreview() {
    MaterialTheme {
        CatalogoContenido(CatalogoUiState(productos = ProductosDemo.productos, todos = ProductosDemo.productos, cargando = false),
            emptySet(), true, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(name = "Horizontal · altura reducida", widthDp = 800, heightDp = 360, showBackground = true)
@Composable
fun LoginHorizontalPreview() { MaterialTheme { LoginScreen(false, null, { _, _ -> }, {}) } }
