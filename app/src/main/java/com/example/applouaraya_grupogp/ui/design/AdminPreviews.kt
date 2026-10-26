package com.example.applouaraya_grupogp.ui.design

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.applouaraya_grupogp.model.*
import com.example.applouaraya_grupogp.repository.ProductosDemo
import com.example.applouaraya_grupogp.ui.adaptive.TresTamanos
import com.example.applouaraya_grupogp.ui.screens.*
import com.example.applouaraya_grupogp.ui.theme.AppLouAraya_grupoGPTheme
import com.example.applouaraya_grupogp.viewmodel.AdminUiState
import java.time.LocalDate

/** Fixtures exclusivas de previews y pruebas: no se guardan ni se usan como datos del taller. */
@Composable
fun AdminPreviewScene(screen: Int) {
    val products = ProductosDemo.productos.take(3)
    val request = CotizacionEntity(1, 2, "preview", "Tapiz personalizado para un espacio de lectura",
        "Interiores", "Algodón", 40.0, 60.0, 1, null, null, null, 0, "PENDIENTE")
    AppLouAraya_grupoGPTheme {
        when(screen) {
            0 -> AdminDashboardContent(AdminUiState(solicitudes = listOf(request), cargando = false), {}, {}, {}, {}, {}, {}, {}, 1)
            1 -> AdminPage("Administrar productos", {}) {
                AdminGrid(products, { it.id }, Modifier.weight(1f)) { AdminProductoCard(it, true, {}, {}) }
            }
            2 -> AdminPage("Administrar colecciones", {}) {
                val values = listOf(ColeccionEntity(1, "Fibras naturales", "Piezas para espacios acogedores", true, true),
                    ColeccionEntity(2, "Celebraciones", "Colección en preparación", true, false))
                AdminGrid(values, { it.id }, Modifier.weight(1f)) { AdminColeccionCard(it, products, products.size, true, {}, {}) }
            }
            3 -> AdminPage("Administrar campañas", {}) {
                val today = LocalDate.now()
                val values = listOf(
                    CampaniaEntity(1, "Diseños de temporada", "Descubre nuevas texturas artesanales", today.minusDays(2).toString(), today.plusDays(5).toString(), true),
                    CampaniaEntity(2, "Próxima colección", "Una selección que pronto estará disponible", today.plusDays(3).toString(), today.plusDays(8).toString(), true))
                AdminGrid(values, { it.id }, Modifier.weight(1f)) { AdminCampaniaCard(it, true, {}, {}) }
            }
            else -> AdminPage("Todas las solicitudes", {}) {
                val values = listOf(request, request.copy(id = 2, estado = "EN_REVISION"), request.copy(id = 3, estado = "APROBADA"))
                AdminGrid(values, { it.id }, Modifier.weight(1f)) { AdminSolicitudCard(it, {}) }
            }
        }
    }
}

@TresTamanos @Composable fun DashboardAdministradorPreview() = AdminPreviewScene(0)
@TresTamanos @Composable fun ProductosAdministradorPreview() = AdminPreviewScene(1)
@TresTamanos @Composable fun ColeccionesAdministradorPreview() = AdminPreviewScene(2)
@TresTamanos @Composable fun CampaniasAdministradorPreview() = AdminPreviewScene(3)
@TresTamanos @Composable fun SolicitudesAdministradorPreview() = AdminPreviewScene(4)
