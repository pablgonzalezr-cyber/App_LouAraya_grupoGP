package com.example.applouaraya_grupogp.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.applouaraya_grupogp.ui.screens.*
import com.example.applouaraya_grupogp.viewmodel.RegistroViewModel
import com.example.applouaraya_grupogp.viewmodel.SesionViewModel

@Composable
fun AppNavigation() {

    val navController = rememberNavController()

    val registroViewModel: RegistroViewModel = viewModel()
    val sesionViewModel: SesionViewModel = viewModel()

    val sesion by sesionViewModel.estado.collectAsState()

    // Navegar cuando el registro o login sea exitoso
    LaunchedEffect(sesion.usuarioId) {
        if (
            sesion.usuarioId != null &&
            navController.currentDestination?.route != "dashboard"
        ) {
            registroViewModel.limpiarFormulario()

            navController.navigate("dashboard") {
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
