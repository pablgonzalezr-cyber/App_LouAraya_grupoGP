package com.example.applouaraya_grupogp.navigation

import android.app.Application
import android.content.Context
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.applouaraya_grupogp.ui.design.LouColors
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
import com.example.applouaraya_grupogp.notifications.NotificacionesLocales
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun AppNavigation(basePrueba: AppDatabase? = null, preferenciasPrueba: PreferenciasRepository? = null,
                  controlador: NavHostController = rememberNavController(), favoritosPrueba: FavoritosRepository? = null,
                  abrirCentro: Boolean = false, centroConsumido: () -> Unit = {}) {
    val contexto = LocalContext.current.applicationContext
    val db = remember(contexto, basePrueba) { basePrueba ?: AppDatabase.obtener(contexto) }
    val nav = controlador
    val registro: RegistroViewModel = viewModel()
    val formularioRegistro by registro.estado.collectAsState()
    val sesionFactory = remember(db, contexto) { viewModelFactory {
        initializer { SesionViewModel(contexto as Application, UsuarioRepository(db.usuarioDao())) }
    } }
    val sesionVm: SesionViewModel = viewModel(factory = sesionFactory)
    val productosRepo = remember(db, sesionVm) { ProductosRepository(db) { sesionVm.estado.value.usuarioId } }
    val catalogoRepo = remember(db, sesionVm, contexto, basePrueba) {
        if (basePrueba != null) productosRepo else {
            val fotos = contexto.getSharedPreferences("lou_catalogo_fotos", Context.MODE_PRIVATE)
            val remoto = ProductosRemotos(fotos, db.productoDao()::buscar)
            ProductosRepository(db, remoto) { sesionVm.estado.value.usuarioId }
        }
    }
    val contenidoRepo = remember(db, productosRepo) { ContenidoRepository(db, productosRepo) }
    val sesion by sesionVm.estado.collectAsState()
    val preferenciasRepo = remember(contexto, preferenciasPrueba) {
        preferenciasPrueba ?: PreferenciasRepository(contexto.getSharedPreferences("lou_preferencias", Context.MODE_PRIVATE))
    }
    val avisosFactory = remember(db, sesionVm, preferenciasRepo) { viewModelFactory {
        initializer { AvisosViewModel(AvisosRepository(db) { sesionVm.estado.value.usuarioId }, preferenciasRepo, sesionVm.estado) }
    } }
    val avisosVm: AvisosViewModel = viewModel(factory = avisosFactory)
    val avisosActuales by avisosVm.estado.collectAsState()
    // Nunca renderizar estado conservado de la cuenta anterior mientras se reinician los Flows.
    val avisos = if(avisosActuales.usuarioId == sesion.usuarioId) avisosActuales else AvisosUiState(usuarioId = sesion.usuarioId)
    val notificaciones = remember(contexto) { NotificacionesLocales(contexto) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var revisionPermiso by remember { mutableIntStateOf(0) }
    val permitido = remember(revisionPermiso) { notificaciones.permitido() }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, e -> if(e == Lifecycle.Event.ON_RESUME) revisionPermiso++ }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); if(basePrueba == null) notificaciones.cancelar() }
    }
    LaunchedEffect(avisos, sesion.usuarioId, revisionPermiso) {
        // Las pantallas de pruebas con Room inyectado no publican ni cancelan alertas reales.
        if(basePrueba == null) notificaciones.actualizar(sesion.usuarioId, avisos.avisos, avisos.preferencias)
    }
    val configFactory = remember(contexto, preferenciasPrueba, sesionVm) { viewModelFactory {
        initializer { ConfiguracionViewModel(preferenciasRepo, sesionVm.estado) }
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
        if(basePrueba == null) notificaciones.cancelar()
        registro.limpiarFormulario()
        sesionVm.cerrarSesion()
        salida = null
        reemplazar(destino)
    }
    fun pedirSalida(destino: String) {
        if (config.preferencias.confirmarSalida) salida = destino else salir(destino)
    }
    fun abrir(destino: String) { nav.navigate(destino) { launchSingleTop = true } }
    LaunchedEffect(abrirCentro, sesion.usuarioId, ruta) {
        // El PendingIntent no contiene destinatario ni ruta privada. Tras autenticar,
        // siempre abre exclusivamente el centro de la sesión actual.
        if(abrirCentro && sesion.usuarioId != null && PoliticaNavegacion.permite("avisos", sesion.usuarioId, sesion.rol) && ruta !in setOf(null, "login", "registro", "resumen", "bienvenida")) {
            abrir("avisos"); centroConsumido()
        }
    }

    // Protege el contenido y corrige destinos privados restaurados sin sesión.
    LaunchedEffect(sesion.usuarioId, sesion.rol, ruta, formularioRegistro.resumenDisponible) {
        if (ruta == null) return@LaunchedEffect
        if (sesion.usuarioId != null && (sesion.usuarioId!! <= 0 || sesion.rol !in setOf("CLIENTE", "ADMINISTRADOR"))) {
            salir("login")
        } else if (sesion.usuarioId != null && ruta in setOf("bienvenida", "login", "registro", "resumen")) {
            registro.limpiarFormulario()
            reemplazar(PoliticaNavegacion.inicio(sesion.rol))
        } else if(ruta == "resumen" && !formularioRegistro.resumenDisponible) {
            // Destino restaurado tras perder el borrador: no confirmar datos vacíos.
            nav.navigate("registro") { popUpTo("resumen") { inclusive = true }; launchSingleTop = true }
        } else if (!PoliticaNavegacion.permite(ruta, sesion.usuarioId, sesion.rol)) {
            reemplazar(if (sesion.usuarioId == null) "login" else PoliticaNavegacion.inicio(sesion.rol))
        }
    }
    // Barra inferior visual para las pantallas principales del cliente.
    // No cambia rutas ni autorizaciones; respeta la navegación MVVM existente.
    val rutasConBarra = setOf("dashboard", "catalogo", "favoritos", "solicitudes", "perfil")
    Scaffold(
        containerColor = LouColors.Fondo,
        bottomBar = {
            if (sesion.usuarioId != null && sesion.rol == "CLIENTE" && ruta in rutasConBarra) {
                NavigationBar(containerColor = LouColors.Superficie, tonalElevation = 5.dp) {
                    listOf(
                        Triple("⌂", "Inicio", "dashboard"),
                        Triple("▦", "Catálogo", "catalogo"),
                        Triple("♡", "Favoritos", "favoritos"),
                        Triple("≡", "Solicitudes", "solicitudes"),
                        Triple("◉", "Perfil", "perfil")
                    ).forEach { (icono, etiqueta, destino) ->
                        NavigationBarItem(
                            selected = ruta == destino,
                            onClick = { if (ruta != destino) abrir(destino) },
                            icon = { Text(icono, color = if (ruta == destino) LouColors.Terracota else LouColors.Cafe) },
                            label = { Text(etiqueta) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = LouColors.Terracota,
                                selectedTextColor = LouColors.Terracota,
                                indicatorColor = LouColors.ArenaClara)
                        )
                    }
                }
            }
        }
    ) { contenidoPadding ->
        Box(Modifier.padding(contenidoPadding)) {
            NavHost(navController = nav, startDestination = "bienvenida") {
                composable("bienvenida") { BienvenidaScreen(onComenzar = { sesionVm.limpiarError(); abrir("login") }, onRegistrarse = { sesionVm.limpiarError(); abrir("registro") }) }
                composable("login") { LoginScreen(sesion.cargando, sesion.error,
                    onIngresar = sesionVm::iniciarSesion, onRegistrarse = { sesionVm.limpiarError(); abrir("registro") }) }
                composable("registro") { RegistroScreen(registro, sesion.cargando, sesion.error, onContinuar = {
                    sesionVm.limpiarError()
                    abrir("resumen")
                }) }
                composable("resumen") {
                    if(sesion.usuarioId == null && formularioRegistro.resumenDisponible) {
                        ResumenScreen(registro, sesion.cargando, sesion.error, onEditar = {
                            if(!sesionVm.estado.value.cargando) {
                                sesionVm.limpiarError()
                                nav.popBackStack()
                                registro.volverAEditar()
                            }
                        }, onConfirmar = {
                            // La credencial permanece en el ViewModel; nunca viaja en rutas.
                            val datos = registro.estado.value
                            if(datos.resumenDisponible) sesionVm.registrar(datos.nombre, datos.correo, datos.clave)
                        })
                    }
                }
                composable("dashboard") { RutaPrivada("dashboard", sesion) {
                    DashboardScreen(sesion.nombre, onCatalogo = { abrir("catalogo") }, onCerrarSesion = { pedirSalida("bienvenida") },
                        onCotizacion = { abrir("cotizacion") }, onHistorial = { abrir("solicitudes") },
                        onFavoritos = { abrir("favoritos") }, onPerfil = { abrir("perfil") }, onNovedades = { abrir("novedades") }, onEscaner = { abrir("escaner") }, onMapa = { abrir("mapa") },
                        onAvisos = { abrir("avisos") }, avisosNoLeidos = avisos.noLeidas)
                } }
                composable("avisos") { RutaPrivada("avisos", sesion) {
                    AvisosScreen(avisos, permitido, onRevisarPermiso = { revisionPermiso++ }, onVolver = { nav.popBackStack() },
                        onLeida = avisosVm::leida, onAbrir = { id -> avisosVm.abrir(id) { destino ->
                            if(PoliticaNavegacion.permite(destino.replace(Regex("/[0-9]+$"), "/{id}"), sesionVm.estado.value.usuarioId, sesionVm.estado.value.rol)) abrir(destino)
                        } })
                } }
                composable("perfil") { RutaPrivada("perfil", sesion) {
                    val factory = remember(db, sesionVm) { viewModelFactory {
                        initializer { PerfilViewModel(PerfilRepository(db.usuarioDao()) { sesionVm.estado.value.usuarioId }) }
                    } }
                    val vm: PerfilViewModel = viewModel(factory = factory)
                    PerfilScreen(vm, config.preferencias, config.guardando, config.error, configVm::guardar,
                        onVolver = { nav.popBackStack() }, onCerrarSesion = { pedirSalida("bienvenida") }, onCambiarUsuario = { pedirSalida("login") })
                } }
                composable("catalogo") { RutaPrivada("catalogo", sesion) { id -> CatalogoScreen(onVolver = { nav.popBackStack() },
                    usuarioId = id, repositorio = catalogoRepo, repositorioFavoritos = favoritosPrueba,
                    onFavoritosLocales = { abrir("favoritos-locales") }) } }
                composable("escaner") { RutaPrivada("escaner", sesion) {
                    val factory = remember(db) { viewModelFactory { initializer { QrViewModel(QrRepository(db.productoDao())) } } }
                    val vm: QrViewModel = viewModel(factory = factory)
                    QrScannerScreen(vm, onVolver = { nav.popBackStack() }, onProducto = { id ->
                        nav.navigate("producto/$id") { popUpTo("escaner") { inclusive = true }; launchSingleTop = true }
                    }, onDemo = { abrir("qr-demo") })
                } }
                composable("qr-demo") { RutaPrivada("qr-demo", sesion) {
                    val factory = remember(productosRepo) { viewModelFactory { initializer { QrDemoViewModel(productosRepo) } } }
                    val vm: QrDemoViewModel = viewModel(factory = factory)
                    QrDemoScreen(vm, onVolver = { nav.popBackStack() })
                } }
                composable("mapa") { RutaPrivada("mapa", sesion) {
                    val vm: MapaViewModel = viewModel()
                    MapaScreen(vm, onVolver = { nav.popBackStack() })
                } }
                composable("favoritos") { RutaPrivada("favoritos", sesion) { id -> CatalogoScreen(onVolver = { nav.popBackStack() },
                    usuarioId = id, abrirFavoritos = true, repositorio = catalogoRepo, repositorioFavoritos = favoritosPrueba,
                    onFavoritosLocales = { abrir("favoritos-locales") }) } }
                composable("favoritos-locales") { RutaPrivada("favoritos-locales", sesion) { id -> CatalogoScreen(onVolver = { nav.popBackStack() },
                    usuarioId = id, abrirFavoritos = true, repositorio = productosRepo, repositorioFavoritos = favoritosPrueba) } }
                composable("admin/productos") { RutaPrivada("admin/productos", sesion) {
                    val factory = remember(catalogoRepo, sesion.usuarioId) { viewModelFactory { initializer { AdminProductosViewModel(catalogoRepo) } } }
                    val vm: AdminProductosViewModel = viewModel(key = "admin-productos-${sesion.usuarioId}", factory = factory)
                    AdminProductosScreen(vm, onVolver = { nav.popBackStack() })
                } }
                composable("admin/contenido") { RutaPrivada("admin/contenido", sesion) {
                    val factory = remember(contenidoRepo) { viewModelFactory { initializer { AdminContenidoViewModel(contenidoRepo) } } }
                    val vm: AdminContenidoViewModel = viewModel(factory = factory)
                    AdminContenidoScreen(vm, onVolver = { nav.popBackStack() })
                } }
                composable("novedades") { RutaPrivada("novedades", sesion) {
                    val factory = remember(contenidoRepo) { viewModelFactory { initializer { NovedadesViewModel(contenidoRepo) } } }
                    val vm: NovedadesViewModel = viewModel(factory = factory)
                    NovedadesScreen(vm, onVolver = { nav.popBackStack() }, onProducto = { abrir("producto/$it") })
                } }
                composable("producto/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) { e ->
                    RutaPrivada("producto/{id}", sesion) { id -> CatalogoScreen(onVolver = { nav.popBackStack() }, usuarioId = id,
                        repositorio = productosRepo, productoInicial = e.arguments?.getInt("id"), repositorioFavoritos = favoritosPrueba) }
                }
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
                    RutaPrivada("solicitud/{id}", sesion) { id ->
                        val factory = remember(db, contexto) { viewModelFactory {
                            initializer { HistorialCotizacionesViewModel(contexto as Application, CotizacionRepository(db.cotizacionDao())) }
                        } }
                        val vm: HistorialCotizacionesViewModel = viewModel(factory = factory)
                        DetalleCotizacionScreen(id, e.arguments?.getLong("id") ?: -1, onVolver = { nav.popBackStack() }, vm = vm)
                    }
                }
                composable("admin") { RutaPrivada("admin", sesion) {
                    AdminConPermiso(db, sesionVm) { vm -> AdminDashboardScreen(vm, onSolicitudes = { abrir("admin/solicitudes") },
                        onCerrarSesion = { pedirSalida("bienvenida") }, onPerfil = { abrir("perfil") }, onProductos = { abrir("admin/productos") },
                        onContenido = { abrir("admin/contenido") }, onAvisos = { abrir("avisos") }, avisosNoLeidos = avisos.noLeidas) }
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

