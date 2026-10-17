package com.example.applouaraya_grupogp.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.applouaraya_grupogp.data.AppDatabase
import com.example.applouaraya_grupogp.repository.AdminCotizacionesRepository
import com.example.applouaraya_grupogp.repository.PoliticaAdministracion
import com.example.applouaraya_grupogp.viewmodel.AdminCotizacionesViewModel
import com.example.applouaraya_grupogp.ui.screens.*
import com.example.applouaraya_grupogp.viewmodel.RegistroViewModel
import com.example.applouaraya_grupogp.viewmodel.SesionViewModel

@Composable
fun AppNavigation() {

    val navController = rememberNavController()

    val registroViewModel: RegistroViewModel = viewModel()
    val sesionViewModel: SesionViewModel = viewModel()

    val sesion by sesionViewModel.estado.collectAsState()
    val cerrarSesion: () -> Unit = {
        sesionViewModel.cerrarSesion()
        navController.navigate("bienvenida") { popUpTo(navController.graph.id) { inclusive = true }; launchSingleTop = true }
    }

    // Navegar cuando el registro o login sea exitoso
    LaunchedEffect(sesion.usuarioId, sesion.rol) {
        val destino = if (PoliticaAdministracion.esAdministrador(sesion.rol)) "admin" else "dashboard"
        if (
            sesion.usuarioId != null &&
            navController.currentDestination?.route != destino
        ) {
            registroViewModel.limpiarFormulario()

            navController.navigate(destino) {
                popUpTo("bienvenida") {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "bienvenida"
    ) {
        composable("admin") {
            AdminConPermiso(sesionViewModel, { navController.navigate("login") { popUpTo("bienvenida"); launchSingleTop = true } }) { vm ->
                AdminDashboardScreen(vm, onSolicitudes = { navController.navigate("admin/solicitudes") }, onCerrarSesion = cerrarSesion)
            }
        }
        composable("admin/solicitudes") {
            AdminConPermiso(sesionViewModel, { navController.navigate("login") { popUpTo("bienvenida"); launchSingleTop = true } }) { vm ->
                AdminSolicitudesScreen(vm, onVolver = { navController.popBackStack() }, onAbrir = { navController.navigate("admin/solicitud/$it") })
            }
        }
        composable("admin/solicitud/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entrada ->
            AdminConPermiso(sesionViewModel, { navController.navigate("login") { popUpTo("bienvenida"); launchSingleTop = true } }) { vm ->
                AdminDetalleSolicitudScreen(vm, entrada.arguments?.getLong("id") ?: -1L, onVolver = { navController.popBackStack() })
            }
        }

        composable("bienvenida") {
            BienvenidaScreen(
                onComenzar = {
                    sesionViewModel.limpiarError()
                    navController.navigate("login")
                },
                onRegistrarse = {
                    sesionViewModel.limpiarError()
                    navController.navigate("registro")
                }
            )
        }

        composable("login") {
            LoginScreen(
                cargando = sesion.cargando,
                errorGeneral = sesion.error,
                onIngresar = { correo, clave ->
                    sesionViewModel.iniciarSesion(
                        correo,
                        clave
                    )
                },
                onRegistrarse = {
                    sesionViewModel.limpiarError()
                    navController.navigate("registro")
                }
            )
        }

        composable("registro") {
            RegistroScreen(
                viewModel = registroViewModel,
                cargando = sesion.cargando,
                errorGeneral = sesion.error,
                onRegistrar = { nombre, correo, clave ->
                    sesionViewModel.registrar(
                        nombre,
                        correo,
                        clave
                    )
                }
            )
        }

        composable("dashboard") {
            DashboardScreen(
                nombre = sesion.nombre,
                onCotizacion = { navController.navigate("cotizacion") { launchSingleTop = true } },
                onHistorial = { navController.navigate("solicitudes") { launchSingleTop = true } },
                onCatalogo = {
                    navController.navigate("catalogo")
                },
                onCerrarSesion = {
                    sesionViewModel.cerrarSesion()

                    navController.navigate("bienvenida") {
                        popUpTo("dashboard") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }


        composable("cotizacion") {
            val usuarioId = sesion.usuarioId
            if (usuarioId != null) {
                CotizacionScreen(usuarioId, onVolver = { navController.popBackStack() }, onGuardada = { id ->
                    navController.navigate("solicitudes") {
                        popUpTo("cotizacion") { inclusive = true }
                        launchSingleTop = true
                    }
                    navController.navigate("solicitud/$id")
                })
            } else {
                LaunchedEffect(Unit) { navController.navigate("login") { popUpTo("bienvenida"); launchSingleTop = true } }
            }
        }
        composable("solicitudes") {
            val usuarioId = sesion.usuarioId
            if (usuarioId != null) {
                HistorialCotizacionesScreen(usuarioId, onVolver = { navController.popBackStack() },
                    onAbrir = { id -> navController.navigate("solicitud/$id") { launchSingleTop = true } })
            } else {
                LaunchedEffect(Unit) { navController.navigate("login") { popUpTo("bienvenida"); launchSingleTop = true } }
            }
        }

        composable(
            route = "solicitud/{id}",
            arguments = listOf(
                navArgument("id") {
                    type = NavType.LongType
                }
            )
        ) { entrada ->

        val usuarioId = sesion.usuarioId
            if (usuarioId != null) {
                DetalleCotizacionScreen(usuarioId, entrada.arguments?.getLong("id") ?: -1L,
                    onVolver = { navController.popBackStack() })
            } else {
                LaunchedEffect(Unit) { navController.navigate("login") { popUpTo("bienvenida"); launchSingleTop = true } }
            }
        }

        composable("catalogo") {
            CatalogoScreen(
                onVolver = {
                    navController.popBackStack()
                },
                usuarioId = sesion.usuarioId
            )
        }

    }
}

@Composable
private fun AdminConPermiso(sesionVm: SesionViewModel, denegado: () -> Unit, contenido: @Composable (AdminCotizacionesViewModel) -> Unit) {
    val sesion by sesionVm.estado.collectAsState()
    if (sesion.usuarioId == null || !PoliticaAdministracion.esAdministrador(sesion.rol)) {
        LaunchedEffect(sesion.usuarioId, sesion.rol) { denegado() }
        return
    }
    val contexto = LocalContext.current.applicationContext
    val factory = remember(contexto, sesionVm) {
        viewModelFactory {
            initializer {
                val db = AppDatabase.obtener(contexto)
                AdminCotizacionesViewModel(AdminCotizacionesRepository(db.usuarioDao(), db.cotizacionDao()) { sesionVm.estado.value.usuarioId })
            }
        }
    }
    val vm: AdminCotizacionesViewModel = viewModel(factory = factory)
    contenido(vm)
}
