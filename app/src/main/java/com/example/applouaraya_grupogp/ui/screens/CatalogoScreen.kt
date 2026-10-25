package com.example.applouaraya_grupogp.ui.screens

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.applouaraya_grupogp.data.AppDatabase
import com.example.applouaraya_grupogp.data.FavoritosDatabase
import com.example.applouaraya_grupogp.repository.ProductosRepository
import com.example.applouaraya_grupogp.repository.FavoritosRepository
import com.example.applouaraya_grupogp.ui.adaptive.*
import com.example.applouaraya_grupogp.ui.components.ProductoCard
import com.example.applouaraya_grupogp.ui.design.*
import com.example.applouaraya_grupogp.viewmodel.CatalogoUiState
import com.example.applouaraya_grupogp.viewmodel.CatalogoViewModel
import com.example.applouaraya_grupogp.viewmodel.FavoritosViewModel

@Composable
fun CatalogoScreen(onVolver: () -> Unit, usuarioId: Long? = null, abrirFavoritos: Boolean = false,
                   repositorio: ProductosRepository? = null, productoInicial: Int? = null,
                   repositorioFavoritos: FavoritosRepository? = null) {
    val contexto = LocalContext.current.applicationContext
    val repo = remember(contexto, repositorio) { repositorio ?: ProductosRepository(AppDatabase.obtener(contexto)) }
    val factory = remember(repo) { viewModelFactory { initializer { CatalogoViewModel(repo) } } }
    val catalogoVm: CatalogoViewModel = viewModel(factory = factory)
    val favoritosFactory = remember(contexto, repositorioFavoritos) { viewModelFactory {
        initializer { FavoritosViewModel(contexto as Application,
            repositorioFavoritos ?: FavoritosRepository(FavoritosDatabase.obtener(contexto).favoritoDao())) }
    } }
    val favoritosVm: FavoritosViewModel = viewModel(factory = favoritosFactory)
    val estado by catalogoVm.estado.collectAsState()
    val favoritos by favoritosVm.ids.collectAsState()
    val puedeGuardar = usuarioId != null && usuarioId > 0L
    LaunchedEffect(usuarioId) { favoritosVm.seleccionarUsuario(usuarioId) }
    var vista by rememberSaveable { mutableStateOf(if (productoInicial != null) "detalle" else if (abrirFavoritos) "favoritos" else "catalogo") }
    var detalleId by rememberSaveable { mutableIntStateOf(productoInicial ?: -1) }
    BackHandler(enabled = vista != "catalogo") { vista = "catalogo" }
    when (vista) {
        "detalle" -> {
            val producto = estado.todos.firstOrNull { it.id == detalleId }
            if (producto == null) {
                if (!estado.cargando) LaunchedEffect(detalleId) { vista = "catalogo" }
            } else {
                DetalleProductoScreen(producto, producto.id in favoritos, puedeGuardar,
                    onVolver = { vista = "catalogo" },
                    onFavorito = { favoritosVm.alternar(usuarioId, producto.id) })
            }
        }
        "favoritos" -> FavoritosScreen(estado.todos, favoritos,
            onVolver = { vista = "catalogo" },
            onAbrir = { detalleId = it; vista = "detalle" },
            onQuitar = { favoritosVm.alternar(usuarioId, it) })
        else -> CatalogoContenido(estado, favoritos, puedeGuardar, onVolver,
            { vista = "favoritos" }, catalogoVm::cambiarConsulta,
            catalogoVm::cambiarCategoria, catalogoVm::cambiarDisponibles,
            catalogoVm::cambiarOrdenPrecio, catalogoVm::cargar,
            { detalleId = it; vista = "detalle" },
            { favoritosVm.alternar(usuarioId, it) })
    }
}

@Composable
fun CatalogoContenido(estado: CatalogoUiState, favoritos: Set<Int>, puedeGuardar: Boolean,
                      onVolver: () -> Unit, onFavoritos: () -> Unit, onConsulta: (String) -> Unit,
                      onCategoria: (String) -> Unit, onDisponibles: (Boolean) -> Unit, onOrden: (Boolean) -> Unit,
                      onReintentar: () -> Unit, onDetalle: (Int) -> Unit, onFavorito: (Int) -> Unit) {
    AdaptiveSurface {
        LazyVerticalGrid(columns = GridCells.Fixed(columnasPara(LocalLouWidth.current)),
            modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 28.dp)) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        LouBackButton(onVolver, "← Inicio")
                        TextButton(onClick = onFavoritos, enabled = puedeGuardar) {
                            Text("♥ Favoritos (${favoritos.size})", color = LouColors.Terracota)
                        }
                    }
                    LouEyebrow("DISEÑOS ARTESANALES")
                    LouPageHeading("Catálogo de macramé", "Encuentra tu próxima pieza favorita")
                    OutlinedTextField(value = estado.consulta, onValueChange = onConsulta,
                        label = { Text("Buscar por nombre o material") },
                        leadingIcon = { Text("⌕") }, singleLine = true,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = LouColors.Superficie,
                            unfocusedContainerColor = LouColors.Superficie))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items((listOf("Todos", "Interiores", "Matrimonios", "Festivales", "Otro") +
                                estado.todos.map { it.categoria }).distinct()) { cat ->
                            FilterChip(selected = estado.categoria == cat, onClick = { onCategoria(cat) },
                                label = { Text(cat) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LouColors.Arena,
                                    selectedLabelColor = LouColors.Cafe))
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = estado.soloDisponibles, onCheckedChange = onDisponibles)
                            Text("Solo disponibles", style = MaterialTheme.typography.bodySmall)
                        }
                        FilterChip(selected = estado.ordenarPorPrecio,
                            onClick = { onOrden(!estado.ordenarPorPrecio) },
                            label = { Text("Precio ↑") })
                    }
                    if (estado.cargando) LinearProgressIndicator(Modifier.fillMaxWidth())
                    estado.error?.let { mensaje ->
                        Text(mensaje, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onReintentar) { Text("Reintentar") }
                    }
                    LouSectionTitle("${estado.productos.size} diseños encontrados")
                    if (estado.productos.isEmpty()) {
                        LouPanel(Modifier.fillMaxWidth()) {
                            Text("No encontramos productos con esos filtros.",
                                color = LouColors.TextoSecundario)
                        }
                    }
                }
            }
            items(estado.productos, key = { it.id }) { producto ->
                ProductoCard(producto, producto.id in favoritos, puedeGuardar,
                    onVerDetalle = { onDetalle(producto.id) },
                    onFavorito = { onFavorito(producto.id) })
            }
        }
    }
}
