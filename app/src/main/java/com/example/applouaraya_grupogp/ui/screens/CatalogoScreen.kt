package com.example.applouaraya_grupogp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.ui.platform.LocalContext
import com.example.applouaraya_grupogp.data.AppDatabase
import com.example.applouaraya_grupogp.repository.ProductosRepository
import com.example.applouaraya_grupogp.repository.FavoritosRepository
import com.example.applouaraya_grupogp.data.FavoritosDatabase
import android.app.Application
import com.example.applouaraya_grupogp.ui.components.ProductoCard
import com.example.applouaraya_grupogp.viewmodel.CatalogoViewModel
import com.example.applouaraya_grupogp.viewmodel.FavoritosViewModel

// Conserva onVolver; usuarioId tiene valor por defecto para que compile
// con el AppNavigation.kt que ya existe, sin modificarlo en esta rama.
@Composable
fun CatalogoScreen(onVolver: () -> Unit, usuarioId: Long? = null, abrirFavoritos: Boolean = false,
    repositorio: ProductosRepository? = null, productoInicial: Int? = null, repositorioFavoritos: FavoritosRepository? = null) {
    val contexto = LocalContext.current.applicationContext
    val repo = remember(contexto, repositorio) { repositorio ?: ProductosRepository(AppDatabase.obtener(contexto)) }
    val factory = remember(repo) { viewModelFactory { initializer { CatalogoViewModel(repo) } } }
    val catalogoVm: CatalogoViewModel = viewModel(factory = factory)
    val favoritosFactory = remember(contexto, repositorioFavoritos) { viewModelFactory {
        initializer { FavoritosViewModel(contexto as Application, repositorioFavoritos ?: FavoritosRepository(FavoritosDatabase.obtener(contexto).favoritoDao())) }
    } }
    val favoritosVm: FavoritosViewModel = viewModel(factory = favoritosFactory)
    val estado by catalogoVm.estado.collectAsState()
    val favoritos by favoritosVm.ids.collectAsState()
    val puedeGuardar = usuarioId != null && usuarioId > 0L

    LaunchedEffect(usuarioId) { favoritosVm.seleccionarUsuario(usuarioId) }
    var vista by rememberSaveable { mutableStateOf(if (productoInicial != null) "detalle" else if (abrirFavoritos) "favoritos" else "catalogo") }
    var detalleId by rememberSaveable { mutableIntStateOf(productoInicial ?: -1) }

    BackHandler(enabled = vista != "catalogo") {
        vista = "catalogo"
    }

    when (vista) {
        "detalle" -> {
            val producto = estado.todos.firstOrNull { it.id == detalleId }
            if (producto == null) {
                if (!estado.cargando) LaunchedEffect(detalleId) { vista = "catalogo" }
            } else {
                DetalleProductoScreen(
                    producto = producto,
                    esFavorito = producto.id in favoritos,
                    permitirFavorito = puedeGuardar,
                    onVolver = { vista = "catalogo" },
                    onFavorito = { favoritosVm.alternar(usuarioId, producto.id) }
                )
            }
        }
        "favoritos" -> FavoritosScreen(
            productos = estado.todos,
            idsFavoritos = favoritos,
            onVolver = { vista = "catalogo" },
            onAbrir = { id -> detalleId = id; vista = "detalle" },
            onQuitar = { id -> favoritosVm.alternar(usuarioId, id) }
        )
        else -> {
            Column(
                modifier = Modifier.fillMaxSize()
                    .background(Color(0xFFF7F1E8))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onVolver) { Text("← Inicio") }
                    TextButton(
                        onClick = { vista = "favoritos" },
                        enabled = puedeGuardar
                    ) { Text("♥ Favoritos (${favoritos.size})") }
                }
                Text("Catálogo de macramé", style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFF51362B))
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = estado.consulta,
                    onValueChange = catalogoVm::cambiarConsulta,
                    label = { Text("Buscar por nombre o material") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("Todos") + (listOf("Interiores", "Matrimonios", "Festivales", "Otro") + estado.todos.map { it.categoria }).distinct()) { cat ->
                        FilterChip(
                            selected = estado.categoria == cat,
                            onClick = { catalogoVm.cambiarCategoria(cat) },
                            label = { Text(cat) }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = estado.soloDisponibles,
                        onCheckedChange = catalogoVm::cambiarDisponibles
                    )
                    Text("Solo disponibles")
                }
                FilterChip(
                    selected = estado.ordenarPorPrecio,
                    onClick = { catalogoVm.cambiarOrdenPrecio(!estado.ordenarPorPrecio) },
                    label = { Text("Ordenar: precio menor primero") }
                )
                if (!puedeGuardar) {
                    Text(
                        "Favoritos pendientes de vincular con el usuario activo. " +
                                "El encargado de integración debe pasar usuarioId desde AppNavigation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF985B42)
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (estado.cargando) CircularProgressIndicator()
                estado.error?.let { Text(it, color = MaterialTheme.colorScheme.error); TextButton(onClick = catalogoVm::cargar) { Text("Reintentar") } }
                Text("${estado.productos.size} diseños encontrados")
                Spacer(Modifier.height(10.dp))
                if (estado.productos.isEmpty()) {
                    Text("No encontramos productos con esos filtros.")
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(estado.productos, key = { it.id }) { producto ->
                            ProductoCard(
                                producto = producto,
                                esFavorito = producto.id in favoritos,
                                permitirFavorito = puedeGuardar,
                                onVerDetalle = { detalleId = producto.id; vista = "detalle" },
                                onFavorito = { favoritosVm.alternar(usuarioId, producto.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
