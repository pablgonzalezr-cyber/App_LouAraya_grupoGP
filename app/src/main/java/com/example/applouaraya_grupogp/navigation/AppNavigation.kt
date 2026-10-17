package com.example.applouaraya_grupogp.navigation

import android.app.Application
import android.content.Context
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.navArgument
import androidx.navigation.compose.*
import com.example.applouaraya_grupogp.data.AppDatabase
import com.example.applouaraya_grupogp.repository.*
import com.example.applouaraya_grupogp.viewmodel.*
import com.example.applouaraya_grupogp.ui.screens.*

@Composable
fun AppNavigation(basePrueba: AppDatabase? = null, preferenciasPrueba: PreferenciasRepository? = null,
    controlador: NavHostController = rememberNavController()) {
    val contexto = LocalContext.current.applicationContext
    val db = remember(contexto, basePrueba) { basePrueba ?: AppDatabase.obtener(contexto) }
    val nav = controlador
    val registro: RegistroViewModel = viewModel()
    val sesionFactory = remember(db, contexto) { viewModelFactory {
        initializer { SesionViewModel(contexto as Application, UsuarioRepository(db.usuarioDao())) }
    } }
    val sesionVm: SesionViewModel = viewModel(factory = sesionFactory)
    val sesion by sesionVm.estado.collectAsState()
    val configFactory = remember(contexto, preferenciasPrueba, sesionVm) { viewModelFactory {
        initializer { ConfiguracionViewModel(preferenciasPrueba ?: PreferenciasRepository(contexto.getSharedPreferences("lou_preferencias", Context.MODE_PRIVATE)), sesionVm.estado) }
    } }
    val configVm: ConfiguracionViewModel = viewModel(factory = configFactory)
    val config by configVm.estado.collectAsState()
    val entrada by nav.currentBackStackEntryAsState()
    val ruta = entrada?.destination?.route
    var salida by remember(sesion.usuarioId) { mutableStateOf<String?>(null) }

    fun reemplazar(destino: String) {
        nav.navigate(destino) { popUpTo(nav.graph.id) { inclusive = true }; launchSingleTop = true }
    }
    fun salir(destino: String) {
        registro.limpiarFormulario()
        sesionVm.cerrarSesion()
        salida = null
        reemplazar(destino)
    }
    fun pedirSalida(destino: String) {
        if (config.preferencias.confirmarSalida) salida = destino else salir(destino)
    }
    fun abrir(destino: String) { nav.navigate(destino) { launchSingleTop = true } }

    // Protege el contenido y corrige destinos privados restaurados sin sesión.
    LaunchedEffect(sesion.usuarioId, sesion.rol, ruta) {
        if (ruta == null) return@LaunchedEffect
        if (sesion.usuarioId != null && (sesion.usuarioId!! <= 0 || sesion.rol !in setOf("CLIENTE", "ADMINISTRADOR"))) {
            salir("login")
        } else if (sesion.usuarioId != null && ruta in setOf("bienvenida", "login", "registro")) {
            registro.limpiarFormulario()
            reemplazar(PoliticaNavegacion.inicio(sesion.rol))
        } else if (!PoliticaNavegacion.permite(ruta, sesion.usuarioId, sesion.rol)) {
            reemplazar(if (sesion.usuarioId == null) "login" else PoliticaNavegacion.inicio(sesion.rol))
        }
    }
    NavHost(navController = nav, startDestination = "bienvenida") {
        composable("bienvenida") { BienvenidaScreen(onComenzar = { sesionVm.limpiarError(); abrir("login") }, onRegistrarse = { sesionVm.limpiarError(); abrir("registro") }) }
        composable("login") { LoginScreen(sesion.cargando, sesion.error,
            onIngresar = sesionVm::iniciarSesion, onRegistrarse = { sesionVm.limpiarError(); abrir("registro") }) }
        composable("registro") { RegistroScreen(registro, sesion.cargando, sesion.error, onRegistrar = sesionVm::registrar) }
        composable("dashboard") { RutaPrivada("dashboard", sesion) {
            DashboardScreen(sesion.nombre, onCatalogo = { abrir("catalogo") }, onCerrarSesion = { pedirSalida("bienvenida") },
                onCotizacion = { abrir("cotizacion") }, onHistorial = { abrir("solicitudes") },
                onFavoritos = { abrir("favoritos") }, onPerfil = { abrir("perfil") })
        } }
        composable("perfil") { RutaPrivada("perfil", sesion) {
            val factory = remember(db, sesionVm) { viewModelFactory {
                initializer { PerfilViewModel(PerfilRepository(db.usuarioDao()) { sesionVm.estado.value.usuarioId }) }
            } }
            val vm: PerfilViewModel = viewModel(factory = factory)
            PerfilScreen(vm, config.preferencias, config.guardando, config.error, configVm::guardar,
                onVolver = { nav.popBackStack() }, onCerrarSesion = { pedirSalida("bienvenida") }, onCambiarUsuario = { pedirSalida("login") })
        } }
        composable("catalogo") { RutaPrivada("catalogo", sesion) { id -> CatalogoScreen(onVolver = { nav.popBackStack() }, usuarioId = id) } }
        composable("favoritos") { RutaPrivada("favoritos", sesion) { id -> CatalogoScreen(onVolver = { nav.popBackStack() }, usuarioId = id, abrirFavoritos = true) } }
        composable("cotizacion") { RutaPrivada("cotizacion", sesion) { id ->
            CotizacionScreen(id, onVolver = { nav.popBackStack() }, onGuardada = { solicitudId ->
                nav.navigate("solicitudes") { popUpTo("cotizacion") { inclusive = true }; launchSingleTop = true }
                abrir("solicitud/$solicitudId")
            }, mostrarAyuda = config.preferencias.mostrarAyuda)
        } }
        composable("solicitudes") { RutaPrivada("solicitudes", sesion) { id ->
            HistorialCotizacionesScreen(id, onVolver = { nav.popBackStack() }, onAbrir = { abrir("solicitud/$it") })
        } }
        composable("solicitud/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            RutaPrivada("solicitud/{id}", sesion) { id -> DetalleCotizacionScreen(id, e.arguments?.getLong("id") ?: -1, onVolver = { nav.popBackStack() }) }
        }
        composable("admin") { RutaPrivada("admin", sesion) {
            AdminConPermiso(db, sesionVm) { vm -> AdminDashboardScreen(vm, onSolicitudes = { abrir("admin/solicitudes") },
                onCerrarSesion = { pedirSalida("bienvenida") }, onPerfil = { abrir("perfil") }) }
        } }
        composable("admin/solicitudes") { RutaPrivada("admin/solicitudes", sesion) {
            AdminConPermiso(db, sesionVm) { vm -> AdminSolicitudesScreen(vm, onVolver = { nav.popBackStack() }, onAbrir = { abrir("admin/solicitud/$it") }) }
        } }
        composable("admin/solicitud/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            RutaPrivada("admin/solicitud/{id}", sesion) {
                AdminConPermiso(db, sesionVm) { vm -> AdminDetalleSolicitudScreen(vm, e.arguments?.getLong("id") ?: -1, onVolver = { nav.popBackStack() }) }
            }
        }
    }
    salida?.let { destino ->
        AlertDialog(onDismissRequest = { salida = null }, title = { Text(if (destino == "login") "Cambiar de usuario" else "Cerrar sesión") },
            text = { Text("Se cerrará la sesión actual. Tus datos guardados se conservarán.") },
            confirmButton = { TextButton(onClick = { salir(destino) }) { Text("Confirmar") } },
            dismissButton = { TextButton(onClick = { salida = null }) { Text("Cancelar") } })
    }
}

@Composable
private fun RutaPrivada(ruta: String, sesion: SesionUiState, contenido: @Composable (Long) -> Unit) {
    if (PoliticaNavegacion.permite(ruta, sesion.usuarioId, sesion.rol)) {
        key(sesion.usuarioId) { contenido(requireNotNull(sesion.usuarioId)) }
    }
}

@Composable
private fun AdminConPermiso(db: AppDatabase, sesionVm: SesionViewModel, contenido: @Composable (AdminCotizacionesViewModel) -> Unit) {
    val factory = remember(db, sesionVm) { viewModelFactory {
        initializer { AdminCotizacionesViewModel(AdminCotizacionesRepository(db.usuarioDao(), db.cotizacionDao()) { sesionVm.estado.value.usuarioId }) }
    } }
    val vm: AdminCotizacionesViewModel = viewModel(factory = factory)
    contenido(vm)
}
